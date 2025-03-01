@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.marketmate.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketmate.R
import com.example.marketmate.domain.CameraScreenViewModel
import com.example.marketmate.presentation.theme.MarketMateTheme
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext

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
                val viewModel = viewModel<CameraScreenViewModel>()
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

@Composable
fun UpperPartPreview(
    viewModel: CameraScreenViewModel = viewModel(),
    selectedLanguage: String
) {
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
        IconButton(onClick = { /* Handle feedback */ }) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Default.Feedback,
                contentDescription = stringResource(R.string.feedback),
                tint = Color.White
            )
        }
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
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showBadQuality by remember { mutableStateOf(false) }

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

            Box(
                modifier = Modifier
                    .offset(y = (-85).dp)
                    .align(Alignment.BottomCenter)
            ) {
                CaptureButton(
                    onCapture = {
                        isLoading = true
                        takePhoto(
                            context = context,
                            controller = controller,
                            onPhotoTaken = { bitmap ->
                                viewModel.onTakePhoto(bitmap)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isLoading = false
                                    if (Math.random() < 0.5) {
                                        showSuccess = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            showSuccess = false
                                        }, 2000)
                                    } else {
                                        showBadQuality = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            showBadQuality = false
                                        }, 2000)
                                    }
                                }, 2000)
                            }
                        )
                    }
                )
            }
        }
    }
    if (isSheetVisible) {
        LanguageSelector(
            onDismiss = { viewModel.hideSheet() },
            onLanguageSelected = onLanguageSelected
        )
    }
    if (isLoading) {
        LoadingDialog()
    }
    if (showSuccess) {
        SuccessDialog(onDismiss = { showSuccess = false })
    }
    if (showBadQuality) {
        FailedDialog(onDismiss = { showBadQuality = false })
    }
}