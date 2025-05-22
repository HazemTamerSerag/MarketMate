package com.example.marketmate.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.marketmate.presentation.navigation.Screen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnBoardingScreen(navController: NavController) {
    var currentPage by remember { mutableIntStateOf(0) }

    val navigateToCameraScreen = {
        navController.navigate(Screen.Camera.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
        }
    }

    val goToNextPage: () -> Unit = {
        if (currentPage < 2) {
            currentPage++
        } else {
            navigateToCameraScreen()
        }
    }

    val transition = updateTransition(targetState = currentPage, label = "OnboardingPage")

    Box(modifier = Modifier.fillMaxSize()) {
        transition.AnimatedContent(
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { fullWidth -> fullWidth } + fadeIn() with
                            slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut()
                } else {
                    EnterTransition.None with ExitTransition.None
                }
            },
        ) { page ->
            when (page) {
                0 -> OnBoard1(
                    onNextClick = goToNextPage,
                    onSkipClick = navigateToCameraScreen
                )
                1 -> OnBoard2(
                    onNextClick = goToNextPage,
                    onSkipClick = navigateToCameraScreen
                )
                2 -> OnBoard3(
                    onNextClick = navigateToCameraScreen,
                    onSkipClick = navigateToCameraScreen
                )
            }
        }
    }
}