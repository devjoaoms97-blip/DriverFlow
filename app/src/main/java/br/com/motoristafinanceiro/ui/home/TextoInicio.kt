package br.com.motoristafinanceiro.ui.home

import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.ui.analises.rotuloDiaSemana
import br.com.motoristafinanceiro.ui.reservas.ReservasUiState
import br.com.motoristafinanceiro.ui.reservas.textoVencimento
import br.com.motoristafinanceiro.ui.veiculo.resumoTexto
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBR = Locale("pt", "BR")
private val formatoDiaEMes = DateTimeFormatter.ofPattern("d 'de' MMMM", ptBR)

/** Título e detalhe de um aviso do Início (manutenção, reservas). */
data class TextoAviso(val titulo: String, val detalhe: String)

/** Linha acima do título do Início: "Quarta, 21 de outubro". */
fun rotuloDataInicio(hoje: LocalDate): String =
    rotuloDiaSemana(hoje.dayOfWeek) + ", " + hoje.format(formatoDiaEMes)

/** "Hoje", "Semana" e "Mês", na ordem do seletor do Início. */
val periodosInicio = listOf("Hoje", "Semana", "Mês")

/** Título do cartão principal para o período escolhido no seletor (0 = hoje, 1 = semana, 2 = mês). */
fun tituloLucroPeriodo(periodo: Int, rotuloSemana: String): String = when (periodo) {
    0 -> "Lucro líquido de hoje"
    1 -> if (rotuloSemana.isBlank()) "Lucro líquido da semana" else "Lucro líquido da semana ($rotuloSemana)"
    else -> "Lucro líquido do mês"
}

/** Km e horas do período: "120 km · 8 h", só um dos dois, ou "—" sem nenhum. */
fun textoRodado(e: Estatisticas): String = listOfNotNull(
    e.kmTotal.takeIf { it > 0 }?.let { "${it.paraTextoNumero()} km" },
    e.minutosOnline.takeIf { it > 0 }?.let { "${it.minutosParaHorasTexto()} h" },
).joinToString(" · ").ifEmpty { "—" }

/** Aviso de manutenção do Início; null sem alertas. Mostra no máximo os dois mais urgentes no detalhe. */
fun textoAtencaoManutencao(alertas: List<AlertaManutencao>): TextoAviso? {
    if (alertas.isEmpty()) return null
    val titulo = if (alertas.size == 1) "1 manutenção pede atenção" else "${alertas.size} manutenções pedem atenção"
    val detalhe = alertas.take(2).joinToString(" · ") {
        descricaoManutencao(it.manutencao) + ": " + it.resumoTexto().replaceFirstChar { c -> c.lowercase() }
    }
    return TextoAviso(titulo, detalhe)
}

/** Aviso de reservas do Início: quanto separar hoje e o próximo vencimento, ou o convite para cadastrar. */
fun textoAvisoReservas(r: ReservasUiState): TextoAviso {
    val proxima = r.itens.firstOrNull()
        ?: return TextoAviso("Reservas", "Cadastre IPVA, seguro e outras despesas fixas")
    return TextoAviso(
        "Reservas: separe ${r.totalDiarioCentavos.formatarReais()} hoje",
        "Próxima: ${proxima.reserva.nome} · " + textoVencimento(proxima.diasParaVencer).replaceFirstChar { it.lowercase() },
    )
}
