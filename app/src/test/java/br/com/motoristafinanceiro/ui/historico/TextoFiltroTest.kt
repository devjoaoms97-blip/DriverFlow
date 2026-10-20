package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Assert.assertEquals
import org.junit.Test

class TextoFiltroTest {
    @Test
    fun variosLancamentos() =
        assertEquals("Uber no mês: ${45_000L.formatarReais()} (3 lançamentos)", textoResumoFiltro(ResumoFiltro("Uber", 45_000, 3)))

    @Test
    fun umLancamento_singular() =
        assertEquals("Lavagem no mês: ${3_000L.formatarReais()} (1 lançamento)", textoResumoFiltro(ResumoFiltro("Lavagem", 3_000, 1)))

    @Test
    fun nenhumLancamento() =
        assertEquals("99 no mês: ${0L.formatarReais()} (0 lançamentos)", textoResumoFiltro(ResumoFiltro("99", 0, 0)))
}
