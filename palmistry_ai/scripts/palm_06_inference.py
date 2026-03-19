"""
Stage 6: Full Inference Pipeline
Input:  Raw palm image (any angle/size)
Output: Complete reading dict (ready for FastAPI response)

Run on VM:
    python3 06_inference.py --image /path/to/palm.jpg --model ~/books/palmistry_ai/models/best_model.pth
"""

import cv2
import numpy as np
import torch
import json
import argparse
import time
from pathlib import Path

import segmentation_models_pytorch as smp

from palm_01_preprocess    import rectify_palm
from palm_04_features import extract_all_features
from palm_05_interpret     import interpret_palm, build_qwen_prompt
from palm_02_dataset       import NUM_CLASSES, CLASS_COLORS

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# Line colors for visualization (BGR)
CLASS_COLORS_VIZ = {
    0: (0,   0,   0),
    1: (10, 101, 232),   # Heart — saffron
    2: (10, 134, 200),   # Head — gold
    3: (217, 40, 109),   # Life — purple
    4: (10, 134, 200),   # Fate — gold
    5: (10, 101, 232),   # Sun — saffron
    6: (170, 139, 122),  # Mercury — steel
    7: (217, 40, 109),   # Marriage — purple
}


def load_model(model_path: str) -> torch.nn.Module:
    """Load trained U-Net model."""
    checkpoint = torch.load(model_path, map_location=DEVICE, weights_only=False)
    encoder    = checkpoint.get("encoder", "resnet34")
    n_classes  = checkpoint.get("num_classes", NUM_CLASSES)

    model = smp.Unet(
        encoder_name=encoder,
        encoder_weights=None,
        in_channels=3,
        classes=n_classes,
        activation=None,
    )
    model.load_state_dict(checkpoint["model_state"])
    model.to(DEVICE)
    model.eval()
    return model


def preprocess_for_model(image: np.ndarray) -> torch.Tensor:
    """Normalize and convert to tensor for model input."""
    img = cv2.cvtColor(image, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
    mean = np.array([0.485, 0.456, 0.406])
    std  = np.array([0.229, 0.224, 0.225])
    img  = (img - mean) / std
    tensor = torch.from_numpy(img.transpose(2, 0, 1)).float().unsqueeze(0)
    return tensor.to(DEVICE)


@torch.no_grad()
def predict_mask(model: torch.nn.Module, image: np.ndarray) -> tuple:
    """
    Run segmentation model on a 512×512 palm image.
    Returns: (class_mask, confidence_map)
    """
    tensor = preprocess_for_model(image)
    logits = model(tensor)                      # (1, C, H, W)
    probs  = torch.softmax(logits, dim=1)       # (1, C, H, W)

    class_mask      = probs.argmax(dim=1)[0]    # (H, W)
    confidence_map  = probs.max(dim=1)[0][0]    # (H, W) — max prob per pixel

    return class_mask.cpu().numpy(), confidence_map.cpu().numpy()


def create_overlay(original: np.ndarray, mask: np.ndarray, alpha: float = 0.5) -> np.ndarray:
    """Draw colored line overlay on the palm image."""
    overlay = original.copy()
    for class_id, color in CLASS_COLORS_VIZ.items():
        if class_id == 0:
            continue
        region = (mask == class_id)
        overlay[region] = color
    return cv2.addWeighted(original, 1 - alpha, overlay, alpha, 0)


def detect_hand_type(image: np.ndarray, landmarks=None) -> str:
    """
    Classify hand as Earth/Air/Water/Fire from palm proportions.
    Uses aspect ratio and finger length estimation.
    """
    h, w = image.shape[:2]
    # Simple heuristic: palm width vs height ratio
    # In a canonical 512×512 warped image:
    # Fingers end around y=100, wrist at y=460
    # Palm proper: y=200 to y=430 (width at y=300)

    palm_region = image[200:430, 80:430]
    ph, pw = palm_region.shape[:2]
    aspect = pw / (ph + 1e-6)

    # Estimate finger length from top of image
    finger_region = image[60:200, 80:430]
    finger_h = 200 - 60  # fixed
    palm_h   = 430 - 200

    finger_ratio = finger_h / (palm_h + 1e-6)

    # Classification:
    # Earth: square palm (aspect ~1.0), short fingers (finger_ratio < 0.6)
    # Air:   square palm (aspect ~1.0), long fingers (finger_ratio > 0.6)
    # Water: rectangular palm (aspect < 0.85), long fingers
    # Fire:  rectangular palm (aspect < 0.85), short fingers

    if aspect >= 0.90:
        return "air" if finger_ratio >= 0.65 else "earth"
    else:
        return "water" if finger_ratio >= 0.65 else "fire"


def run_full_pipeline(image_path: str, model: torch.nn.Module) -> dict:
    """
    Complete pipeline from raw image → Vedic reading.
    """
    t_start = time.time()

    # 1. Load image
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Cannot read image: {image_path}")
    original_size = img.shape[:2]

    # 2. Rectify (MediaPipe warp)
    warped, transform_M, mediapipe_ok = rectify_palm(img, output_size=512)
    t_preprocess = time.time()

    # 3. Segment
    mask, confidence_map = predict_mask(model, warped)
    t_segment = time.time()

    # 4. Extract features
    features = extract_all_features(mask, palm_width_px=512)
    t_features = time.time()

    # 5. Detect hand type
    hand_type = detect_hand_type(warped)

    # 6. Interpret (rules engine)
    result = interpret_palm(features, hand_type)
    t_interpret = time.time()

    # 7. Build Qwen prompt (LLM narrative generated separately)
    qwen_prompt = build_qwen_prompt(result, features)

    # 8. Create overlay image (base64 for API)
    import base64
    overlay = create_overlay(warped, mask, alpha=0.45)
    _, buf = cv2.imencode(".jpg", overlay, [cv2.IMWRITE_JPEG_QUALITY, 85])
    overlay_b64 = base64.b64encode(buf).decode("utf-8")

    timing = {
        "preprocess_ms": round((t_preprocess - t_start) * 1000, 1),
        "segment_ms":    round((t_segment - t_preprocess) * 1000, 1),
        "features_ms":   round((t_features - t_segment) * 1000, 1),
        "interpret_ms":  round((t_interpret - t_features) * 1000, 1),
        "total_ms":      round((t_interpret - t_start) * 1000, 1),
    }

    return {
        "hand_type":         result["hand_type"],
        "interpretations":   result["interpretations"],
        "life_scores":       result["life_scores"],
        "strengths":         result["strengths"],
        "challenges":        result["challenges"],
        "raw_features":      {k: v for k, v in features.items() if k != "derived"},
        "mediapipe_success": mediapipe_ok,
        "confidence_mean":   round(float(confidence_map.mean()), 3),
        "overlay_b64":       overlay_b64,
        "qwen_prompt":       qwen_prompt,
        "timing":            timing,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True, help="Input palm image path")
    parser.add_argument("--model", required=True, help="Trained model .pth path")
    parser.add_argument("--no-overlay", action="store_true", help="Skip overlay generation")
    args = parser.parse_args()

    print(f"Loading model from {args.model}...")
    model = load_model(args.model)
    print(f"Model loaded on {DEVICE}")

    print(f"Processing {args.image}...")
    result = run_full_pipeline(args.image, model)

    # Print result (minus base64 image)
    display = {k: v for k, v in result.items() if k not in ("overlay_b64", "qwen_prompt")}
    print(json.dumps(display, indent=2))
    print(f"\nTotal time: {result['timing']['total_ms']}ms")
    print(f"\nQwen Prompt preview:\n{result['qwen_prompt'][:300]}...")
