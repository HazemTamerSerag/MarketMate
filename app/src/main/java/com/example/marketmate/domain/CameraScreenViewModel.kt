package com.example.marketmate.domain

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
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketmate.data.AnalysisResult
import com.example.marketmate.data.ApiService
import com.example.marketmate.data.FeedbackApiService
import com.example.marketmate.data.LanguageUtils
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
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    private fun getDeviceId(): String = Build.ID
    private fun checkAndRequestFeedbackPermissions(): Boolean = true
    init {
        textToSpeech = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("ar")
            }
        }
    }
    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun checkInternetAndSetMode(context: Context) {
        _isOnline.value = isInternetAvailable(context)
    }
    fun getDisplayText(): String {
        return if (_isOnline.value) {
            // Online mode - use server prediction with extra context
            val prediction = _serverPrediction.value
            if (prediction != null) {
                "Server detected: $prediction"
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
                "Detected: $freshStatus $itemType"
            }
        }
    }
    object RetrofitClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        private val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService: ApiService = retrofit.create(ApiService::class.java)
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
        viewModelScope.launch {
            checkInternetAndSetMode(context)
            if (_isOnline.value) {
                uploadToServer(bitmap)
            } else {
                runLocalClassification(bitmap)
            }
        }
    }
    private fun uploadToServer(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                setLoadingDialog(true)
                val file = bitmapToFile(bitmap)
                val deviceIdBody = Build.ID.toRequestBody("text/plain".toMediaType())
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                val response = apiService.uploadImage(deviceIdBody, imagePart)
                setLoadingDialog(false)

                if (response.isSuccessful) {
                    response.body()?.let { uploadResponse ->
                        Log.d("ServerResponse", "Prediction: '${uploadResponse.prediction}'")
                        Log.d("ServerResponse", "Audio file: '${uploadResponse.audio_file}'")

                        _serverPrediction.value = uploadResponse.prediction

                        if (uploadResponse.prediction.contains("Fresh", ignoreCase = true)) {
                            _showSuccessDialog.value = true
                            playAudioFromUrl(uploadResponse.audio_file)
                            delay(RESULT_DURATION)
                            _showSuccessDialog.value = false
                        } else if (uploadResponse.prediction.contains(
                                "Rotten",
                                ignoreCase = true
                            )
                        ) {
                            _showFailedDialog.value = true
                            playAudioFromUrl(uploadResponse.audio_file)
                            delay(RESULT_DURATION)
                            _showFailedDialog.value = false
                        } else {
                            Log.e(
                                "ServerResponse",
                                "Unexpected prediction: '${uploadResponse.prediction}'"
                            )
                            playAudioFromUrl(uploadResponse.audio_file)
                            handleServerError()
                        }
                        incrementAndCheckFeedback()
                    }
                } else {
                    handleException()
                }
            } catch (e: Exception) {
                handleException()
            }
        }
    }
    private suspend fun runLocalClassification(bitmap: Bitmap) {
        try {
            setLoadingDialog(true)

            val processedBitmap = preprocessBitmap(bitmap)

            val (className, confidence) = withContext(Dispatchers.Default) {
                tfLiteClassifier.classifyImage(processedBitmap)
            }

            val itemType = when {
                className.startsWith("fresh", ignoreCase = true) -> className.removePrefix("fresh").lowercase()
                className.startsWith("rotten", ignoreCase = true) -> className.removePrefix("rotten").lowercase()
                else -> className.lowercase()
            }
            val isFresh = className.startsWith("fresh", ignoreCase = true)

            if (confidence > 0.6f) {
                _detectedItemType.value = itemType
                _isFreshDetection.value = isFresh
                _selectedItem.value = itemType

                if (isFresh) {
                    _showSuccessDialog.value = true
                    try {
                        playLocalAudio("Fresh${itemType.capitalize()}.mp3")
                    } catch (e: Exception) {
                        playLocalAudio("${itemType}.mp3")
                    }
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false
                } else {
                    _showFailedDialog.value = true
                    try {
                        playLocalAudio("Rotten${itemType.capitalize()}.mp3")
                    } catch (e: Exception) {
                        playLocalAudio("${itemType}.mp3")
                    }
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                }
            } else {
                _showErrorDialog.value = true
                playLocalAudio("error.mp3")
                delay(RESULT_DURATION)
                _showErrorDialog.value = false
            }

            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }

            incrementAndCheckFeedback()

        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _showErrorDialog.value = true
                playLocalAudio("error.mp3")
                delay(RESULT_DURATION)
                _showErrorDialog.value = false
            }
        } finally {
            setLoadingDialog(false)
        }
    }
    private fun preprocessBitmap(original: Bitmap): Bitmap {
        val minDimension = Math.min(original.width, original.height)
        val xOffset = (original.width - minDimension) / 2
        val yOffset = (original.height - minDimension) / 2

        val centerCropped = if (original.width != original.height) {
            Bitmap.createBitmap(original, xOffset, yOffset, minDimension, minDimension)
        } else original

        val resized = Bitmap.createScaledBitmap(centerCropped, 224, 224, true)

        if (centerCropped != original && centerCropped != resized) {
            centerCropped.recycle()
        }

        return resized
    }
    private fun bitmapToFile(bitmap: Bitmap): File {
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, true)
        val file = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
            it.flush()
        }
        return file
    }
    private fun incrementAndCheckFeedback() {
        _finalAnalysisCount.value++
        Log.d(TAG, "Analysis count: ${_finalAnalysisCount.value}")

        // Show feedback dialog after 5 responses
        if (_finalAnalysisCount.value >= 5) {
            viewModelScope.launch {
                delay(5000) // Small delay after response completes
                showFeedbackDialog()
                _finalAnalysisCount.value = 0 // Reset counter
            }
        }
    }
    private fun handleServerError() {
        viewModelScope.launch {
            _showErrorDialog.value = true
            playLocalAudio("error.mp3")
            delay(RESULT_DURATION)
            _showErrorDialog.value = false
        }
    }
    private fun handleException() {
        viewModelScope.launch {
            _showErrorDialog.value = true
            playLocalAudio("error.mp3")
            delay(RESULT_DURATION)
            _showErrorDialog.value = false
        }
        setLoadingDialog(false)
    }
    private fun setLoadingDialog(show: Boolean) {
        _showLoadingDialog.value = show
    }
    private fun playAudioFromUrl(relativeUrl: String) {
        try {
            audioPlayer?.release()
            val baseUrl = "http://127.0.0.1:8080/"
            val fullUrl =
                if (relativeUrl.startsWith("http")) relativeUrl else baseUrl + relativeUrl.removePrefix(
                    "/"
                )
            audioPlayer = MediaPlayer().apply {
                setDataSource(fullUrl)
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}")
        }
    }
    private fun playLocalAudio(audioFileName: String) {
        try {
            audioPlayer?.release()
            audioPlayer = MediaPlayer().apply {
                context.assets.openFd("audio/$audioFileName").use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    setOnCompletionListener { release() }
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play local audio: ${e.message}")
            e.printStackTrace()
        }
    }
    fun dismissFeedbackDialog() {
        Log.d(TAG, "Dismissing feedback dialog")
        _showFeedbackDialog.value = false
        _showThanksDialog.value = false

        // Stop recording if still active
        if (isRecording) {
            stopFeedbackRecording()
            isRecording = false
        }
        currentRecordingFile = null
    }
    fun showSheet() {
        _isSheetVisible.value = true
    }
    fun hideSheet() {
        _isSheetVisible.value = false
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
    fun showFeedbackDialog() {
        Log.d(TAG, "Showing feedback dialog")
        _showFeedbackDialog.value = true
        viewModelScope.launch {
            checkInternetAndSetMode(context)
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
            Log.d(TAG, "Recording started: ${audioFile.absolutePath}")
            audioFile
        } catch (e: Exception) {
            Log.e(TAG, "Recording error: ${e.message}")
            audioRecorder?.release()
            audioRecorder = null
            null
        }
    }
    fun stopFeedbackRecording(): File? {
        Log.d(TAG, "Stopping recording")
        return try {
            audioRecorder?.stop()
            audioRecorder?.release()
            isRecording = false
            val file = currentRecordingFile
            Log.d(TAG, "Recording stopped, file: ${file?.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording failed: ${e.message}")
            null
        } finally {
            audioRecorder = null
            isRecording = false
        }
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val feedbackApiService = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FeedbackApiService::class.java)
    fun submitFeedback(audioFile: File?) {
        val file = audioFile ?: currentRecordingFile ?: run {
            Log.e(TAG, "No audio file to submit")
            return
        }

        Log.d(TAG, "Submitting feedback - File: ${file.name}, Online: ${_isOnline.value}")
        viewModelScope.launch {
            if (_isOnline.value) {
                trySubmitFeedbackViaAPI(file)
            } else {
                Log.d(TAG, "Device is offline, switching to offline analysis using ONNX model")
                sendFeedbackToWhatsApp(file)
            }
            showThanksDialog()
        }
    }
    private suspend fun trySubmitFeedbackViaAPI(file: File): Boolean {
        return try {
            val deviceId = getDeviceId()
            val audioPart = MultipartBody.Part.createFormData(
                "voice_message", file.name,
                file.asRequestBody("audio/m4a".toMediaTypeOrNull())
            )

            val idPart = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())


            val response = feedbackApiService.submitFeedback(idPart, audioPart)

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d(TAG, "API Response: ${responseBody}")
                val feedbackId = responseBody?.feedback_ID ?: "N/A"
                Log.d(TAG, "Feedback sent successfully - ID: $feedbackId")
                file.delete()
                return true
            } else {
                val errorMsg = response.errorBody()?.string()
                Log.e(TAG, "API Error (${response.code()}): $errorMsg")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.localizedMessage}")
            return false
        }
    }
    private fun showThanksDialog() {
        viewModelScope.launch {
            _showFeedbackDialog.value = false
            _showThanksDialog.value = true
            delay(THANKS_DIALOG_DURATION)
            _showThanksDialog.value = false
        }
    }
    fun sendFeedbackToWhatsApp(file: File?) {
        if (file == null) {
            Log.e(TAG, "WhatsApp fallback failed: file is null")
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp3"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$WHATSAPP_NUMBER@s.whatsapp.net") // رقم واتساب بصيغة دولية من غير "+"
            putExtra(Intent.EXTRA_TEXT, "تعليق صوتي على التطبيق")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp intent failed: ${e.localizedMessage}")
        } finally {
            _showFeedbackDialog.value = false
        }
    }
}