@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.marketmate.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.marketmate.R
import com.example.marketmate.domain.AnalysisResultType
import com.example.marketmate.domain.CameraScreenViewModel
import com.example.marketmate.presentation.components.camera.CameraPreview
import com.example.marketmate.presentation.components.dialogs.FailedDialog
import com.example.marketmate.presentation.components.dialogs.FeedbackDialog
import com.example.marketmate.presentation.components.dialogs.LoadingDialog
import com.example.marketmate.presentation.components.dialogs.SuccessDialog
import com.example.marketmate.presentation.components.dialogs.ThanksDialog
import com.example.marketmate.presentation.components.sheet.LanguageSelector
import com.example.marketmate.presentation.theme.MarketMateTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

const val FEEDBACK_PERMISSION_REQUEST_CODE = 200
private var audioRecorder: MediaRecorder? = null
class CameraScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedLanguage()
        requestCameraPermissionIfNeeded()
        setContent {
            MarketMateTheme {
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                    }
                }
                val viewModel = remember { CameraScreenViewModel(application) }
                val isSheetVisible by viewModel.isSheetVisible.collectAsState()
                val selectedLanguage by viewModel.selectedLanguage.collectAsState()

                MainScreenContent(
                    controller = controller,
                    viewModel = viewModel,
                    isSheetVisible = isSheetVisible,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { language -> viewModel.setLanguage(language, this) }
                )
            }
        }
    }
    private fun requestCameraPermissionIfNeeded() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
    }
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    private fun applySavedLanguage() {
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onPhotoTaken: (Bitmap) -> Unit
) {
    Log.d("CameraDebug", "Taking photo...")
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                Log.d("CameraDebug", "Photo captured successfully")
                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )
                onPhotoTaken(rotatedBitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Couldn't take photo: ", exception)
            }
        }
    )
}


// Updated permission check function
fun checkAndRequestFeedbackPermissions(context: Context): Boolean {
    // Define required permissions based on Android version
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

    // Add storage permission for versions below Android 10
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val missingPermissions = permissions.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    return if (missingPermissions.isNotEmpty()) {
        // Ensure context is an Activity before requesting permissions
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                missingPermissions.toTypedArray(),
                FEEDBACK_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            Log.e("PermissionCheck", "Context is not an Activity. Cannot request permissions.")
            false
        }
    } else {
        true
    }
}

// Updated start recording function
fun CameraScreenViewModel.startFeedbackRecording(context: Context): File? {
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
fun CameraScreenViewModel.stopFeedbackRecording() {
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
fun CameraScreenViewModel.sendFeedbackToWhatsApp(context: Context, audioFile: File?) {
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


@Composable
fun UpperPartPreview(
    viewModel: CameraScreenViewModel,
    selectedLanguage: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showFeedbackRequestDialog by remember { mutableStateOf(false) }
    var showFeedbackSuccessDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var job: Job? by remember { mutableStateOf(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Language Button
        IconButton(onClick = { viewModel.showSheet() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Default.Translate,
                    contentDescription = stringResource(R.string.translate),
                    tint = Color.White
                )
            }
        }

        // Logo
        Icon(
            modifier = Modifier
                .size(56.dp)
                .align(alignment = Alignment.CenterVertically),
            painter = painterResource(R.drawable.marketmatelogo3x),
            contentDescription = stringResource(R.string.market_mate_logo),
            tint = Color.White
        )

        // Feedback Button
        IconButton(onClick = {
            if (checkAndRequestFeedbackPermissions(context)) {
                showFeedbackRequestDialog = true
                isRecording = true

                job = coroutineScope.launch {
                    // Start recording
                    val audioFile = viewModel.startFeedbackRecording(context)

                    // Record for 5 seconds
                    delay(5000)

                    // Stop recording
                    viewModel.stopFeedbackRecording()
                    isRecording = false
                    showFeedbackRequestDialog = false

                    // Send to WhatsApp
                    viewModel.sendFeedbackToWhatsApp(context, audioFile)

                    // Show success dialog
                    showFeedbackSuccessDialog = true
                    delay(3000)
                    showFeedbackSuccessDialog = false
                }
            }
        }) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Default.Feedback,
                contentDescription = stringResource(R.string.feedback),
                tint = Color.White
            )
        }
    }

    if (showFeedbackRequestDialog) {
        FeedbackDialog(
            onDismiss = {
                showFeedbackRequestDialog = false
                if (isRecording) {
                    viewModel.stopFeedbackRecording()
                    isRecording = false
                }
                job?.cancel() // Cancels WhatsApp sending if dismissed
            },
            onSend = {
                job = coroutineScope.launch {
                    val audioFile = viewModel.startFeedbackRecording(context)
                    delay(5000)
                    viewModel.stopFeedbackRecording()
                    isRecording = false
                    viewModel.sendFeedbackToWhatsApp(context, audioFile)
                }
            }
        )
    }

    // Feedback Success Dialog
    if (showFeedbackSuccessDialog) {
        ThanksDialog(onDismiss = {
            showFeedbackSuccessDialog = false
        })
    }
}
@Composable
fun CameraViewfinderOverlay() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val innerBoxWidth = 335.dp.toPx()
        val innerBoxHeight = 335.dp.toPx()
        val innerBoxLeft = (size.width - innerBoxWidth) / 2
        val innerBoxTop = (size.height - innerBoxHeight) / 2

        // Outer area overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Clear inner area
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(innerBoxLeft, innerBoxTop),
            size = Size(innerBoxWidth, innerBoxHeight),
            blendMode = BlendMode.Clear
        )

        // Inner area light overlay
        drawRect(
            color = Color.White.copy(alpha = 0.0f),
            topLeft = Offset(innerBoxLeft, innerBoxTop),
            size = Size(innerBoxWidth, innerBoxHeight)
        )
    }
}

@Composable
fun ViewfinderCorners() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 6f
        val cornerLength = 160f
        val width = size.width
        val height = size.height

        // Draw all corners
        drawViewfinderCorner(0f, 0f, cornerLength, true, true)
        drawViewfinderCorner(width - cornerLength, 0f, cornerLength, false, true)
        drawViewfinderCorner(0f, height - cornerLength, cornerLength, true, false)
        drawViewfinderCorner(width - cornerLength, height - cornerLength, cornerLength, false, false)
    }
}

fun DrawScope.drawViewfinderCorner(
    x: Float,
    y: Float,
    length: Float,
    isLeft: Boolean,
    isTop: Boolean
) {
    val strokeWidth = 6f
    drawLine(
        color = Color.White,
        start = Offset(x, if (isTop) y else y + length),
        end = Offset(x + length, if (isTop) y else y + length),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(if (isLeft) x else x + length, y),
        end = Offset(if (isLeft) x else x + length, y + length),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Composable
fun CaptureButton(onCapture: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .border(2.dp, Color.Black, CircleShape)
            .background(
                shape = CircleShape,
                color = Color.White,
            )
            .clickable(onClick = onCapture)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.camerabutton),
            contentDescription = stringResource(R.string.take_photo),
            tint = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun MainScreenContent(
    controller: LifecycleCameraController,
    viewModel: CameraScreenViewModel,
    isSheetVisible: Boolean,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsState()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()
    val showSuccessDialog by viewModel.showSuccessDialog.collectAsState()
    val showFailedDialog by viewModel.showFailedDialog.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { UpperPartPreview(viewModel, selectedLanguage) },
                modifier = Modifier.background(Color.Black)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                controller = controller,
                modifier = Modifier
                    .fillMaxSize()
                    .border(6.dp, Color.Black, RoundedCornerShape(8.dp))
            )
            CameraViewfinderOverlay()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(335.dp, 335.dp)
            ) {
                ViewfinderCorners()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black)
                    .padding(46.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.please_point_the_camera_at_the_product),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Camera capture button
            Box(
                modifier = Modifier
                    .offset(y = (-85).dp)
                    .align(Alignment.BottomCenter)
            ) {
                CaptureButton(
                    onCapture = {
                        takePhoto(
                            context = context,
                            controller = controller,
                            onPhotoTaken = { bitmap ->
                                viewModel.onTakePhoto(bitmap)
                            }
                        )
                    })
            }
            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = {
                        viewModel.dismissFeedbackDialog()
                    },
                    onSend = {
                        coroutineScope.launch {
                            val audioFile = viewModel.startFeedbackRecording(context)
                            delay(5000)
                            viewModel.stopFeedbackRecording()
                            viewModel.sendFeedbackToWhatsApp(context, audioFile)
                        }
                    })
            }
        }

        // Language Selector Sheet
        if (isSheetVisible) {
            LanguageSelector(
                onDismiss = { viewModel.hideSheet() },
                onLanguageSelected = onLanguageSelected
            )
        }
        // Loading Dialog
        if (showLoadingDialog) {
            LoadingDialog()
        }

        // Success Dialog
        if (showSuccessDialog) {
            SuccessDialog(
                onDismiss = {} // Auto-dismisses after delay
            )
        }

        // Failed Dialog
        if (showFailedDialog) {
            FailedDialog(
                onDismiss = {} // Auto-dismisses after delay
            )
        }
    }
}