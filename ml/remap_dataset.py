"""Convert a YOLO-format dataset with arbitrary tile class names into our 37-class scheme.

Typical use, after exporting a dataset from Roboflow Universe in "YOLOv11" format:

    python remap_dataset.py downloads/riichi-tiles-1 datasets/riichi

The source folder must contain a data.yaml with `names` and split folders such as
train/, valid/, test/, each holding images/ and labels/. The output is a dataset with
train/ and val/ splits plus a data.yaml (absolute paths) ready for train.py.

Class names vary a lot between datasets ("1m", "Man1", "1-character", "dragon-red", ...).
normalize() handles common spellings; anything it can't map is listed and the script stops.
Fix those with --map overrides.json, e.g. {"bonus-flower": null, "weird5": "0p"}
(null drops that class's boxes).
"""

import argparse
import json
import re
import shutil
import sys
from pathlib import Path

import yaml

from classes import CLASSES

SUIT_WORDS = {
    "m": "m", "man": "m", "wan": "m", "character": "m", "characters": "m", "char": "m", "crak": "m",
    "p": "p", "pin": "p", "dot": "p", "dots": "p", "circle": "p", "circles": "p", "tong": "p",
    "s": "s", "sou": "s", "so": "s", "bamboo": "s", "bam": "s", "bams": "s", "tiao": "s", "suo": "s",
    "z": "z", "honor": "z", "honors": "z", "ji": "z",
}

# Only consulted when the name has no digit, so single letters like "s" mean South here.
HONOR_WORDS = {
    "east": "1z", "ton": "1z", "e": "1z", "dong": "1z",
    "south": "2z", "nan": "2z", "s": "2z",
    "west": "3z", "sha": "3z", "shaa": "3z", "xi": "3z", "w": "3z",
    "north": "4z", "pei": "4z", "bei": "4z", "n": "4z",
    "white": "5z", "haku": "5z", "bai": "5z",
    "green": "6z", "hatsu": "6z", "fa": "6z",
    "red": "7z", "chun": "7z", "zhong": "7z",
}

RED_WORDS = {"red", "aka", "dora", "r"}
FILLER_WORDS = {"dragon", "wind", "tile"}

SPLITS = {"train": "train", "valid": "val", "val": "val", "test": "val"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def normalize(name: str) -> str | None:
    """Best-effort mapping of a dataset class name to one of CLASSES."""
    lowered = name.strip().lower()
    if lowered in CLASSES:
        return lowered

    # Split a glued "tile" suffix off words like "Easttile" or "doratile".
    tokens = [re.sub(r"(?<=.)tile$", "", t) for t in re.findall(r"[a-z]+|\d+", lowered)]
    digits = [t for t in tokens if t.isdigit()]
    words = [t for t in tokens if t.isalpha() and t not in FILLER_WORDS]

    if not digits:
        key = "".join(words)
        if key in HONOR_WORDS:
            return HONOR_WORDS[key]
        # e.g. "red dragon" -> ["red"], "dragon green" -> ["green"]
        if len(words) == 1 and words[0] in HONOR_WORDS:
            return HONOR_WORDS[words[0]]
        return None

    if len(digits) != 1 or len(digits[0]) != 1:
        return None
    number = int(digits[0])

    suit = None
    red = number == 0
    for w in words:
        if w in SUIT_WORDS:
            suit = SUIT_WORDS[w]
        elif w.endswith("r") and w[:-1] in SUIT_WORDS:  # "5mr"
            suit = SUIT_WORDS[w[:-1]]
            red = True
        elif w in RED_WORDS:
            red = True
    if suit is None:
        return None
    if suit == "z":
        return f"{number}z" if 1 <= number <= 7 else None
    if red:
        return f"0{suit}" if number in (0, 5) else None
    return f"{number}{suit}" if 1 <= number <= 9 else None


def load_names(data_yaml: Path) -> list[str]:
    names = yaml.safe_load(data_yaml.read_text(encoding="utf-8"))["names"]
    if isinstance(names, dict):
        return [names[k] for k in sorted(names)]
    return list(names)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", type=Path, help="Downloaded YOLO dataset folder")
    parser.add_argument("dest", type=Path, help="Output folder, e.g. datasets/riichi")
    parser.add_argument("--map", type=Path, help="JSON of {source_name: our_id or null} overrides")
    args = parser.parse_args()

    source_names = load_names(args.source / "data.yaml")
    overrides = json.loads(args.map.read_text(encoding="utf-8")) if args.map else {}

    mapping: dict[int, int | None] = {}
    unknown = []
    for idx, name in enumerate(source_names):
        target = overrides[name] if name in overrides else normalize(name)
        if target is None and name not in overrides:
            unknown.append(name)
            continue
        mapping[idx] = CLASSES.index(target) if target is not None else None
        print(f"  {name!r:28} -> {target}")

    if unknown:
        print(f"\nCould not map {len(unknown)} class name(s): {unknown}", file=sys.stderr)
        print("Add them to a --map JSON file (use null to drop a class).", file=sys.stderr)
        sys.exit(1)

    missing = set(range(len(CLASSES))) - {v for v in mapping.values() if v is not None}
    if missing:
        print(f"\nWarning: no source class covers {[CLASSES[i] for i in sorted(missing)]}. "
              "Add your own labelled photos for these.")

    counts = {"train": 0, "val": 0}
    for split_dir in sorted(p for p in args.source.iterdir() if p.is_dir()):
        split = SPLITS.get(split_dir.name)
        if split is None:
            continue
        out_images = args.dest / "images" / split
        out_labels = args.dest / "labels" / split
        out_images.mkdir(parents=True, exist_ok=True)
        out_labels.mkdir(parents=True, exist_ok=True)

        for image in (split_dir / "images").iterdir():
            if image.suffix.lower() not in IMAGE_EXTS:
                continue
            # Prefix with the source split so train/test files with the same name don't collide.
            stem = f"{split_dir.name}_{image.stem}"
            shutil.copy2(image, out_images / f"{stem}{image.suffix}")

            label_file = split_dir / "labels" / f"{image.stem}.txt"
            lines = []
            if label_file.exists():
                for line in label_file.read_text(encoding="utf-8").splitlines():
                    parts = line.split()
                    if not parts:
                        continue
                    new_cls = mapping.get(int(parts[0]))
                    if new_cls is not None:
                        lines.append(" ".join([str(new_cls), *parts[1:]]))
            (out_labels / f"{stem}.txt").write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")
            counts[split] += 1

    dest = args.dest.resolve()
    data_yaml = {
        "path": str(dest),
        "train": "images/train",
        "val": "images/val",
        "names": dict(enumerate(CLASSES)),
    }
    (dest / "data.yaml").write_text(yaml.safe_dump(data_yaml, sort_keys=False), encoding="utf-8")
    print(f"\nWrote {counts['train']} train / {counts['val']} val images to {dest}")
    print(f"Train with: python train.py --data {dest / 'data.yaml'}")


if __name__ == "__main__":
    main()
