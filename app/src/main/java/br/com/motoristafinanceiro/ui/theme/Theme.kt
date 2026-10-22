package br.com.motoristafinanceiro.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import br.com.motoristafinanceiro.data.ModoTema

/**
 * Tema do DriverFlow, com paleta própria escura e clara (nunca as cores dinâmicas do Android, que vêm do
 * papel de parede): o app tem a mesma cara em qualquer celular. [modo] vem de Ajustes.
 */
@Composable
fun MotoristaTheme(modo: ModoTema = ModoTema.PADRAO, content: @Composable () -> Unit) {
    val escuro = temaEscuro(modo, isSystemInDarkTheme())
    val esquema = if (escuro) EsquemaDriverFlow else EsquemaClaroDriverFlow
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val janela = (view.context as Activity).window
            val fundo = esquema.background.toArgb()
            // Barras do sistema e fundo da janela na cor do fundo (o teclado abrindo não mostra outra cor).
            janela.statusBarColor = fundo
            janela.navigationBarColor = fundo
            janela.setBackgroundDrawable(ColorDrawable(fundo))
            WindowCompat.getInsetsController(janela, view).apply {
                isAppearanceLightStatusBars = !escuro
                isAppearanceLightNavigationBars = !escuro
            }
        }
    }
    MaterialTheme(
        colorScheme = esquema,
        typography = TipografiaDriverFlow,
        shapes = FormasDriverFlow,
        content = content,
    )
}
