package com.example.marketmate.presentation.components.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.marketmate.R

@Composable
fun RotatingImageLoader() {
    val infiniteTransition = rememberInfiniteTransition()

    val rotationAngle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Image(
        painter = painterResource(id = R.drawable.loading),
        contentDescription = "Rotating Image Loader",
        modifier = Modifier
            .size(48.dp) // Set the size
            .rotate(rotationAngle.value) // Apply rotation animation
    )
}
