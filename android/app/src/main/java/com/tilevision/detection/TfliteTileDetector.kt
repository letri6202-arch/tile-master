package com.tilevision.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.tilevision.model.Tile
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Runs a float32-input YOLO TFLite model (as produced by `ml/export.py`) on-device via LiteRT.
 */
class TfliteTileDetector private constructor(
    private val interpreter: Interpreter,
    private val classToTile: (Int) -> Tile?,
) : TileDetector, Closeable {

    private val inputSize: Int
    private val numClasses: Int
    private val numAnchors: Int
    private val channelsFirst: Boolean

    init {
        val input = interpreter.getInputTensor(0)
        require(input.dataType() == DataType.FLOAT32) {
            "Expected a float32 input model, got ${input.dataType()}. Export without int8 quantization."
        }
        // NHWC: [1, size, size, 3]
        inputSize = input.shape()[1]

        // [1, 4 + classes, anchors] or [1, anchors, 4 + classes]
        val outShape = interpreter.getOutputTensor(0).shape()
        channelsFirst = outShape[1] < outShape[2]
        numClasses = (if (channelsFirst) outShape[1] else outShape[2]) - 4
        numAnchors = if (channelsFirst) outShape[2] else outShape[1]
    }

    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder())
    private val outputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(4 * (4 + numClasses) * numAnchors).order(ByteOrder.nativeOrder())
    private val output = FloatArray((4 + numClasses) * numAnchors)
    private val pixels = IntArray(inputSize * inputSize)
    private val letterboxBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(letterboxBitmap)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    // The interpreter and buffers are reused, so live analysis and capture must not overlap.
    @Synchronized
    override fun detect(bitmap: Bitmap): List<Detection> {
        val letterbox = Letterbox(bitmap.width, bitmap.height, inputSize)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(
            bitmap,
            null,
            Rect(
                letterbox.padX,
                letterbox.padY,
                letterbox.padX + letterbox.scaledWidth,
                letterbox.padY + letterbox.scaledHeight,
            ),
            paint,
        )
        letterboxBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        inputBuffer.rewind()
        for (p in pixels) {
            inputBuffer.putFloat(((p shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((p shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((p and 0xFF) / 255f)
        }
        inputBuffer.rewind()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(output)

        return YoloDecoder.decode(
            output = output,
            numAnchors = numAnchors,
            numClasses = numClasses,
            letterbox = letterbox,
            classToTile = classToTile,
            channelsFirst = channelsFirst,
        )
    }

    override fun close() = interpreter.close()

    companion object {
        const val MODEL_ASSET = "tiles.tflite"
        const val LABELS_ASSET = "labels.txt"

        /**
         * Loads the model from assets, or returns null when no model has been added yet.
         * If `labels.txt` is present (one tile id per line, in class-index order) it defines the
         * class mapping; otherwise class indices are assumed to follow [Tile] declaration order.
         */
        fun create(context: Context, threads: Int = 4): TfliteTileDetector? {
            val assets = context.assets
            val names = assets.list("")?.toSet().orEmpty()
            if (MODEL_ASSET !in names) return null

            val model: MappedByteBuffer = assets.openFd(MODEL_ASSET).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }

            val classToTile: (Int) -> Tile? = if (LABELS_ASSET in names) {
                val labels = assets.open(LABELS_ASSET).bufferedReader().readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { Tile.fromId(it) }
                ({ index -> labels.getOrNull(index) })
            } else {
                { index -> Tile.fromClassIndex(index) }
            }

            val options = Interpreter.Options().setNumThreads(threads)
            return TfliteTileDetector(Interpreter(model, options), classToTile)
        }
    }
}
