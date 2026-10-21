package br.com.motoristafinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import br.com.motoristafinanceiro.notificacoes.AgendadorDeNotificacoes
import br.com.motoristafinanceiro.ui.navigation.AppNav
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MotoristaApp
        AgendadorDeNotificacoes.agendarVerificacaoDiaria(applicationContext)
        setContent {
            // Verifica os alertas de manutenção a cada mudança nos dados (e ao abrir o app).
            LaunchedEffect(Unit) { AgendadorDeNotificacoes.verificarACadaMudanca(app) }
            MotoristaTheme {
                AppNav()
            }
        }
    }
}
