package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RelatorioMesTest {
    private val outubro = YearMonth.of(2026, 10)
    private val d5 = LocalDate.of(2026, 10, 5)
    private val d6 = LocalDate.of(2026, 10, 6)

    private val ganhos = listOf(
        Ganho(data = d6, plataforma = Plataforma.UBER, valorCentavos = 20_000, gorjetaBonusCentavos = 1_000, km = 100.0, minutosOnline = 300),
        Ganho(data = d5, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 8_000),
        Ganho(data = d5, plataforma = Plataforma.UBER, valorCentavos = 5_000),
        Ganho(data = LocalDate.of(2026, 9, 30), plataforma = Plataforma.UBER, valorCentavos = 99_999), // fora do mês
    )
    private val despesas = listOf(
        Despesa(data = d5, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000),
        Despesa(data = d6, categoria = CategoriaDespesa.ALIMENTACAO, valorCentavos = 2_500),
        Despesa(data = d6, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 4_000),
    )

    @Test
    fun totaisDoMes_ignoramOutrosMeses() {
        val r = montarRelatorioMes(outubro, ganhos, despesas)
        assertEquals(34_000L, r.totais.ganhoBrutoCentavos)
        assertEquals(12_500L, r.totais.despesasCentavos)
        assertEquals(21_500L, r.totais.lucroLiquidoCentavos)
        assertEquals(100.0, r.totais.kmTotal, 0.0)
        assertEquals(300, r.totais.minutosOnline)
        assertEquals(2, r.diasTrabalhados)
    }

    @Test
    fun porPlataformaEPorCategoria_doMaiorParaOMenor() {
        val r = montarRelatorioMes(outubro, ganhos, despesas)
        assertEquals(listOf(Plataforma.UBER to 26_000L, Plataforma.NOVE_NOVE to 8_000L), r.porPlataforma)
        assertEquals(listOf(CategoriaDespesa.COMBUSTIVEL to 10_000L, CategoriaDespesa.ALIMENTACAO to 2_500L), r.porCategoria)
    }

    @Test
    fun linhas_porDataComGanhosAntesDasDespesas_eDespesaNegativa() {
        val linhas = montarRelatorioMes(outubro, ganhos, despesas).linhas
        assertEquals(6, linhas.size)
        assertEquals(listOf(d5, d5, d5, d6, d6, d6), linhas.map { it.data })
        assertTrue(linhas.take(2).all { it.valorCentavos > 0 }) // ganhos do dia 5 primeiro
        assertEquals(LinhaRelatorio(d5, "Combustível", -6_000), linhas[2])
        assertEquals("Ganho Uber", linhas[3].descricao)
        assertEquals(21_000L, linhas[3].valorCentavos) // com gorjeta
    }

    @Test
    fun mesSemLancamentos_vazio() {
        val r = montarRelatorioMes(YearMonth.of(2026, 8), ganhos, despesas)
        assertTrue(r.vazio)
        assertEquals(0L, r.totais.lucroLiquidoCentavos)
    }
}
