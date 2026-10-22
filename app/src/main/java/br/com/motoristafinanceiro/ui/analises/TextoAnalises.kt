package br.com.motoristafinanceiro.ui.analises

import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.domain.DesempenhoFaixa
import br.com.motoristafinanceiro.domain.FaixaHorario
import br.com.motoristafinanceiro.domain.ResumoMes
import br.com.motoristafinanceiro.util.formatarReais
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

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

/** Sigla do dia embaixo das barras do gráfico: "Seg", "Ter"... */
fun siglaDiaSemana(dia: DayOfWeek): String = rotuloDiaSemana(dia).take(3)

/** Valor em cima da barra, em reais inteiros por hora ("38"); vazio sem horas informadas. */
fun rotuloBarraPorHora(centavos: Long?): String =
    centavos?.takeIf { it > 0 }?.let { ((it + 50) / 100).toString() } ?: ""

/**
 * Percentual de cada valor no total, arredondado de modo que a soma dê 100 (o resto vai para os
 * maiores restos). Total zero: tudo 0.
 */
fun percentuais(valores: List<Long>): List<Int> {
    val total = valores.sum()
    if (total <= 0) return valores.map { 0 }
    val exatos = valores.map { it * 100.0 / total }
    val base = exatos.map { it.toInt() }.toMutableList()
    var falta = 100 - base.sum()
    exatos.indices.sortedByDescending { exatos[it] - base[it] }.forEach { i ->
        if (falta > 0) {
            base[i]++
            falta--
        }
    }
    return base
}

/** "06h–12h" (a Noite termina em "24h"). */
fun intervaloFaixa(f: FaixaHorario): String =
    f.inicioHora.toString().padStart(2, '0') + "h–" + f.fimHora.toString().padStart(2, '0') + "h"

/** "R$ 32,00/h · 5 turnos"; sem horas na faixa, "Sem turnos". */
fun textoFaixa(d: DesempenhoFaixa): String {
    val porHora = d.ganhoPorHoraCentavos ?: return "Sem turnos"
    val turnos = if (d.turnos == 1) "1 turno" else "${d.turnos} turnos"
    return "${porHora.formatarReais()}/h · $turnos"
}

fun textoMelhorFaixa(d: DesempenhoFaixa): String =
    "Melhor horário: ${d.faixa.rotulo.lowercase()} (${(d.ganhoPorHoraCentavos ?: 0L).formatarReais()}/h)"

/** Aviso dos lançamentos que não entram na análise por faixa; vazio quando todos têm horário. */
fun textoSemHorario(quantidade: Int): String = when (quantidade) {
    0 -> ""
    1 -> "1 lançamento sem horário de início (ou sem horas) não entra aqui."
    else -> "$quantidade lançamentos sem horário de início (ou sem horas) não entram aqui."
}

/** "out" (3 letras, minúsculas) embaixo das barras da evolução. */
fun siglaMes(mes: YearMonth): String = mes.month.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).take(3).lowercase()

/** Valor em cima da barra, em reais inteiros abreviados: "R$ 3,1 mil", "R$ 850", "−R$ 120". */
fun rotuloBarraLucro(centavos: Long): String {
    val reais = centavos / 100
    val sinal = if (reais < 0) "−" else ""
    val abs = abs(reais)
    val texto = if (abs >= 1_000) {
        val mil = abs / 100 / 10.0 // uma casa decimal
        (if (mil % 1.0 == 0.0) mil.toLong().toString() else mil.toString().replace('.', ',')) + " mil"
    } else {
        abs.toString()
    }
    return "${sinal}R$ $texto"
}

/** "R$ 31/h" embaixo do mês; vazio sem horas. */
fun rotuloPorHoraMes(centavos: Long?): String = centavos?.let { "R$ ${(it + 50) / 100}/h" } ?: ""

fun textoMelhorMes(m: ResumoMes): String =
    "Melhor mês: ${nomeMesCompleto(m.mes)} (${m.lucroCentavos.formatarReais()} de lucro)"

private fun nomeMesCompleto(mes: YearMonth): String = mes.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
