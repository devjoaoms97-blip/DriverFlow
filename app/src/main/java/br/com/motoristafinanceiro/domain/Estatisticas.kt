package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import kotlin.math.roundToLong

/**
 * Corridas dos ganhos que informam a quantidade. O ganho por corrida usa só esses ganhos, e o tempo médio
 * só os que têm corridas e horas, para um lançamento sem o dado não puxar a média.
 */
data class ResumoCorridas(
    val corridas: Int = 0,
    val ganhoCentavos: Long = 0,
    val minutos: Int = 0,
    val corridasComMinutos: Int = 0,
) {
    val ganhoPorCorridaCentavos: Long?
        get() = if (corridas > 0) (ganhoCentavos.toDouble() / corridas).roundToLong() else null

    val minutosPorCorrida: Double?
        get() = if (corridasComMinutos > 0) minutos.toDouble() / corridasComMinutos else null
}

fun resumoCorridas(ganhos: List<Ganho>): ResumoCorridas {
    val comCorridas = ganhos.filter { (it.corridas ?: 0) > 0 }
    val comMinutos = comCorridas.filter { it.minutosOnline > 0 }
    return ResumoCorridas(
        corridas = comCorridas.sumOf { it.corridas ?: 0 },
        ganhoCentavos = comCorridas.sumOf { it.totalCentavos },
        minutos = comMinutos.sumOf { it.minutosOnline },
        corridasComMinutos = comMinutos.sumOf { it.corridas ?: 0 },
    )
}

data class Estatisticas(
    val ganhoBrutoCentavos: Long = 0,
    val despesasCentavos: Long = 0,
    /** Km de trabalho (dos ganhos). */
    val kmTotal: Double = 0.0,
    val minutosOnline: Int = 0,
    /** Depreciação dos km de trabalho. */
    val depreciacaoCentavos: Long = 0,
    /** Km de uso pessoal do carro no período. */
    val kmPessoal: Double = 0.0,
    /** Depreciação dos km pessoais (entra só no custo por km). */
    val depreciacaoPessoalCentavos: Long = 0,
    val corridas: ResumoCorridas = ResumoCorridas(),
) {
    val lucroLiquidoCentavos: Long get() = ganhoBrutoCentavos - despesasCentavos

    /** Lucro já descontando a depreciação estimada do carro. */
    val lucroAposDepreciacaoCentavos: Long get() = lucroLiquidoCentavos - depreciacaoCentavos

    val ganhoPorKmCentavos: Long?
        get() = if (kmTotal > 0) (ganhoBrutoCentavos / kmTotal).roundToLong() else null

    val ganhoPorHoraCentavos: Long?
        get() = if (minutosOnline > 0) (ganhoBrutoCentavos * 60.0 / minutosOnline).roundToLong() else null

    /** Km que o carro rodou no período: trabalho + uso pessoal. */
    val kmDoCarro: Double get() = kmTotal + kmPessoal

    /**
     * Custo por km = (despesas + depreciação) / km do carro. Os km pessoais entram no divisor porque as
     * despesas (combustível, seguro...) também pagam o uso pessoal.
     */
    val custoPorKmCentavos: Long?
        get() = if (kmDoCarro > 0) {
            ((despesasCentavos + depreciacaoCentavos + depreciacaoPessoalCentavos) / kmDoCarro).roundToLong()
        } else {
            null
        }

    /** Parte dos custos do período que foi para o uso pessoal (custo por km × km pessoais); null sem km pessoal. */
    val custoUsoPessoalCentavos: Long?
        get() = if (kmPessoal > 0) custoPorKmCentavos?.let { (it * kmPessoal).roundToLong() } else null
}

fun calcularEstatisticas(
    ganhos: List<Ganho>,
    despesas: List<Despesa>,
    depreciacaoPorKmCentavos: Long = 0,
    kmPessoais: List<KmPessoal> = emptyList(),
): Estatisticas {
    val km = ganhos.sumOf { it.km }
    val kmPessoal = kmPessoais.sumOf { it.km.coerceAtLeast(0.0) }
    return Estatisticas(
        ganhoBrutoCentavos = ganhos.sumOf { it.totalCentavos },
        despesasCentavos = despesas.sumOf { it.valorCentavos },
        kmTotal = km,
        minutosOnline = ganhos.sumOf { it.minutosOnline },
        depreciacaoCentavos = (km * depreciacaoPorKmCentavos).roundToLong(),
        kmPessoal = kmPessoal,
        depreciacaoPessoalCentavos = (kmPessoal * depreciacaoPorKmCentavos).roundToLong(),
        corridas = resumoCorridas(ganhos),
    )
}
