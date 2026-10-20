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
import org.junit.Assert.assertNull

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

    // ---- Filtros por plataforma (Ganhos) e categoria (Despesas) ----

    private val ganhosVarios = listOf(
        Ganho(id = 1, data = d10, plataforma = Plataforma.UBER, valorCentavos = 10_000, gorjetaBonusCentavos = 500),
        Ganho(id = 2, data = d10, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 8_000),
        Ganho(id = 3, data = d12, plataforma = Plataforma.UBER, valorCentavos = 5_000),
    )
    private val despesasVarias = listOf(
        Despesa(id = 1, data = d10, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 3_000),
        Despesa(id = 2, data = d12, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 2_000),
        Despesa(id = 3, data = d12, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 4_000),
    )

    @Test
    fun plataforma_filtraSoOsGanhosDaquelaPlataforma() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias, plataforma = Plataforma.UBER)

        assertEquals(listOf(d12, d10), s.dias.map { it.data })
        assertEquals(2, s.dias.sumOf { it.itens.size })
        assertTrue(s.dias.all { dia -> dia.itens.all { (it as ItemHistorico.DeGanho).ganho.plataforma == Plataforma.UBER } })
        assertEquals(Plataforma.UBER, s.plataforma)
    }

    @Test
    fun plataforma_resumoSomaGanhosComGorjetaEConta() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias, plataforma = Plataforma.UBER)

        assertEquals(ResumoFiltro("Uber", 15_500, 2), s.resumoFiltro) // 10000 + 500 + 5000
    }

    @Test
    fun plataformaSemLancamentos_listaVaziaEResumoZerado() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias, plataforma = Plataforma.INDRIVE)

        assertTrue(s.dias.isEmpty())
        assertEquals(ResumoFiltro("InDrive", 0, 0), s.resumoFiltro)
    }

    @Test
    fun categoria_filtraSoAsDespesasDaquelaCategoria() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.DESPESAS, ganhosVarios, despesasVarias, categoria = CategoriaDespesa.COMBUSTIVEL)

        assertEquals(2, s.dias.sumOf { it.itens.size })
        assertTrue(s.dias.all { dia -> dia.itens.all { (it as ItemHistorico.DeDespesa).despesa.categoria == CategoriaDespesa.COMBUSTIVEL } })
        assertEquals(ResumoFiltro("Combustível", 7_000, 2), s.resumoFiltro)
        assertEquals(CategoriaDespesa.COMBUSTIVEL, s.categoria)
    }

    @Test
    fun saldoDoDia_considerasoOsItensExibidos() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.DESPESAS, ganhosVarios, despesasVarias, categoria = CategoriaDespesa.LAVAGEM)

        assertEquals(listOf(d12), s.dias.map { it.data })
        assertEquals(-2_000L, s.dias[0].saldoCentavos)
    }

    @Test
    fun plataforma_ignoradaForaDoFiltroGanhos() {
        val todos = montarEstadoHistorico(mes, FiltroHistorico.TODOS, ganhosVarios, despesasVarias, plataforma = Plataforma.UBER)
        val despesas = montarEstadoHistorico(mes, FiltroHistorico.DESPESAS, ganhosVarios, despesasVarias, plataforma = Plataforma.UBER)

        assertEquals(6, todos.dias.sumOf { it.itens.size }) // 3 ganhos + 3 despesas, nada escondido
        assertNull(todos.plataforma)
        assertNull(todos.resumoFiltro)
        assertEquals(3, despesas.dias.sumOf { it.itens.size })
        assertNull(despesas.resumoFiltro)
    }

    @Test
    fun categoria_ignoradaForaDoFiltroDespesas() {
        val s = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias, categoria = CategoriaDespesa.LAVAGEM)

        assertEquals(3, s.dias.sumOf { it.itens.size })
        assertNull(s.categoria)
        assertNull(s.resumoFiltro)
    }

    @Test
    fun semRefino_semResumoDeFiltro() {
        assertNull(montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias).resumoFiltro)
        assertNull(montarEstadoHistorico(mes, FiltroHistorico.DESPESAS, ganhosVarios, despesasVarias).resumoFiltro)
    }

    @Test
    fun resumoDoMes_naoMudaComOsRefinos() {
        val sem = montarEstadoHistorico(mes, FiltroHistorico.TODOS, ganhosVarios, despesasVarias)
        val com = montarEstadoHistorico(mes, FiltroHistorico.GANHOS, ganhosVarios, despesasVarias, plataforma = Plataforma.UBER)

        assertEquals(sem.resumo, com.resumo)
        assertEquals(14_500L, com.resumo.lucroLiquidoCentavos) // 23500 de ganhos - 9000 de despesas
    }
}
