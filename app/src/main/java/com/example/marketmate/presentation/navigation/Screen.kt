package com.example.marketmate.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash_screen")
    data object Onboarding : Screen("onboarding_screen")
    data object Camera : Screen("camera_screen")
}