package com.example.marketmate.presentation.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketmate.R
import com.example.marketmate.data.AnalysisResultType
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.data.checkAndRequestFeedbackPermissions
import com.example.marketmate.data.drawViewfinderCorner
import com.example.marketmate.data.takePhoto
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

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    // Apply saved language
    LaunchedEffect(Unit) {
        LanguageUtils.applySavedLanguage(context)
    }
    MarketMateTheme {
        val controller = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            }
        }
        val viewModel = viewModel<CameraScreenViewModel>()
        val isSheetVisible by viewModel.isSheetVisible.collectAsState()
        // Request camera permission
        LaunchedEffect(Unit) {
            if (!hasCameraPermission(context)) {
                requestCameraPermission(context)
            }
        }
        MainScreenContent(
            controller = controller,
            viewModel = viewModel,
            isSheetVisible = isSheetVisible,
            onLanguageSelected = { language ->
                viewModel.setLanguage(language, context as ComponentActivity)
            }
        )
    }
}

// Helper functions for camera permissions
private fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
    context, Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED

private fun requestCameraPermission(context: Context) {
    if (context is ComponentActivity) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.CAMERA),
            0
        )
    }
}


@Composable
fun UpperPartPreview(
    viewModel: CameraScreenViewModel)
{
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    controller: LifecycleCameraController,
    viewModel: CameraScreenViewModel,
    isSheetVisible: Boolean,
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
                title = { UpperPartPreview(viewModel) },
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

            // Loading Dialog
            if (showLoadingDialog) {
                LoadingDialog()
            }

            // Success Dialog
            if (showSuccessDialog) {
                SuccessDialog(
                    // Auto-dismisses after delay
                )
            }

            // Failed Dialog
            if (showFailedDialog) {
                FailedDialog(
                    // Auto-dismisses after delay
                )
            }

            // Display analysis result message if available
            analysisResult?.let { result ->
                Text(
                    text = result.message,
                    color = when (result.type) {
                        AnalysisResultType.SUITABLE -> Color.Green
                        AnalysisResultType.NOT_SUITABLE -> Color.Red
                        AnalysisResultType.ERROR -> Color.Gray
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(8.dp)
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
                    })
            }
        }
        }

        // Language Selector Sheet
        if (isSheetVisible) {
            LanguageSelector(
                onDismiss = { viewModel.hideSheet() },
                onLanguageSelected = onLanguageSelected
            )
        }
    }
