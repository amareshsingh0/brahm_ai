"""
Stage 4: Feature Extraction from Segmentation Masks
Input:  8-class segmentation mask (512×512)
Output: dict of geometric features per line (length, curvature, breaks, forks, etc.)

Run on VM:
    python3 04_extract_features.py --mask /path/to/mask.png
"""

import cv2
import numpy as np
from skimage.morphology import skeletonize
from skimage.measure import label, regionprops
from scipy.ndimage import distance_transform_edt
import json
import argparse

# Class IDs
CLASS_HEART    = 1
CLASS_HEAD     = 2
CLASS_LIFE     = 3
CLASS_FATE     = 4
CLASS_SUN      = 5
CLASS_MERCURY  = 6
CLASS_MARRIAGE = 7

CLASS_NAMES = {
    1: "Heart Line",   2: "Head Line",  3: "Life Line",
    4: "Fate Line",    5: "Sun Line",   6: "Mercury Line",
    7: "Marriage Lines",
}


def extract_line_features(mask: np.ndarray, class_id: int, palm_width_px: int = 512) -> dict:
    """
    Extract geometric features for one palm line class.
    All length measurements normalized as % of palm width.
    """
    binary = (mask == class_id).astype(np.uint8)
    pixel_count = int(binary.sum())

    if pixel_count < 30:
        return {
            "present": False,
            "confidence": 0.0,
            "pixel_count": pixel_count,
        }

    # ── Skeletonize → 1px thin line ──
    skeleton = skeletonize(binary.astype(bool)).astype(np.uint8)
    skel_pixels = int(skeleton.sum())

    if skel_pixels < 10:
        return {"present": False, "confidence": 0.0, "pixel_count": pixel_count}

    # ── Arc length ──
    # Sum Euclidean distances between neighboring skeleton pixels
    coords = np.argwhere(skeleton)  # (N, 2) array of (row, col)
    arc_length_px = float(skel_pixels)   # rough estimate
    arc_length_pct = round(arc_length_px / palm_width_px * 100, 2)

    # ── Chord length (straight-line distance from first to last skeleton point) ──
    if len(coords) >= 2:
        # Sort coordinates along the line (approximate ordering)
        from sklearn.decomposition import PCA
        pca = PCA(n_components=1)
        pca.fit(coords)
        projections = pca.transform(coords).flatten()
        sorted_idx = np.argsort(projections)
        start = coords[sorted_idx[0]]
        end   = coords[sorted_idx[-1]]
        chord_length = float(np.linalg.norm(start - end))
    else:
        chord_length = 1.0

    # ── Tortuosity (curvature measure) ──
    # 1.0 = perfectly straight, >1.2 = curved, >1.5 = very curved
    tortuosity = round(arc_length_px / (chord_length + 1e-6), 3)

    # ── Connected components (breaks detection) ──
    labeled_map = label(skeleton)
    n_components = int(labeled_map.max())
    n_breaks = max(0, n_components - 1)   # breaks = extra segments beyond 1

    # ── Fork detection ──
    # A fork pixel has ≥3 skeleton neighbors
    kernel = np.ones((3, 3), np.uint8)
    neighbor_count = cv2.filter2D(skeleton.astype(np.float32), -1, kernel)
    # Skeleton pixels with 4+ neighbors in 3×3 window (self=1, so 3+ neighbors → total 4)
    fork_map = (neighbor_count >= 4) & (skeleton > 0)
    n_forks = int(fork_map.sum())

    # ── Thickness (mean diameter) ──
    dist_map = distance_transform_edt(binary)
    skel_mask = skeleton.astype(bool)
    thickness_px = float(dist_map[skel_mask].mean()) * 2 if skel_mask.any() else 0.0

    # ── Orientation (dominant angle) ──
    regions = regionprops(binary)
    if regions:
        orientation_rad = regions[0].orientation
        orientation_deg = round(float(np.degrees(orientation_rad)), 1)
    else:
        orientation_deg = 0.0

    # ── Depth (vertical position on palm) ──
    # 0 = fingers, 1 = wrist
    if len(coords) > 0:
        mean_row = float(coords[:, 0].mean()) / 512
    else:
        mean_row = 0.5

    # ── Confidence (how prominent the line is) ──
    confidence = min(1.0, float(pixel_count) / (palm_width_px * 5))

    return {
        "present":         True,
        "length_pct":      arc_length_pct,
        "tortuosity":      tortuosity,
        "breaks":          n_breaks,
        "forks":           n_forks,
        "thickness_px":    round(thickness_px, 2),
        "orientation_deg": orientation_deg,
        "depth_normalized": round(mean_row, 3),
        "pixel_count":     pixel_count,
        "confidence":      round(confidence, 3),
    }


def extract_all_features(mask: np.ndarray, palm_width_px: int = 512) -> dict:
    """Extract features for all 7 line classes from a segmentation mask."""
    features = {}
    for class_id, class_name in CLASS_NAMES.items():
        key = class_name.lower().replace(" ", "_")
        features[key] = extract_line_features(mask, class_id, palm_width_px)

    # ── Cross-line derived features ──
    heart = features.get("heart_line", {})
    head  = features.get("head_line",  {})
    life  = features.get("life_line",  {})
    fate  = features.get("fate_line",  {})

    derived = {}
    if heart.get("present") and head.get("present"):
        derived["heart_longer_than_head"] = heart["length_pct"] > head["length_pct"]
        derived["heart_more_curved"]      = heart["tortuosity"] > head["tortuosity"]
    if life.get("present"):
        derived["life_wide_arc"] = life.get("tortuosity", 1.0) > 1.08
    if fate.get("present"):
        derived["fate_present"]  = True
        derived["fate_deep"]     = fate.get("confidence", 0) > 0.5
    else:
        derived["fate_present"]  = False

    features["derived"] = derived
    return features


def print_features(features: dict):
    print("\n── Palm Line Features ──")
    for key, val in features.items():
        if key == "derived":
            print(f"\n  Derived:")
            for k, v in val.items():
                print(f"    {k}: {v}")
        elif isinstance(val, dict):
            if val.get("present"):
                name = key.replace("_", " ").title()
                print(f"\n  {name}:")
                for k, v in val.items():
                    if k != "present":
                        print(f"    {k}: {v}")
            else:
                print(f"\n  {key.replace('_', ' ').title()}: Not detected")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--mask",  required=True, help="Path to segmentation mask PNG")
    parser.add_argument("--width", type=int, default=512, help="Palm width in pixels")
    args = parser.parse_args()

    mask = cv2.imread(args.mask, cv2.IMREAD_GRAYSCALE)
    if mask is None:
        print(f"Error: cannot read mask at {args.mask}")
        exit(1)

    features = extract_all_features(mask, args.width)
    print_features(features)
    print("\nJSON output:")
    print(json.dumps(features, indent=2))
