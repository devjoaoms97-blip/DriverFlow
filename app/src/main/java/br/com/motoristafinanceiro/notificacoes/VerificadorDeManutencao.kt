package br.com.motoristafinanceiro.notificacoes

import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.domain.alertasParaNotificar
import br.com.motoristafinanceiro.domain.alertasUrgentes
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.chavesParaGuardar
import br.com.motoristafinanceiro.domain.odometroAtual
import kotlinx.coroutines.flow.first

/**
 * Decide se é hora de avisar sobre manutenção. Não conhece o Android: a entrega da notificação
 * ([notificar], que devolve true se ela foi mesmo mostrada) e a remoção dela ([limpar]) chegam de fora,
 * o que permite testar a regra no PC.
 */
class VerificadorDeManutencao(
    private val repositorio: Repositorio,
    private val configuracoes: Configuracoes,
    private val notificar: (TextoNotificacao) -> Boolean,
    private val limpar: () -> Unit = {},
) {
    enum class Resultado { DESLIGADO, NADA_NOVO, NOTIFICOU, NAO_ENTREGUE }

    suspend fun verificar(): Resultado {
        if (!configuracoes.notificacoesManutencaoAtivas.first()) {
            limpar()
            return Resultado.DESLIGADO
        }

        val manutencoes = repositorio.manutencoes().first()
        val abastecimentos = repositorio.abastecimentos().first()
        val odometro = odometroAtual(abastecimentos, manutencoes)
        val alertas = if (odometro == null) emptyList() else calcularAlertas(manutencoes, odometro)
        val urgentes = alertasUrgentes(alertas)
        val jaNotificados = configuracoes.alertasNotificados.first()

        var resultado = Resultado.NADA_NOVO
        if (alertasParaNotificar(alertas, jaNotificados).isNotEmpty()) {
            val texto = montarTextoNotificacao(urgentes)
            if (texto != null) {
                // Sem permissão (ou notificações bloqueadas): não marca como avisado, para tentar de novo depois.
                if (!notificar(texto)) return Resultado.NAO_ENTREGUE
                resultado = Resultado.NOTIFICOU
            }
        }
        if (urgentes.isEmpty()) limpar()

        val atuais = chavesParaGuardar(alertas)
        if (atuais != jaNotificados) configuracoes.salvarAlertasNotificados(atuais)
        return resultado
    }
}
