# Palmistry AI Scripts

Run these in order on the VM.

| Script | Purpose | Run When |
|---|---|---|
| `01_preprocess.py` | MediaPipe warp → 512×512 canonical palm | Before annotation |
| `02_dataset.py`    | PyTorch Dataset class for training | During training |
| `03_train.py`      | U-Net ResNet34 training | After 200+ images annotated |
| `04_extract_features.py` | Geometric features from mask | After model trained |
| `05_interpret.py`  | Vedic rules engine + Qwen prompt builder | After features |
| `06_inference.py`  | Full end-to-end pipeline | Production |

## Upload to VM

```bash
# From local PC — upload all scripts
scp -r "c:\desktop\Brahm AI\palmistry_ai\scripts\" amareshsingh2005@34.135.70.190:~/books/palmistry_ai/scripts/
```

## VM Run Commands

```bash
source ~/ai-env/bin/activate
cd ~/books/palmistry_ai

# Preprocess raw images
python3 scripts/01_preprocess.py \
    --input  data/raw/my_palms \
    --output data/processed

# Test interpretation engine (no model needed)
python3 scripts/05_interpret.py

# Full inference (needs trained model)
python3 scripts/06_inference.py \
    --image data/raw/my_palms/palm1.jpg \
    --model models/best_model.pth
```
