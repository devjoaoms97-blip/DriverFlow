package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.domain.FaixaHorario.MADRUGADA
import br.com.motoristafinanceiro.domain.FaixaHorario.MANHA
import br.com.motoristafinanceiro.domain.FaixaHorario.NOITE
import br.com.motoristafinanceiro.domain.FaixaHorario.TARDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FaixasHorarioTest {
    private val dia = LocalDate.of(2026, 10, 20)

    private fun h(hora: Int, minuto: Int = 0) = hora * 60 + minuto

    private fun ganho(valor: Long, inicio: Int?, minutos: Int, gorjeta: Long = 0) = Ganho(
        data = dia, plataforma = Plataforma.UBER, valorCentavos = valor,
        gorjetaBonusCentavos = gorjeta, minutosOnline = minutos, inicioTurnoMinutos = inicio,
    )

    @Test
    fun turnoDentroDeUmaFaixa() = assertEquals(mapOf(TARDE to 180), minutosPorFaixa(h(13), 180))

    @Test
    fun turnoQuePassaDeUmaFaixaParaOutra() =
        assertEquals(mapOf(TARDE to 60, NOITE to 120), minutosPorFaixa(h(17), 180))

    @Test
    fun turnoQuePassaDaMeiaNoite() =
        assertEquals(mapOf(NOITE to 120, MADRUGADA to 120), minutosPorFaixa(h(22), 240))

    @Test
    fun turnoLongoQueCruzaVariasFaixas() =
        assertEquals(mapOf(MADRUGADA to 60, MANHA to 360, TARDE to 360, NOITE to 60), minutosPorFaixa(h(5), 840))

    @Test
    fun semDuracao_nada() = assertTrue(minutosPorFaixa(h(10), 0).isEmpty())

    @Test
    fun ganhoRepartidoNaProporcaoDoTempo() {
        // R$ 90,00 (com gorjeta) das 17h às 20h: 1 h de tarde (R$ 30) e 2 h de noite (R$ 60).
        val faixas = analisarPorFaixa(listOf(ganho(8_000, h(17), 180, gorjeta = 1_000)))

        assertEquals(listOf(MADRUGADA, MANHA, TARDE, NOITE), faixas.map { it.faixa })
        val tarde = faixas.single { it.faixa == TARDE }
        val noite = faixas.single { it.faixa == NOITE }
        assertEquals(3_000L, tarde.ganhoCentavos)
        assertEquals(6_000L, noite.ganhoCentavos)
        assertEquals(3_000L, tarde.ganhoPorHoraCentavos)
        assertEquals(3_000L, noite.ganhoPorHoraCentavos)
        assertEquals(1, noite.turnos)
        assertNull(faixas.single { it.faixa == MANHA }.ganhoPorHoraCentavos)
    }

    @Test
    fun somaVariosTurnosNaMesmaFaixa() {
        val noite = analisarPorFaixa(
            listOf(ganho(6_000, h(18), 120), ganho(12_000, h(19), 180))
        ).single { it.faixa == NOITE }
        assertEquals(18_000L, noite.ganhoCentavos)
        assertEquals(300, noite.minutos)
        assertEquals(2, noite.turnos)
        assertEquals(3_600L, noite.ganhoPorHoraCentavos)
    }

    @Test
    fun semHorarioOuSemHoras_ficamDeFora() {
        val ganhos = listOf(ganho(5_000, null, 120), ganho(5_000, h(9), 0), ganho(5_000, h(9), 60))
        val manha = analisarPorFaixa(ganhos).single { it.faixa == MANHA }
        assertEquals(5_000L, manha.ganhoCentavos)
        assertEquals(1, manha.turnos)
        assertEquals(2, ganhosSemHorario(ganhos))
    }

    @Test
    fun melhorFaixa_precisaDeDuasFaixasComHoras() {
        val umaSo = analisarPorFaixa(listOf(ganho(6_000, h(13), 120)))
        assertNull(melhorFaixa(umaSo))

        val duas = analisarPorFaixa(listOf(ganho(6_000, h(13), 120), ganho(10_000, h(19), 120)))
        assertEquals(NOITE, melhorFaixa(duas)!!.faixa)
    }
}
