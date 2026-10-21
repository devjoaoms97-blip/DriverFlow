package br.com.motoristafinanceiro

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.motoristafinanceiro.notificacoes.AgendadorDeNotificacoes
import br.com.motoristafinanceiro.notificacoes.EXTRA_ABRIR
import br.com.motoristafinanceiro.notificacoes.NotificadorAndroid
import br.com.motoristafinanceiro.notificacoes.destinoPedido
import br.com.motoristafinanceiro.ui.abertura.AberturaAnimada
import br.com.motoristafinanceiro.ui.ajustes.devePedirPermissaoAoAbrir
import br.com.motoristafinanceiro.ui.navigation.AppNav
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    /** Tela pedida pelo Intent (toque na notificação); some depois de aberta. */
    private var destino by mutableStateOf<String?>(null)

    private val pedirPermissaoDeNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* o estado é conferido em Ajustes */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tem que vir antes do super.onCreate: troca o tema de abertura pelo tema normal do app.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as MotoristaApp
        destino = lerDestino(intent)
        // Quem chega pelo toque na notificação vai direto ao destino, sem esperar a animação.
        val comAbertura = destino == null
        AgendadorDeNotificacoes.agendarVerificacaoDiaria(applicationContext)
        setContent {
            // Verifica os alertas de manutenção a cada mudança nos dados (e ao abrir o app).
            LaunchedEffect(Unit) { AgendadorDeNotificacoes.verificarACadaMudanca(app) }
            LaunchedEffect(Unit) { perguntarPermissaoUmaVez(app) }
            MotoristaTheme {
                // rememberSaveable: girar a tela depois da abertura não toca a animação de novo.
                var abertura by rememberSaveable { mutableStateOf(comAbertura) }
                Box(Modifier.fillMaxSize()) {
                    // O app carrega por baixo enquanto a animação toca.
                    AppNav(destinoPedido = destino, aoAbrirDestino = { destino = null })
                    AnimatedVisibility(visible = abertura, exit = fadeOut(tween(300))) {
                        AberturaAnimada(aoTerminar = { abertura = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destino = lerDestino(intent)
    }

    /** Lê e consome o extra, para girar a tela ou recriar a Activity não reabrir a mesma aba. */
    private fun lerDestino(intent: Intent?): String? {
        val pedido = destinoPedido(intent?.getStringExtra(EXTRA_ABRIR))
        intent?.removeExtra(EXTRA_ABRIR)
        return pedido
    }

    private suspend fun perguntarPermissaoUmaVez(app: MotoristaApp) {
        val deve = devePedirPermissaoAoAbrir(
            ativas = app.configuracoes.notificacoesManutencaoAtivas.first(),
            jaPerguntou = app.configuracoes.permissaoNotificacaoPerguntada.first(),
            sdkInt = Build.VERSION.SDK_INT,
            sistemaPermite = NotificadorAndroid.permitido(this),
        )
        if (deve) {
            app.configuracoes.marcarPermissaoNotificacaoPerguntada()
            pedirPermissaoDeNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
