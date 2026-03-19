"""
Stage 3: U-Net Training Script
Our model: ResNet34 encoder (vs ref ResNet18), 8 classes (vs ref 4), 512×512 (vs ref 256×256)

Run on VM:
    python3 03_train.py \
        --images ~/books/palmistry_ai/data/annotated/images \
        --masks  ~/books/palmistry_ai/data/annotated/masks \
        --epochs 60 \
        --output ~/books/palmistry_ai/models
"""

import torch
import torch.nn as nn
import numpy as np
import argparse
import json
import time
from pathlib import Path

import segmentation_models_pytorch as smp
from segmentation_models_pytorch.losses import DiceLoss, SoftCrossEntropyLoss

from palm_02_dataset import PalmDataset, get_dataloaders, NUM_CLASSES, CLASS_NAMES

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"


# ─── Model ────────────────────────────────────────────────────────────────────

def build_model(encoder="resnet34"):
    """
    U-Net with ImageNet pre-trained encoder.
    Our choice: resnet34 (better than ref resnet18, fits in L4 GPU).
    Future upgrade: mit_b2 (SegFormer) for better thin-line quality.
    """
    model = smp.Unet(
        encoder_name=encoder,
        encoder_weights="imagenet",
        in_channels=3,
        classes=NUM_CLASSES,
        activation=None,         # Raw logits — use CrossEntropy loss
    )
    return model.to(DEVICE)


# ─── Loss ─────────────────────────────────────────────────────────────────────

class CombinedLoss(nn.Module):
    """
    Dice + CrossEntropy combined loss.
    Dice handles class imbalance (thin lines vs large background).
    CE provides stable gradients.
    """
    def __init__(self, class_weights=None):
        super().__init__()
        self.dice = DiceLoss(mode="multiclass", from_logits=True)
        self.ce   = nn.CrossEntropyLoss(weight=class_weights.to(DEVICE) if class_weights is not None else None)

    def forward(self, pred, target):
        return 0.5 * self.dice(pred, target) + 0.5 * self.ce(pred, target)


# ─── Metrics ──────────────────────────────────────────────────────────────────

def compute_miou(pred_mask, true_mask, num_classes=NUM_CLASSES):
    """Compute per-class IoU and mean IoU."""
    pred_mask = pred_mask.cpu().numpy()
    true_mask = true_mask.cpu().numpy()

    ious = []
    for cls in range(num_classes):
        pred_cls = (pred_mask == cls)
        true_cls = (true_mask == cls)
        intersection = (pred_cls & true_cls).sum()
        union        = (pred_cls | true_cls).sum()
        if union == 0:
            ious.append(float("nan"))  # class not present
        else:
            ious.append(intersection / union)

    miou = np.nanmean(ious)
    return miou, ious


# ─── Training ─────────────────────────────────────────────────────────────────

def train_epoch(model, loader, optimizer, criterion):
    model.train()
    total_loss = 0.0
    for images, masks in loader:
        images = images.to(DEVICE)
        masks  = masks.to(DEVICE)

        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, masks)
        loss.backward()

        torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        optimizer.step()
        total_loss += loss.item()

    return total_loss / len(loader)


@torch.no_grad()
def eval_epoch(model, loader, criterion):
    model.eval()
    total_loss = 0.0
    all_miou   = []

    for images, masks in loader:
        images = images.to(DEVICE)
        masks  = masks.to(DEVICE)

        outputs = model(images)
        loss    = criterion(outputs, masks)
        total_loss += loss.item()

        preds = outputs.argmax(dim=1)
        for i in range(preds.shape[0]):
            miou, _ = compute_miou(preds[i], masks[i])
            all_miou.append(miou)

    return total_loss / len(loader), float(np.mean(all_miou))


# ─── Main ─────────────────────────────────────────────────────────────────────

def main(args):
    print(f"\n{'='*55}")
    print(f"  Brahm AI — Palm Line Segmentation Training")
    print(f"  Device: {DEVICE} ({torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'CPU'})")
    print(f"  Encoder: {args.encoder} | Classes: {NUM_CLASSES} | Size: 512×512")
    print(f"{'='*55}\n")

    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    # ── Data ──
    train_loader, val_loader, test_loader = get_dataloaders(
        images_dir=args.images,
        masks_dir=args.masks,
        batch_size=args.batch_size,
        num_workers=4,
    )

    # ── Model ──
    model = build_model(args.encoder)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Model parameters: {total_params:,}")

    # ── Loss with class weights ──
    full_ds = PalmDataset(args.images, args.masks)
    class_weights = full_ds.class_weights()
    print(f"Class weights: {class_weights.numpy().round(3)}")
    criterion = CombinedLoss(class_weights)

    # ── Optimizer + Scheduler ──
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=args.epochs, eta_min=1e-6)

    # ── Training Loop ──
    best_miou  = 0.0
    history    = []

    for epoch in range(1, args.epochs + 1):
        t0 = time.time()

        train_loss             = train_epoch(model, train_loader, optimizer, criterion)
        val_loss, val_miou     = eval_epoch(model, val_loader, criterion)
        scheduler.step()

        elapsed = time.time() - t0
        lr      = optimizer.param_groups[0]["lr"]

        print(f"Epoch {epoch:3d}/{args.epochs} | "
              f"Train Loss: {train_loss:.4f} | "
              f"Val Loss: {val_loss:.4f} | "
              f"Val mIoU: {val_miou:.4f} | "
              f"LR: {lr:.2e} | "
              f"Time: {elapsed:.1f}s")

        history.append({
            "epoch": epoch, "train_loss": train_loss,
            "val_loss": val_loss, "val_miou": val_miou,
        })

        # Save best model
        if val_miou > best_miou:
            best_miou = val_miou
            torch.save({
                "epoch": epoch,
                "model_state": model.state_dict(),
                "optimizer_state": optimizer.state_dict(),
                "val_miou": val_miou,
                "encoder": args.encoder,
                "num_classes": NUM_CLASSES,
            }, output_dir / "best_model.pth")
            print(f"  ✓ Best model saved (mIoU: {best_miou:.4f})")

    # Save training history
    with open(output_dir / "history.json", "w") as f:
        json.dump(history, f, indent=2)

    # Final test evaluation
    print("\n── Test Set Evaluation ──")
    model.load_state_dict(
        torch.load(output_dir / "best_model.pth")["model_state"]
    )
    test_loss, test_miou = eval_epoch(model, test_loader, criterion)
    print(f"Test Loss: {test_loss:.4f} | Test mIoU: {test_miou:.4f}")

    if test_miou >= 0.70:
        print("✓ TARGET REACHED: mIoU ≥ 0.70")
    else:
        print(f"⚠ More training/data needed. Current: {test_miou:.4f}, Target: 0.70")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--images",     required=True,        help="Annotated images folder")
    parser.add_argument("--masks",      required=True,        help="Masks folder")
    parser.add_argument("--output",     required=True,        help="Model output folder")
    parser.add_argument("--encoder",    default="resnet34",   help="Encoder backbone")
    parser.add_argument("--epochs",     type=int, default=60, help="Training epochs")
    parser.add_argument("--batch_size", type=int, default=8,  help="Batch size")
    parser.add_argument("--lr",         type=float, default=3e-4, help="Learning rate")
    args = parser.parse_args()
    main(args)
