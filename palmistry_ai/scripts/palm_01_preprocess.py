"""
Stage 1: Hand Rectification — MediaPipe + Perspective Warp
Converts any angled palm photo → canonical 512×512 top-down view.

Run on VM:
    python3 01_preprocess.py --input ~/books/palmistry_ai/data/raw/my_palms \
                              --output ~/books/palmistry_ai/data/processed
"""

import cv2
import mediapipe as mp
import numpy as np
import os
import argparse
from pathlib import Path

mp_hands = mp.solutions.hands

# MediaPipe 21 landmarks used for warping:
# 0=WRIST, 5=INDEX_MCP, 9=MIDDLE_MCP, 13=RING_MCP, 17=PINKY_MCP
WARP_LANDMARKS = [0, 5, 9, 17]

# Canonical destination points (normalized for 512×512 output)
# Wrist → bottom center, Index MCP → top-left area, Middle MCP → top-center, Pinky MCP → top-right
CANONICAL_DST = np.float32([
    [256, 460],   # 0: WRIST → bottom center
    [100,  70],   # 5: INDEX_MCP → top-left
    [256,  60],   # 9: MIDDLE_MCP → top-center
    [412,  70],   # 17: PINKY_MCP → top-right
])


def rectify_palm(image: np.ndarray, hands, output_size: int = 512) -> tuple:
    """
    Detect hand landmarks with MediaPipe.
    Warp palm to canonical top-down view.

    Returns:
        (warped_image, transform_matrix, success)
        If MediaPipe fails, returns (center_crop, None, False)
    """
    h, w = image.shape[:2]
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    result = hands.process(rgb)

    if not result.multi_hand_landmarks:
        # Fallback: simple center crop
        warped = _center_crop(image, output_size)
        return warped, None, False

    lm = result.multi_hand_landmarks[0].landmark

    # Source points from detected landmarks
    src_pts = np.float32([
        [lm[idx].x * w, lm[idx].y * h]
        for idx in WARP_LANDMARKS
    ])

    # Scale canonical pts to output size
    scale = output_size / 512
    dst_pts = CANONICAL_DST * scale

    # Perspective transform (4 points → exact)
    M = cv2.getPerspectiveTransform(src_pts, dst_pts)
    warped = cv2.warpPerspective(image, M, (output_size, output_size))

    return warped, M, True


def _center_crop(image: np.ndarray, size: int) -> np.ndarray:
    h, w = image.shape[:2]
    s = min(h, w)
    y = (h - s) // 2
    x = (w - s) // 2
    return cv2.resize(image[y:y+s, x:x+s], (size, size))


def process_folder(input_dir: str, output_dir: str, size: int = 512):
    """Process all images in a folder."""
    input_path  = Path(input_dir)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    exts = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
    images = [f for f in input_path.iterdir() if f.suffix.lower() in exts]

    success_count = 0
    fallback_count = 0
    total = len(images)

    # Initialize MediaPipe ONCE for all images (not per-image)
    with mp_hands.Hands(
        static_image_mode=True,
        max_num_hands=1,
        min_detection_confidence=0.4,
        min_tracking_confidence=0.4,
    ) as hands:
        for i, img_file in enumerate(images, 1):
            img = cv2.imread(str(img_file))
            if img is None:
                print(f"  ✗ Cannot read: {img_file.name}")
                continue

            warped, _, ok = rectify_palm(img, hands, size)
            out_file = output_path / img_file.name
            cv2.imwrite(str(out_file), warped)

            if ok:
                success_count += 1
                if i % 100 == 0:
                    print(f"  [{i}/{total}] ✓ {img_file.name} — MediaPipe warp")
            else:
                fallback_count += 1
                if i % 100 == 0:
                    print(f"  [{i}/{total}] ⚠ {img_file.name} — Fallback crop")

    print(f"\nDone: {success_count} warped, {fallback_count} fallback, out of {total}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Palm rectification preprocessing")
    parser.add_argument("--input",  required=True, help="Input folder with raw palm images")
    parser.add_argument("--output", required=True, help="Output folder for 512×512 warped images")
    parser.add_argument("--size",   type=int, default=512, help="Output size (default: 512)")
    args = parser.parse_args()

    process_folder(args.input, args.output, args.size)
