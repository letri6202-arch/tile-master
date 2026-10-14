package com.tilemaster.scoring

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.tilemaster.model.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface Scorer {
    suspend fun score(tiles: List<Tile>, context: HandContext): ScoreOutcome
}

/** Scores on-device by calling `scorer.score` in the embedded Python runtime. */
class PythonScorer(appContext: Context) : Scorer {

    private val appContext = appContext.applicationContext
    private val lock = Mutex()
    private var module: PyObject? = null

    /** Starts Python and imports the scorer, which takes a moment the first time. */
    suspend fun warmUp() {
        runCatching { module() }.onFailure { Log.e(TAG, "Could not start the scorer", it) }
    }

    override suspend fun score(tiles: List<Tile>, context: HandContext): ScoreOutcome =
        withContext(Dispatchers.Default) {
            try {
                val request = ScoreRequest.toJson(tiles, context)
                val response = module().callAttr("score", request).toString()
                ScoreOutcome.fromJson(response, isTsumo = context.isTsumo == true)
            } catch (e: Exception) {
                Log.e(TAG, "Scoring failed", e)
                ScoreOutcome.Failed("Something went wrong while scoring: ${e.message}")
            }
        }

    private suspend fun module(): PyObject = withContext(Dispatchers.Default) {
        lock.withLock {
            module ?: run {
                if (!Python.isStarted()) Python.start(AndroidPlatform(appContext))
                Python.getInstance().getModule("scorer").also { module = it }
            }
        }
    }

    private companion object {
        const val TAG = "PythonScorer"
    }
}
