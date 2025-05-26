package com.example.marketmate.domain

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import com.example.marketmate.data.ONNXClassifier
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
        private const val MAX_RECORDING_DURATION = 7000L // 7 seconds for feedback recording
        private const val THANKS_DIALOG_DURATION = 5000L // 5 seconds for thanks dialog
        private const val WHATSAPP_NUMBER = "201271669552" // Egyptian number format
    }

    private val context = application.applicationContext
    private val modelClasses = listOf(
        "FreshApple", "FreshBanana", "FreshMango", "FreshOrange", "FreshStrawberry",
        "RottenApple", "RottenBanana", "RottenMango", "RottenOrange", "RottenStrawberry",
        "FreshCarrot", "FreshPotato", "FreshTomato", "FreshCucumber", "FreshBellpepper",
        "RottenCarrot", "RottenPotato", "RottenTomato", "RottenCucumber", "RottenBellpepper"
    )
    private val classifier = ONNXClassifier(context, modelClasses) // Offline ONNX classifier
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

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun checkInternetAndSetMode(context: Context) {
        _isOnline.value = isInternetAvailable(context)
    }

    /**
     * Get display text for the dialog based on online/offline mode
     * Optimized for blind users with clear, descriptive text
     */
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

    /**
     * Get the latest classification result
     */
    fun getLatestResult(): String {
        val itemType = _detectedItemType.value
        return if (itemType.isEmpty()) {
            "No classification yet"
        } else {
            val freshStatus = if (_isFreshDetection.value) "Fresh" else "Rotten"
            "$freshStatus $itemType"
        }
    }


    object RetrofitClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.127:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService: ApiService = retrofit.create(ApiService::class.java)
    }


    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline
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

    // Item type and freshness for UI display
    private val _detectedItemType = MutableStateFlow("")
    val detectedItemType = _detectedItemType.asStateFlow()
    private val _isFreshDetection = MutableStateFlow(true)
    val isFreshDetection = _isFreshDetection.asStateFlow()
    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()
    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible: StateFlow<Boolean> = _isSheetVisible.asStateFlow()

    // Item selection state
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
    private var recordingStartTime = 0L
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
            Log.d(TAG, "Using offline ResNet18 ONNX classifier with multiple strategies")
            
            Log.d(TAG, "Input image size: ${bitmap.width}x${bitmap.height}")
            
            // DIAGNOSTIC: Log model classes to verify they match
            Log.d(TAG, "Model classes: ${modelClasses.joinToString(", ")}")
            
            // Use multiple image processing strategies to improve accuracy
            val allResults = mutableListOf<Pair<String, Float>>()

            // Strategy 1: Standard center crop and resize
            val standardProcessed = preprocessBitmap(bitmap)
            val standardResult = withContext(Dispatchers.Default) {
                classifier.classify(standardProcessed)
            }
            standardResult?.let { allResults.add(it) }
            Log.d(TAG, "Strategy 1 result: $standardResult")

            // Strategy 2: Adjust brightness up slightly
            try {
                val brighterBitmap = adjustBrightness(standardProcessed, 1.2f)
                val brighterResult = withContext(Dispatchers.Default) {
                    classifier.classify(brighterBitmap)
                }
                brighterResult?.let { allResults.add(it) }
                Log.d(TAG, "Strategy 2 result: $brighterResult")
                brighterBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error in strategy 2: ${e.message}")
            }

            // Strategy 3: Adjust brightness down slightly
            try {
                val darkerBitmap = adjustBrightness(standardProcessed, 0.8f)
                val darkerResult = withContext(Dispatchers.Default) {
                    classifier.classify(darkerBitmap)
                }
                darkerResult?.let { allResults.add(it) }
                Log.d(TAG, "Strategy 3 result: $darkerResult")
                darkerBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error in strategy 3: ${e.message}")
            }

            // Strategy 4: Try with a slightly different crop (more padding)
            try {
                val paddedCropBitmap = preprocessWithPadding(bitmap, 0.9f)
                val paddedResult = withContext(Dispatchers.Default) {
                    classifier.classify(paddedCropBitmap)
                }
                paddedResult?.let { allResults.add(it) }
                Log.d(TAG, "Strategy 4 result: $paddedResult")
                paddedCropBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Error in strategy 4: ${e.message}")
            }

            // Count class frequencies
            val classCounts = mutableMapOf<String, Int>()
            val classConfidences = mutableMapOf<String, Float>()

            // Process results with additional emphasis on rotten items
            allResults.forEach { (className, confidence) ->
                // Only consider results with reasonable confidence
                if (confidence > 0.15f) { // Lower threshold for more potential matches
                    // Give a slight boost to rotten items to help with rotten detection
                    val adjustedConfidence = if (className.startsWith("Rotten", ignoreCase = true)) {
                        confidence * 1.15f  // 15% boost for rotten items
                    } else {
                        confidence
                    }
                    classCounts[className] = (classCounts[className] ?: 0) + 1
                    classConfidences[className] = (classConfidences[className] ?: 0f) + adjustedConfidence
                }
            }

            // Calculate average confidence per class
            classConfidences.forEach { (className, totalConfidence) ->
                classConfidences[className] = totalConfidence / (classCounts[className] ?: 1)
            }

            // IMPORTANT: Check if any "Rotten" class has reasonable confidence
            // This helps detect rotten items even if they're not the top confidence
            val topRottenClass = classConfidences.entries
                .filter { it.key.startsWith("Rotten", ignoreCase = true) && it.value > 0.3f }
                .maxByOrNull { it.value }

            // Use rotten class if available with good confidence
            if (topRottenClass != null) {
                Log.d(TAG, "Found potential rotten item: ${topRottenClass.key} with ${topRottenClass.value * 100}%")
            }

            // Log all candidates
            Log.d(TAG, "Classification candidates after multiple strategies:")
            classConfidences.entries.sortedByDescending { it.value }.forEach { (className, avgConfidence) ->
                Log.d(TAG, "  * $className: ${avgConfidence * 100}% (found ${classCounts[className]} times)")
            }

            // Select final result with rotten item prioritization
            val result = if (topRottenClass != null) {
                // Prioritize rotten detection if it has good confidence
                Pair(topRottenClass.key, topRottenClass.value)
            } else {
                // Otherwise use standard approach - first by frequency, then by confidence
                val bestClass = classCounts.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                        .thenByDescending { classConfidences[it.key] ?: 0f })
                    .firstOrNull()?.key

                val bestConfidence = classConfidences[bestClass] ?: 0f

                if (bestClass != null && bestConfidence > 0) {
                    Pair(bestClass, bestConfidence)
                } else if (allResults.isNotEmpty()) {
                    // Fallback to best single result if aggregation failed
                    allResults.maxByOrNull { it.second }!!
                } else {
                    null
                }
            }

            Log.d(TAG, "Final selected result: $result")

            if (result != null) {
                val (detectedClass, confidence) = result

                // Extract item type and freshness from classification result
                val detectedItemType = detectedClass.replace("Fresh", "").replace("Rotten", "")
                val isFresh = detectedClass.startsWith("Fresh", ignoreCase = true)

                // Log detailed results
                Log.d(TAG, "===============================")
                Log.d(TAG, "Offline Classification Result:")
                Log.d(TAG, "- Raw detection: $detectedClass")
                Log.d(TAG, "- Item type: $detectedItemType")
                Log.d(TAG, "- Freshness: ${if (isFresh) "Fresh" else "Rotten"}")
                Log.d(TAG, "- Confidence: ${confidence * 100}%")

                // DIAGNOSTIC: Check for model class match
                val classExists = modelClasses.any { it.equals(detectedClass, ignoreCase = true) }
                Log.d(TAG, "- Class exists in model: $classExists")
                Log.d(TAG, "===============================")

                // Update state for UI display
                _detectedItemType.value = detectedItemType
                _isFreshDetection.value = isFresh

                // Also update selected item to match what was detected
                // This helps blind users know what was detected
                _selectedItem.value = detectedItemType

                // Show appropriate dialog and play corresponding audio
                if (isFresh) {
                    _showSuccessDialog.value = true
                    playLocalAudio("Fresh${detectedItemType}.mp3")
                    delay(RESULT_DURATION)
                    _showSuccessDialog.value = false
                } else {
                    _showFailedDialog.value = true
                    playLocalAudio("Rotten${detectedItemType}.mp3")
                    delay(RESULT_DURATION)
                    _showFailedDialog.value = false
                }

                // Cleanup
                if (standardProcessed != bitmap) {
                    standardProcessed.recycle()
                }
            } else {
                // Classification failed
                Log.e(TAG, "Offline classification failed - model returned null")
                viewModelScope.launch {
                    _showErrorDialog.value = true
                    playLocalAudio("error.mp3")
                    delay(RESULT_DURATION)
                    _showErrorDialog.value = false
                }
            }

            incrementAndCheckFeedback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in offline classification: ${e.message}")
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

    /**
     * Preprocess bitmap exactly like PyTorch model
     * Matches preprocessing from PyTorch training code:
     * transforms.Resize((224, 224)),
     * transforms.ToTensor(),
     * transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
     */
    private fun preprocessBitmap(original: Bitmap): Bitmap {
        Log.d(TAG, "Preprocessing image ${original.width}x${original.height}")

        // IMPORTANT: Center crop the image first (just like PyTorch's default transforms)
        // This helps ensure the subject is centered like in the training data
        val minDimension = Math.min(original.width, original.height)
        val xOffset = (original.width - minDimension) / 2
        val yOffset = (original.height - minDimension) / 2

        // Create a square centered crop
        val centerCropped = if (original.width != original.height) {
            try {
                val cropped = Bitmap.createBitmap(original, xOffset, yOffset, minDimension, minDimension)
                Log.d(TAG, "Center cropped to ${cropped.width}x${cropped.height}")
                cropped
            } catch (e: Exception) {
                Log.e(TAG, "Error cropping: ${e.message}, using original")
                original
            }
        } else {
            original
        }

        // Step 1: Resize to exactly 224x224 like PyTorch
        val resized = Bitmap.createScaledBitmap(centerCropped, 224, 224, true)
        Log.d(TAG, "Resized to ${resized.width}x${resized.height}")

        // Note: The ONNXClassifier already handles normalization steps internally
        // with the same ImageNet means and stds: [0.485, 0.456, 0.406], [0.229, 0.224, 0.225]
        // So we only need to resize here

        // Cleanup temporary bitmap if we created one
        if (centerCropped != original && centerCropped != resized) {
            centerCropped.recycle()
        }

        return resized
    }

    /**
     * Preprocess with different padding factor for more robust classification
     */
    private fun preprocessWithPadding(original: Bitmap, scaleFactor: Float): Bitmap {
        // Use a slightly different crop area with padding
        val minDimension = Math.min(original.width, original.height)
        val targetDimension = (minDimension * scaleFactor).toInt() // Slightly smaller crop

        val xOffset = (original.width - targetDimension) / 2
        val yOffset = (original.height - targetDimension) / 2

        // Create a square centered crop with padding
        val centerCropped = try {
            val cropped = Bitmap.createBitmap(original, xOffset, yOffset, targetDimension, targetDimension)
            Log.d(TAG, "Padded crop to ${cropped.width}x${cropped.height} with factor $scaleFactor")
            cropped
        } catch (e: Exception) {
            Log.e(TAG, "Error padded cropping: ${e.message}, using original")
            original
        }

        // Resize to 224x224
        val resized = Bitmap.createScaledBitmap(centerCropped, 224, 224, true)

        // Cleanup if needed
        if (centerCropped != original && centerCropped != resized) {
            centerCropped.recycle()
        }

        return resized
    }

    /**
     * Adjust the brightness of a bitmap
     * @param original The bitmap to adjust
     * @param factor Brightness factor: >1 for brighter, <1 for darker
     */
    private fun adjustBrightness(original: Bitmap, factor: Float): Bitmap {
        // Create a mutable copy of the bitmap
        // Handle potential null config by using ARGB_8888 as default
        val config = original.config ?: Bitmap.Config.ARGB_8888
        val result = original.copy(config, true)

        // Apply brightness adjustment using a color matrix
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setScale(factor, factor, factor, 1f) // RGB scaling
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        // Draw the bitmap with the adjusted brightness
        val canvas = Canvas(result)
        canvas.drawBitmap(result, 0f, 0f, paint)

        Log.d(TAG, "Applied brightness factor: $factor")
        return result
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

    private fun playAudioFromUrl(relativeUrl: String) {
        try {
            audioPlayer?.release()
            val baseUrl = "http://192.168.1.127:8080/"
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
        .baseUrl("http://192.168.1.127:8080/")
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
            file
        )

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
