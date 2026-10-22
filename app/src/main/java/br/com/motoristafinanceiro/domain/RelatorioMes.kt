package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import java.time.LocalDate
import java.time.YearMonth

/** Uma linha da lista de lançamentos do relatório; [valorCentavos] negativo nas despesas. */
data class LinhaRelatorio(val data: LocalDate, val descricao: String, val valorCentavos: Long)

/** Tudo o que vai no PDF do mês, já calculado (o desenho do PDF só posiciona). */
data class RelatorioMes(
    val mes: YearMonth,
    val totais: Estatisticas,
    val diasTrabalhados: Int,
    /** Ganho bruto por plataforma, da maior para a menor. */
    val porPlataforma: List<Pair<Plataforma, Long>>,
    /** Despesas por categoria, da maior para a menor. */
    val porCategoria: List<Pair<CategoriaDespesa, Long>>,
    /** Por data (mais antiga primeiro); no mesmo dia, ganhos antes das despesas. */
    val linhas: List<LinhaRelatorio>,
) {
    val vazio: Boolean get() = linhas.isEmpty()
}

/** Monta o relatório com os lançamentos do mês (os de fora do mês são ignorados). */
fun montarRelatorioMes(mes: YearMonth, ganhos: List<Ganho>, despesas: List<Despesa>): RelatorioMes {
    val g = ganhos.filter { YearMonth.from(it.data) == mes }
    val d = despesas.filter { YearMonth.from(it.data) == mes }
    val linhas = g.map { LinhaRelatorio(it.data, "Ganho " + it.plataforma.rotulo, it.totalCentavos) } +
        d.map { LinhaRelatorio(it.data, it.categoria.rotulo, -it.valorCentavos) }
    return RelatorioMes(
        mes = mes,
        totais = calcularEstatisticas(g, d),
        diasTrabalhados = g.map { it.data }.distinct().size,
        porPlataforma = g.groupBy { it.plataforma }
            .map { (p, lista) -> p to lista.sumOf { it.totalCentavos } }
            .sortedByDescending { it.second },
        porCategoria = d.groupBy { it.categoria }
            .map { (c, lista) -> c to lista.sumOf { it.valorCentavos } }
            .sortedByDescending { it.second },
        linhas = linhas.sortedWith(compareBy<LinhaRelatorio> { it.data }.thenBy { it.valorCentavos < 0 }),
    )
}
