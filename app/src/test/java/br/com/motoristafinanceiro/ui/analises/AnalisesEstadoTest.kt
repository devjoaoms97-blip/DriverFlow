package br.com.motoristafinanceiro.ui.analises

import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AnalisesEstadoTest {
    private val seg12 = LocalDate.of(2026, 10, 12)
    private val sex16 = LocalDate.of(2026, 10, 16)

    @Test
    fun semGanhos_semDadosMasComOsSeteDias() {
        val s = montarEstadoAnalises(PeriodoAnalise.ULTIMOS_30, emptyList())

        assertFalse(s.temDados)
        assertEquals(7, s.dias.size)
        assertTrue(s.plataformas.isEmpty())
        assertNull(s.melhorDia)
    }

    @Test
    fun comGanhos_montaTotalDiasPlataformasEMelhorDia() {
        val ganhos = listOf(
            Ganho(data = seg12, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = 50.0, minutosOnline = 120),
            Ganho(data = sex16, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 6_000, km = 20.0, minutosOnline = 60),
        )

        val s = montarEstadoAnalises(PeriodoAnalise.ULTIMOS_90, ganhos)

        assertTrue(s.temDados)
        assertEquals(PeriodoAnalise.ULTIMOS_90, s.periodo)
        assertEquals(16_000L, s.total.ganhoBrutoCentavos)
        assertEquals(2, s.total.diasTrabalhados)
        assertEquals(listOf(Plataforma.UBER, Plataforma.NOVE_NOVE), s.plataformas.map { it.plataforma })
        assertEquals(DayOfWeek.FRIDAY, s.melhorDia?.diaSemana) // 6000/h contra 5000/h
    }
}
