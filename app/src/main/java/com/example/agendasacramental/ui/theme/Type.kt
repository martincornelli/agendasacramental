package com.example.agendasacramental.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = 0.sp
)
val Typography = Typography(
    displaySmall = type(36, 44, FontWeight.Bold),
    headlineLarge = type(28, 36, FontWeight.Bold),
    headlineMedium = type(26, 34, FontWeight.Bold),
    headlineSmall = type(22, 28, FontWeight.SemiBold),
    titleLarge = type(20, 26, FontWeight.SemiBold),
    titleMedium = type(16, 24, FontWeight.SemiBold),
    titleSmall = type(16, 24, FontWeight.SemiBold),
    bodyLarge = type(16, 24), bodyMedium = type(14, 22), bodySmall = type(13, 20),
    labelLarge = type(14, 20, FontWeight.SemiBold), labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(12, 16, FontWeight.Medium)
)
