@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.marketmate.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
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

class CameraScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedLanguage()
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            MarketMateTheme {
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE
                        )
                    }
                }
                val viewModel = viewModel<CameraScreenViewModel>()
                val isSheetVisible by viewModel.isSheetVisible.collectAsState()
                val selectedLanguage by viewModel.selectedLanguage.collectAsState()

                if (isSheetVisible) {
                    LanguageSelector(
                        onDismiss = { viewModel.hideSheet() },
                        onLanguageSelected = { language -> viewModel.setLanguage(language, this) }
                    )
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.showSheet()  }
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(16.dp),
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = stringResource(R.string.translate),
                                            tint = Color.White
                                        )
                                    }
                                    Icon(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .align(alignment = Alignment.CenterVertically),
                                        painter = painterResource(R.drawable.marketmatelogo3x),
                                        contentDescription = stringResource(R.string.market_mate_logo),
                                        tint = Color.White
                                    )
                                    IconButton(
                                        onClick = { /* Handle sound toggle */ }
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(16.dp),
                                            imageVector = Icons.Default.Feedback,
                                            contentDescription = stringResource(R.string.feedback),
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            Modifier
                                .background(Color.Black)
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 6.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        // Transparent outer area with light inner area
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
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
                                color = Color.White.copy(alpha = 0.1f),
                                topLeft = Offset(innerBoxLeft, innerBoxTop),
                                size = Size(innerBoxWidth, innerBoxHeight)
                            )
                        }

                        // Inner box border
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(335.dp, 335.dp)
                        ){
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 6f
                                val cornerLength = 160f
                                val width = size.width
                                val height = size.height

                                // Top left corner
                                drawLine(
                                    color = Color.White,
                                    start = Offset(0f, 0f),
                                    end = Offset(cornerLength, 0f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, cornerLength),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )

                                // Top right corner
                                drawLine(
                                    color = Color.White,
                                    start = Offset(width - cornerLength, 0f),
                                    end = Offset(width, 0f),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(width, 0f),
                                    end = Offset(width, cornerLength),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )

                                // Bottom left corner
                                drawLine(
                                    color = Color.White,
                                    start = Offset(0f, height - cornerLength),
                                    end = Offset(0f, height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(0f, height),
                                    end = Offset(cornerLength, height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )

                                // Bottom right corner
                                drawLine(
                                    color = Color.White,
                                    start = Offset(width - cornerLength, height),
                                    end = Offset(width, height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = Offset(width, height - cornerLength),
                                    end = Offset(width, height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }



                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black)
                                .padding(46.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp) // Space between button and text
                        ) {
                            // Instruction text
                            Text(
                                text = stringResource(R.string.please_point_the_camera_at_the_product),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Circular shutter button
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.BottomCenter)
                                .offset(0.dp, (-85).dp) // Position above the text as specified
                                .border(2.dp, Color.Black, CircleShape)
                                .background(
                                    shape = CircleShape,
                                    color = Color.White,
                                )
                                .clickable {
                                    takePhoto(
                                        controller = controller,
                                        onPhotoTaken = viewModel::onTakePhoto
                                    )
                                }
                        ){
                            Icon(
                                painter = painterResource(id = R.drawable.camerabutton),
                                contentDescription = stringResource(R.string.take_photo),
                                tint = Color.Black,
                                modifier = Modifier.align(Alignment.Center)
                            )

                        }

                    }
                }
            }
        }
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

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
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
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