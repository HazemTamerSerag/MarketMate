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
import com.example.marketmate.data.TFLiteClassifier
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
import java.io.IOException

class CameraScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOADING_DURATION = 2000L  // 2 seconds
        private const val RESULT_DURATION = 3000L   // 3 seconds
    }
    private var audioRecorder: MediaRecorder? = null
    private val context = application.applicationContext
    private val classifier = TFLiteClassifier(context)
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

    // Update onTakePhoto function
    fun onTakePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _bitmaps.value += bitmap
                setLoadingDialog(true)
                // Ensure loading dialog shows for minimum duration
                val startTime = System.currentTimeMillis()
                // Analyze image and get confidence
                val (fruitType, confidence) = classifier.classifyImage(bitmap)
                Log.d("Result", "Detected: $fruitType with confidence: ${confidence * 100}%")

                // Ensure minimum loading time
                val analysisTime = System.currentTimeMillis() - startTime
                if (analysisTime < LOADING_DURATION) {
                    delay(LOADING_DURATION - analysisTime)
                }
                setLoadingDialog(false)
                // Set analysis result and show appropriate dialog
                if (fruitType.contains("Fresh") && confidence >= 0.97) {
                    _showSuccessDialog.value = true
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false
                    _finalAnalysisCount.value++
                    if (_finalAnalysisCount.value >= 5) {
                        _showFeedbackDialog.value = true
                        _finalAnalysisCount.value = 0 // Reset counter after triggering dialog
                    }
                } else {
                    _showFailedDialog.value = true
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                    _finalAnalysisCount.value++
                    if (_finalAnalysisCount.value >= 5) {
                        _showFeedbackDialog.value = true
                        _finalAnalysisCount.value = 0 // Reset counter after triggering dialog
                    }

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