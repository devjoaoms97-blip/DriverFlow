package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class EvolucaoTest {
    private val outubro = YearMonth.of(2026, 10)

    private fun ganho(data: LocalDate, valor: Long, minutos: Int = 0) =
        Ganho(data = data, plataforma = Plataforma.UBER, valorCentavos = valor, minutosOnline = minutos)

    private fun despesa(data: LocalDate, valor: Long) =
        Despesa(data = data, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = valor)

    @Test
    fun seisMeses_doMaisAntigoAoAtual_semDadosZerados() {
        val meses = evolucaoMensal(emptyList(), emptyList(), outubro)
        assertEquals((5 downTo 0).map { outubro.minusMonths(it.toLong()) }, meses.map { it.mes })
        assertTrue(meses.all { it.lucroCentavos == 0L && it.ganhoPorHoraCentavos == null && !it.temLancamentos })
    }

    @Test
    fun lucroEPorHoraDeCadaMes() {
        val meses = evolucaoMensal(
            listOf(
                ganho(LocalDate.of(2026, 9, 3), 300_000, minutos = 6_000), // R$ 3.000 em 100 h
                ganho(LocalDate.of(2026, 10, 1), 100_000),
            ),
            listOf(despesa(LocalDate.of(2026, 9, 10), 80_000)),
            outubro,
        )
        val setembro = meses.single { it.mes == YearMonth.of(2026, 9) }
        assertEquals(220_000L, setembro.lucroCentavos)
        assertEquals(3_000L, setembro.ganhoPorHoraCentavos)
        assertNull(meses.last().ganhoPorHoraCentavos) // outubro sem horas
        assertEquals(100_000L, meses.last().lucroCentavos)
    }

    @Test
    fun mesSoComDespesa_contaComoPrejuizo() {
        val m = evolucaoMensal(emptyList(), listOf(despesa(LocalDate.of(2026, 8, 5), 30_000)), outubro)
            .single { it.mes == YearMonth.of(2026, 8) }
        assertEquals(-30_000L, m.lucroCentavos)
        assertTrue(m.temLancamentos)
    }

    @Test
    fun lancamentosForaDaJanela_ficamDeFora() {
        val meses = evolucaoMensal(listOf(ganho(LocalDate.of(2026, 3, 1), 999_999)), emptyList(), outubro)
        assertFalse(meses.any { it.temLancamentos })
    }

    @Test
    fun melhorMes_ignoraOMesEmAndamento_ePrecisaDeDoisFechados() {
        val meses = evolucaoMensal(
            listOf(
                ganho(LocalDate.of(2026, 8, 1), 200_000),
                ganho(LocalDate.of(2026, 9, 1), 350_000),
                ganho(LocalDate.of(2026, 10, 1), 900_000), // mês atual: não conta
            ),
            emptyList(),
            outubro,
        )
        assertEquals(YearMonth.of(2026, 9), melhorMes(meses, outubro)!!.mes)

        val soUm = evolucaoMensal(listOf(ganho(LocalDate.of(2026, 9, 1), 350_000)), emptyList(), outubro)
        assertNull(melhorMes(soUm, outubro))
    }
}
