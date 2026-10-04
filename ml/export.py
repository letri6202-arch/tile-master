"""Export trained weights to TFLite and install them into the Android app's assets."""

import argparse
import shutil
from pathlib import Path

from ultralytics import YOLO

from classes import CLASSES

ML = Path(__file__).resolve().parent
ASSETS = ML.parent / "android" / "app" / "src" / "main" / "assets"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--weights", default=ML / "runs" / "tiles" / "weights" / "best.pt")
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--out", type=Path, default=ASSETS)
    args = parser.parse_args()

    model = YOLO(args.weights)
    names = [model.names[i] for i in sorted(model.names)]
    unexpected = [n for n in names if n not in CLASSES]
    if unexpected:
        raise SystemExit(f"Model has class names the app doesn't know: {unexpected}")

    # float16 weights with float32 input/output: half the size, and the app needs no quantization logic.
    exported = Path(model.export(format="tflite", imgsz=args.imgsz, half=True))
    if exported.is_dir():
        exported = next(exported.glob("*float16.tflite"))

    args.out.mkdir(parents=True, exist_ok=True)
    shutil.copy2(exported, args.out / "tiles.tflite")
    (args.out / "labels.txt").write_text("\n".join(names) + "\n", encoding="utf-8")
    print(f"Installed {exported.name} -> {args.out / 'tiles.tflite'} ({len(names)} classes)")


if __name__ == "__main__":
    main()
