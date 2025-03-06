package com.example.marketmate.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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