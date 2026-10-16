package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistoricoEstadoTest {
    private val mes = YearMonth.of(2026, 10)
    private val d10 = LocalDate.of(2026, 10, 10)
    private val d12 = LocalDate.of(2026, 10, 12)

    private val ganhos = listOf(
        Ganho(id = 1, data = d10, plataforma = Plataforma.UBER, valorCentavos = 10_000, gorjetaBonusCentavos = 500),
    )
    private val despesas = listOf(
        Despesa(id = 1, data = d10, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 3_000),
        Despesa(id = 2, data = d12, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 2_000),
    )

    @Test
    fun todos_agrupaPorDia_maisRecentePrimeiro_comSaldo() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.TODOS, ganhos, despesas)

        assertEquals(listOf(d12, d10), s.dias.map { it.data })
        assertEquals(-2_000L, s.dias[0].saldoCentavos)
        assertEquals(7_500L, s.dias[1].saldoCentavos) // 10000 + 500 - 3000
        assertTrue(s.dias[1].itens[0] is ItemHistorico.DeGanho)
        assertTrue(s.dias[1].itens[1] is ItemHistorico.DeDespesa)
    }

    @Test
    fun filtroGanhos_escondeDespesas_masResumoContinuaDoMesInteiro() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhos, despesas)

        assertEquals(listOf(d10), s.dias.map { it.data })
        assertEquals(10_500L, s.dias[0].saldoCentavos)
        assertEquals(5_500L, s.resumo.lucroLiquidoCentavos) // 10500 - 3000 - 2000
    }

    @Test
    fun filtroDespesas_escondeGanhos() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.DESPESAS, ganhos, despesas)

        assertEquals(2, s.dias.size)
        assertEquals(-3_000L, s.dias[1].saldoCentavos)
        assertTrue(s.dias.all { dia -> dia.itens.all { it is ItemHistorico.DeDespesa } })
    }

    @Test
    fun semLancamentos_listaVazia() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.TODOS, emptyList(), emptyList())
        assertTrue(s.dias.isEmpty())
        assertEquals(0L, s.resumo.lucroLiquidoCentavos)
    }

    @Test
    fun podeAvancar_soQuandoMesEhAnteriorAoAtual() {
        assertTrue(montarEstadoHistorico(mes, FiltroHistorico.TODOS, emptyList(), emptyList(), mesAtual = YearMonth.of(2026, 11)).podeAvancar)
        assertFalse(montarEstadoHistorico(mes, FiltroHistorico.TODOS, emptyList(), emptyList(), mesAtual = mes).podeAvancar)
    }
}
