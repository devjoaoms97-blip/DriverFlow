package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import kotlin.math.roundToLong

data class Estatisticas(
    val ganhoBrutoCentavos: Long = 0,
    val despesasCentavos: Long = 0,
    val kmTotal: Double = 0.0,
    val minutosOnline: Int = 0,
    val depreciacaoCentavos: Long = 0,
) {
    val lucroLiquidoCentavos: Long get() = ganhoBrutoCentavos - despesasCentavos

    /** Lucro já descontando a depreciação estimada do carro. */
    val lucroAposDepreciacaoCentavos: Long get() = lucroLiquidoCentavos - depreciacaoCentavos

    val ganhoPorKmCentavos: Long?
        get() = if (kmTotal > 0) (ganhoBrutoCentavos / kmTotal).roundToLong() else null

    val ganhoPorHoraCentavos: Long?
        get() = if (minutosOnline > 0) (ganhoBrutoCentavos * 60.0 / minutosOnline).roundToLong() else null

    /** Custo por km = (despesas + depreciação) / km rodado. */
    val custoPorKmCentavos: Long?
        get() = if (kmTotal > 0) ((despesasCentavos + depreciacaoCentavos) / kmTotal).roundToLong() else null
}

fun calcularEstatisticas(
    ganhos: List<Ganho>,
    despesas: List<Despesa>,
    depreciacaoPorKmCentavos: Long = 0,
): Estatisticas {
    val km = ganhos.sumOf { it.km }
    return Estatisticas(
        ganhoBrutoCentavos = ganhos.sumOf { it.totalCentavos },
        despesasCentavos = despesas.sumOf { it.valorCentavos },
        kmTotal = km,
        minutosOnline = ganhos.sumOf { it.minutosOnline },
        depreciacaoCentavos = (km * depreciacaoPorKmCentavos).roundToLong(),
    )
}
