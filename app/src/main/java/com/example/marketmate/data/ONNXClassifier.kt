package com.example.marketmate.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.FloatBuffer

class ONNXClassifier(private val context: Context, private val labels: List<String>) {
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private val inputSize = 224  // ResNet18 input size // Model input size


    private val numClasses = labels.size

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            Log.d("ONNXClassifier", "Starting ONNX model loading process...")
            val modelFile = loadModelFile("market_mate_2.onnx")
            Log.d("ONNXClassifier", "Model file loaded from assets: ${modelFile.absolutePath}")
            Log.d("ONNXClassifier", "Model file size: ${modelFile.length()} bytes")

            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.d("ONNXClassifier", "OrtEnvironment created successfully")

            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            Log.d("ONNXClassifier", "Session options configured with optimization level: ALL_OPT")

            ortSession = ortEnvironment.createSession(modelFile.absolutePath, sessionOptions)
            Log.d("ONNXClassifier", "ONNX Runtime session created successfully")

            val inputNames = ortSession.inputNames
            val outputNames = ortSession.outputNames
            Log.d("ONNXClassifier", "Model metadata:")
            Log.d("ONNXClassifier", "- Input names (${inputNames.size}): ${inputNames.joinToString()}")
            Log.d("ONNXClassifier", "- Output names (${outputNames.size}): ${outputNames.joinToString()}")

            // Get input node info
            val inputInfo = ortSession.inputInfo
            inputNames.forEach { name ->
                val info = inputInfo[name]
                Log.d("ONNXClassifier", "Input '$name' info:")
                Log.d("ONNXClassifier", "- Info: $info")
            }
        } catch (e: Exception) {
            Log.e("ONNXClassifier", "Error loading model: ${e.message}")
            Log.e("ONNXClassifier", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            throw e // Re-throw to ensure the error is properly handled
        }
    }

    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        return try {
            classifyImage(bitmap)
        } catch (e: Exception) {
            Log.e("ONNXClassifier", "Classification failed: ${e.message}")
            null
        }
    }

    internal fun classifyImage(bitmap: Bitmap): Pair<String, Float> {
        Log.d("ONNXClassifier", "Starting image classification process...")
        Log.d("ONNXClassifier", "Original image size: ${bitmap.width}x${bitmap.height}")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        Log.d("ONNXClassifier", "Image resized to: ${inputSize}x${inputSize}")
        val inputArray = convertBitmapToFloatArray(resizedBitmap)
        Log.d("ONNXClassifier", "Image converted to float array of size: ${inputArray.size}")

        try {
            val floatBufferInput = FloatBuffer.wrap(inputArray)
            Log.d("ONNXClassifier", "Creating ONNX tensor with shape: [1, 3, $inputSize, $inputSize]")

            // Get input names and info
            val inputName = ortSession.inputNames.firstOrNull() ?: throw IllegalStateException("No input nodes found in model")
            val inputInfo = ortSession.inputInfo[inputName] ?: throw IllegalStateException("No info found for input node: $inputName")
            Log.d("ONNXClassifier", "Using input node: $inputName")
            Log.d("ONNXClassifier", "Input info: $inputInfo")

            // Create and validate input tensor
            // Create input tensor with correct shape and data
            val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
            val tensorBuffer = FloatBuffer.wrap(inputArray)
            
            Log.d("ONNXClassifier", "Creating tensor with shape: ${shape.joinToString()}")
            Log.d("ONNXClassifier", "Tensor buffer size: ${tensorBuffer.capacity()}")
            
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                tensorBuffer,
                shape
            )
            inputTensor.use {
                if (!ortSession.inputNames.contains(inputName)) {
                    throw IllegalStateException("Model does not contain input name: $inputName. Available inputs: ${ortSession.inputNames.joinToString()}")
                }

                Log.d("ONNXClassifier", "Running ONNX inference...")
                val inputs = mapOf(inputName to inputTensor)
                Log.d("ONNXClassifier", "Input tensor map created with key: $inputName")

                val output = ortSession.run(inputs)
                Log.d("ONNXClassifier", "Inference completed successfully")

                val outputTensor = output[0].value as Array<FloatArray>
                val logits = outputTensor[0]
                
                // Apply softmax
                val maxLogit = logits.maxOrNull() ?: 0f
                val expValues = logits.map { Math.exp((it - maxLogit).toDouble()) }
                val expSum = expValues.sum()
                val probabilities = expValues.map { (it / expSum).toFloat() }

                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
                val detectedClass = labels[maxIndex]
                val confidence = probabilities[maxIndex]

                Log.d("ONNXClassifier", "Prediction results:")
                Log.d("ONNXClassifier", "- Detected class: $detectedClass")
                Log.d("ONNXClassifier", "- Confidence: ${confidence * 100}%")
                Log.d("ONNXClassifier", "- Top 3 predictions:")
                val top3 = probabilities.indices.sortedByDescending { probabilities[it] }.take(3)
                top3.forEach { idx ->
                    Log.d("ONNXClassifier", "  ${labels[idx]}: ${probabilities[idx] * 100}%")
                }
                return Pair(detectedClass, confidence)
            }
        } catch (e: Exception) {
            Log.e("ONNXClassifier", "Error during classification: ${e.message}")
            Log.e("ONNXClassifier", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            throw e
        }
    }

    private fun convertBitmapToFloatArray(bitmap: Bitmap): FloatArray {
        Log.d("ONNXClassifier", "Starting image to float array conversion...")

        // Resize bitmap to 224x224 (same as PyTorch model)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val pixels = IntArray(224 * 224)
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        val floatArray = FloatArray(3 * 224 * 224)

        // ImageNet mean and std values for normalization (same as PyTorch)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        // Convert to RGB float values and normalize using ImageNet stats
        // Process each channel separately (R, G, B) in PyTorch order
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = pixels[y * 224 + x]
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                // Store in CHW format (PyTorch format)
                floatArray[0 * 224 * 224 + y * 224 + x] = (r - mean[0]) / std[0]
                floatArray[1 * 224 * 224 + y * 224 + x] = (g - mean[1]) / std[1]
                floatArray[2 * 224 * 224 + y * 224 + x] = (b - mean[2]) / std[2]
            }
        }

        // Log sample values for debugging
        Log.d("ONNXClassifier", "Sample normalized values:")
        Log.d("ONNXClassifier", "R: ${floatArray[0]}, G: ${floatArray[224 * 224]}, B: ${floatArray[2 * 224 * 224]}")
        Log.d("ONNXClassifier", "Float array size: ${floatArray.size}")

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return floatArray
    }

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