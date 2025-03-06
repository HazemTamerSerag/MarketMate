package com.example.marketmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.marketmate.data.LanguageUtils
import com.example.marketmate.presentation.navigation.AppNavigation
import com.example.marketmate.presentation.screen.MarketMateWelcomeScreen
import com.example.marketmate.presentation.screen.MarketMateWelcomeScreen2
import com.example.marketmate.presentation.screen.MarketMateWelcomeScreen3
import com.example.marketmate.presentation.screen.OnboardingPage
import com.example.marketmate.presentation.screen.SplashScreen
import com.example.marketmate.presentation.theme.MarketMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LanguageUtils.applySavedLanguage(this)
        setContent {
//            MarketMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    MarketMateWelcomeScreen3()
//                }
            }
        }
    }
}