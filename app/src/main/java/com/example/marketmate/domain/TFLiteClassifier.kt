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
        Log.d("TFLiteClassifier", "Initializing TFLiteClassifier")
        loadModel()
    }

    private fun loadModel() {
        Log.d("TFLiteClassifier", "Starting model loading process")
        try {
            val modelFile = loadModelFile("model.tflite")
            interpreter = Interpreter(modelFile)
            val inputShape = interpreter.getInputTensor(0).shape()
            val inputType = interpreter.getInputTensor(0).dataType()
            Log.d("TFLiteClassifier", "Model loaded successfully")
            Log.d("TFLiteClassifier", inputShape.joinToString())
            Log.d("TFLiteClassifier", inputType.name)
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Failed to load model: ${e.message}", e)
            throw e
        }
    }

    fun classifyImage(bitmap: Bitmap): Pair<String, Float> {
        Log.d("TFLiteClassifier", "Starting image classification")
        Log.d("TFLiteClassifier", "Input bitmap size: ${bitmap.width}x${bitmap.height}")

        try {
            val input = convertBitmapToByteBuffer(bitmap)
            val output = Array(1) { FloatArray(numClasses) }

            Log.d("TFLiteClassifier", "Running interpreter...")
            interpreter.run(input, output)
            Log.d("TFLiteClassifier", "Raw logits: ${output[0].joinToString()}")

            val probabilities = softmax(output[0])
            Log.d("TFLiteClassifier", "Softmax probabilities calculated")

            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val label = labels[maxIndex]
            val confidence = probabilities[maxIndex]

            Log.d("TFLiteClassifier", "Classification result: $label with confidence: $confidence")
            return Pair(label, confidence)
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Classification failed: ${e.message}", e)
            throw e
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        Log.d("TFLiteClassifier", "Converting bitmap to ByteBuffer with ImageNet normalization and CHW format")
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        // CHW format requires separating channels
        val rChannel = FloatArray(224 * 224)
        val gChannel = FloatArray(224 * 224)
        val bChannel = FloatArray(224 * 224)

        for (i in intValues.indices) {
            val pixelValue = intValues[i]
            rChannel[i] = (((pixelValue shr 16 and 0xFF) / 255.0f) - mean[0]) / std[0]
            gChannel[i] = (((pixelValue shr 8 and 0xFF) / 255.0f) - mean[1]) / std[1]
            bChannel[i] = (((pixelValue and 0xFF) / 255.0f) - mean[2]) / std[2]
        }

        // Write channels to ByteBuffer in CHW order
        rChannel.forEach { byteBuffer.putFloat(it) }
        gChannel.forEach { byteBuffer.putFloat(it) }
        bChannel.forEach { byteBuffer.putFloat(it) }

        Log.d("TFLiteClassifier", "ByteBuffer conversion completed")
        return byteBuffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        Log.d("TFLiteClassifier", "Calculating softmax")
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        val result = exps.map { it / sum }.toFloatArray()
        Log.d("TFLiteClassifier", "Softmax calculation completed")
        return result
    }

    private fun loadModelFile(modelName: String): File {
        Log.d("TFLiteClassifier", "Loading model file: $modelName")
        val file = File(context.filesDir, modelName)
        if (!file.exists()) {
            Log.d("TFLiteClassifier", "Model file doesn't exist, copying from assets")
            context.assets.open(modelName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                    Log.d("TFLiteClassifier", "Model file copied successfully")
                }
            }
        } else {
            Log.d("TFLiteClassifier", "Model file already exists")
        }
        return file
    }
}