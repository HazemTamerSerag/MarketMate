package com.example.marketmate.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.marketmate.presentation.navigation.Screen

@Composable
fun OnBoardingScreen(navController: NavController) {
    // State to track current page
    var currentPage by remember { mutableIntStateOf(0) }

    // Function to navigate to camera screen
    val navigateToCameraScreen = {
        navController.navigate(Screen.Camera.route) {
            popUpTo(Screen.Onboarding.route) {
                inclusive = true
            }
        }
    }

    // Function to go to next page
    val goToNextPage: () -> Unit = {
        if (currentPage < 2) {
            currentPage++
        } else {
            navigateToCameraScreen()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Display the current onboarding screen based on currentPage
        when (currentPage) {
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