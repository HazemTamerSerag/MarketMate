package com.example.marketmate.presentation.screen

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.marketmate.R
import kotlinx.coroutines.delay

private const val PREFS_NAME = "MarketMatePrefs"
private const val KEY_FIRST_TIME = "isFirstTime"

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val alpha = remember {
        androidx.compose.animation.core.Animatable(0f)
    }

    LaunchedEffect(key1 = true) {
        alpha.animateTo(1f, animationSpec = tween(1500))
        delay(2000)
        if (isFirstTimeUser(context)) {
            setFirstTimeUser(context, false)
            onNavigateToOnboarding()
        } else {
            onNavigateToCamera()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .alpha(alpha = alpha.value),
            painter = painterResource(id = R.drawable.splash),
            contentDescription = stringResource(R.string.market_mate)
        )
    }
}

private fun isFirstTimeUser(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_FIRST_TIME, true)
}

private fun setFirstTimeUser(context: Context, isFirstTime: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).apply()
}