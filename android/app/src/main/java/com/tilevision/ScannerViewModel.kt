package com.tilevision

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tilevision.detection.Detection
import com.tilevision.detection.FakeTileDetector
import com.tilevision.detection.TfliteTileDetector
import com.tilevision.detection.TileDetector
import com.tilevision.model.Hand
import com.tilevision.model.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

sealed interface Screen {
    data object Camera : Screen
    data class Review(val photo: Bitmap, val detections: List<Detection>) : Screen
}

class ScannerViewModel(app: Application) : AndroidViewModel(app) {

    val detector: TileDetector = runCatching { TfliteTileDetector.create(app) }
        .onFailure { Log.e("ScannerViewModel", "Could not load tile model; using demo detector", it) }
        .getOrNull()
        ?: FakeTileDetector()

    var screen by mutableStateOf<Screen>(Screen.Camera)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    /** The hand being reviewed. Starts as the detected tiles and is edited by the user. */
    var tiles by mutableStateOf<List<Tile>>(emptyList())
        private set

    fun onPhotoCaptured(photo: Bitmap) {
        if (isProcessing) return
        isProcessing = true
        viewModelScope.launch {
            val ordered = withContext(Dispatchers.Default) {
                Hand.orderLeftToRight(detector.detect(photo))
            }
            tiles = ordered.map { it.tile }
            screen = Screen.Review(photo, ordered)
            isProcessing = false
        }
    }

    fun replaceTile(index: Int, tile: Tile) {
        tiles = tiles.toMutableList().also { it[index] = tile }
    }

    fun removeTile(index: Int) {
        tiles = tiles.toMutableList().also { it.removeAt(index) }
    }

    fun addTile(tile: Tile) {
        tiles = tiles + tile
    }

    fun retake() {
        tiles = emptyList()
        screen = Screen.Camera
    }

    override fun onCleared() {
        (detector as? Closeable)?.close()
    }
}
