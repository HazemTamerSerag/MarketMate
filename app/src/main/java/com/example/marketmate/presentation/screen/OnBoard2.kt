package com.example.marketmate.presentation.screen

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketmate.R
import com.example.marketmate.presentation.theme.PrimaryDarker
import com.example.marketmate.presentation.theme.PrimaryNormal
import com.example.marketmate.presentation.theme.SecondLightActive
import java.util.Locale

@Composable
fun OnBoard2(
    onNextClick: () -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isArabic = LocalContext.current.resources.configuration.locales[0].language == "ar"

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    val title = stringResource(R.string.title2Onborad2)
    var spokenText by remember { mutableStateOf("") }
    var isTtsReady by remember { mutableStateOf(false) }
    // Initialize TTS
    LaunchedEffect(Unit) {
        Log.d("OnBoard2", "Initializing TTS...")

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("OnBoard2", "TTS Initialized successfully.")
                tts?.language = if (isArabic) Locale("ar") else Locale.US
                isTtsReady = true
                if (!isMuted) {
                    speakText(
                        tts, title,
                        onWordSpoken = { spoken ->
                            spokenText = spoken
                        },
                        onNextClick = onNextClick
                    )
                }
            } else {
                Log.e("OnBoard2", "TTS Initialization Failed!")
            }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.backgroundonboarding2),
                contentDescription = stringResource(R.string.onboarding_background),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Skip Button
            TextButton(
                onClick = {
                    tts?.stop()
                    onSkipClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
            }

            // Mute Button
            IconButton(
                onClick = {
                    if (!isMuted) {
                        Log.d("OnBoard2", "Muting TTS.")
                        tts?.stop()
                    } else {
                        Log.d("OnBoard2", "Resuming TTS.")
                        speakText(
                            tts, title,
                            onWordSpoken = { spoken ->
                                spokenText = spoken
                            },
                            onNextClick = onNextClick
                        )
                    }
                    isMuted = !isMuted
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 24.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.mute),
                    tint = Color.DarkGray
                )
            }

            // Main Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(140.dp))

                // Large Box with Hand and Phone
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(60.dp))
                        .background(SecondLightActive)
                ) {

                    // Hand with phone image
                    Image(
                        painter = painterResource(id = R.drawable.handonboarding2),
                        contentDescription = stringResource(R.string.hand_holding_phone),
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.9f)
                            .align(Alignment.CenterEnd),
                        contentScale = ContentScale.FillHeight
                    )
                    // Text overlay for "Point, Scan, Listen"
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 62.dp, bottom = 36.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.point),
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarker
                            )
                        )
                        Text(
                            text = stringResource(R.string.scan),
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarker
                            )
                        )
                        Text(
                            text = stringResource(R.string.listen),
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarker
                            )
                        )
                    }

                    // Logo overlay in bottom left of box
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.marketmatelogoonboarding),
                            contentDescription = stringResource(R.string.market_mate_logo),
                            modifier = Modifier
                                .size(65.dp)
                                .padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Description Text
                Text(
                    text = buildAnnotatedString {
                        title.split(" ").forEach { word ->
                            if (spokenText.contains(word)) {
                                withStyle(style = SpanStyle(color = PrimaryNormal)) {
                                    append("$word ")
                                }
                            } else {
                                withStyle(style = SpanStyle(color = PrimaryDarker)) {
                                    append("$word ")
                                }
                            }
                        }
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(22.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Next Button (Arrow)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .clickable {
                            tts?.stop()
                            onNextClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = stringResource(R.string.next),
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
