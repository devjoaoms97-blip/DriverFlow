package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import java.time.YearMonth

/** Meses mostrados no gráfico de evolução das Análises (o atual e os anteriores). */
const val MESES_NA_EVOLUCAO = 6

/** Resumo de um mês para o gráfico de evolução. */
data class ResumoMes(
    val mes: YearMonth,
    val lucroCentavos: Long,
    /** Ganho bruto ÷ horas online do mês; null sem horas. */
    val ganhoPorHoraCentavos: Long?,
    val temLancamentos: Boolean,
)

/**
 * Os [meses] meses até [ateMes] (incluído), do mais antigo para o mais novo; meses sem lançamento vêm zerados.
 * Lucro = ganhos − despesas do mês (mesma conta do Início); R$/h = ganho bruto ÷ horas online.
 */
fun evolucaoMensal(
    ganhos: List<Ganho>,
    despesas: List<Despesa>,
    ateMes: YearMonth,
    meses: Int = MESES_NA_EVOLUCAO,
): List<ResumoMes> {
    val ganhosPorMes = ganhos.groupBy { YearMonth.from(it.data) }
    val despesasPorMes = despesas.groupBy { YearMonth.from(it.data) }
    return (meses - 1 downTo 0).map { atras ->
        val mes = ateMes.minusMonths(atras.toLong())
        val g = ganhosPorMes[mes].orEmpty()
        val d = despesasPorMes[mes].orEmpty()
        val e = calcularEstatisticas(g, d)
        ResumoMes(mes, e.lucroLiquidoCentavos, e.ganhoPorHoraCentavos, g.isNotEmpty() || d.isNotEmpty())
    }
}

/** Mês fechado de maior lucro (o mês em curso não entra: ainda não acabou). Null com menos de 2 meses fechados com lançamento. */
fun melhorMes(evolucao: List<ResumoMes>, mesAtual: YearMonth): ResumoMes? {
    val fechados = evolucao.filter { it.mes < mesAtual && it.temLancamentos }
    if (fechados.size < 2) return null
    return fechados.maxByOrNull { it.lucroCentavos }
}
