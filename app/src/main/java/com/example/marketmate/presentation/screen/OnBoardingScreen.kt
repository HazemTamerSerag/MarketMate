package com.example.marketmate.presentation.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.marketmate.R
import com.example.marketmate.presentation.theme.MarketMateTheme

class OnBoardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarketMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    OnBoardingScreen()
                }
            }
        }
    }
}

@Composable
fun OnBoardingScreen(modifier: Modifier = Modifier) {
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ){
        Image(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f),
            painter = painterResource(id = R.drawable.backgroundonboarding1),
            contentDescription = null)
    }
}