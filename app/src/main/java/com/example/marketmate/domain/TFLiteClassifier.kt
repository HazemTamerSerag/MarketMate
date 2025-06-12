package com.example.marketmate.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class TFLiteClassifier(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private val inputSize = 224
    private val numClasses = 20

    private val labels = listOf(
        "freshapple", "freshbanana", "freshmango", "freshorange", "freshstrawberry",
        "rottenapple", "rottenbanana", "rottenmango", "rottenorange", "rottenstrawberry",
        "freshcarrot", "freshpotato", "freshtomato", "freshcucumber", "freshbellpepper",
        "rottencarrot", "rottenpotato", "rottentomato", "rottencucumber", "rottenbellpepper"
    )

    init {
        loadModel()
    }

    private fun loadModel() {
        val modelFile = loadModelFile("model.tflite")
        interpreter = Interpreter(modelFile)
        val inputShape = interpreter.getInputTensor(0).shape()  // لازم تكون [1, 224, 224, 3]
        val inputType = interpreter.getInputTensor(0).dataType()
        Log.d("ModelShape", inputShape.joinToString())
        Log.d("ModelType", inputType.name)


    }

    fun classifyImage(bitmap: Bitmap): Pair<String, Float> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = convertBitmapToByteBuffer(resized)
        val output = Array(1) { FloatArray(numClasses) }
        Log.d("TFLiteOutput", "Raw logits: ${output[0].joinToString()}")

        interpreter.run(input, output)
        val probabilities = softmax(output[0])

        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val label = labels[maxIndex]
        val confidence = probabilities[maxIndex]

        return Pair(label, confidence)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        // Create 3 arrays for channel-first format
        val r = FloatArray(inputSize * inputSize)
        val g = FloatArray(inputSize * inputSize)
        val b = FloatArray(inputSize * inputSize)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val rf = ((pixel shr 16) and 0xFF) / 255.0f
            val gf = ((pixel shr 8) and 0xFF) / 255.0f
            val bf = (pixel and 0xFF) / 255.0f

            r[i] = (rf - mean[0]) / std[0]
            g[i] = (gf - mean[1]) / std[1]
            b[i] = (bf - mean[2]) / std[2]
        }

        // Append in CHW format: all R, then G, then B
        r.forEach { buffer.putFloat(it) }
        g.forEach { buffer.putFloat(it) }
        b.forEach { buffer.putFloat(it) }

        return buffer

    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }

    private fun loadModelFile(modelName: String): File {
        val file = File(context.filesDir, modelName)
        if (!file.exists()) {
            context.assets.open(modelName).use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
        }
        return file
    }
}
