package com.example.marketmate.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.marketmate.R

val segoeFontFamily = FontFamily(
    Font(R.font.segoeuithis, FontWeight.Normal),
    Font(R.font.segoeuithibd, FontWeight.Bold),
)

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
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
//    h2
    headlineMedium = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
//    h3
    headlineSmall = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
//    body1
    bodyLarge = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
//    body2
    bodyMedium = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
//    subtitle1
    titleLarge = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
//    subtitle2
    titleMedium = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
//    caption
    displayLarge = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
//    button
    displayMedium = TextStyle(
        fontFamily = segoeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
)
