package com.example.marketmate.domain

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketmate.data.AnalysisResult
import com.example.marketmate.data.AnalysisResultType
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.data.checkAndRequestFeedbackPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class CameraScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val LOADING_DURATION = 2000L  // 2 seconds
        private const val RESULT_DURATION = 3000L   // 3 seconds
    }
    private var audioRecorder: MediaRecorder? = null
    private val context = application.applicationContext
    private var interpreter: Interpreter? = null
    private var isRecording = false // Track recording state

    // State management
    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())

    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible = _isSheetVisible.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("en")

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog = _showLoadingDialog.asStateFlow()

    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog = _showSuccessDialog.asStateFlow()

    private val _showFailedDialog = MutableStateFlow(false)
    val showFailedDialog = _showFailedDialog.asStateFlow()

    private val _finalAnalysisCount = MutableStateFlow(0) // Track confirmed analysis results

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog

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
                setLoadingDialog(true)
                // Ensure loading dialog shows for minimum duration
                val startTime = System.currentTimeMillis()

                // Analyze image and get confidence
                val confidence = analyzeFruit(bitmap)
                Log.d("Debug", "Analysis complete. Confidence: $confidence")
                // Ensure minimum loading time
                val analysisTime = System.currentTimeMillis() - startTime
                if (analysisTime < LOADING_DURATION) {
                    delay(LOADING_DURATION - analysisTime)
                }
                setLoadingDialog(false)
                // Set analysis result and show appropriate dialog
                if (confidence > CONFIDENCE_THRESHOLD) {
                    _showSuccessDialog.value = true
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false
                } else {
                    _showFailedDialog.value = true
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                }
            } catch (e: Exception) {
                Log.e("Debug", "Error in analysis process: ${e.message}")
                _showLoadingDialog.value = false
                _analysisResult.value = AnalysisResult(
                    type = AnalysisResultType.ERROR,
                    message = "Analysis failed: ${e.message}"
                )
            }
        }
    }

    // Update the analyzeFruit function to return a confidence value
    private fun analyzeFruit(bitmap: Bitmap): Float {
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

    // Add these functions to control dialog visibility
    private fun setLoadingDialog(show: Boolean) {
        _showLoadingDialog.value = show
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
        viewModelScope.launch {
            _selectedLanguage.value = language
            LanguageUtils.saveLanguage(activity, language)
            LanguageUtils.updateLocale(activity, language)
            hideSheet()
            withContext(Dispatchers.Main) {
                activity.recreate()
            }
        }
    }
    // Updated start recording function
    fun startFeedbackRecording(context: Context): File? {
        // Ensure permissions are granted
        if (!checkAndRequestFeedbackPermissions(context)) {
            Log.e("FeedbackRecording", "Permissions not granted")
            return null
        }

        // Create MediaRecorder
        val recorder = MediaRecorder()

        // Generate unique filename
        val audioFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "feedback_recording_${System.currentTimeMillis()}.mp3"
        )

        return try {
            // For Android versions below Q
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                }
            }
            // For Android Q and above
            else {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                }
            }

            // Prepare and start recording
            recorder.prepare()
            recorder.start()

            // Store recorder reference
            audioRecorder = recorder

            audioFile
        } catch (e: IOException) {
            Log.e("FeedbackRecording", "Error starting recording", e)

            // Clean up resources
            try {
                recorder.release()
            } catch (releaseEx: Exception) {
                Log.e("FeedbackRecording", "Error releasing recorder", releaseEx)
            }

            null
        } catch (e: SecurityException) {
            Log.e("FeedbackRecording", "Security exception during recording", e)
            null
        }
    }

    // Updated stop recording function
    fun stopFeedbackRecording() {
        audioRecorder?.let { recorder ->
            try {
                // Stop recording
                recorder.stop()
                isRecording = false

                // Release resources
                recorder.release()
            } catch (e: RuntimeException) {
                // Handle potential exceptions during stop
                Log.e("FeedbackRecording", "Error stopping recording", e)
            } finally {
                // Ensure recorder is nullified
                audioRecorder = null
            }
        }
    }

    // Updated send to WhatsApp function
    fun sendFeedbackToWhatsApp(context: Context, audioFile: File?) {
        audioFile ?: run {
            Log.e("FeedbackSend", "No audio file to send")
            return
        }

        // Verify file exists and is readable
        if (!audioFile.exists() || !audioFile.canRead()) {
            Log.e("FeedbackSend", "Audio file does not exist or is not readable")
            return
        }

        val phoneNumber = "201271669552"

        // Get a content URI using FileProvider
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            audioFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp3"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra("jid", "$phoneNumber@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            Log.e("FeedbackSend", "Error sending to WhatsApp", e)
        }
    }
    // Clean up resources
    override fun onCleared() {
        super.onCleared()
        interpreter?.close()
    }
}