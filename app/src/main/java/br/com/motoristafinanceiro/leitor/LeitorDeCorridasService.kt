package br.com.motoristafinanceiro.leitor

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.domain.LimitesCorrida
import br.com.motoristafinanceiro.domain.OfertaCorrida
import br.com.motoristafinanceiro.domain.avaliarOferta
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.lerOferta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Leitor de corridas: quando a Uber/99 mostra uma oferta, lê os textos do card e mostra por cima a conta
 * (R$/km com o deslocamento até o passageiro, R$/h, lucro estimado e a cor pelas faixas de Ajustes).
 * Só lê e mostra: nunca toca, aceita nem recusa nada no outro app.
 */
class LeitorDeCorridasService : AccessibilityService() {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val principal = Handler(Looper.getMainLooper())
    private lateinit var janela: JanelaDaOferta
    private lateinit var gravador: GravadorDeTela

    private var gravando = false
    private var limites = LimitesCorrida()
    private var custoPorKmCentavos: Long? = null
    private var custoCalculadoEm = Long.MIN_VALUE
    private var leituraAgendada = false
    private var ofertaNaTela: OfertaCorrida? = null
    private var ultimaVezComOferta = 0L

    private val app get() = application as MotoristaApp

    override fun onServiceConnected() {
        janela = JanelaDaOferta(this)
        gravador = GravadorDeTela(File(filesDir, GravadorDeTela.PASTA))
        escopo.launch { app.configuracoes.leitorGravando.collect { gravando = it } }
        escopo.launch { app.configuracoes.limitesCorrida.collect { limites = it } }
        atualizarCustoSePreciso()
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        if (evento?.packageName?.toString() !in PACOTES) return
        // Os eventos chegam em rajadas: lê a tela uma vez a cada INTERVALO_MS.
        if (leituraAgendada) return
        leituraAgendada = true
        principal.postDelayed(::lerTela, INTERVALO_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        principal.removeCallbacksAndMessages(null)
        if (::janela.isInitialized) janela.esconder()
        escopo.cancel()
        super.onDestroy()
    }

    private fun lerTela() {
        leituraAgendada = false
        val agora = SystemClock.elapsedRealtime()
        val telas = telasDosApps()
        telas.forEach { (pacote, nos) ->
            if (gravando) {
                val linhas = nos.map { it.paraGravacao() }
                escopo.launch(Dispatchers.IO) { gravador.registrar(System.currentTimeMillis(), pacote, linhas) }
            }
        }
        val oferta = telas.firstNotNullOfOrNull { (_, nos) -> lerOferta(nos.map { it.texto }) }
        when {
            oferta != null -> {
                ultimaVezComOferta = agora
                if (oferta != ofertaNaTela || !janela.visivel) {
                    ofertaNaTela = oferta
                    mostrar(oferta)
                    agendarSumico()
                }
            }
            ofertaNaTela != null && agora - ultimaVezComOferta >= SUMIR_SEM_OFERTA_MS -> esconder()
        }
    }

    private fun mostrar(oferta: OfertaCorrida) {
        janela.mostrar(avaliarOferta(oferta, custoPorKmCentavos, limites))
        atualizarCustoSePreciso()
    }

    private fun esconder() {
        ofertaNaTela = null
        janela.esconder()
    }

    /** Se a tela parar de mudar (sem eventos), a janela some sozinha depois de TEMPO_MAXIMO_MS. */
    private fun agendarSumico() {
        principal.removeCallbacks(sumir)
        principal.postDelayed(sumir, TEMPO_MAXIMO_MS)
    }

    private val sumir = Runnable { esconder() }

    /** Custo por km dos últimos 30 dias (despesas + depreciação ÷ km), recalculado a cada 10 minutos. */
    private fun atualizarCustoSePreciso() {
        val agora = SystemClock.elapsedRealtime()
        if (agora - custoCalculadoEm < RECALCULAR_CUSTO_MS) return
        custoCalculadoEm = agora
        escopo.launch {
            val custo = withContext(Dispatchers.IO) {
                runCatching {
                    val hoje = LocalDate.now()
                    val inicio = hoje.minusDays(29)
                    calcularEstatisticas(
                        app.repositorio.ganhos(inicio, hoje).first(),
                        app.repositorio.despesas(inicio, hoje).first(),
                        app.configuracoes.depreciacaoPorKmCentavos.first(),
                    ).custoPorKmCentavos
                }.getOrNull()
            }
            custoPorKmCentavos = custo
            ofertaNaTela?.let { if (janela.visivel) janela.mostrar(avaliarOferta(it, custo, limites)) }
        }
    }

    /** Textos visíveis de cada janela da Uber/99 (o card pode estar numa janela separada do mapa). */
    private fun telasDosApps(): List<Pair<String, List<NoDeTela>>> {
        val raizes = runCatching { windows.mapNotNull { it.root } }.getOrDefault(emptyList())
            .ifEmpty { listOfNotNull(rootInActiveWindow) }
        return raizes.mapNotNull { raiz ->
            val pacote = raiz.packageName?.toString()
            if (pacote !in PACOTES) return@mapNotNull null
            val nos = mutableListOf<NoDeTela>()
            coletar(raiz, nos)
            if (nos.isEmpty()) null else pacote!! to nos
        }
    }

    private fun coletar(no: AccessibilityNodeInfo, saida: MutableList<NoDeTela>) {
        if (saida.size >= MAXIMO_DE_NOS || !no.isVisibleToUser) return
        val texto = no.text?.toString()?.trim().orEmpty()
        val descricao = no.contentDescription?.toString()?.trim().orEmpty()
        if (texto.isNotEmpty() || descricao.isNotEmpty()) {
            saida += NoDeTela(
                texto = texto.ifEmpty { descricao },
                descricao = descricao.takeIf { it.isNotEmpty() && texto.isNotEmpty() && it != texto },
                classe = no.className?.toString()?.substringAfterLast('.'),
                id = no.viewIdResourceName?.substringAfterLast('/'),
            )
        }
        for (i in 0 until no.childCount) no.getChild(i)?.let { coletar(it, saida) }
    }

    companion object {
        /** Uber Driver e 99 Motorista. */
        val PACOTES = setOf("com.ubercab.driver", "com.app99.driver")
        private const val INTERVALO_MS = 250L
        private const val SUMIR_SEM_OFERTA_MS = 1_500L
        private const val TEMPO_MAXIMO_MS = 30_000L
        private const val RECALCULAR_CUSTO_MS = 10 * 60_000L
        private const val MAXIMO_DE_NOS = 400
    }
}

/** Um texto da tela, com o que ajuda a reconhecer o card na gravação (tipo e id do componente). */
internal data class NoDeTela(val texto: String, val descricao: String?, val classe: String?, val id: String?) {
    fun paraGravacao(): String = buildString {
        id?.let { append('[').append(it).append("] ") }
        classe?.let { append(it).append(": ") }
        append(texto)
        descricao?.let { append("  (desc: ").append(it).append(')') }
    }
}
