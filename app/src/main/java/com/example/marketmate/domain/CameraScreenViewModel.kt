package com.example.marketmate.domain

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketmate.presentation.screen.stopFeedbackRecording
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale

// Enum for different analysis results
enum class AnalysisResultType {
    SUITABLE,
    NOT_SUITABLE,
    ERROR
}

// Data class for analysis result
data class AnalysisResult(
    val type: AnalysisResultType,
    val message: String,
    val confidence: Float? = null
)

class CameraScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _finalAnalysisCount = MutableStateFlow(0) // Track confirmed analysis results
    val finalAnalysisCount: StateFlow<Int> = _finalAnalysisCount

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog

    private val context = application.applicationContext
    private var interpreter: Interpreter? = null
    var isRecording = false // Track recording state

    init {
        loadModel()
    }
    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd("project_graduation.tflite")
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(modelBuffer)
            Log.d("Debug", "TFLite Model Loaded Successfully")
        } catch (e: Exception) {
            Log.e("Debug", "Error loading model: ${e.message}", e)
        }
    }

    // Bitmap storage
    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    // Language selection
    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible: StateFlow<Boolean> = _isSheetVisible

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    // Analysis states
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    // Add these constants at the top of the class
    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.55f
        private const val LOADING_DURATION = 2000L  // 2 seconds minimum loading time
        private const val RESULT_DURATION = 3000L   // 3 seconds for result dialogs
    }

    // Add these state flows at the top of your ViewModel class
    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog = _showLoadingDialog.asStateFlow()

    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog = _showSuccessDialog.asStateFlow()

    private val _showFailedDialog = MutableStateFlow(false)
    val showFailedDialog = _showFailedDialog.asStateFlow()

    // Add these functions to control dialog visibility
    fun setLoadingDialog(show: Boolean) {
        _showLoadingDialog.value = show
    }

    fun setSuccessDialog(show: Boolean) {
        _showSuccessDialog.value = show
    }

    fun setFailedDialog(show: Boolean) {
        _showFailedDialog.value = show
    }

    // Update the analyzeFruit function to return a confidence value
    fun analyzeFruit(bitmap: Bitmap): Float {
        return try {
            if (interpreter == null) {
                Log.e("Debug", "Interpreter is null!")
                return 0f
            }

            // Get model's input shape
            val inputShape = interpreter!!.getInputTensor(0).shape()
            val modelInputSize = inputShape[1]

            // Resize bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true)

            // Process image
            val inputBuffer = if (interpreter!!.getInputTensor(0).dataType() == org.tensorflow.lite.DataType.UINT8) {
                convertBitmapToByteBuffer(resizedBitmap, modelInputSize)
            } else {
                convertBitmapToByteBufferFloat32(resizedBitmap, modelInputSize)
            }

            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val numClasses = outputShape[1]
            val outputArray = Array(1) { FloatArray(numClasses) }

            interpreter!!.run(inputBuffer, outputArray)

            // Get the confidence value
            val confidence = outputArray[0].maxOrNull() ?: 0f
            Log.d("Debug", "Confidence value: $confidence")
            
            confidence
        } catch (e: Exception) {
            Log.e("Debug", "Error during classification: ${e.message}", e)
            0f
        }
    }

    // ✅ **Helper function to convert Bitmap to ByteBuffer (UINT8 Format)**
    private fun convertBitmapToByteBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(size * size * 3) // UINT8 (1 byte per channel)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(size * size)
        bitmap.getPixels(intValues, 0, size, 0, 0, size, size)

        for (pixel in intValues) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            byteBuffer.put(r.toByte())
            byteBuffer.put(g.toByte())
            byteBuffer.put(b.toByte())
        }
        return byteBuffer
    }

    // ✅ **Alternative: Convert Bitmap to ByteBuffer (FLOAT32 Format)**
    private fun convertBitmapToByteBufferFloat32(bitmap: Bitmap, size: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(size * size * 3 * 4) // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(size * size)
        bitmap.getPixels(intValues, 0, size, 0, 0, size, size)

        for (pixel in intValues) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }

    fun resetAnalysisResult() {
        _analysisResult.value = null
    }

    fun showSheet() {
        _isSheetVisible.value = true
    }

    fun hideSheet() {
        _isSheetVisible.value = false
    }
    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
        if (isRecording) {
            stopFeedbackRecording()
        }
    }


    fun setLanguage(language: String, activity: Activity) {
        _selectedLanguage.value = language
        saveLanguageToPreferences(activity, language)
        updateLocale(activity, language)
        hideSheet()
        activity.runOnUiThread {
            activity.recreate()
        }
    }

    private fun saveLanguageToPreferences(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("language", language).apply()
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getCurrentLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("language", "en") ?: "en"
    }

    // Update onTakePhoto function
    fun onTakePhoto(bitmap: Bitmap) {

        viewModelScope.launch {
            try {
                _bitmaps.value += bitmap
                _finalAnalysisCount.value++
                if (_finalAnalysisCount.value >= 5) {
                    _showFeedbackDialog.value = true
                    _finalAnalysisCount.value = 0 // Reset counter after triggering dialog
                }
                // Start loading dialog
                _showLoadingDialog.value = true
                val startTime = System.currentTimeMillis()
                
                // Analyze image
                val confidence = analyzeFruit(bitmap)
                Log.d("Debug", "Analysis complete. Confidence: $confidence")
                
                // Ensure loading shows for at least LOADING_DURATION
                val analysisTime = System.currentTimeMillis() - startTime
                if (analysisTime < LOADING_DURATION) {
                    delay(LOADING_DURATION - analysisTime)
                }
                
                // Hide loading dialog
                _showLoadingDialog.value = false
                
                // Show result dialog
                if (confidence > CONFIDENCE_THRESHOLD) {
                    Log.d("Analysis", "Good quality detected! Confidence: $confidence")
                    _showSuccessDialog.value = true
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false

                } else {
                    Log.d("Analysis", "Poor quality detected! Confidence: $confidence")
                    _showFailedDialog.value = true
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                }
                
            } catch (e: Exception) {
                Log.e("Debug", "Error in analysis process: ${e.message}")
                // Ensure loading shows for at least LOADING_DURATION even in case of error
                delay(LOADING_DURATION)
                _showLoadingDialog.value = false
                
                // Show failed dialog for errors
                _showFailedDialog.value = true
                delay(RESULT_DURATION)
                _showFailedDialog.value = false
            }
        }
    }
}