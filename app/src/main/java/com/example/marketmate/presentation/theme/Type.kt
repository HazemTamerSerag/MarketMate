package com.example.marketmate.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.marketmate.R



val andadaProFontFamily = FontFamily(
    Font(R.font.andada_pro, FontWeight.Normal),
    Font(R.font.andada_pro_medium, FontWeight.Medium),
    Font(R.font.andada_pro_semibold, FontWeight.SemiBold),
    Font(R.font.andada_pro_bold, FontWeight.Bold),
)

// Set of Material typography styles to start with
val Typography = Typography(
//    h1
    headlineLarge = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
//    h2
    headlineMedium = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
//    h3
    headlineSmall = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
//    body1
    bodyLarge = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
//    body2
    bodyMedium = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
//    subtitle1
    titleLarge = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
//    subtitle2
    titleMedium = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
//    caption
    displayLarge = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
//    button
    displayMedium = TextStyle(
        fontFamily = andadaProFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
)
