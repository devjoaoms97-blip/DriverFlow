package br.com.motoristafinanceiro.notificacoes

import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.ui.veiculo.resumoTexto

/** Textos da notificação: [resumo] na versão fechada e [detalhes] (uma linha por serviço) ao expandir. */
data class TextoNotificacao(val titulo: String, val resumo: String, val detalhes: String)

/** Null quando não há nada urgente. Recebe todos os alertas urgentes, não só os novos. */
fun montarTextoNotificacao(urgentes: List<AlertaManutencao>): TextoNotificacao? {
    if (urgentes.isEmpty()) return null
    val ordenados = urgentes.sortedBy { it.kmRestantes }
    val linhas = ordenados.map { "${descricaoManutencao(it.manutencao)} — ${it.resumoTexto()}" }
    if (ordenados.size == 1) {
        val titulo = if (ordenados[0].status == StatusAlerta.ATRASADA) "Manutenção atrasada" else "Manutenção próxima"
        return TextoNotificacao(titulo, linhas[0], linhas[0])
    }
    return TextoNotificacao(
        titulo = "${ordenados.size} manutenções precisam de atenção",
        resumo = "${linhas[0]} e mais ${ordenados.size - 1}",
        detalhes = linhas.joinToString("\n"),
    )
}
