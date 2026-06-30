package app.tastile.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.tastile.android.R

val TastileFontFamily = FontFamily(
    Font(R.font.rubik_variable, FontWeight.Normal),
    Font(R.font.rubik_variable, FontWeight.Medium),
    Font(R.font.rubik_variable, FontWeight.SemiBold),
    Font(R.font.rubik_variable, FontWeight.Bold),
)

private val baseBody = TextStyle(
    fontFamily = TastileFontFamily,
    fontWeight = FontWeight.Normal,
)

private val baseLabel = TextStyle(
    fontFamily = TastileFontFamily,
    fontWeight = FontWeight.Medium,
)

val TastileTypography = Typography(
    displayLarge = baseBody.copy(fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em, fontWeight = FontWeight.SemiBold),
    displayMedium = baseBody.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em, fontWeight = FontWeight.SemiBold),
    displaySmall = baseBody.copy(fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.SemiBold),

    headlineLarge = baseBody.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em, fontWeight = FontWeight.SemiBold),
    headlineMedium = baseBody.copy(fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.005).em, fontWeight = FontWeight.SemiBold),
    headlineSmall = baseBody.copy(fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Medium),

    titleLarge = baseLabel.copy(fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium = baseLabel.copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.005.em),
    titleSmall = baseLabel.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.005.em),

    bodyLarge = baseBody.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.005.em),
    bodyMedium = baseBody.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    bodySmall = baseBody.copy(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.02.em),

    labelLarge = baseLabel.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.005.em),
    labelMedium = baseLabel.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.03.em),
    labelSmall = baseLabel.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
)
