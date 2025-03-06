package com.example.marketmate.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketmate.R
import com.example.marketmate.presentation.theme.PrimaryLight
import com.example.marketmate.presentation.theme.PrimaryLightActive

@Composable
fun MarketMateWelcomeScreen(
    onNextClick: () -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.backgroundonboarding1),
            contentDescription = "Onboarding Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Ensures the image covers the entire Box
        )
        // Skip Button
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .padding(top = 24.dp)
        ) {
            Text(
                text = "skip",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            )
        }

        // Mute Button
        IconButton(
            onClick = { /* Toggle mute */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = "Mute",
                tint = Color.DarkGray
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Welcome Title
            Text(
                text = "Welcome To",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A2533),
                    textAlign = TextAlign.Center
                )
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val boxWidth = maxWidth * 0.95f
                // Background Circle
                Box(
                    modifier = Modifier
                        .size(boxWidth)
                        .clip(RoundedCornerShape(60.dp))
                        .background(PrimaryLightActive)
                        .align(Alignment.TopCenter)
                ) {
                    // Logo in bottom left of circle
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Market Mate",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A2533),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    // Logo in bottom left of circle
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.marketmatelogoonboarding),
                            contentDescription = "Market Mate Logo",
                            modifier = Modifier.size(65.dp)
                                .padding(16.dp)
                        )
                    }
                }
                // Robot Image positioned to overlap the circle
                Image(
                    painter = painterResource(id = R.drawable.robet3x),
                    contentDescription = "Market Mate Assistant Robot",
                    modifier = Modifier
                        .width(boxWidth * 1.5f) // Keep it slightly smaller
                        .height(450.dp) //
                        .align(Alignment.BottomCenter)// Stretch height for better proportions
                        .offset(y = -50.dp) // Move upwards to overlap
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            // Description Text & Next Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your AI-powered shopping assistant, making it easy for visually impaired users to independently check the quality of fruits and vegetables with a simple camera scan!",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A2533),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 22.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Next Button (Arrow)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .clickable { onNextClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketMateWelcomeScreenPreview() {
    MarketMateWelcomeScreen()
}