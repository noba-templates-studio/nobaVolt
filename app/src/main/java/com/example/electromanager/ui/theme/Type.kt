package com.example.electromanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.electromanager.R

/** Inter: tipografía moderna y muy legible (licencia SIL Open Font License). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val base = Typography()

private fun TextStyle.inter(peso: FontWeight? = null, tracking: Float? = null) = copy(
    fontFamily = Inter,
    fontWeight = peso ?: fontWeight,
    letterSpacing = if (tracking != null) tracking.sp else letterSpacing
)

val Typography = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.SemiBold, -1.5f),
    displayMedium = base.displayMedium.inter(FontWeight.SemiBold, -1f),
    displaySmall = base.displaySmall.inter(FontWeight.SemiBold, -0.8f),
    headlineLarge = base.headlineLarge.inter(FontWeight.SemiBold, -0.8f),
    headlineMedium = base.headlineMedium.inter(FontWeight.SemiBold, -0.6f),
    headlineSmall = base.headlineSmall.inter(FontWeight.SemiBold, -0.4f),
    titleLarge = base.titleLarge.inter(FontWeight.SemiBold, -0.3f),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold, -0.1f),
    titleSmall = base.titleSmall.inter(FontWeight.Medium),
    bodyLarge = base.bodyLarge.inter(tracking = 0f),
    bodyMedium = base.bodyMedium.inter(tracking = 0f),
    bodySmall = base.bodySmall.inter(tracking = 0f),
    labelLarge = base.labelLarge.inter(FontWeight.Medium),
    labelMedium = base.labelMedium.inter(FontWeight.Medium),
    labelSmall = base.labelSmall.inter(FontWeight.Medium)
)
