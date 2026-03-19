# Hasta Samudrika AI — Advanced Palm Reading System
## Engineering Roadmap for Brahm AI

> **Vision**: Build the world's most accurate AI-powered Vedic palmistry system. Users scan their palm → receive a detailed, consistent, computer-vision-grounded Vedic reading powered by Gemini LLM.

---

## Current Status (2026-03-19)

| Component | Status | Notes |
|---|---|---|
| VM folders | ✅ Done | `~/books/data/palmistry/`, `~/books/models/palmistry/` |
| All 6 scripts uploaded | ✅ Done | `palm_01` to `palm_06` in `~/books/scripts/` |
| mediapipe install | ✅ Done | v0.10.14 (solutions API works) |
| MediaPipe warp pipeline | ✅ Working | 4/4 images warped successfully |
| Vedic rules engine | ✅ Working | `palm_05_interpret.py` generates JSON + scores |
| Gemini Vision API | ✅ Working | `models/gemini-2.5-flash`, full Vedic reading in one call |
| FastAPI endpoint | ✅ Done | `api/routers/palmistry.py` — POST `/api/palmistry/analyze` |
| Frontend integration | ✅ Done | AI Reading button + `ai_report` step in PalmistryPage.tsx |
| Qwen on VM | ❌ Deleted | Replaced by Gemini API. GPU not used for LLM. |
| U-Net model | ⬜ Pending | Need annotated data first (Phase 2) |
| Annotation (Label Studio) | ⬜ Pending | Phase 2 |

## Architecture Decision: Gemini API replaces Qwen (2026-03-19)

**Decision**: Use Google Gemini API for ALL LLM narrative generation, including palmistry. Qwen model deleted from VM.

**Why**:
- GPU already full (14GB used by other services) → Qwen OOM
- Gemini Vision API reads the palm image directly → no need for U-Net in MVP
- Gemini gives richer, more accurate Vedic readings than local 7B model
- No GPU cost for LLM inference

**Active model**: `models/gemini-2.5-flash` (confirmed working via `client.models.list()`)
**SDK**: `google-genai` (NOT deprecated `google-generativeai`)
**API key env var**: `GEMINI_API_KEY`

```python
from google import genai
from google.genai import types
client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
response = client.models.generate_content(
    model="models/gemini-2.5-flash",
    contents=[types.Content(parts=[
        types.Part(inline_data=types.Blob(mime_type=mime, data=image_bytes)),
        types.Part(text=SYSTEM_PROMPT + "\n\n" + ANALYSIS_PROMPT),
    ])]
)
```

**GPU on VM**: Now reserved exclusively for U-Net training (future phase). Not for LLM.

---

## VM Folder Structure (fits ~/books/ convention)

```
~/books/
├── data/
│   └── palmistry/                      ← NEW (image data, separate from text/PDFs)
│       ├── raw/                        ← original palm photos (phone camera)
│       ├── processed/                  ← MediaPipe-warped 512×512 images
│       ├── annotated/
│       │   ├── images/                 ← warped images ready for annotation
│       │   └── masks/                  ← PNG class masks (pixel value = class id)
│       └── augmented/                  ← albumentations output (auto-generated)
│
├── models/                             ← NEW (alongside indexes/)
│   └── palmistry/
│       ├── best_model.pth              ← trained U-Net checkpoint
│       └── history.json                ← loss/mIoU per epoch
│
├── scripts/                            ← EXISTING — palm_ prefix scripts added here
│   ├── 01_ingest_books.py              (existing RAG pipeline)
│   ├── palm_01_preprocess.py           ← MediaPipe warp
│   ├── palm_02_dataset.py              ← PyTorch Dataset class
│   ├── palm_03_train.py                ← U-Net training
│   ├── palm_04_features.py             ← geometric feature extraction
│   ├── palm_05_interpret.py            ← Vedic rules engine
│   └── palm_06_inference.py            ← full pipeline test
│
├── src/
│   └── special/                        ← EXISTING domain modules
│       ├── kundali.py                  (existing)
│       ├── panchang_service.py         (existing)
│       └── palmistry_service.py        ← NEW: inference class for API
│
└── api/
    ├── routers/
    │   ├── compatibility.py            (existing)
    │   └── palmistry.py                ← NEW: FastAPI router
    └── services/
        ├── kundali_service.py          (existing)
        └── palmistry_service.py        ← NEW: wraps inference pipeline
```

### VM One-Time Setup Commands
```bash
source ~/ai-env/bin/activate
mkdir -p ~/books/data/palmistry/{raw,processed,annotated/images,annotated/masks,augmented}
mkdir -p ~/books/models/palmistry
```

### Local PC Mirror (c:\desktop\Brahm AI\)
```
palmistry_ai\
├── scripts\
│   ├── palm_01_preprocess.py
│   ├── palm_02_dataset.py
│   ├── palm_03_train.py
│   ├── palm_04_features.py
│   ├── palm_05_interpret.py
│   └── palm_06_inference.py
└── api\
    ├── routers\palmistry.py
    └── services\palmistry_service.py
```

---

## Verified Research Findings (Fetched Live)

| Item | Verified Fact |
|---|---|
| Kaggle 11k Hands | 11,076 images, 1600×1200px, 190 subjects ages 18–75, both dorsal+palmar sides. **No palm line labels.** ODbL license. 664MB. |
| Roboflow PalmLinesDetection | Exists — accessible via Roboflow Universe. Small (~46–200 images). Life/Head/Heart masks. |
| lakshay102/Palm-Astro | U-Net + **ResNet18** encoder. **3 lines only** (Life/Head/Heart, 4-class with background). Input 512×512. Uses dummy data generator — **no real training dataset**. Gradio UI. Inference <1s CPU. |
| yeonsumia/palmistry | MediaPipe landmarks → perspective warp → U-Net. 4 sample test images provided. Dataset private/unspecified. |
| HuggingFace "palm line" | **0 datasets found.** No public labeled palmistry dataset exists anywhere. |
| segmentation-models-pytorch | Supports 40+ encoders: ResNet, EfficientNet, MobileNet, VGG, DenseNet, Mix Vision Transformer (mit_b0–b5), MobileOne. All ImageNet pre-trained. |
| Our advantage over Palm-Astro | They detect 3 lines with no real training data. We will detect 7 lines with real annotated data + Vedic interpretation depth. |

---

## What We Are Building (Big Picture)

```
Phone Camera / Upload
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  STAGE 1: Hand Rectification (MediaPipe)                        │
│  Normalize any angle/tilt → canonical 512×512 palm view         │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  STAGE 2: Line Segmentation (U-Net / SAM2)                      │
│  Pixel-accurate mask: Heart / Head / Life / Fate / Sun lines    │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  STAGE 3: Feature Extraction                                    │
│  Length, curvature, breaks, forks, angles, mount zones          │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  STAGE 4: Interpretation Engine (Gemini Vision API)             │
│  Palm image + Vedic prompt → Gemini → structured JSON reading   │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  STAGE 5: React Frontend (Brahm AI)                             │
│  Visual overlay + life scores + full Vedic report               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Our Key Advantages (vs. competitors)

| Advantage | What it means |
|---|---|
| **NVIDIA L4 24GB GPU** on our VM | Can train U-Net full model locally — no cloud GPU cost |
| **Qwen 2.5-7B already loaded** | No external LLM API needed for narrative generation |
| **Existing FastAPI + React** | Zero new infra — just add routes |
| **Vedic depth** | Our interpretation layer goes deeper than any commercial app |
| **No API cost per reading** | Once model is trained, readings are free at scale |

---

## Phase 0 — Environment & Research Foundation
**Duration: 3–5 days**

### 0.1 Install Dependencies (on VM)

```bash
# Activate existing venv
source ~/ai-env/bin/activate

# Core CV stack
pip install mediapipe opencv-python-headless scikit-image pillow

# Deep learning
pip install torch torchvision  # already installed
pip install segmentation-models-pytorch  # U-Net with pre-trained backbones

# Data/annotation tools
pip install albumentations  # image augmentation
pip install supervision     # annotation utilities

# Evaluation
pip install scikit-learn matplotlib seaborn

# Optional: SAM2 (Meta's Segment Anything v2)
# pip install git+https://github.com/facebookresearch/segment-anything-2.git
```

### 0.2 Fork / Clone Reference Repos

```bash
cd ~/books

# Reference 1: View-invariant warping pipeline
git clone https://github.com/yeonsumia/palmistry palmistry_ref1

# Reference 2: U-Net segmentation + Gradio UI
git clone https://github.com/lakshay102/Palm-Astro-Application palmistry_ref2

# Our project folder
mkdir -p ~/books/palmistry_ai/{data,models,scripts,api}
```

### 0.3 Study Deliverables
- [ ] Read both repos, understand their preprocessing and model code
- [ ] Run Palm-Astro-Application demo on one test image
- [ ] Document: what classes they detect, what data they used, what metrics they report

---

## Phase 1 — Data Collection & Annotation
**Duration: 2–4 weeks (most critical phase)**

### 1.1 Existing Public Datasets

#### Primary: Kaggle 11k Hands
- **URL**: https://www.kaggle.com/datasets/shyambhu/hands-and-palm-images-dataset
- **Size**: ~11,000 hand images (diverse ages, ethnicities, lighting)
- **Labeling**: NOT labeled for palm lines — raw images only
- **Use**: Pre-training backbone + unlabeled pool for pseudo-labeling
- **Download**:
  ```bash
  pip install kaggle
  kaggle datasets download -d shyambhu/hands-and-palm-images-dataset -p ~/books/palmistry_ai/data/raw
  ```

#### Secondary: Roboflow Palm Line Detection
- **URL**: https://roboflow.com/search?q=palm+line
- **Size**: ~46–100 pre-labeled images (Life, Head, Heart, Fate lines)
- **Format**: YOLO / COCO segmentation masks
- **Use**: Starting labeled set + validation reference
- **Action**: Create free Roboflow account → export in "Segmentation Mask" format

#### Tertiary: Palm-Astro Training Data
- **Source**: The lakshay102 repo includes their training dataset (check repo)
- **Format**: Masks already generated — review and reuse if license permits

#### HuggingFace Search
- Search: `https://huggingface.co/datasets?search=palm+line`
- Search: `https://huggingface.co/datasets?search=palmistry`
- Also check: hand landmark datasets, skin line detection

#### What Does NOT Exist (our gap to fill)
- No large-scale (1000+) pixel-labeled palmistry dataset exists publicly
- CASIA Palmprint DB = fingerprint-style palmprints, NOT palmistry lines
- This is our moat — building and owning this dataset is a competitive advantage

### 1.2 Our Data Collection Strategy

**Target: 500 annotated images minimum (200 for MVP)**

#### Collection Protocol
```
Subjects: Diverse (age 18-70, male/female, all skin tones)
Hands: Both left and right (label which)
Lighting: Natural daylight, indoor white light, indoor warm light
Camera: Phone cameras (multiple models — our users will use these)
Format: JPEG, minimum 1080×1080px
Background: Plain white or light-colored surface
Pose: Flat on surface, fingers slightly spread, NO extreme angles
```

#### Annotation Tool: Label Studio (Free, Self-hosted)
```bash
pip install label-studio
label-studio start --port 8081
# Creates web annotation UI at localhost:8081
```

**Classes to annotate (8 classes)**:
```
0: Background
1: Heart Line (Hridaya Rekha)
2: Head Line (Mastishka Rekha)
3: Life Line (Jeevan Rekha)
4: Fate Line (Bhagya Rekha)
5: Sun Line (Surya Rekha)
6: Mercury Line (Budha Rekha)
7: Marriage Lines (Vivah Rekha) [optional — very fine lines]
```

#### Annotation Specification (CRITICAL — must be followed exactly)

```
HEART LINE:
  Start: Point where line emerges from below Little finger, within 20px of palm edge
  End: Wherever line terminates (index finger base, middle finger base, or palm edge)
  Include: All visible forks if branch > 5% of palm width
  Exclude: Fine capillary lines < 1px wide at 512×512 resolution

HEAD LINE:
  Start: Point between index finger and thumb (often connects with Life line start)
  End: Wherever line terminates toward outer palm edge
  Include: Simian crease (when Head+Heart are fused, label as Head line)
  Bifurcations: Label longest branch as main; secondary if > 8% palm width

LIFE LINE:
  Start: Between index finger and thumb (often same area as Head line start)
  End: Near wrist at base of palm, following the arc around Venus mount
  Islands: Label within the line mask (they appear as white gaps in the mask)
  Double life line: Annotate both as Life Line class

FATE LINE:
  Start: Can emerge from Life line, wrist base, or Mount of Moon (note which)
  End: Toward middle finger base
  Absent: Simply do not draw — never force a label where no line exists

SUN LINE:
  Start: Various origins (Mount of Moon, Head line, Heart line)
  End: Below Ring finger
  Often faint — only label if clearly visible at normal contrast

MERCURY LINE:
  Diagonal, from lower palm toward little finger
  Often absent — do not force label

FORKS, BRANCHES, BREAKS:
  Fork: label all branches meeting the length threshold
  Break: label as two separate segments of the same class
  Island: Leave as background within the line mask (creates "hole" in polygon)
```

#### Inter-Annotator Agreement Protocol
- Every image annotated by minimum 2 annotators
- Compute **Intersection over Union (IoU)** between their masks per class
- Accept image if mean IoU ≥ 0.65 across annotators
- Flag and re-annotate if IoU < 0.65
- Final dataset: use majority-vote masks (pixel voted "line" by ≥50% annotators)

### 1.3 Pseudo-Labeling with SAM2 (Our Key Advantage — Speeds Up Annotation 10×)

Meta's SAM2 can segment anything with a point prompt. We use it to semi-automate annotation:

```python
# Script: scripts/pseudo_label_sam2.py
import torch
from sam2.build_sam import build_sam2
from sam2.sam2_image_predictor import SAM2ImagePredictor

# Load SAM2
predictor = SAM2ImagePredictor.from_pretrained("facebook/sam2-hiera-large")

# For each unlabeled hand image:
# 1. Run MediaPipe to get landmarks
# 2. Use landmark positions as point prompts for SAM2
#    e.g., "click" on known approximate location of heart line
# 3. SAM2 generates pixel mask automatically
# 4. Human verifies/corrects in Label Studio (5 min vs 30 min from scratch)
```

**This reduces annotation time from ~30 min/image to ~5 min/image.**

### 1.4 Synthetic Data Generation (Our Secret Weapon)

When real annotated data is scarce, generate synthetic palm images:

```python
# Approach: Procedurally generate palm line drawings on skin-textured backgrounds
# Tools: Blender (3D hand models) OR pure numpy/cv2 procedural generation

import cv2
import numpy as np

def generate_synthetic_palm(size=512):
    """Generate a synthetic palm image with ground-truth line masks."""
    # Start with skin-colored base
    palm = np.zeros((size, size, 3), dtype=np.uint8)
    # ... (Bézier curves for lines, skin texture, lighting variation)
    # Returns: (palm_image, {class_name: binary_mask})
```

Benefits:
- Unlimited data, free
- Perfect ground truth (we know exactly where lines are)
- Can simulate rare cases: broken lines, crossed lines, scars, tattoos
- Different skin tones by just changing HSV values

---

## Phase 2 — Model Training: Line Segmentation
**Duration: 2–3 weeks**

### 2.1 Architecture Choices

#### Option A: U-Net with ResNet34 backbone (Recommended for MVP)
```python
# Using segmentation-models-pytorch (VERIFIED: this is exactly what Palm-Astro uses but with ResNet18)
# We upgrade to ResNet34 for better accuracy at similar speed
import segmentation_models_pytorch as smp

model = smp.Unet(
    encoder_name="resnet34",           # ImageNet pre-trained (Palm-Astro uses resnet18 — we go bigger)
    encoder_weights="imagenet",
    in_channels=3,                     # RGB
    classes=8,                         # Background + 7 line classes (Palm-Astro only does 4)
    activation=None,                   # Raw logits (use CrossEntropy)
)
# Parameters: ~24M — fits easily in L4 24GB
# Training time: ~2-4 hours on our L4 GPU for 50 epochs
# Input: 512×512 (same as Palm-Astro)
# Output: 8-class mask (we detect 7 lines vs their 3)
```

#### Option B: SegFormer-B2 (Better quality, slightly slower)
```python
from transformers import SegformerForSemanticSegmentation, SegformerConfig

model = SegformerForSemanticSegmentation.from_pretrained(
    "nvidia/mit-b2",
    num_labels=8,
    id2label={0:"bg",1:"heart",2:"head",3:"life",4:"fate",5:"sun",6:"mercury",7:"marriage"},
    label2id={v:k for k,v in id2label.items()},
    ignore_mismatched_sizes=True,
)
# HuggingFace: https://huggingface.co/nvidia/mit-b2
# Better at fine structures (thin lines) than ResNet U-Net
```

#### Option C: SAM2 Fine-tuned (Best quality, most complex)
- Fine-tune Meta SAM2 on our annotated dataset
- Automatically handles multi-scale features
- Can be prompted at inference (point → segment the line)
- HuggingFace: `facebook/sam2-hiera-large`

**Our recommendation**: Start with Option A (U-Net ResNet34), then try SegFormer-B2. Use SAM2 for pseudo-labeling only in MVP.

### 2.2 Preprocessing Pipeline

```python
# scripts/preprocess.py

import cv2
import mediapipe as mp
import numpy as np

mp_hands = mp.solutions.hands

def rectify_palm(image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """
    Detect hand landmarks with MediaPipe.
    Warp palm to canonical 512×512 top-down view.
    Returns: (warped_image, transform_matrix) for mask warping.
    """
    with mp_hands.Hands(static_image_mode=True, max_num_hands=1) as hands:
        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        result = hands.process(rgb)

        if not result.multi_hand_landmarks:
            # FALLBACK: simple center crop if MediaPipe fails
            return center_crop_fallback(image), None

        lm = result.multi_hand_landmarks[0].landmark
        h, w = image.shape[:2]

        # Key landmarks for wrist-to-finger-base transform:
        # 0=WRIST, 5=INDEX_MCP, 9=MIDDLE_MCP, 13=RING_MCP, 17=PINKY_MCP
        src_pts = np.float32([
            [lm[0].x * w,  lm[0].y * h],   # wrist
            [lm[5].x * w,  lm[5].y * h],   # index base
            [lm[17].x * w, lm[17].y * h],  # pinky base
        ])

        # Canonical destination points
        dst_pts = np.float32([
            [256, 450],  # wrist bottom center
            [100, 80],   # index base top-left
            [412, 80],   # pinky base top-right
        ])

        M = cv2.getAffineTransform(src_pts, dst_pts)
        warped = cv2.warpAffine(image, M, (512, 512))
        return warped, M

def center_crop_fallback(image: np.ndarray) -> np.ndarray:
    """Simple center crop when MediaPipe fails."""
    h, w = image.shape[:2]
    s = min(h, w)
    y = (h - s) // 2
    x = (w - s) // 2
    return cv2.resize(image[y:y+s, x:x+s], (512, 512))
```

### 2.3 Training Script

```python
# scripts/train_segmentation.py

import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import segmentation_models_pytorch as smp
from segmentation_models_pytorch.losses import DiceLoss, SoftCrossEntropyLoss
import albumentations as A
from albumentations.pytorch import ToTensorV2

DEVICE = "cuda"  # L4 GPU
NUM_CLASSES = 8
EPOCHS = 60
BATCH_SIZE = 8   # fits in L4 24GB with ResNet34 U-Net

# ── Augmentation ──
train_transform = A.Compose([
    A.RandomRotate90(p=0.3),
    A.HorizontalFlip(p=0.5),
    A.RandomBrightnessContrast(brightness_limit=0.3, contrast_limit=0.3, p=0.6),
    A.HueSaturationValue(hue_shift_limit=10, sat_shift_limit=30, val_shift_limit=20, p=0.5),
    A.GaussNoise(var_limit=(10, 50), p=0.3),
    A.ElasticTransform(alpha=30, sigma=5, p=0.2),  # simulate skin deformation
    A.ShiftScaleRotate(shift_limit=0.05, scale_limit=0.1, rotate_limit=15, p=0.4),
    A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
    ToTensorV2(),
])

# ── Model ──
model = smp.Unet(
    encoder_name="resnet34",
    encoder_weights="imagenet",
    in_channels=3,
    classes=NUM_CLASSES,
    activation=None,
).to(DEVICE)

# ── Loss: Combine Dice + Cross Entropy for thin lines ──
dice_loss = DiceLoss(mode="multiclass", from_logits=True)
ce_loss = SoftCrossEntropyLoss(smooth_factor=0.1)

def combined_loss(pred, target):
    return 0.5 * dice_loss(pred, target) + 0.5 * ce_loss(pred, target)

# ── Optimizer + Scheduler ──
optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4, weight_decay=1e-4)
scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=EPOCHS)

# ── Training loop ──
# ... (standard PyTorch loop)
# Save: torch.save(model.state_dict(), "~/books/palmistry_ai/models/unet_resnet34.pth")
```

### 2.4 Evaluation Metrics

```python
# Per-class mIoU (primary metric)
# Boundary-F1 (important for thin lines — regular mIoU misses thin line quality)
# Length % error (compare detected line length to GT length)
# Confidence calibration (Expected Calibration Error)

TARGET_METRICS = {
    "miou_heart":    0.72,   # ≥ 0.72 per class
    "miou_head":     0.70,
    "miou_life":     0.75,   # Life line is thicker → easier
    "miou_fate":     0.65,   # Often faint
    "boundary_f1":   0.68,   # Thin line boundary quality
    "length_err_pct": 8.0,   # ≤ 8% length measurement error
}
```

### 2.5 Model Export for Production

```python
# Export to ONNX for browser or mobile deployment
torch.onnx.export(
    model,
    dummy_input,
    "palmistry_unet.onnx",
    opset_version=17,
    input_names=["image"],
    output_names=["masks"],
    dynamic_axes={"image": {0: "batch"}, "masks": {0: "batch"}},
)

# For browser: onnxruntime-web can run this in WebAssembly!
# This means: palm analysis runs IN THE BROWSER — image never leaves device
```

---

## Phase 3 — Feature Extraction Engine
**Duration: 1 week**

```python
# scripts/extract_features.py

import numpy as np
import cv2
from skimage.morphology import skeletonize
from skimage.measure import label, regionprops
from scipy.spatial.distance import cdist

def extract_line_features(mask: np.ndarray, line_class: int, palm_width_px: int) -> dict:
    """
    Extract geometric features from a segmentation mask.
    All lengths expressed as % of palm width for scale invariance.
    """
    binary = (mask == line_class).astype(np.uint8)
    if binary.sum() < 50:  # line not present or too faint
        return {"present": False, "confidence": 0.0}

    # ── Skeletonize → thin 1-pixel line ──
    skeleton = skeletonize(binary)

    # ── Arc length (true length along the curve) ──
    coords = np.argwhere(skeleton)
    arc_length_px = len(coords)  # rough; proper: sum of step distances
    arc_length_pct = arc_length_px / palm_width_px * 100

    # ── Curvature / Tortuosity ──
    if len(coords) >= 2:
        chord_length = np.linalg.norm(coords[0] - coords[-1])
        tortuosity = arc_length_px / (chord_length + 1e-6)
    else:
        tortuosity = 1.0

    # ── Breaks detection ──
    labeled = label(skeleton)
    n_breaks = labeled.max() - 1  # connected components - 1

    # ── Fork detection ──
    # At each skeleton pixel, count 8-neighbors that are also skeleton
    kernel = np.ones((3, 3), np.uint8)
    neighbors = cv2.filter2D(skeleton.astype(np.float32), -1, kernel) * skeleton
    forks = int((neighbors >= 4).sum())  # pixels with 3+ branches

    # ── Orientation ──
    region = regionprops(binary)[0] if regionprops(binary) else None
    orientation_deg = np.degrees(region.orientation) if region else 0

    # ── Thickness ──
    # Mean distance from skeleton to boundary (gives half-width)
    from scipy.ndimage import distance_transform_edt
    dist_map = distance_transform_edt(binary)
    thickness_px = float(dist_map[skeleton.astype(bool)].mean()) * 2

    return {
        "present": True,
        "length_pct": round(arc_length_pct, 2),
        "tortuosity": round(tortuosity, 3),   # 1.0 = straight; >1.2 = curved
        "breaks": n_breaks,
        "forks": forks,
        "orientation_deg": round(orientation_deg, 1),
        "thickness_px": round(thickness_px, 2),
        "confidence": round(float(binary.sum()) / (palm_width_px * 10), 3),  # rough
    }

def extract_all_features(segmentation_mask: np.ndarray, palm_bbox: tuple) -> dict:
    """Extract features for all 7 line classes."""
    x, y, w, h = palm_bbox
    palm_width_px = w

    CLASS_NAMES = {1:"heart", 2:"head", 3:"life", 4:"fate", 5:"sun", 6:"mercury", 7:"marriage"}
    features = {}
    for cls_id, cls_name in CLASS_NAMES.items():
        features[cls_name] = extract_line_features(segmentation_mask, cls_id, palm_width_px)

    # ── Derived features (cross-line relationships) ──
    if features["heart"]["present"] and features["head"]["present"]:
        features["heart_longer_than_head"] = features["heart"]["length_pct"] > features["head"]["length_pct"]

    return features
```

---

## Phase 4 — Interpretation Engine (Rules + Qwen)
**Duration: 1 week**

### 4.1 Rule Engine (Traditional Samudrika → Features)

```python
# scripts/interpret.py

VEDIC_RULES = {
    "heart": [
        {
            "condition": lambda f: f["length_pct"] > 70 and f["tortuosity"] > 1.15,
            "type": "Long & Curved",
            "meaning": "Deeply romantic, emotionally expressive. Strong Shukra (Venus) influence.",
            "vedic_note": "Anahata Chakra is open and generous — capacity for unconditional love.",
            "life_scores": {"love": +3, "spiritual": +1},
        },
        {
            "condition": lambda f: f["length_pct"] > 70 and f["tortuosity"] <= 1.10,
            "type": "Long & Straight",
            "meaning": "Practical in love. Expresses affection through loyalty and actions.",
            "vedic_note": "Balanced Chandra — disciplined emotional nature.",
            "life_scores": {"love": +1, "career": +1},
        },
        {
            "condition": lambda f: f["forks"] >= 1,
            "type": "Forked",
            "meaning": "Balance of heart and mind. Natural counselor and empath.",
            "vedic_note": "Budha-Chandra yoga — rare gift of mind-heart harmony.",
            "life_scores": {"love": +2, "mental": +2},
        },
        {
            "condition": lambda f: f["breaks"] >= 1,
            "type": "Broken",
            "meaning": "Emotional transformation through deep experience.",
            "vedic_note": "Rahu/Ketu karmic lessons — growth through vulnerability.",
            "life_scores": {"spiritual": +2, "love": -1},
        },
    ],
    # ... (similar rules for head, life, fate, sun lines)
}

def apply_rules(features: dict) -> dict:
    """Match detected features to Vedic interpretation rules."""
    interpretations = {}
    life_scores = {"love": 5, "career": 5, "wealth": 5, "mental": 5, "health": 5, "spiritual": 5}

    for line_name, rules in VEDIC_RULES.items():
        f = features.get(line_name, {})
        if not f.get("present"):
            interpretations[line_name] = {"type": "Not clearly visible", "meaning": "...", "vedic_note": "..."}
            continue

        matched = None
        for rule in rules:
            try:
                if rule["condition"](f):
                    matched = rule
                    break
            except Exception:
                continue

        if matched:
            interpretations[line_name] = {
                "type": matched["type"],
                "meaning": matched["meaning"],
                "vedic_note": matched["vedic_note"],
                "raw_features": f,
            }
            for k, v in matched.get("life_scores", {}).items():
                life_scores[k] = min(10, max(1, life_scores[k] + v))

    return {"interpretations": interpretations, "life_scores": life_scores}
```

### 4.2 Gemini LLM Narrative (Google AI — Replaces Local Qwen)

```python
# api/services/palmistry_service.py

import json, os
from google import genai

_gemini_client = None

def _get_client():
    global _gemini_client
    if _gemini_client is None:
        _gemini_client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])
    return _gemini_client

GEMINI_MODEL = "gemini-2.0-flash-lite"   # update after running models.list()

PALMISTRY_PROMPT_TEMPLATE = """You are a master of Hasta Samudrika Shastra — the ancient Vedic science of palm reading.
Speak with warmth, wisdom, and spiritual depth. For entertainment and educational purposes only.

HAND TYPE: {hand_type}
PALM LINE ANALYSIS:
{line_summary}
LIFE AREA SCORES (1-10): {life_scores}

Write a 200-250 word personalized palm reading in the Vedic tradition. Include:
1. One opening statement about their overall karmic path
2. Specific insights for their 2 strongest lines
3. One growth area with a specific Vedic remedy (mantra, gemstone, or practice)
4. A closing Sanskrit shloka that resonates with their soul's journey

Tone: Wise, compassionate, specific — not generic. Speak directly (use "you"/"your").
End with: "This reading is based on Hasta Samudrika Shastra tradition — for educational and spiritual reflection."
"""

def generate_palm_narrative(features: dict, interpretations: dict, hand_type: str) -> str:
    """Use Gemini API to generate personalized Vedic palm narrative."""
    line_summary = "\n".join([
        f"- {line}: {data['type']} — {data['meaning']}"
        for line, data in interpretations.items()
        if data.get("type") != "Not clearly visible"
    ])
    prompt = PALMISTRY_PROMPT_TEMPLATE.format(
        hand_type=hand_type,
        line_summary=line_summary,
        life_scores=json.dumps(features.get("life_scores", {})),
    )
    client = _get_client()
    response = client.models.generate_content(model=GEMINI_MODEL, contents=prompt)
    return response.text
```

### 4.3 API Endpoint

```python
# api/routers/palmistry.py (updated)

from fastapi import APIRouter, UploadFile, File
import cv2, numpy as np

router = APIRouter()

@router.post("/palmistry/analyze")
async def analyze_palm(file: UploadFile = File(...)):
    """
    Upload palm image → returns full Vedic reading.
    All processing: our VM. Zero external API calls.
    """
    # 1. Read image
    img_bytes = await file.read()
    img = cv2.imdecode(np.frombuffer(img_bytes, np.uint8), cv2.IMREAD_COLOR)

    # 2. Rectify with MediaPipe
    from scripts.preprocess import rectify_palm
    warped, transform_matrix = rectify_palm(img)

    # 3. Segment with U-Net
    from scripts.segment import run_segmentation
    mask, confidence_map = run_segmentation(warped)  # returns (512,512) class mask

    # 4. Extract features
    from scripts.extract_features import extract_all_features
    features = extract_all_features(mask, palm_bbox=(0, 0, 512, 512))

    # 5. Apply Vedic rules
    from scripts.interpret import apply_rules, detect_hand_type
    rule_output = apply_rules(features)
    hand_type = detect_hand_type(warped)  # Earth/Air/Water/Fire from palm proportions

    # 6. Qwen narrative
    from api.services.palmistry_service import generate_palm_narrative
    narrative = generate_palm_narrative(features, rule_output["interpretations"], hand_type)

    # 7. Return structured response
    return {
        "hand_type": hand_type,
        "lines": rule_output["interpretations"],
        "life_scores": rule_output["life_scores"],
        "narrative": narrative,
        "confidence_map": confidence_map.tolist(),  # for frontend overlay
        "raw_features": features,
    }
```

---

## Phase 5 — Frontend Integration
**Duration: 3–4 days**

### 5.1 Scan Tab Updates

The current wizard flow stays as **fallback** for when camera is unavailable.
Primary flow becomes:
1. Upload / Camera → image preview
2. "Analyze" button → backend API call → real CV-powered reading
3. Visual overlay: colored line masks drawn on top of the palm photo
4. Full report with confidence scores per line

### 5.2 Visual Overlay (Canvas)

```typescript
// Draw segmentation mask overlay on canvas
function drawMaskOverlay(canvas: HTMLCanvasElement, image: HTMLImageElement, maskData: number[][], colorMap: Record<number, string>) {
  const ctx = canvas.getContext("2d")!;
  ctx.drawImage(image, 0, 0, canvas.width, canvas.height);

  // For each mask pixel, draw semi-transparent color
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  maskData.forEach((row, y) => {
    row.forEach((cls, x) => {
      if (cls > 0) {
        const [r, g, b] = hexToRgb(colorMap[cls]);
        const idx = (y * canvas.width + x) * 4;
        imageData.data[idx] = r;
        imageData.data[idx+1] = g;
        imageData.data[idx+2] = b;
        imageData.data[idx+3] = 140;  // semi-transparent
      }
    });
  });
  ctx.putImageData(imageData, 0, 0);
}
```

---

## Phase 6 — Mobile / Browser-Side Inference (Optional Advanced)
**Duration: 1 week (do after Phase 5 is working)**

### 6.1 ONNX in Browser (Privacy-First — Image Never Leaves Device)

```bash
pip install onnx onnxruntime
# Export trained model
python scripts/export_onnx.py  # creates palmistry_unet.onnx

# Place in React public folder
cp palmistry_unet.onnx public/models/
```

```typescript
// In React: run model in WebAssembly
import * as ort from "onnxruntime-web";

const session = await ort.InferenceSession.create("/models/palmistry_unet.onnx");
const result = await session.run({ image: tensorFromCanvas });
// Instant local inference — no server needed
```

**This means**: Even if our VM is down, basic line detection works offline in the browser.

---

## Datasets & Resources Reference

### Downloadable Datasets (VERIFIED)

| Dataset | Source | Size | Labels | Notes | Use |
|---|---|---|---|---|---|
| **11k Hands** | Kaggle (shyambhu/hands-and-palm-images-dataset) | 11,076 imgs, 664MB | **None — raw images** | 1600×1200px, 190 subjects, ages 18–75, both dorsal+palmar, ODbL license. **Palmar side only for us (~5,500 usable images)** | Unlabeled pool for pseudo-labeling |
| **Roboflow PalmLinesDetection** | universe.roboflow.com → search "PalmLinesDetection" | ~46–200 imgs | Life/Head/Heart masks | Small but labeled — starting point. Free download with account. | Seed labeled set |
| **Palm-Astro Training Data** | github.com/lakshay102/Palm-Astro-Application | Dummy only (no real dataset) | 3-class: Life/Head/Heart | Uses dummy generator for testing. **No real training data included.** | Study their approach only |
| **CASIA Palmprint** | Academic only | ~600 subjects | Palmprint ridges (NOT lines) | Biometric identification, NOT palmistry | Ignore |
| **FreiHAND** | huggingface.co/datasets/gvlab/freihand | 130k frames | 3D hand keypoints | Good for MediaPipe fallback training | Landmark pre-training |
| **HuggingFace search: "palm line"** | huggingface.co/datasets | **0 results** | — | **Confirmed: no public palmistry line dataset exists** | Our gap to fill |

> **Key finding**: No large-scale labeled palmistry dataset exists anywhere publicly. This is our competitive moat. We build it; we own it.

### Reference Repos (VERIFIED)

| Repo | What it does | Lines detected | Dataset |
|---|---|---|---|
| **yeonsumia/palmistry** | MediaPipe landmarks → perspective warp → U-Net detection | Not documented | Private dataset (4 sample images provided) |
| **lakshay102/Palm-Astro-Application** | U-Net ResNet18 → 4-class segmentation (bg/life/head/heart) → Gradio UI | 3 lines: Life, Head, Heart | Dummy generator (no real data) |

### segmentation-models-pytorch Encoders (VERIFIED)

All support ImageNet pre-training:
```
ResNet:       resnet18, resnet34, resnet50, resnet101, resnet152
ResNeXt:      resnext50_32x4d, resnext101 variants
EfficientNet: efficientnet-b0 through b7
MobileNet:    mobilenet_v2
VGG:          vgg11 through vgg19
DenseNet:     densenet121, densenet169, densenet201
ViT/Transformers: mit_b0, mit_b1, mit_b2, mit_b3, mit_b4, mit_b5 (Mix Vision Transformer)
MobileOne:    mobileone_s0 through s4 (for mobile/ONNX export)
```

**Our choice**: `resnet34` for MVP (fast, proven, ~21M params). Upgrade to `mit_b2` (SegFormer) for better thin-line quality.

### HuggingFace Models to Fine-Tune

| Model | HuggingFace ID | Why |
|---|---|---|
| SegFormer-B2 | `nvidia/mit-b2` | Best thin-line segmentation; uses mit_b2 encoder |
| SAM2 | `facebook/sam2-hiera-large` | Pseudo-labeling + few-shot prompting |
| DINOv2 | `facebook/dinov2-large` | Strong visual backbone for transfer |
| MobileOne-S2 | via smp `mobileone_s2` | Lightweight for ONNX browser inference |

### Python Libraries

```
mediapipe>=0.10.0          # hand landmark detection
opencv-python-headless     # image processing
segmentation-models-pytorch # U-Net, FPN, DeepLab with pre-trained encoders
albumentations             # training augmentation
scikit-image               # skeletonize, morphology
label-studio               # annotation tool (self-hosted)
supervision                # annotation format conversion
onnx, onnxruntime          # model export
torch, torchvision         # already installed on VM
```

---

## Our Improvements Beyond Grok/ChatGPT Recommendations

### 1. Use Qwen (Already on VM) for Narrative — Zero API Cost
- Grok/ChatGPT suggest Groq/OpenAI API
- **Our approach**: prompt our existing Qwen2.5-7B-Instruct
- Cost: $0 per reading vs ~$0.002/reading with OpenAI
- Privacy: palm image + reading never leaves our server

### 2. Browser-Side ONNX Inference Option
- Model runs in WebAssembly via ONNX Runtime Web
- **Image never uploaded to any server** — maximum privacy
- Works offline — even if VM is down
- Instant response (no network latency)

### 3. SAM2 for Pseudo-Labeling (10× Faster Annotation)
- Use SAM2 + landmark prompts to auto-annotate unlabeled 11k Hands images
- Reduces annotation from 30 min/image to 5 min/image
- Gets us to 500 labeled images much faster

### 4. Synthetic Data Generation
- Procedurally generate palm images with perfect ground-truth masks
- Simulate rare cases: scars, tattoos, dry skin, heavy creases
- No collection needed — unlimited training data for hard cases

### 5. Vedic-Specific Feature Rules
- Encode actual Samudrika Shastra rules (from our RAG database!)
- Cross-reference palm features with our existing Bhagavad Gita / Brihat Samhita knowledge
- Provide authentic Vedic context, not just Western palmistry

### 6. Combine with Kundali (Unique Feature No One Has)
- If user has their Kundali, correlate palm features with chart indicators
- e.g., "Your Life Line's break at ~35 years aligns with your Saturn Return period"
- This cross-modality reading is genuinely unprecedented

### 7. Confidence Scores Per Line
- Show per-line detection confidence in UI ("Heart Line: 94% confidence")
- If confidence < 50%, show "Line not clearly visible — please retake photo"
- Guides users to better photos rather than giving wrong readings

---

## Ethics, Privacy & Legal (Non-Negotiable)

```
✓ Prominent disclaimer: "For entertainment and educational purposes only.
  This is not medical, psychological, or legal advice."

✓ Age gate: Must confirm age 18+ before using

✓ Image privacy:
  - Option A: Process on server → delete immediately after analysis
  - Option B: ONNX in browser → image NEVER leaves device (best privacy)
  - Never store palm images without explicit opt-in consent

✓ GDPR/CCPA: Allow users to request deletion of any stored data

✓ No discriminatory framing: Do not frame challenges as permanent or fatalistic

✓ Cultural respect: Frame all content as tradition/wisdom, not scientific fact
```

---

## Step-by-Step Execution Order

```
WEEK 1:  Phase 0 — Setup, clone repos, run existing demos
WEEK 2:  Phase 1 (Part A) — Download Kaggle/Roboflow data, setup Label Studio
WEEK 3:  Phase 1 (Part B) — Annotate 50 images manually + SAM2 pseudo-label 150 more
WEEK 4:  Phase 1 (Part C) — Reach 200+ annotated images, split train/val/test
WEEK 5:  Phase 2 (Part A) — Training pipeline, first U-Net training run
WEEK 6:  Phase 2 (Part B) — Evaluate, iterate, reach target mIoU
WEEK 7:  Phase 3 — Feature extraction on top of segmentation
WEEK 8:  Phase 4 — Rule engine + Qwen narrative integration
WEEK 9:  Phase 5 — Frontend integration, overlay visualization
WEEK 10: Phase 6 — ONNX export, browser inference (optional)
```

---

## Progress Tracker

- [ ] **Phase 0**: Environment setup complete
- [ ] **Phase 0**: Both reference repos cloned and studied
- [ ] **Phase 1**: Kaggle 11k Hands downloaded
- [ ] **Phase 1**: Roboflow dataset downloaded
- [ ] **Phase 1**: Label Studio annotation tool running
- [ ] **Phase 1**: Annotation spec finalized and distributed
- [ ] **Phase 1**: 50 images annotated manually
- [ ] **Phase 1**: SAM2 pseudo-labeling script working
- [ ] **Phase 1**: 200 annotated images ready
- [ ] **Phase 2**: Training pipeline implemented
- [ ] **Phase 2**: First training run completed
- [ ] **Phase 2**: Target mIoU ≥ 0.70 achieved
- [ ] **Phase 2**: Model exported to ONNX
- [ ] **Phase 3**: Feature extraction working for all 7 lines
- [ ] **Phase 4**: Rule engine implemented
- [ ] **Phase 4**: Qwen narrative integrated
- [ ] **Phase 4**: FastAPI endpoint tested end-to-end
- [ ] **Phase 5**: Frontend shows real CV-powered reading
- [ ] **Phase 5**: Visual line overlay working on palm photo
- [ ] **Phase 6**: Browser ONNX inference (optional)

---

## Quick Decision Guide: What to Do RIGHT NOW

```
TODAY:
  1. Run Phase 0 setup — install libraries on VM
  2. Clone the two reference repos
  3. Download Kaggle 11k Hands (free, 11k images ready for pseudo-labeling)
  4. Create Roboflow account → download Palm Line dataset
  5. Install Label Studio → annotate your first 10 palm images to understand the task

THIS WEEK:
  6. Study lakshay102/Palm-Astro repo code in detail
  7. Run their demo to see what U-Net output looks like
  8. Start writing SAM2 pseudo-labeling script

DECISION POINT (end of week 2):
  - If we have 200+ labeled images → proceed to Phase 2 training
  - If we have <100 labeled images → use synthetic generation to fill the gap
```

---

*Brahm AI — Hasta Samudrika Shastra AI System*
*For educational and entertainment purposes only.*
