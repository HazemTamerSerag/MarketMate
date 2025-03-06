package com.example.marketmate.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketmate.R

@Composable
fun MarketMateWelcomeScreen2(
    onNextClick: () -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.backgroundonboarding2),
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
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Main Content Box
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xBBC4DEE8)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Point,\nScan,\nListen",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A2533),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.handonboarding2),
                        contentDescription = "Phone Scanning",
                        modifier = Modifier.size(180.dp)
                            .align(Alignment.End)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))


            // Description Text & Next Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Just point your camera at any product, and Market Mate will analyze it, delivering instant audio feedback on freshness and quality.",
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

