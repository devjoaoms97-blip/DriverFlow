package br.com.motoristafinanceiro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Azul-marinho do logo e da abertura (res/raw/driverflow_splash.json e themes.xml usam o mesmo valor). */
val FundoDriverFlow = Color(0xFF080E20)

/**
 * Tema escuro do DriverFlow, tirado do logo: azul-marinho de fundo, azul elétrico nas ações,
 * verde nos ganhos e vermelho nas despesas. Todos os pares texto/fundo passam de 4,5:1 de contraste
 * (conferido em EsquemaDeCoresTest).
 */
val EsquemaDriverFlow = darkColorScheme(
    primary = Color(0xFF3D9CFF),
    onPrimary = Color(0xFF03142F),
    primaryContainer = Color(0xFF0F3F95),
    onPrimaryContainer = Color(0xFFD6E8FF),
    inversePrimary = Color(0xFF0F5BD0),

    secondary = Color(0xFF1ADB8F),
    onSecondary = Color(0xFF00231A),
    secondaryContainer = Color(0xFF0A4B38),
    onSecondaryContainer = Color(0xFFB8F7DA),

    tertiary = Color(0xFFFFB547),
    onTertiary = Color(0xFF2F1C00),
    tertiaryContainer = Color(0xFF573700),
    onTertiaryContainer = Color(0xFFFFE0B0),

    background = FundoDriverFlow,
    onBackground = Color(0xFFE6EEFB),
    surface = Color(0xFF0C142B),
    onSurface = Color(0xFFE6EEFB),
    surfaceVariant = Color(0xFF1A2540),
    onSurfaceVariant = Color(0xFFA9B8D4),
    surfaceTint = Color(0xFF3D9CFF),
    inverseSurface = Color(0xFFE6EEFB),
    inverseOnSurface = Color(0xFF0A1226),

    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0009),
    errorContainer = Color(0xFF661522),
    onErrorContainer = Color(0xFFFFDADB),

    outline = Color(0xFF5F7194),
    outlineVariant = Color(0xFF26334F),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF1F2D4D),
    surfaceDim = Color(0xFF060B1A),
    surfaceContainerLowest = Color(0xFF060B1A),
    surfaceContainerLow = Color(0xFF0B1329),
    surfaceContainer = Color(0xFF101A33),
    surfaceContainerHigh = Color(0xFF16213D),
    surfaceContainerHighest = Color(0xFF1C2947),
)

/**
 * Tema claro do DriverFlow: mesma identidade (azul, verde, vermelho), com tons mais escuros nas cores de
 * destaque para o texto colorido continuar legível sobre fundo claro (conferido em EsquemaDeCoresTest).
 */
val EsquemaClaroDriverFlow = lightColorScheme(
    primary = Color(0xFF0D52BC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF00225A),
    inversePrimary = Color(0xFF8FC0FF),

    secondary = Color(0xFF066B48),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFF3DC),
    onSecondaryContainer = Color(0xFF00311F),

    tertiary = Color(0xFF7F5200),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B0),
    onTertiaryContainer = Color(0xFF2F1C00),

    background = Color(0xFFF5F8FE),
    onBackground = Color(0xFF0B1226),
    surface = Color(0xFFF5F8FE),
    onSurface = Color(0xFF0B1226),
    surfaceVariant = Color(0xFFE2E9F6),
    onSurfaceVariant = Color(0xFF44516B),
    surfaceTint = Color(0xFF0D52BC),
    inverseSurface = Color(0xFF1C2947),
    inverseOnSurface = Color(0xFFE6EEFB),

    error = Color(0xFFB71C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF410008),

    outline = Color(0xFF75829D),
    outlineVariant = Color(0xFFC5CEE0),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFF5F8FE),
    surfaceDim = Color(0xFFD3DBEA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FC),
    surfaceContainer = Color(0xFFE9EFFA),
    surfaceContainerHigh = Color(0xFFE2E9F7),
    surfaceContainerHighest = Color(0xFFDAE3F3),
)
