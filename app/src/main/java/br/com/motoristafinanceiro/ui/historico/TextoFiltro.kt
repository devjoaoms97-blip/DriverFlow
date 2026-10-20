package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.util.formatarReais

/** "Uber no mês: R$ 450,00 (3 lançamentos)". */
fun textoResumoFiltro(r: ResumoFiltro): String {
    val lancamentos = if (r.quantidade == 1) "1 lançamento" else "${r.quantidade} lançamentos"
    return "${r.rotulo} no mês: ${r.totalCentavos.formatarReais()} ($lancamentos)"
}
