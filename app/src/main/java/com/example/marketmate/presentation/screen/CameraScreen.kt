package com.example.marketmate.presentation.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketmate.R
import com.example.marketmate.data.AnalysisResultType
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.data.checkAndRequestFeedbackPermissions
import com.example.marketmate.data.takePhoto
import com.example.marketmate.domain.CameraScreenViewModel
import com.example.marketmate.presentation.components.camera.CameraPreview
import com.example.marketmate.presentation.components.dialogs.ErrorDialog
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var showFirstText by remember { mutableStateOf(true) }
    val viewModel: CameraScreenViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.checkInternetAndSetMode(context)
    }

    LaunchedEffect(Unit) {
        LanguageUtils.applySavedLanguage(context)
    }

    MarketMateTheme {
        val controller = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                isPinchToZoomEnabled = true
                enableTorch(false)
                setZoomRatio(1f)
            }
        }

        DisposableEffect(controller) {
            controller.bindToLifecycle(lifecycleOwner)
            onDispose {
                controller.unbind()
            }
        }

        val isSheetVisible by viewModel.isSheetVisible.collectAsState()

        val multiplePermissionsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.CAMERA] == false && permissions[Manifest.permission.RECORD_AUDIO] == false -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.camera_and_audio_permissions_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
                permissions[Manifest.permission.CAMERA] == false -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.camera_permission_is_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
                permissions[Manifest.permission.RECORD_AUDIO] == false -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.audio_permission_is_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        LaunchedEffect(Unit) {
            if (!hasRequiredPermissions(context)) {
                multiplePermissionsLauncher.launch(REQUIRED_PERMISSIONS)
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(5000)
                showFirstText = !showFirstText
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            MainScreenContent(
                controller = controller,
                viewModel = viewModel,
                isSheetVisible = isSheetVisible,
                onLanguageSelected = { language -> viewModel.setLanguage(language, context as ComponentActivity) },
                alternatingText = if (showFirstText) {
                    stringResource(R.string.please_point_the_camera_at_the_product)
                } else {
                    stringResource(R.string.take_picture_now)
                }
            )
        }
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

private fun hasRequiredPermissions(context: Context): Boolean {
    return REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun UpperPartPreview(
    viewModel: CameraScreenViewModel,
    onFeedbackClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showFeedbackRequestDialog by remember { mutableStateOf(false) }
    var showFeedbackSuccessDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var job: Job? by remember { mutableStateOf(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.showSheet() }) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = stringResource(R.string.translate),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Icon(
            painter = painterResource(R.drawable.marketmatelogo3x),
            contentDescription = stringResource(R.string.market_mate_logo),
            tint = Color.White,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterVertically)
        )

        IconButton(onClick = {
            if (checkAndRequestFeedbackPermissions(context)) {
                showFeedbackRequestDialog = true
                isRecording = true

                job = coroutineScope.launch {
                    val audioFile = viewModel.startFeedbackRecording()
                    delay(5000)
                    viewModel.stopFeedbackRecording()
                    isRecording = false
                    showFeedbackRequestDialog = false
                    viewModel.sendFeedbackToWhatsApp(audioFile)

                    showFeedbackSuccessDialog = true
                    delay(3000)
                    showFeedbackSuccessDialog = false
                }
            }
        }) {
            Icon(
                imageVector = Icons.Default.Feedback,
                contentDescription = stringResource(R.string.feedback),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
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
                job?.cancel()
            }
        )
    }

    if (showFeedbackSuccessDialog) {
        ThanksDialog(
            onDismiss = { showFeedbackSuccessDialog = false }
        )
    }
}

@Composable
fun CameraViewfinderOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val innerBoxWidth = 335.dp.toPx()
        val innerBoxHeight = 335.dp.toPx()
        val innerBoxLeft = (size.width - innerBoxWidth) / 2
        val innerBoxTop = (size.height - innerBoxHeight) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        drawRect(
            color = Color.Transparent,
            topLeft = Offset(innerBoxLeft, innerBoxTop),
            size = Size(innerBoxWidth, innerBoxHeight),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun ViewfinderCorners() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cornerLength = 160f
        val width = size.width
        val height = size.height

        fun drawViewfinderCorner(x: Float, y: Float, length: Float, isLeft: Boolean, isTop: Boolean) {
            val strokeWidth = 6f
            val cornerColor = Color.White

            drawLine(
                color = cornerColor,
                start = Offset(x, y),
                end = Offset(x + if (isLeft) length else -length, y),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(x, y),
                end = Offset(x, y + if (isTop) length else -length),
                strokeWidth = strokeWidth
            )
        }

        drawViewfinderCorner(0f, 0f, cornerLength, true, true)
        drawViewfinderCorner(width, 0f, cornerLength, false, true)
        drawViewfinderCorner(0f, height, cornerLength, true, false)
        drawViewfinderCorner(width, height, cornerLength, false, false)
    }
}

@Composable
fun CaptureButton(onCapture: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .border(8.dp, Color.Black, CircleShape)
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

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    controller: LifecycleCameraController,
    viewModel: CameraScreenViewModel,
    isSheetVisible: Boolean,
    onLanguageSelected: (String) -> Unit,
    alternatingText: String
) {
    val TAG = "MainScreenContent"
    Log.d(TAG, "MainScreenContent Composable entered")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsState()
    var showFeedbackSuccessDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var job: Job? by remember { mutableStateOf(null) }
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()
    val showSuccessDialog by viewModel.showSuccessDialog.collectAsState()
    val showFailedDialog by viewModel.showFailedDialog.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val showErrorDialog by viewModel.showErrorDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { UpperPartPreview(viewModel) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            CameraPreview(
                controller = controller,
                modifier = Modifier
                    .fillMaxSize()
                    .border(6.dp, Color.Black, shape = MaterialTheme.shapes.medium)
            )

            CameraViewfinderOverlay()

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(335.dp)
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
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = alternatingText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showLoadingDialog) {
                LoadingDialog()
            }

            if (showSuccessDialog) {
                SuccessDialog(viewModel)
            }

            if (showFailedDialog) {
                FailedDialog(viewModel)
            }
            if (showErrorDialog) {
                ErrorDialog(viewModel)
            }

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
                                Log.d("Camera", "Photo taken, sending to ViewModel")
                                viewModel.onTakePhoto(bitmap)
                            }
                        )
                    }
                )
            }

            LaunchedEffect(showFeedbackDialog) {
                if (showFeedbackDialog && checkAndRequestFeedbackPermissions(context)) {
                    isRecording = true
                    viewModel.startFeedbackRecording()
                    
                    // Auto close feedback dialog after 8 seconds
                    delay(8000)
                    if (isRecording) {
                        val recordedFile = viewModel.stopFeedbackRecording()
                        isRecording = false
                        viewModel.submitFeedback(recordedFile)
                    }
                }
            }

            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = {
                        viewModel.dismissFeedbackDialog()
                        if (isRecording) {
                            viewModel.stopFeedbackRecording()
                            isRecording = false
                        }
                        job?.cancel()
                    }
                )
            }
            
            val showThanksDialog by viewModel.showThanksDialog.collectAsState()
            if (showThanksDialog) {
                ThanksDialog(
                    onDismiss = {}
                )
            }
        }
        if (isSheetVisible) {
            LanguageSelector(
                onDismiss = { viewModel.hideSheet() },
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}
