"""
Stage 2: Dataset class for training.
Loads annotated images + masks from Label Studio COCO export.

Folder structure expected:
    data/annotated/
        images/   ← warped 512×512 palm photos
        masks/    ← PNG masks (same filename, pixel value = class id)

Class IDs:
    0 = Background
    1 = Heart Line  (Hridaya Rekha)
    2 = Head Line   (Mastishka Rekha)
    3 = Life Line   (Jeevan Rekha)
    4 = Fate Line   (Bhagya Rekha)
    5 = Sun Line    (Surya Rekha)
    6 = Mercury Line (Budha Rekha)
    7 = Marriage Lines (Vivah Rekha)
"""

import cv2
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader, random_split
from pathlib import Path
import albumentations as A
from albumentations.pytorch import ToTensorV2

NUM_CLASSES = 8

CLASS_NAMES = {
    0: "Background",
    1: "Heart Line",
    2: "Head Line",
    3: "Life Line",
    4: "Fate Line",
    5: "Sun Line",
    6: "Mercury Line",
    7: "Marriage Lines",
}

# Overlay colors for visualization (BGR)
CLASS_COLORS = {
    0: (0,   0,   0),    # Background — black
    1: (10, 101, 232),   # Heart — saffron/orange (E8650A in BGR)
    2: (10, 134, 200),   # Head — gold (C8860A)
    3: (217, 40, 109),   # Life — purple (6D28D9)
    4: (10, 134, 200),   # Fate — gold
    5: (10, 101, 232),   # Sun — saffron
    6: (170, 139, 122),  # Mercury — steel
    7: (217, 40, 109),   # Marriage — purple
}

# Augmentation pipelines
def get_train_transform():
    return A.Compose([
        A.HorizontalFlip(p=0.5),
        A.RandomRotate90(p=0.2),
        A.ShiftScaleRotate(shift_limit=0.05, scale_limit=0.1, rotate_limit=15, border_mode=0, p=0.5),
        A.RandomBrightnessContrast(brightness_limit=0.3, contrast_limit=0.3, p=0.6),
        A.HueSaturationValue(hue_shift_limit=8, sat_shift_limit=25, val_shift_limit=20, p=0.4),
        A.GaussNoise(p=0.3),
        A.ElasticTransform(alpha=30, sigma=5, p=0.2),
        A.Resize(512, 512),
        A.Normalize(mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)),
        ToTensorV2(),
    ])

def get_val_transform():
    return A.Compose([
        A.Resize(512, 512),
        A.Normalize(mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)),
        ToTensorV2(),
    ])


class PalmDataset(Dataset):
    """
    Palm line segmentation dataset.
    Expects:
        images_dir/  *.jpg or *.png
        masks_dir/   same filename as image but with class pixel values
    """

    def __init__(self, images_dir: str, masks_dir: str, transform=None):
        self.images_dir = Path(images_dir)
        self.masks_dir  = Path(masks_dir)
        self.transform  = transform

        exts = {".jpg", ".jpeg", ".png"}
        self.image_files = sorted([
            f for f in self.images_dir.iterdir()
            if f.suffix.lower() in exts
        ])

        # Only keep images that have a corresponding mask
        self.image_files = [
            f for f in self.image_files
            if (self.masks_dir / f.name).exists() or
               (self.masks_dir / (f.stem + ".png")).exists()
        ]

        print(f"Dataset: {len(self.image_files)} annotated images found")

    def __len__(self):
        return len(self.image_files)

    def __getitem__(self, idx):
        img_path = self.image_files[idx]

        # Load image (BGR → RGB)
        image = cv2.imread(str(img_path))
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        # Load mask (grayscale, pixel = class id)
        mask_path = self.masks_dir / img_path.name
        if not mask_path.exists():
            mask_path = self.masks_dir / (img_path.stem + ".png")
        mask = cv2.imread(str(mask_path), cv2.IMREAD_GRAYSCALE)

        if mask is None:
            mask = np.zeros(image.shape[:2], dtype=np.uint8)

        # Clamp mask to valid class range
        mask = np.clip(mask, 0, NUM_CLASSES - 1)

        if self.transform:
            transformed = self.transform(image=image, mask=mask)
            image = transformed["image"]
            mask  = transformed["mask"].long()
        else:
            image = torch.from_numpy(image.transpose(2, 0, 1)).float() / 255.0
            mask  = torch.from_numpy(mask).long()

        return image, mask

    def class_weights(self) -> torch.Tensor:
        """Compute inverse-frequency class weights for weighted loss."""
        counts = torch.zeros(NUM_CLASSES)
        for img_file in self.image_files:
            mask_path = self.masks_dir / img_file.name
            if not mask_path.exists():
                mask_path = self.masks_dir / (img_file.stem + ".png")
            mask = cv2.imread(str(mask_path), cv2.IMREAD_GRAYSCALE)
            if mask is None:
                continue
            for c in range(NUM_CLASSES):
                counts[c] += (mask == c).sum()
        counts = counts.clamp(min=1)
        weights = 1.0 / counts
        weights = weights / weights.sum() * NUM_CLASSES
        return weights


def get_dataloaders(images_dir, masks_dir, val_split=0.15, test_split=0.10,
                    batch_size=8, num_workers=4):
    """Split dataset into train/val/test and return DataLoaders."""

    full_dataset = PalmDataset(images_dir, masks_dir)
    n = len(full_dataset)

    n_test  = max(1, int(n * test_split))
    n_val   = max(1, int(n * val_split))
    n_train = n - n_val - n_test

    train_ds, val_ds, test_ds = random_split(
        full_dataset, [n_train, n_val, n_test],
        generator=torch.Generator().manual_seed(42)
    )

    # Apply transforms
    train_ds.dataset.transform = get_train_transform()
    val_ds.dataset.transform   = get_val_transform()
    test_ds.dataset.transform  = get_val_transform()

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True,
                              num_workers=num_workers, pin_memory=True)
    val_loader   = DataLoader(val_ds,   batch_size=batch_size, shuffle=False,
                              num_workers=num_workers, pin_memory=True)
    test_loader  = DataLoader(test_ds,  batch_size=1, shuffle=False,
                              num_workers=1)

    print(f"Split — Train: {len(train_ds)}, Val: {len(val_ds)}, Test: {len(test_ds)}")
    return train_loader, val_loader, test_loader


if __name__ == "__main__":
    # Quick test with dummy data
    print("Dataset class loaded OK")
    print(f"Classes: {CLASS_NAMES}")
