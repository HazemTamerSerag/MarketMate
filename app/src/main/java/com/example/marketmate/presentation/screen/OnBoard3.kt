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
import com.example.marketmate.presentation.theme.SecondLightActive
import com.example.marketmate.presentation.theme.ThirdLightActive

@Composable
fun MarketMateWelcomeScreen3(
    onNextClick: () -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.backgroundonboarding3),
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

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Main Content Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xBBC4DEE8))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Effortless\nGuidance",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A2533),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.blindonboarding2),
                        contentDescription = "AI Shopping Assistant",
                        modifier = Modifier.size(180.dp)
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
                    text = "With clear audio instructions and easy app navigation, shopping for the best produce has never been this accessible!",
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

