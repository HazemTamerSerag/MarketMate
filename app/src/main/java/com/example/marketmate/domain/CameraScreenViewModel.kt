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
import com.example.marketmate.data.AnalysisResultType
import com.example.marketmate.data.ApiService
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.data.TFLiteClassifier
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
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class CameraScreenViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CameraScreenVM"
        private const val RESULT_DURATION = 3000L
    }

    private val context = application.applicationContext
    private val classifier = TFLiteClassifier(context) // Offline TFLite classifier

    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("ar")
            }
        }
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    object RetrofitClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.127:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService: ApiService = retrofit.create(ApiService::class.java)
    }

    private val apiService = RetrofitClient.apiService

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

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


    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible = _isSheetVisible.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("en")

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog = _showFeedbackDialog.asStateFlow()
    private val _finalAnalysisCount = MutableStateFlow(0)

    private var audioRecorder: MediaRecorder? = null
    private var isRecording = false

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
                        // Add this logging to see what you're actually getting
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
                }
            }catch (e: Exception) {

                handleException()
            }
        }
    }
    private suspend fun runLocalClassification(bitmap: Bitmap) {
        try {
            setLoadingDialog(true)
            val startTime = System.currentTimeMillis()

            val (fruitType, confidence) = classifier.classifyImage(bitmap)
            val analysisTime = System.currentTimeMillis() - startTime
            if (RESULT_DURATION > analysisTime) delay(RESULT_DURATION - analysisTime)

            setLoadingDialog(false)

            if (fruitType.contains("Fresh", ignoreCase = true) && confidence >= 0.97f) {
                _showSuccessDialog.value = true
                if (!isOnline.value) speakText("النتيجة: طازجة")
                delay(RESULT_DURATION)
                _showSuccessDialog.value = false
            } else if (fruitType.contains("Rotten", ignoreCase = true) && confidence < 0.97f) {
                _showFailedDialog.value = true
                if (!isOnline.value) speakText("النتيجة: تالفة")
                delay(RESULT_DURATION)
                _showFailedDialog.value = false
            }else {
                handleServerError()
            }
            incrementAndCheckFeedback()
        } catch (e: Exception) {
            handleException()
        }
    }

    private fun incrementAndCheckFeedback() {
        _finalAnalysisCount.value++
        if (_finalAnalysisCount.value >= 5) {
            _showFeedbackDialog.value = true
            _finalAnalysisCount.value = 0
        }
    }

    private fun handleServerError() {
        viewModelScope.launch {
            _showErrorDialog.value = true

            delay(RESULT_DURATION)
            _showErrorDialog.value = false
        }
    }

    private fun handleException() {
        viewModelScope.launch {
            _showErrorDialog.value = true
            delay(RESULT_DURATION)
            _showErrorDialog.value = false
        }
        setLoadingDialog(false)
    }

    private fun setLoadingDialog(show: Boolean) {
        _showLoadingDialog.value = show
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, true)
        val file = File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
            it.flush()
        }
        return file
    }

    private fun playAudioFromUrl(relativeUrl: String) {
        try {
            audioPlayer?.release()
            val baseUrl = "http://192.168.1.127:8080/"
            val fullUrl = if (relativeUrl.startsWith("http")) relativeUrl else baseUrl + relativeUrl.removePrefix("/")
            audioPlayer = MediaPlayer().apply {
                setDataSource(fullUrl)
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}")
        }
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
        _showFeedbackDialog.value = true
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
    }
    private fun checkAndRequestFeedbackPermissions(context: Context): Boolean {
        return true
    }
    fun startFeedbackRecording(context: Context): File? {
        if (!checkAndRequestFeedbackPermissions(context)) return null

        val recorder = MediaRecorder()
        val audioFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "feedback_recording_${System.currentTimeMillis()}.mp3"
        )

        return try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            audioRecorder = recorder
            isRecording = true
            audioFile
        } catch (e: IOException) {
            recorder.release()
            Log.e(TAG, "Failed to start recording: ${e.message}")
            null
        }
    }

    fun stopFeedbackRecording() {
        audioRecorder?.let {
            try {
                it.stop()
                it.release()
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to stop recording: ${e.message}")
            }
        }
        audioRecorder = null
        isRecording = false
    }


    fun sendFeedbackToWhatsApp(context: Context, audioFile: File?) {
        audioFile ?: return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            audioFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp3"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "201271669552@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
            _showFeedbackDialog.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send feedback via WhatsApp: ${e.message}")
        }
    }
    override fun onCleared() {
        super.onCleared()
        audioPlayer?.release()
    }
}
