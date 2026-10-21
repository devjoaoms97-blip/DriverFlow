package br.com.motoristafinanceiro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema único e escuro do DriverFlow. Não usa as cores dinâmicas do Android (que vêm do papel de parede):
 * o app tem a mesma cara em qualquer celular e combina com a abertura.
 */
@Composable
fun MotoristaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val janela = (view.context as Activity).window
            // Barras do sistema na cor do fundo, com ícones claros.
            janela.statusBarColor = EsquemaDriverFlow.background.toArgb()
            janela.navigationBarColor = EsquemaDriverFlow.background.toArgb()
            WindowCompat.getInsetsController(janela, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = EsquemaDriverFlow, content = content)
}
