# Tile detector training

Trains a YOLO11-nano detector for the 37 tile classes in `classes.py` and exports it to TFLite for the app.

## Environment

TFLite export needs TensorFlow, which usually lags behind the newest Python. Use **Python 3.11 or 3.12**
in a virtual environment, or simply use **Google Colab** (free GPU, everything preinstalled):

```
python -m venv .venv
.venv\Scripts\activate          # Windows
pip install -r requirements.txt
```

## 1. Get a starting dataset

Search [Roboflow Universe](https://universe.roboflow.com) for "riichi mahjong" or "mahjong tiles".
Pick a dataset with bounding boxes for all tile types (ideally including red fives). Then
**Download Dataset**, choose format **YOLOv11**, and unzip it into `ml/downloads/<name>`.

## 2. Remap its classes to ours

```
python remap_dataset.py downloads/<name> datasets/riichi
```

This prints how each source class name was mapped. Names it can't figure out stop the script.
Put them in a JSON file and pass `--map overrides.json`, e.g. `{"flower-1": null, "5-dot-red": "0p"}`.
`null` drops that class.

## 3. Add photos of *your* tiles (important)

Public datasets use other tile sets, tables and lighting, so accuracy on your set will be weaker until
you add your own images. Take 100-200 photos of hands on your table: vary lighting, angle and distance,
and include red fives. Label them in Roboflow or [Label Studio](https://labelstud.io), using our tile
ids (`1m`, `0p`, `7z`, ...) as label names. Export as YOLO and run `remap_dataset.py` on that export into
a second folder, or merge the two in Roboflow before downloading.

## 4. Train

```
python train.py --data datasets/riichi/data.yaml --epochs 100 --device 0
```

Use `--device cpu` without a GPU (slow). Results go to `runs/tiles/`; check `mAP50` at the end
(aim for > 0.95 on your own photos). Augmentation settings in `train.py` turn off flips and keep hue
shifts small so red fives stay distinguishable.

## 5. Export to the app

Ultralytics 8.4+ can only export TFLite on Linux/macOS, so on Windows export from a separate
environment pinned to 8.3 (one-time setup):

```
py -3.12 -m venv .venv-export
.venv-export\Scripts\pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
.venv-export\Scripts\pip install -r requirements-export.txt
```

Then:

```
.venv-export\Scripts\python export.py
```

This writes `tiles.tflite` and `labels.txt` into `android/app/src/main/assets/`. Rebuild the app and the
status chip shows "Model ready". The real model is now in use.
