package com.example.marketmate.presentation.screen

import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketmate.R
import com.example.marketmate.presentation.theme.PrimaryDarker
import com.example.marketmate.presentation.theme.PrimaryLightActive
import com.example.marketmate.presentation.theme.PrimaryNormal
import com.example.marketmate.presentation.theme.andadaProFontFamily
import java.util.Locale

@Preview
@Composable
fun OnBoard1(
    onNextClick: () -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isArabic = LocalContext.current.resources.configuration.locales[0].language == "ar"
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    val title = stringResource(R.string.title1Onbo)
    var spokenText by remember { mutableStateOf("") }
    var isTtsReady by remember { mutableStateOf(false) }
    // Initialize TTS
    LaunchedEffect(Unit) {
        Log.d("OnBoard1", "Initializing TTS...")

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("OnBoard1", "TTS Initialized successfully.")
                tts?.language = if (isArabic) Locale("ar") else Locale.US
                isTtsReady = true
                if (!isMuted) {
                    speakText(tts, title, onNextClick = onNextClick, onWordSpoken = { spoken ->
                        spokenText = spoken
                    })
                }
            } else {
                Log.e("OnBoard1", "TTS Initialization Failed!")
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
                painter = painterResource(id = R.drawable.backgroundonboarding1),
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
                        fontFamily = andadaProFontFamily,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                )
            }

            // Mute Button
            IconButton(
                onClick = {
                    if (!isMuted) {
                        Log.d("OnBoard1", "Muting TTS.")
                        tts?.stop()
                    } else {
                        Log.d("OnBoard1", "Resuming TTS.")
                        speakText(
                            tts, title, onWordSpoken = { spoken ->
                                spokenText = spoken
                            }, onNextClick = onNextClick
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
                    painter = if (isMuted) painterResource(R.drawable.sound_on) else painterResource(
                        R.drawable.ic_sound_on
                    ),
                    contentDescription = stringResource(R.string.mute),
                    tint = Color.DarkGray
                )
            }
            // Main Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(100.dp))

                // Welcome Title
                Text(
                    text = stringResource(R.string.welcome_to),
                    style = TextStyle(
                        fontFamily = andadaProFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = PrimaryDarker,
                        textAlign = TextAlign.Center
                    )
                )
                // Large Robot in Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(60.dp))
                        .background(PrimaryLightActive)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.market_mate),
                            style = TextStyle(
                                fontFamily = andadaProFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                color = PrimaryDarker,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    // Robot Image completely filling the box
                    Image(
                        painter = painterResource(id = R.drawable.robet3x),
                        contentDescription = stringResource(R.string.market_mate_assistant_robot),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Use Crop to ensure it fills the entire box
                    )
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

                Spacer(modifier = Modifier.height(16.dp))

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
                    style = TextStyle(
                        fontFamily = andadaProFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Next Button (Arrow)
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .width(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3C7D1))
                        .clickable {
                            tts?.stop()
                            onNextClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow),
                        contentDescription = stringResource(R.string.next),
                        tint = Color.DarkGray,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    }
}
fun speakText(
    tts: TextToSpeech?,
    text: String,
    onWordSpoken: (String) -> Unit,
    onNextClick: () -> Unit
) {
    val params = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts_id")
    }

    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_id")
    onWordSpoken("") // Reset spoken words

    // Use the listener to detect when the speech finishes
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            Log.d("TTS", "Speech started")
        }

        override fun onDone(utteranceId: String?) {
            Log.d("TTS", "Speech completed")
            onNextClick()
        }

        override fun onError(utteranceId: String?) {
            Log.e("TTS", "Speech error")
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val spokenPart = text.substring(0, end)
            onWordSpoken(spokenPart)
        }
    })
}
