package br.com.motoristafinanceiro.ui.analises

import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.util.formatarReais
import java.time.DayOfWeek

fun rotuloDiaSemana(dia: DayOfWeek): String = when (dia) {
    DayOfWeek.MONDAY -> "Segunda"
    DayOfWeek.TUESDAY -> "Terça"
    DayOfWeek.WEDNESDAY -> "Quarta"
    DayOfWeek.THURSDAY -> "Quinta"
    DayOfWeek.FRIDAY -> "Sexta"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}

/** "1 dia" ou "5 dias". */
fun textoDias(quantidade: Int): String = if (quantidade == 1) "1 dia" else "$quantidade dias"

/** "R$ 32,00/h · 5 dias · média R$ 180,00/dia"; sem horas informadas troca o primeiro trecho por "sem horas". */
fun textoDesempenhoDia(d: DesempenhoDia): String {
    val e = d.desempenho
    if (e.lancamentos == 0) return "Sem lançamentos"
    val partes = buildList {
        add(e.ganhoPorHoraCentavos?.let { "${it.formatarReais()}/h" } ?: "sem horas")
        add(textoDias(e.diasTrabalhados))
        e.mediaPorDiaCentavos?.let { add("média ${it.formatarReais()}/dia") }
    }
    return partes.joinToString(" · ")
}

fun textoMelhorDia(d: DesempenhoDia): String =
    "Melhor dia por hora: ${rotuloDiaSemana(d.diaSemana)} (${(d.desempenho.ganhoPorHoraCentavos ?: 0L).formatarReais()}/h)"

/** Tamanho da barra (0 a 1) de um valor em relação ao maior valor da lista. */
fun fracaoDaBarra(valor: Long?, maximo: Long?): Float {
    if (valor == null || maximo == null || maximo <= 0 || valor <= 0) return 0f
    return (valor.toDouble() / maximo).coerceIn(0.0, 1.0).toFloat()
}
