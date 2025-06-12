package com.example.marketmate.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.ContextCompat

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

fun DrawScope.drawViewfinderCorner(
    x: Float,
    y: Float,
    length: Float,
    isLeft: Boolean,
    isTop: Boolean,
    cornerRadius: Float = 55f
) {
    val strokeWidth = 6f
    
    val path = Path().apply {
        if (isLeft) {
            if (isTop) {
                // Top-left corner
                moveTo(x + cornerRadius, y)
                lineTo(x + length, y)
                moveTo(x, y + cornerRadius)
                lineTo(x, y + length)
                arcTo(
                    Rect(x, y, x + 2 * cornerRadius, y + 2 * cornerRadius),
                    180f,
                    90f,
                    false
                )
            } else {
                // Bottom-left corner
                moveTo(x, y)
                lineTo(x, y + length - cornerRadius)
                moveTo(x + cornerRadius, y + length)
                lineTo(x + length, y + length)
                arcTo(
                    Rect(x, y + length - 2 * cornerRadius, x + 2 * cornerRadius, y + length),
                    90f,
                    90f,
                    false
                )
            }
        } else {
            if (isTop) {
                // Top-right corner
                moveTo(x, y)
                lineTo(x + length - cornerRadius, y)
                moveTo(x + length, y + cornerRadius)
                lineTo(x + length, y + length)
                moveTo(x + length - cornerRadius, y)
                arcTo(
                    Rect(x + length - 2 * cornerRadius, y, x + length, y + 2 * cornerRadius),
                    270f,
                    90f,
                    false
                )
            } else {
                // Bottom-right corner
                moveTo(x, y + length)
                lineTo(x + length - cornerRadius, y + length)
                moveTo(x + length, y)
                lineTo(x + length, y + length - cornerRadius)
                arcTo(
                    Rect(x + length - 2 * cornerRadius, y + length - 2 * cornerRadius, x + length, y + length),
                    0f,
                    90f,
                    false
                )
            }
        }
    }
    
    drawPath(
        path = path,
        color = Color.White,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}