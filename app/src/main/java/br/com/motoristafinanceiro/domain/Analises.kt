package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToLong

/** Período analisado na tela Análises. */
enum class PeriodoAnalise(val rotulo: String, val dias: Int?) {
    ULTIMOS_30("30 dias", 30),
    ULTIMOS_90("90 dias", 90),
    TUDO("Tudo", null),
}

/** Primeiro dia do período; o último é sempre hoje. "Tudo" começa em 2000 só para ter um limite. */
fun PeriodoAnalise.inicio(hoje: LocalDate): LocalDate =
    dias?.let { hoje.minusDays(it - 1L) } ?: LocalDate.of(2000, 1, 1)

/**
 * Números de um conjunto de ganhos (um dia da semana, uma plataforma ou o total).
 *
 * Ganho por hora usa só os lançamentos que têm horas informadas, e ganho por km só os que têm km.
 * Assim um lançamento sem horas não infla o valor por hora.
 */
data class Desempenho(
    val ganhoBrutoCentavos: Long = 0,
    val lancamentos: Int = 0,
    /** Datas diferentes com ganho lançado. */
    val diasTrabalhados: Int = 0,
    val ganhoComHorasCentavos: Long = 0,
    val minutosOnline: Int = 0,
    val ganhoComKmCentavos: Long = 0,
    val kmTotal: Double = 0.0,
) {
    val ganhoPorHoraCentavos: Long?
        get() = if (minutosOnline > 0) (ganhoComHorasCentavos * 60.0 / minutosOnline).roundToLong() else null

    val ganhoPorKmCentavos: Long?
        get() = if (kmTotal > 0) (ganhoComKmCentavos / kmTotal).roundToLong() else null

    /** Ganho bruto médio por dia em que houve trabalho. */
    val mediaPorDiaCentavos: Long?
        get() = if (diasTrabalhados > 0) (ganhoBrutoCentavos.toDouble() / diasTrabalhados).roundToLong() else null

    /** Ganho bruto médio por lançamento (turno). */
    val mediaPorLancamentoCentavos: Long?
        get() = if (lancamentos > 0) (ganhoBrutoCentavos.toDouble() / lancamentos).roundToLong() else null
}

fun desempenhoDe(ganhos: List<Ganho>): Desempenho {
    val comHoras = ganhos.filter { it.minutosOnline > 0 }
    val comKm = ganhos.filter { it.km > 0 }
    return Desempenho(
        ganhoBrutoCentavos = ganhos.sumOf { it.totalCentavos },
        lancamentos = ganhos.size,
        diasTrabalhados = ganhos.map { it.data }.distinct().size,
        ganhoComHorasCentavos = comHoras.sumOf { it.totalCentavos },
        minutosOnline = comHoras.sumOf { it.minutosOnline },
        ganhoComKmCentavos = comKm.sumOf { it.totalCentavos },
        kmTotal = comKm.sumOf { it.km },
    )
}

data class DesempenhoDia(val diaSemana: DayOfWeek, val desempenho: Desempenho)

data class DesempenhoPlataforma(val plataforma: Plataforma, val desempenho: Desempenho)

/** Sempre 7 itens, de segunda a domingo; dias sem lançamento vêm zerados. */
fun analisarPorDiaDaSemana(ganhos: List<Ganho>): List<DesempenhoDia> =
    DayOfWeek.values().map { dia ->
        DesempenhoDia(dia, desempenhoDe(ganhos.filter { it.data.dayOfWeek == dia }))
    }

/** Só as plataformas que têm ganho, da que mais rendeu para a que menos rendeu. */
fun analisarPorPlataforma(ganhos: List<Ganho>): List<DesempenhoPlataforma> =
    ganhos.groupBy { it.plataforma }
        .map { (plataforma, lista) -> DesempenhoPlataforma(plataforma, desempenhoDe(lista)) }
        .sortedByDescending { it.desempenho.ganhoBrutoCentavos }

/**
 * Dia da semana com maior ganho por hora. Só há "melhor" quando pelo menos dois dias da semana
 * têm horas informadas (com um só não há o que comparar). Empate: vence o dia que vem primeiro na semana.
 */
fun melhorDiaPorHora(dias: List<DesempenhoDia>): DesempenhoDia? {
    val comHora = dias.filter { it.desempenho.ganhoPorHoraCentavos != null }
    if (comHora.size < 2) return null
    return comHora.maxByOrNull { it.desempenho.ganhoPorHoraCentavos ?: Long.MIN_VALUE }
}
