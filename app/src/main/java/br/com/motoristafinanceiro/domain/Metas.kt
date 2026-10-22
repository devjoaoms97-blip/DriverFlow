package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate
import java.time.YearMonth

/** Teto de sanidade da meta mensal: R$ 100.000,00. */
const val META_MAXIMA_CENTAVOS = 10_000_000L

/**
 * Converte o texto digitado na tela de meta ("5000" ou "5000,50") em centavos.
 * Retorna null para vazio, inválido, zero, negativo ou acima do teto.
 */
fun parseMetaMensal(texto: String): Long? {
    val centavos = texto.reaisParaCentavos() ?: return null
    return centavos.takeIf { it in 1..META_MAXIMA_CENTAVOS }
}

/** Quanto a meta equivale por dia se o lucro fosse igual em todos os dias do mês. */
fun metaPorDiaDoMesCentavos(metaCentavos: Long, mes: YearMonth): Long =
    Math.round(metaCentavos.toDouble() / mes.lengthOfMonth())

/** Situação da meta do mês em um dia específico. */
data class ProgressoMeta(
    val metaCentavos: Long,
    val lucroCentavos: Long,
    /** Quanto falta para bater a meta (nunca negativo). */
    val faltaCentavos: Long,
    /** Dias que ainda restam no mês, contando hoje. */
    val diasRestantes: Int,
    /** [faltaCentavos] dividido pelos [diasRestantes], arredondado para cima. */
    val faltaPorDiaCentavos: Long,
) {
    val atingida: Boolean get() = lucroCentavos >= metaCentavos

    /** Quanto passou da meta (0 se ainda não bateu). */
    val excedenteCentavos: Long get() = (lucroCentavos - metaCentavos).coerceAtLeast(0)

    /** Para a barra de progresso: entre 0 e 1. */
    val fracao: Float get() = (lucroCentavos.coerceAtLeast(0).toDouble() / metaCentavos).coerceIn(0.0, 1.0).toFloat()

    /** Percentual inteiro (arredondado para baixo). Passa de 100 quando a meta é superada. */
    val percentual: Int get() = (lucroCentavos.coerceAtLeast(0) * 100 / metaCentavos).toInt()
}

/**
 * Meta = lucro líquido do mês (mesma base do progresso do Início).
 * "Hoje" conta como dia restante: se hoje é dia 15 de um mês de 31 dias, restam 17 dias.
 */
fun calcularProgressoMeta(metaCentavos: Long, lucroCentavos: Long, hoje: LocalDate): ProgressoMeta {
    val falta = (metaCentavos - lucroCentavos).coerceAtLeast(0)
    val dias = hoje.lengthOfMonth() - hoje.dayOfMonth + 1
    return ProgressoMeta(
        metaCentavos = metaCentavos,
        lucroCentavos = lucroCentavos,
        faltaCentavos = falta,
        diasRestantes = dias,
        faltaPorDiaCentavos = (falta + dias - 1) / dias,
    )
}

/**
 * Meta que dá para repetir com um toque: a do mês anterior, quando o mês atual ainda não tem meta válida.
 * Null quando o mês atual já tem meta ou o anterior não tinha (ou tinha um valor fora do limite).
 */
fun metaParaRepetirCentavos(metaDoMes: Long?, metaDoMesAnterior: Long?): Long? {
    if (metaDoMes != null && metaDoMes > 0) return null
    return metaDoMesAnterior?.takeIf { it in 1..META_MAXIMA_CENTAVOS }
}
