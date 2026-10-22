package br.com.motoristafinanceiro

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.data.ModoTema
import br.com.motoristafinanceiro.data.backup.AgendadorDeBackup
import br.com.motoristafinanceiro.notificacoes.AgendadorDeLembrete
import br.com.motoristafinanceiro.notificacoes.AgendadorDeNotificacoes
import br.com.motoristafinanceiro.notificacoes.EXTRA_ABRIR
import br.com.motoristafinanceiro.notificacoes.NotificadorAndroid
import br.com.motoristafinanceiro.notificacoes.destinoPedido
import br.com.motoristafinanceiro.ui.abertura.AberturaAnimada
import br.com.motoristafinanceiro.ui.ajustes.devePedirPermissaoAoAbrir
import br.com.motoristafinanceiro.ui.bloqueio.TelaBloqueio
import br.com.motoristafinanceiro.ui.bloqueio.deveBloquearAoVoltar
import br.com.motoristafinanceiro.ui.bloqueio.pedirAutenticacao
import br.com.motoristafinanceiro.ui.navigation.AppNav
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** FragmentActivity (e não só ComponentActivity) porque o pedido de digital do Android precisa dela. */
class MainActivity : FragmentActivity() {

    /** Tela pedida pelo Intent (toque na notificação); some depois de aberta. */
    private var destino by mutableStateOf<String?>(null)

    /** O app está coberto pela tela de bloqueio. */
    private var bloqueado by mutableStateOf(false)

    /** Até ler a configuração do bloqueio, a tela de abertura do Android fica na frente (nada aparece sem bloqueio). */
    private var lendoBloqueio = true

    /** Cópia do ajuste "Bloquear o app", atualizada sempre que muda. */
    private var bloqueioAtivo = false

    /** Quando o app saiu da tela (para bloquear ao voltar depois de um tempo); null = não saiu. */
    private var saiuEmMs: Long? = null

    private val pedirPermissaoDeNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* o estado é conferido em Ajustes */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tem que vir antes do super.onCreate: troca o tema de abertura pelo tema normal do app.
        installSplashScreen().setKeepOnScreenCondition { lendoBloqueio }
        super.onCreate(savedInstanceState)
        val app = application as MotoristaApp
        lifecycleScope.launch {
            app.configuracoes.bloqueioAtivo.collect { ativo ->
                // Na primeira leitura, ao abrir o app (girar a tela não bloqueia de novo: o estado é salvo).
                if (lendoBloqueio && savedInstanceState == null) bloqueado = ativo
                bloqueioAtivo = ativo
                lendoBloqueio = false
            }
        }
        destino = lerDestino(intent)
        // Quem chega pelo toque na notificação vai direto ao destino, sem esperar a animação.
        val comAbertura = destino == null
        AgendadorDeNotificacoes.agendarVerificacaoDiaria(applicationContext)
        AgendadorDeBackup.agendar(applicationContext)
        AgendadorDeBackup.rodarAgora(applicationContext)
        setContent {
            // Verifica os alertas de manutenção a cada mudança nos dados (e ao abrir o app).
            LaunchedEffect(Unit) { AgendadorDeNotificacoes.verificarACadaMudanca(app) }
            LaunchedEffect(Unit) { perguntarPermissaoUmaVez(app) }
            LaunchedEffect(Unit) { AgendadorDeLembrete.garantir(applicationContext, app.configuracoes) }
            val modoTema by app.configuracoes.modoTema.collectAsStateWithLifecycle(initialValue = ModoTema.PADRAO)
            MotoristaTheme(modo = modoTema) {
                // rememberSaveable: girar a tela depois da abertura não toca a animação de novo.
                var abertura by rememberSaveable { mutableStateOf(comAbertura) }
                Box(Modifier.fillMaxSize()) {
                    // O app carrega por baixo enquanto a animação toca.
                    AppNav(destinoPedido = destino, aoAbrirDestino = { destino = null })
                    // Por baixo da abertura: a animação toca inteira e, ao sumir, revela o bloqueio, que só então pede a digital.
                    if (bloqueado) {
                        TelaBloqueio(pedirAgora = !abertura, onDesbloquear = {
                            pedirAutenticacao(this@MainActivity, "Desbloquear o DriverFlow", aoConseguir = { bloqueado = false })
                        })
                    }
                    AnimatedVisibility(visible = abertura, exit = fadeOut(tween(300))) {
                        AberturaAnimada(aoTerminar = { abertura = false })
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Girar a tela também passa por aqui, mas volta em menos de 2 minutos e não bloqueia.
        saiuEmMs = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (deveBloquearAoVoltar(bloqueioAtivo, saiuEmMs, System.currentTimeMillis())) bloqueado = true
        saiuEmMs = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ESTADO_BLOQUEADO, bloqueado)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        bloqueado = savedInstanceState.getBoolean(ESTADO_BLOQUEADO, false)
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

    private companion object {
        const val ESTADO_BLOQUEADO = "bloqueado"
    }
}
