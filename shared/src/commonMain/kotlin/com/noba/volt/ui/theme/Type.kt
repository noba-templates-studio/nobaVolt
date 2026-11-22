package com.noba.volt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.noba.volt.recursos.Res
import com.noba.volt.recursos.inter_bold
import com.noba.volt.recursos.inter_medium
import com.noba.volt.recursos.inter_regular
import com.noba.volt.recursos.inter_semibold
import org.jetbrains.compose.resources.Font

/** Inter: tipografía moderna y muy legible (licencia SIL Open Font License). */
@Composable
fun interFamilia(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold)
)

/** Tipografía de la app (en Compose Multiplatform las fuentes se cargan dentro de un @Composable). */
@Composable
fun tipografiaNoba(): Typography {
    val inter = interFamilia()
    return remember(inter) {
        val base = Typography()
        fun TextStyle.inter(peso: FontWeight? = null, tracking: Float? = null) = copy(
            fontFamily = inter,
            fontWeight = peso ?: fontWeight,
            letterSpacing = if (tracking != null) tracking.sp else letterSpacing
        )
        Typography(
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
    }
}
