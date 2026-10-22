package br.com.motoristafinanceiro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.motoristafinanceiro.R

/** Pesos usados no app; todos saem do mesmo arquivo variável (res/font/manrope.ttf). */
internal val pesosManrope = listOf(400, 500, 600, 700, 800)

/** Manrope, a fonte do DriverFlow (licença OFL em docs/licencas/Manrope-OFL.txt). */
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    pesosManrope.map { peso ->
        Font(
            R.font.manrope,
            weight = FontWeight(peso),
            variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
        )
    }
)

/** Algarismos tabulares: valores alinham nas listas e não "pulam" quando mudam. */
internal const val NUMEROS_TABULARES = "tnum"

private fun estilo(
    tamanho: Int,
    peso: Int,
    altura: Int,
    espacamento: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight(peso),
    fontSize = tamanho.sp,
    lineHeight = altura.sp,
    letterSpacing = espacamento,
    fontFeatureSettings = NUMEROS_TABULARES,
)

/**
 * Escala do "Painel de bordo": números grandes em destaque (displaySmall no lucro, titleLarge nos
 * indicadores), títulos de tela em headlineSmall e rótulos em labelMedium.
 */
val TipografiaDriverFlow = Typography(
    displayLarge = estilo(57, 800, 64),
    displayMedium = estilo(45, 800, 52),
    displaySmall = estilo(44, 800, 48, (-0.8).sp),
    headlineLarge = estilo(32, 800, 40, (-0.5).sp),
    headlineMedium = estilo(30, 800, 36, (-0.5).sp),
    headlineSmall = estilo(24, 800, 32, (-0.2).sp),
    titleLarge = estilo(22, 800, 28),
    titleMedium = estilo(16, 700, 22),
    titleSmall = estilo(14, 700, 20),
    bodyLarge = estilo(16, 500, 24),
    bodyMedium = estilo(14, 500, 20),
    bodySmall = estilo(12, 500, 16),
    labelLarge = estilo(14, 700, 20),
    labelMedium = estilo(12, 600, 16),
    labelSmall = estilo(11, 600, 16),
)

/** Cantos: 24 nos cartões grandes, 18 nos médios, 14 nos campos e 10 nos botões pequenos. */
val FormasDriverFlow = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
