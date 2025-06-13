package com.example.marketmate.domain

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketmate.data.AnalysisResult
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.data.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale


class CameraScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CameraScreenVM"
        private const val RESULT_DURATION = 3000L
        private const val THANKS_DIALOG_DURATION = 5000L // 5 seconds for thanks dialog
        private const val WHATSAPP_NUMBER = "201271669552" // Egyptian number format
    }

    private val context = application.applicationContext
    private val tfLiteClassifier = TFLiteClassifier(context) // Offline TFLite classifier
    private var textToSpeech: TextToSpeech? = null
    private val apiService = RetrofitClient.apiService
    private val feedbackApiService = RetrofitClient.feedbackApiService

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        Log.d(TAG, "Device ID retrieved: $deviceId")
        return deviceId
    }

    private fun checkAndRequestFeedbackPermissions(): Boolean {
        Log.d(TAG, "Checking feedback permissions")
        return true
    }

    init {
        Log.d(TAG, "Initializing CameraScreenViewModel")
        textToSpeech = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TextToSpeech initialized successfully")
                textToSpeech?.language = Locale("ar")
                Log.d(TAG, "TextToSpeech language set to Arabic")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
            }
        }
        Log.d(TAG, "CameraScreenViewModel initialization completed")
    }

    private fun isInternetAvailable(context: Context): Boolean {
        Log.d(TAG, "Checking internet availability")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        if (network == null) {
            Log.d(TAG, "No active network found")
            return false
        }

        val capabilities = cm.getNetworkCapabilities(network)
        if (capabilities == null) {
            Log.d(TAG, "No network capabilities found")
            return false
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        Log.d(TAG, "Internet available: $hasInternet")
        return hasInternet
    }

    fun checkInternetAndSetMode(context: Context) {
        Log.d(TAG, "Checking internet and setting mode")
        val isOnline = isInternetAvailable(context)
        _isOnline.value = isOnline
        Log.d(TAG, "Mode set to: ${if (isOnline) "Online" else "Offline"}")
    }

    fun getDisplayText(): String {
        Log.d(TAG, "Getting display text")
        val displayText = if (_isOnline.value) {
            // Online mode - use server prediction with extra context
            val prediction = _serverPrediction.value
            if (prediction != null) {
                prediction
            } else {
                "Waiting for server analysis... Please hold the camera steady."
            }
        } else {
            // Offline mode - use detected item type and freshness state with confidence info
            val itemType = _detectedItemType.value
            if (itemType.isEmpty()) {
                "Point camera at fruit or vegetable and hold steady."
            } else {
                val freshStatus = if (_isFreshDetection.value) "Fresh" else "Rotten"
                "$freshStatus $itemType"
            }
        }
        Log.d(TAG, "Display text: $displayText")
        return displayText
    }

    private val _isOnline = MutableStateFlow(false)
    private var audioPlayer: MediaPlayer? = null
    private val _serverPrediction = MutableStateFlow<String?>(null)
    val serverPrediction = _serverPrediction.asStateFlow()
    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog = _showLoadingDialog.asStateFlow()
    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog = _showSuccessDialog.asStateFlow()
    private val _showFailedDialog = MutableStateFlow(false)
    val showFailedDialog = _showFailedDialog.asStateFlow()
    private val _showErrorDialog = MutableStateFlow(false)
    val showErrorDialog = _showErrorDialog.asStateFlow()
    private val _detectedItemType = MutableStateFlow("")
    private val _isFreshDetection = MutableStateFlow(true)
    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()
    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible: StateFlow<Boolean> = _isSheetVisible.asStateFlow()
    private val _selectedItem = MutableStateFlow("Apple")
    val selectedItem = _selectedItem.asStateFlow()
    private val availableItems = listOf(
        "Apple", "Banana", "Mango", "Orange", "Strawberry",
        "Carrot", "Potato", "Tomato", "Cucumber", "Bellpepper"
    )
    val items = availableItems
    private val _showThanksDialog = MutableStateFlow(false)
    val showThanksDialog: StateFlow<Boolean> = _showThanksDialog.asStateFlow()
    private val _selectedLanguage = MutableStateFlow("ar")
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog = _showFeedbackDialog.asStateFlow()
    private val _finalAnalysisCount = MutableStateFlow(0)
    private var audioRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null

    fun onTakePhoto(bitmap: Bitmap) {
        Log.d(TAG, "onTakePhoto called with bitmap size: ${bitmap.width}x${bitmap.height}")
        viewModelScope.launch {
            try {
                checkInternetAndSetMode(context)
                if (_isOnline.value) {
                    Log.d(TAG, "Using online mode - uploading to server")
                    uploadToServer(bitmap)
                } else {
                    Log.d(TAG, "Using offline mode - running local classification")
                    runLocalClassification(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onTakePhoto: ${e.message}", e)
                handleException()
            }
        }
    }

    private fun uploadToServer(bitmap: Bitmap) {
        Log.d(TAG, "Starting server upload process")
        viewModelScope.launch {
            try {
                setLoadingDialog(true)
                Log.d(TAG, "Converting bitmap to file")
                val file = bitmapToFile(bitmap)
                Log.d(TAG, "File created: ${file.name}, size: ${file.length()} bytes")

                val deviceIdBody = Build.ID.toRequestBody("text/plain".toMediaType())
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                Log.d(TAG, "Making API call to server")
                val response = apiService.uploadImage(deviceIdBody, imagePart)
                setLoadingDialog(false)
                Log.d(TAG, "API response received: ${response.code()}")

                if (response.isSuccessful) {
                    response.body()?.let { uploadResponse ->
                        Log.d("ServerResponse", "Prediction: '${uploadResponse.prediction}'")
                        Log.d("ServerResponse", "Audio file: '${uploadResponse.audio_file}'")

                        _serverPrediction.value = uploadResponse.prediction

                        if (uploadResponse.prediction.contains("Fresh", ignoreCase = true)) {
                            Log.d(TAG, "Fresh item detected - showing success dialog")
                            _showSuccessDialog.value = true
                            playAudioFromUrl(uploadResponse.audio_file)
                            delay(RESULT_DURATION)
                            _showSuccessDialog.value = false
                            Log.d(TAG, "Success dialog hidden")
                        } else if (uploadResponse.prediction.contains("Rotten", ignoreCase = true)) {
                            Log.d(TAG, "Rotten item detected - showing failed dialog")
                            _showFailedDialog.value = true
                            playAudioFromUrl(uploadResponse.audio_file)
                            delay(RESULT_DURATION)
                            _showFailedDialog.value = false
                            Log.d(TAG, "Failed dialog hidden")
                        } else {
                            Log.e("ServerResponse", "Unexpected prediction: '${uploadResponse.prediction}'")
                            playAudioFromUrl(uploadResponse.audio_file)
                            handleServerError()
                        }
                        incrementAndCheckFeedback()
                    } ?: run {
                        Log.e(TAG, "Response body is null")
                        handleException()
                    }
                } else {
                    Log.e(TAG, "Server response not successful: ${response.code()}")
                    handleException()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in uploadToServer: ${e.message}", e)
                handleException()
            }
        }
    }

    private suspend fun runLocalClassification(bitmap: Bitmap) {
        Log.d(TAG, "Starting local classification")
        try {
            setLoadingDialog(true)
            val preprocessedBitmap = preprocessBitmap(bitmap)

            Log.d(TAG, "Running TensorFlow Lite classification")
            val (className, confidence) = withContext(Dispatchers.Default) {
                tfLiteClassifier.classifyImage(preprocessedBitmap)
            }
            Log.d(TAG, "Classification result: $className with confidence: $confidence")

            val itemType = when {
                className.startsWith("fresh", ignoreCase = true) -> className.removePrefix("fresh").lowercase()
                className.startsWith("rotten", ignoreCase = true) -> className.removePrefix("rotten").lowercase()
                else -> className.lowercase()
            }
            val isFresh = className.startsWith("fresh", ignoreCase = true)

            Log.d(TAG, "Processed result - Item: $itemType, Fresh: $isFresh")

            if (confidence > 0.6f) {
                Log.d(TAG, "Confidence threshold met (${confidence} > 0.6)")
                _detectedItemType.value = itemType
                _isFreshDetection.value = isFresh
                _selectedItem.value = itemType

                if (isFresh) {
                    Log.d(TAG, "Showing success dialog for fresh item")
                    _showSuccessDialog.value = true
                    try {
                        playLocalAudio("Fresh${itemType.capitalize()}.mp3")
                        Log.d(TAG, "Playing fresh audio: Fresh${itemType.capitalize()}.mp3")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to play specific fresh audio, trying generic: ${e.message}")
                        playLocalAudio("${itemType}.mp3")
                    }
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false
                    Log.d(TAG, "Success dialog hidden")
                } else {
                    Log.d(TAG, "Showing failed dialog for rotten item")
                    _showFailedDialog.value = true
                    try {
                        playLocalAudio("Rotten${itemType.capitalize()}.mp3")
                        Log.d(TAG, "Playing rotten audio: Rotten${itemType.capitalize()}.mp3")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to play specific rotten audio, trying generic: ${e.message}")
                        playLocalAudio("${itemType}.mp3")
                    }
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                    Log.d(TAG, "Failed dialog hidden")
                }
            } else {
                Log.w(TAG, "Confidence threshold not met (${confidence} <= 0.6)")
                _showErrorDialog.value = true
                playLocalAudio("error.mp3")
                delay(RESULT_DURATION)
                _showErrorDialog.value = false
                Log.d(TAG, "Error dialog hidden")
            }

            if (preprocessedBitmap != bitmap) {
                preprocessedBitmap.recycle()
                Log.d(TAG, "Processed bitmap recycled")
            }

            incrementAndCheckFeedback()

        } catch (e: Exception) {
            Log.e(TAG, "Exception in runLocalClassification: ${e.message}", e)
            viewModelScope.launch {
                _showErrorDialog.value = true
                playLocalAudio("error.mp3")
                delay(RESULT_DURATION)
                _showErrorDialog.value = false
            }
        } finally {
            setLoadingDialog(false)
            Log.d(TAG, "Local classification completed")
        }
    }

    private fun preprocessBitmap(original: Bitmap): Bitmap {
        Log.d(TAG, "Preprocessing bitmap - original size: ${original.width}x${original.height}")
        val minDimension = Math.min(original.width, original.height)
        val xOffset = (original.width - minDimension) / 2
        val yOffset = (original.height - minDimension) / 2
        Log.d(TAG, "Crop parameters - minDimension: $minDimension, xOffset: $xOffset, yOffset: $yOffset")

        val centerCropped = if (original.width != original.height) {
            Log.d(TAG, "Cropping bitmap to square")
            Bitmap.createBitmap(original, xOffset, yOffset, minDimension, minDimension)
        } else {
            Log.d(TAG, "Bitmap is already square, no cropping needed")
            original
        }

        val resized = Bitmap.createScaledBitmap(centerCropped, 224, 224, true)
        Log.d(TAG, "Bitmap resized to 224x224")

        if (centerCropped != original && centerCropped != resized) {
            centerCropped.recycle()
            Log.d(TAG, "Center cropped bitmap recycled")
        }

        return resized
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        Log.d(TAG, "Converting bitmap to file")
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, true)
        Log.d(TAG, "Bitmap scaled for file conversion: ${scaledBitmap.width}x${scaledBitmap.height}")

        val file = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
            it.flush()
        }
        Log.d(TAG, "File created: ${file.absolutePath}, size: ${file.length()} bytes")
        return file
    }

    private fun incrementAndCheckFeedback() {
        _finalAnalysisCount.value++
        Log.d(TAG, "Analysis count incremented to: ${_finalAnalysisCount.value}")

        // Show feedback dialog after 5 responses
        if (_finalAnalysisCount.value >= 5) {
            Log.d(TAG, "Analysis count threshold reached, preparing feedback dialog")
            viewModelScope.launch {
                delay(5000) // Small delay after response completes
                Log.d(TAG, "Showing feedback dialog after delay")
                showFeedbackDialog()
                _finalAnalysisCount.value = 0 // Reset counter
                Log.d(TAG, "Analysis count reset to 0")
            }
        }
    }

    private fun handleServerError() {
        Log.e(TAG, "Handling server error")
        viewModelScope.launch {
            _showErrorDialog.value = true
            playLocalAudio("error.mp3")
            delay(RESULT_DURATION)
            _showErrorDialog.value = false
            Log.d(TAG, "Server error dialog sequence completed")
        }
    }

    private fun handleException() {
        Log.e(TAG, "Handling general exception")
        viewModelScope.launch {
            _showErrorDialog.value = true
            playLocalAudio("error.mp3")
            delay(RESULT_DURATION)
            _showErrorDialog.value = false
            Log.d(TAG, "Exception error dialog sequence completed")
        }
        setLoadingDialog(false)
    }

    private fun setLoadingDialog(show: Boolean) {
        Log.d(TAG, "Setting loading dialog: $show")
        _showLoadingDialog.value = show
    }

    private fun playAudioFromUrl(relativeUrl: String) {
        Log.d(TAG, "Playing audio from URL: $relativeUrl")
        try {
            audioPlayer?.release()
            val baseUrl = "http://192.168.1.186:8080/"
            val fullUrl = if (relativeUrl.startsWith("http")) relativeUrl else baseUrl + relativeUrl.removePrefix("/")
            Log.d(TAG, "Full audio URL: $fullUrl")

            audioPlayer = MediaPlayer().apply {
                setDataSource(fullUrl)
                setOnPreparedListener {
                    Log.d(TAG, "Audio prepared, starting playback")
                    start()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio playback error: what=$what, extra=$extra")
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio from URL: ${e.message}", e)
        }
    }

    private fun playLocalAudio(audioFileName: String) {
        Log.d(TAG, "Playing local audio: $audioFileName")
        try {
            audioPlayer?.release()
            audioPlayer = MediaPlayer().apply {
                context.assets.openFd("audio/$audioFileName").use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    setOnCompletionListener {
                        Log.d(TAG, "Local audio playback completed")
                        release()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Local audio playback error: what=$what, extra=$extra")
                        false
                    }
                    prepare()
                    start()
                    Log.d(TAG, "Local audio started playing")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play local audio '$audioFileName': ${e.message}", e)
        }
    }

    fun dismissFeedbackDialog() {
        Log.d(TAG, "Dismissing feedback dialog")
        _showFeedbackDialog.value = false
        _showThanksDialog.value = false

        // Stop recording if still active
        if (isRecording) {
            Log.d(TAG, "Stopping active recording")
            stopFeedbackRecording()
            isRecording = false
        }
        currentRecordingFile = null
        Log.d(TAG, "Feedback dialog dismissed")
    }

    fun showSheet() {
        Log.d(TAG, "Showing language selection sheet")
        _isSheetVisible.value = true
    }

    fun hideSheet() {
        Log.d(TAG, "Hiding language selection sheet")
        _isSheetVisible.value = false
    }

    fun setLanguage(language: String, activity: Activity) {
        Log.d(TAG, "Setting language to: $language")
        viewModelScope.launch {
            _selectedLanguage.value = language
            LanguageUtils.saveLanguage(activity, language)
            LanguageUtils.updateLocale(activity, language)
            hideSheet()
            Log.d(TAG, "Language settings saved, recreating activity")
            withContext(Dispatchers.Main) {
                activity.recreate()
            }
        }
    }

    fun showFeedbackDialog() {
        Log.d(TAG, "Showing feedback dialog")
        _showFeedbackDialog.value = true
        viewModelScope.launch {
            checkInternetAndSetMode(context)
            Log.d(TAG, "Starting feedback recording")
            startFeedbackRecording()
        }
    }

    fun startFeedbackRecording(): File? {
        Log.d(TAG, "Start recording - isOnline: ${_isOnline.value}")

        if (isRecording) {
            Log.w(TAG, "Already recording. Skipping duplicate call.")
            return null
        }

        if (!checkAndRequestFeedbackPermissions()) {
            Log.e(TAG, "Missing recording permissions")
            return null
        }

        val audioFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "feedback_${System.currentTimeMillis()}.m4a"
        )
        Log.d(TAG, "Audio file path: ${audioFile.absolutePath}")

        return try {
            audioRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            currentRecordingFile = audioFile
            Log.d(TAG, "Recording started successfully: ${audioFile.absolutePath}")
            audioFile
        } catch (e: Exception) {
            Log.e(TAG, "Recording start failed: ${e.message}", e)
            audioRecorder?.release()
            audioRecorder = null
            null
        }
    }

    fun stopFeedbackRecording(): File? {
        Log.d(TAG, "Stopping feedback recording")
        return try {
            audioRecorder?.stop()
            audioRecorder?.release()
            isRecording = false
            val file = currentRecordingFile
            Log.d(TAG, "Recording stopped successfully, file: ${file?.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording failed: ${e.message}", e)
            null
        } finally {
            audioRecorder = null
            isRecording = false
        }
    }

    fun submitFeedback(audioFile: File?) {
        val file = audioFile ?: currentRecordingFile
        if (file == null) {
            Log.e(TAG, "Submit feedback failed: file is null")
            return
        }

        Log.d(TAG, "Submitting feedback - File: ${file.name}, Online: ${_isOnline.value}")

        viewModelScope.launch {
            val success = if (_isOnline.value) {
                trySubmitFeedbackViaAPI(file)
            } else {
                false
            }

            if (!success) {
                Log.w(TAG, "API submission failed or offline, falling back to WhatsApp")
                sendFeedbackToWhatsApp(file)
            }

            showThanksDialog()
        }
    }

    private suspend fun trySubmitFeedbackViaAPI(file: File): Boolean {
        Log.d(TAG, "Attempting to submit feedback via API")
        return try {
            val deviceId = getDeviceId().toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull()) // Correct MIME for .m4a
            val audioPart = MultipartBody.Part.createFormData("voice_message", file.name, requestFile) // Correct part name

            Log.d(TAG, "Submitting audio feedback via API")
            val response = feedbackApiService.submitFeedback(deviceId, audioPart)

            if (response.isSuccessful) {
                Log.d(TAG, "Audio feedback submitted successfully")
            } else {
                Log.e(TAG, "Failed to submit audio feedback, code: ${response.code()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.localizedMessage}", e)
            false
        }
    }

    private fun showThanksDialog() {
        Log.d(TAG, "Showing thanks dialog")
        viewModelScope.launch {
            _showFeedbackDialog.value = false
            _showThanksDialog.value = true
            Log.d(TAG, "Thanks dialog shown, will hide after ${THANKS_DIALOG_DURATION}ms")
            delay(THANKS_DIALOG_DURATION)
            _showThanksDialog.value = false
            Log.d(TAG, "Thanks dialog hidden")
        }
    }

    fun sendFeedbackToWhatsApp(file: File?) {
        Log.d(TAG, "Attempting to send feedback to WhatsApp")
        if (file == null) {
            Log.e(TAG, "WhatsApp fallback failed: file is null")
            return
        }

        Log.d(TAG, "Creating WhatsApp intent for file: ${file.name}")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        Log.d(TAG, "File URI created: $uri")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp3"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$WHATSAPP_NUMBER@s.whatsapp.net") // رقم واتساب بصيغة دولية من غير "+"
            putExtra(Intent.EXTRA_TEXT, "تعليق صوتي على التطبيق")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            Log.d(TAG, "Launching WhatsApp intent")
            context.startActivity(intent)
            Log.d(TAG, "WhatsApp intent launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp intent failed: ${e.localizedMessage}", e)
        } finally {
            _showFeedbackDialog.value = false
            Log.d(TAG, "Feedback dialog dismissed after WhatsApp attempt")
        }
    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel onCleared called")
        super.onCleared()

        // Clean up TextToSpeech
        textToSpeech?.let {
            Log.d(TAG, "Shutting down TextToSpeech")
            it.stop()
            it.shutdown()
        }

        // Clean up MediaPlayer
        audioPlayer?.let {
            Log.d(TAG, "Releasing MediaPlayer")
            it.release()
        }

        // Clean up MediaRecorder
        audioRecorder?.let {
            Log.d(TAG, "Releasing MediaRecorder")
            try {
                if (isRecording) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder: ${e.message}", e)
            }
        }

        // Clean up current recording file
        currentRecordingFile?.let {
            Log.d(TAG, "Cleaning up current recording file: ${it.name}")
            if (it.exists()) {
                it.delete()
                Log.d(TAG, "Recording file deleted")
            }
        }

        Log.d(TAG, "ViewModel cleanup completed")
    }
}