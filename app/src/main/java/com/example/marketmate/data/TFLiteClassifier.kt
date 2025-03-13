package com.example.marketmate.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteClassifier(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private val inputSize = 224 // Model input size
    private val numClasses = 20 // Number of classes

    private val labels = listOf(
        "FreshApple", "FreshBanana", "FreshMango", "FreshOrange", "FreshStrawberry",
        "RottenApple", "RottenBanana", "RottenMango", "RottenOrange", "RottenStrawberry",
        "FreshCarrot", "FreshPotato", "FreshTomato", "FreshCucumber", "FreshBellpepper",
        "RottenCarrot", "RottenPotato", "RottenTomato", "RottenCucumber", "RottenBellpepper"
    )

    init {
        loadModel()
    }

    // Load the model from assets
    private fun loadModel() {
        try {
            Log.d("TFLiteClassifier", "Loading TFLite model...")
            val modelFile = loadModelFile("best_model.tflite")
            interpreter = Interpreter(modelFile)
            Log.d("TFLiteClassifier", "Model loaded successfully!")
        } catch (e: Exception) {
            Log.e("TFLiteClassifier", "Error loading model: ${e.message}")
        }
    }

    // Function to classify an image
    fun classifyImage(bitmap: Bitmap): Pair<String, Float> {
        Log.d("TFLiteClassifier", "Classifying image...")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val outputArray = Array(1) { FloatArray(numClasses) }
        interpreter.run(byteBuffer, outputArray)

        Log.d("TFLiteClassifier", "Raw Output: ${outputArray[0].joinToString()}")

        // Apply softmax
        val probabilities = outputArray[0].map { Math.exp(it.toDouble()).toFloat() }.toFloatArray()
        val sum = probabilities.sum()
        val normalized = probabilities.map { it / sum }

        // Get the predicted class and confidence
        val maxIndex = normalized.indices.maxByOrNull { normalized[it] } ?: -1
        val detectedClass = labels[maxIndex]
        val confidence = normalized[maxIndex]

        Log.d("TFLiteClassifier", "Predicted: $detectedClass (Confidence: $confidence)")

        return Pair(detectedClass, confidence)
    }

    // Convert bitmap to ByteBuffer
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        Log.d("TFLiteClassifier", "Converting image to ByteBuffer...")
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        Log.d("TFLiteClassifier", "Image converted to ByteBuffer successfully.")
        return byteBuffer
    }
    // Load TFLite model from assets
    private fun loadModelFile(modelName: String): File {
        val assetManager = context.assets
        val file = File(context.filesDir, modelName)

        if (!file.exists()) {
            val inputStream: InputStream = assetManager.open(modelName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        }
        return file
    }
}
