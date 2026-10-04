package com.tilemaster.detection

import android.graphics.Bitmap

interface TileDetector {
    /** True when results are canned demo data rather than real model output. */
    val isDemo: Boolean get() = false

    /** Returns detected tiles with boxes normalized to 0..1 of [bitmap]. Blocking; call off the main thread. */
    fun detect(bitmap: Bitmap): List<Detection>
}
