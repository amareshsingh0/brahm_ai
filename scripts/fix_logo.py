"""
Fix logo PNG corners (make transparent) and generate all required icon sizes.
Run from project root: python scripts/fix_logo.py

Requires: pip install Pillow
"""
from PIL import Image, ImageDraw
import os

SRC = os.path.join("public", "android-chrome-512x512.png")
OUT_DIR = "public"

SIZES = {
    "favicon-16x16.png":         (16, 16),
    "favicon-32x32.png":         (32, 32),
    "apple-touch-icon.png":      (180, 180),
    "android-chrome-192x192.png":(192, 192),
    "android-chrome-512x512.png":(512, 512),
}


def make_transparent_corners(img: Image.Image, radius_ratio: float = 0.18) -> Image.Image:
    """Make image corners transparent using rounded-rect mask."""
    img = img.convert("RGBA")
    w, h = img.size
    radius = int(min(w, h) * radius_ratio)

    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (w - 1, h - 1)], radius=radius, fill=255)

    result = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    result.paste(img, mask=mask)
    return result


def main():
    if not os.path.exists(SRC):
        print(f"ERROR: {SRC} not found. Run from project root.")
        return

    src_img = Image.open(SRC).convert("RGBA")
    print(f"Source: {SRC} ({src_img.size[0]}x{src_img.size[1]})")

    for filename, (w, h) in SIZES.items():
        resized = src_img.resize((w, h), Image.LANCZOS)
        fixed = make_transparent_corners(resized, radius_ratio=0.18)
        out_path = os.path.join(OUT_DIR, filename)
        fixed.save(out_path, "PNG", optimize=True)
        print(f"  OK {filename} ({w}x{h})")

    # Android mipmap foreground PNGs (referenced by adaptive icon XML)
    android_sizes = {
        "android-app/app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png":    (48, 48),
        "android-app/app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png":    (72, 72),
        "android-app/app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png":   (96, 96),
        "android-app/app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png":  (144, 144),
        "android-app/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png": (192, 192),
    }
    for path, (w, h) in android_sizes.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        resized = src_img.resize((w, h), Image.LANCZOS)
        # No corner rounding for foreground — adaptive icon system handles shape
        resized.save(path, "PNG", optimize=True)
        print(f"  OK {path} ({w}x{h})")

    # Also save favicon.ico (multi-size)
    sizes_for_ico = [(16, 16), (32, 32), (48, 48)]
    ico_imgs = []
    for s in sizes_for_ico:
        r = src_img.resize(s, Image.LANCZOS)
        ico_imgs.append(make_transparent_corners(r, radius_ratio=0.18))
    ico_path = os.path.join(OUT_DIR, "favicon.ico")
    ico_imgs[0].save(ico_path, format="ICO", sizes=[(i.width, i.height) for i in ico_imgs],
                     append_images=ico_imgs[1:])
    print(f"  OK favicon.ico (16, 32, 48)")

    print("\nDone! All icons generated with transparent corners.")


if __name__ == "__main__":
    main()
