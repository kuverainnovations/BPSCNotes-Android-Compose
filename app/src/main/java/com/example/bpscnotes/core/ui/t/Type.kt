// core/ui/theme/Type.kt
package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.R

val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular,   FontWeight.Normal),
    Font(R.font.nunito_semibold,  FontWeight.SemiBold),
    Font(R.font.nunito_bold,      FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

val BpscTypography = Typography(
    headlineLarge  = TextStyle(fontFamily = NunitoFamily, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontFamily = NunitoFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold),
    headlineSmall  = TextStyle(fontFamily = NunitoFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontFamily = NunitoFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontFamily = NunitoFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = TextStyle(fontFamily = NunitoFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontFamily = NunitoFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelSmall     = TextStyle(fontFamily = NunitoFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium),
)