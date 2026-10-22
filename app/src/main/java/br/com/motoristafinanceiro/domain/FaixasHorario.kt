package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Ganho
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

const val MINUTOS_NO_DIA = 24 * 60

/** Faixas do dia usadas nas Análises; [inicioHora] incluída, [fimHora] não. */
enum class FaixaHorario(val rotulo: String, val inicioHora: Int, val fimHora: Int) {
    MADRUGADA("Madrugada", 0, 6),
    MANHA("Manhã", 6, 12),
    TARDE("Tarde", 12, 18),
    NOITE("Noite", 18, 24),
}

/** Ganho e minutos que caíram numa faixa, somando todos os turnos com horário de início. */
data class DesempenhoFaixa(
    val faixa: FaixaHorario,
    val ganhoCentavos: Long = 0,
    val minutos: Int = 0,
    /** Turnos que passaram por esta faixa (um turno das 17h às 20h conta na Tarde e na Noite). */
    val turnos: Int = 0,
) {
    val ganhoPorHoraCentavos: Long?
        get() = if (minutos > 0) (ganhoCentavos * 60.0 / minutos).roundToLong() else null
}

/**
 * Minutos de um turno que caem em cada faixa. O turno começa em [inicioMinutos] e dura [duracaoMinutos];
 * passa da meia-noite normalmente (um turno das 22h às 2h tem 2 h de Noite e 2 h de Madrugada).
 */
fun minutosPorFaixa(inicioMinutos: Int, duracaoMinutos: Int): Map<FaixaHorario, Int> {
    if (duracaoMinutos <= 0) return emptyMap()
    val fim = inicioMinutos + duracaoMinutos
    val resultado = mutableMapOf<FaixaHorario, Int>()
    // Cada dia que o turno toca: 0 = o dia em que começou, 1 = o seguinte...
    for (dia in 0..(fim - 1) / MINUTOS_NO_DIA) {
        val base = dia * MINUTOS_NO_DIA
        FaixaHorario.entries.forEach { f ->
            val sobreposicao = min(fim, base + f.fimHora * 60) - max(inicioMinutos, base + f.inicioHora * 60)
            if (sobreposicao > 0) resultado[f] = (resultado[f] ?: 0) + sobreposicao
        }
    }
    return resultado
}

/**
 * Ganho por hora em cada faixa, sempre 4 itens (madrugada a noite). O ganho de cada turno é repartido
 * entre as faixas na proporção do tempo que passou em cada uma. Só entram ganhos com horário de início e
 * horas online; os demais não dizem em que hora o dinheiro entrou.
 */
fun analisarPorFaixa(ganhos: List<Ganho>): List<DesempenhoFaixa> {
    val acumulado = FaixaHorario.entries.associateWith { DesempenhoFaixa(it) }.toMutableMap()
    ganhos.filter { it.inicioTurnoMinutos != null && it.minutosOnline > 0 }.forEach { g ->
        minutosPorFaixa(g.inicioTurnoMinutos!!, g.minutosOnline).forEach { (faixa, minutos) ->
            val atual = acumulado.getValue(faixa)
            acumulado[faixa] = atual.copy(
                ganhoCentavos = atual.ganhoCentavos + (g.totalCentavos * minutos.toDouble() / g.minutosOnline).roundToLong(),
                minutos = atual.minutos + minutos,
                turnos = atual.turnos + 1,
            )
        }
    }
    return FaixaHorario.entries.map { acumulado.getValue(it) }
}

/** Faixa com maior ganho por hora; só existe quando pelo menos duas faixas têm horas (senão não há comparação). */
fun melhorFaixa(faixas: List<DesempenhoFaixa>): DesempenhoFaixa? {
    val comHora = faixas.filter { it.ganhoPorHoraCentavos != null }
    if (comHora.size < 2) return null
    return comHora.maxByOrNull { it.ganhoPorHoraCentavos ?: Long.MIN_VALUE }
}

/** Ganhos que não entram na análise por faixa (sem horário de início ou sem horas online). */
fun ganhosSemHorario(ganhos: List<Ganho>): Int =
    ganhos.count { it.inicioTurnoMinutos == null || it.minutosOnline <= 0 }
