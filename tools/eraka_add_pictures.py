#!/usr/bin/env python3
"""
Adds the drawings for Eraka's new pictograms (the "add" list in
app/src/main/assets/eraka_corrections/vocabulary_corrections.json).

    1. Download each drawing from arasaac.org (PNG) and save it in
       eraka_base_data/new_pictures/ named after the word: sleep.png, hurt.png, ...
    2. python tools/eraka_add_pictures.py
    3. python tools/eraka_base_import.py     (the base model now uses the new words)
    4. Rebuild the app.

Real photos (JPG) are cropped to their middle square; drawings with a transparent
background are placed on white. Each picture is made 500 x 500 like OTTAA's pictures, and
saved as res/drawable-xxhdpi/<picto>.webp. Until its drawing exists, the app does not add
the word (no empty tiles).
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORR = os.path.join(ROOT, "app/src/main/assets/eraka_corrections/vocabulary_corrections.json")
SRC = os.path.join(ROOT, "eraka_base_data/new_pictures")
OUT = os.path.join(ROOT, "app/src/main/res/drawable-xxhdpi")
SIZE = 500


def main():
    adds = json.load(open(CORR, encoding="utf-8")).get("add", {})
    files = {os.path.splitext(f)[0].lower().replace("_", " ").strip(): f
             for f in os.listdir(SRC) if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp"))}
    done, missing = [], []
    for pid, a in adds.items():
        word, picto = a["en"].lower(), a["picto"]
        short = picto.replace("eraka_", "").replace("_", " ")     # "do laundry" -> laundry.png
        src = files.get(word) or files.get(short)
        if not src:
            missing.append(short)
            continue
        im = Image.open(os.path.join(SRC, src))
        if im.mode not in ("RGBA", "LA", "P"):        # a photo: crop the middle square
            side = min(im.size)
            left, top = (im.width - side) // 2, (im.height - side) // 2
            im = im.crop((left, top, left + side, top + side))
        im = im.convert("RGBA")
        im.thumbnail((SIZE, SIZE), Image.LANCZOS)
        canvas = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 255))
        canvas.paste(im, ((SIZE - im.width) // 2, (SIZE - im.height) // 2), im)
        dest = os.path.join(OUT, picto + ".webp")
        canvas.convert("RGB").save(dest, "WEBP", quality=90)
        done.append(f"{word} -> {os.path.relpath(dest, ROOT)} (id {pid})")
    print("Added:\n  " + ("\n  ".join(done) if done else "none"))
    if missing:
        print("Still needed in eraka_base_data/new_pictures/: " + ", ".join(w + ".png/.jpg" for w in missing))
    return 0


if __name__ == "__main__":
    sys.exit(main())
