package com.tilemaster

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tilemaster.detection.Detection
import com.tilemaster.detection.FakeTileDetector
import com.tilemaster.detection.TfliteTileDetector
import com.tilemaster.detection.TileDetector
import com.tilemaster.model.Hand
import com.tilemaster.model.Tile
import com.tilemaster.scoring.HandContext
import com.tilemaster.scoring.MAX_DORA_INDICATORS
import com.tilemaster.scoring.Meld
import com.tilemaster.scoring.MeldType
import com.tilemaster.scoring.PythonScorer
import com.tilemaster.scoring.QuestionStep
import com.tilemaster.scoring.ScoreOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

/** What a photo is for: the hand itself, or the dora / ura dora indicators. */
enum class CaptureTarget { HAND, DORA, URA_DORA }

sealed interface Screen {
    data object Home : Screen
    data class Camera(val target: CaptureTarget = CaptureTarget.HAND) : Screen
    data object Review : Screen
    data class Questions(val step: QuestionStep) : Screen
    data class Result(val outcome: ScoreOutcome) : Screen
}

/** The photo of the hand and what was detected in it. */
data class Scan(val photo: Bitmap, val detections: List<Detection>)

class ScannerViewModel(app: Application) : AndroidViewModel(app) {

    val detector: TileDetector = runCatching { TfliteTileDetector.create(app) }
        .onFailure { Log.e("ScannerViewModel", "Could not load tile model; using demo detector", it) }
        .getOrNull()
        ?: FakeTileDetector()

    private val scorer = PythonScorer(app)

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var scan by mutableStateOf<Scan?>(null)
        private set

    /** The hand being reviewed. Starts as the detected tiles and is edited by the user. */
    var tiles by mutableStateOf<List<Tile>>(emptyList())
        private set

    /** Answers to the question flow for the current hand. */
    var handContext by mutableStateOf(HandContext())
        private set

    init {
        // Python takes a moment to start; do it while the player is still scanning.
        viewModelScope.launch { scorer.warmUp() }
    }

    fun onPhotoCaptured(photo: Bitmap) {
        if (isProcessing) return
        val target = (screen as? Screen.Camera)?.target ?: return
        isProcessing = true
        viewModelScope.launch {
            val ordered = withContext(Dispatchers.Default) {
                Hand.orderLeftToRight(detector.detect(photo))
            }
            val found = ordered.map { it.tile }
            when (target) {
                CaptureTarget.HAND -> {
                    scan = Scan(photo, ordered)
                    tiles = found
                    // Seat and round wind usually carry over to the next hand.
                    handContext = HandContext(seatWind = handContext.seatWind, roundWind = handContext.roundWind)
                    screen = Screen.Review
                }
                CaptureTarget.DORA -> {
                    updateContext { it.copy(doraIndicators = (it.doraIndicators + found).take(MAX_DORA_INDICATORS)) }
                    screen = Screen.Questions(QuestionStep.DORA)
                }
                CaptureTarget.URA_DORA -> {
                    updateContext { it.copy(uraDoraIndicators = (it.uraDoraIndicators + found).take(MAX_DORA_INDICATORS)) }
                    screen = Screen.Questions(QuestionStep.URA_DORA)
                }
            }
            isProcessing = false
        }
    }

    fun replaceTile(index: Int, tile: Tile) {
        tiles = tiles.toMutableList().also { it[index] = tile }
        // A meld that held the old tile may no longer be valid.
        updateContext { ctx -> ctx.copy(melds = ctx.melds.filterNot { index in it.indices }) }
    }

    fun removeTile(index: Int) {
        tiles = tiles.toMutableList().also { it.removeAt(index) }
        // Positions after the removed tile shift, so marked melds and the winning tile are stale.
        updateContext { it.copy(melds = emptyList(), winIndex = null) }
    }

    fun addTile(tile: Tile) {
        tiles = tiles + tile
    }

    fun startScoring() {
        retake()
    }

    fun goHome() {
        scan = null
        tiles = emptyList()
        screen = Screen.Home
    }

    fun retake() {
        scan = null
        tiles = emptyList()
        screen = Screen.Camera()
    }

    fun startQuestions() {
        goToStep(QuestionStep.MELDS)
    }

    fun updateContext(transform: (HandContext) -> HandContext) {
        handContext = transform(handContext).sanitized()
    }

    /** Adds a meld over the tiles at [indices], or returns why it can't. */
    fun addMeld(type: MeldType, indices: List<Int>): String? {
        if (indices.any { it in handContext.meldIndices }) return "A tile can only be in one meld."
        Meld.problem(type, indices.map { tiles[it] })?.let { return it }
        updateContext { it.copy(melds = it.melds + Meld(type, indices.sorted())) }
        return null
    }

    fun removeMeld(meld: Meld) {
        updateContext { it.copy(melds = it.melds - meld) }
    }

    fun goToStep(step: QuestionStep) {
        if (step == QuestionStep.WINNING_TILE && handContext.winIndex == null) {
            // Players usually set the winning tile apart at the end of the row.
            val lastFree = tiles.indices.lastOrNull { it !in handContext.meldIndices }
            updateContext { it.copy(winIndex = lastFree) }
        }
        screen = Screen.Questions(step)
    }

    fun nextStep() {
        val step = (screen as? Screen.Questions)?.step ?: return
        if (step.blocker(handContext, tiles) != null) return
        val next = QuestionStep.next(step, handContext)
        if (next == null) score() else goToStep(next)
    }

    fun previousStep() {
        val step = (screen as? Screen.Questions)?.step ?: return
        val previous = QuestionStep.previous(step, handContext)
        screen = if (previous == null) Screen.Review else Screen.Questions(previous)
    }

    fun scanIndicators(ura: Boolean) {
        screen = Screen.Camera(if (ura) CaptureTarget.URA_DORA else CaptureTarget.DORA)
    }

    fun closeIndicatorCamera() {
        val target = (screen as? Screen.Camera)?.target ?: return
        screen = Screen.Questions(if (target == CaptureTarget.URA_DORA) QuestionStep.URA_DORA else QuestionStep.DORA)
    }

    private fun score() {
        if (isProcessing) return
        isProcessing = true
        viewModelScope.launch {
            val outcome = scorer.score(tiles, handContext)
            screen = Screen.Result(outcome)
            isProcessing = false
        }
    }

    fun editAnswers() {
        screen = Screen.Questions(QuestionStep.SUMMARY)
    }

    override fun onCleared() {
        (detector as? Closeable)?.close()
    }
}
