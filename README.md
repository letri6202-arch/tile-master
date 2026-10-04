# Tile Vision

An Android app that reads a Riichi mahjong hand through the camera and (eventually) scores it.
Tile detection runs fully on-device with a YOLO model in TFLite (LiteRT), so it works offline at the table.

```
android/   Kotlin + Jetpack Compose app (CameraX, LiteRT)
ml/        Python pipeline to train and export the tile detector
```

## Getting started

1. Install [Android Studio](https://developer.android.com/studio). It bundles the JDK and Android SDK.
2. On your phone, enable **Developer options** (tap *Build number* 7 times) and turn on **USB debugging**.
3. In Android Studio, **Open** the `android/` folder and let Gradle sync.
4. Plug in your phone, select it in the device dropdown, and press **Run**.

Until a model is trained, the app uses a **demo detector** that draws a fixed, fake hand,
so you can try the camera, capture and review flow right away.

Run unit tests from `android/`:

```
./gradlew testDebugUnitTest
```

## How it works

1. **Camera** (`camera/CameraScreen.kt`): a CameraX preview. A throttled analyzer draws a live box overlay.
2. **Scan**: captures a full-resolution photo and runs the detector on it.
3. **Detection** (`detection/TfliteTileDetector.kt`): letterboxes the photo to 640x640, runs YOLO,
   and decodes the boxes with class-agnostic NMS (`YoloDecoder.kt`).
4. **Review** (`ui/HandReviewScreen.kt`): tiles are sorted left to right into a hand. Tap any tile to
   fix it, add a missed tile, or remove an extra one. The hand is shown in Riichi notation
   (e.g. `123m406p789s11155z`) and sanity-checked (tile count, at most 4 copies, at most 1 red five per suit).

## Tile classes

37 classes in this order (shared by `model/Tile.kt` and `ml/classes.py`):
`1m-9m`, `1p-9p`, `1s-9s`, `1z-7z` (East, South, West, North, Haku, Hatsu, Chun), `0m 0p 0s` (red fives).

## Roadmap

- [x] Project scaffold, camera preview, capture, live overlay
- [x] Demo detector + hand review/correction UI
- [x] TFLite detector + YOLO decoding (waiting on a trained model)
- [ ] Train the tile model (see `ml/README.md`) and drop it into `android/app/src/main/assets/`
- [ ] Hit 14/14 correct on photos of your own tiles in normal lighting
- [ ] GPU delegate for faster live detection
- [ ] Hand context: called melds (pon/chi/kan), winning tile, tsumo/ron, dora indicators, seat/round wind, riichi
- [ ] Scoring: hand decomposition, yaku, han/fu, points (Kotlin, tested against known hands)
