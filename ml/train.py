"""Fine-tune a YOLO11 detector on the remapped tile dataset."""

import argparse
from pathlib import Path

from ultralytics import YOLO

# Absolute, so results land in ml/runs/tiles whatever the working directory
# (Ultralytics nests relative project paths under its own runs_dir).
RUNS = Path(__file__).resolve().parent / "runs"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", default="datasets/riichi/data.yaml")
    parser.add_argument("--model", default="yolo11n.pt", help="Pretrained weights to start from")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument("--device", default=None, help="e.g. 0 for the first GPU, or cpu")
    args = parser.parse_args()

    model = YOLO(args.model)
    model.train(
        data=args.data,
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        project=str(RUNS),
        name="tiles",
        # Overwrite the previous run so export.py always finds the latest weights.
        exist_ok=True,
        # Mirrored tiles don't exist in real life (numbers and kanji would read backwards).
        fliplr=0.0,
        flipud=0.0,
        # Hands are photographed at slight angles, so allow some rotation and perspective.
        degrees=10.0,
        perspective=0.0005,
        # Keep hue shifts small so red fives stay red and can be told apart from normal fives.
        hsv_h=0.005,
    )
    metrics = model.val()
    print(f"mAP50: {metrics.box.map50:.3f}  mAP50-95: {metrics.box.map:.3f}")


if __name__ == "__main__":
    main()
