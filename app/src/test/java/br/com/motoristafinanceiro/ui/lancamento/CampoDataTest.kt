package br.com.motoristafinanceiro.ui.lancamento

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CampoDataTest {
    private val hoje = LocalDate.of(2026, 10, 21)

    @Test
    fun rotuloData_hojeOntemOuSoAData() {
        assertEquals("Hoje · 21/10/2026", rotuloData(hoje, hoje))
        assertEquals("Ontem · 20/10/2026", rotuloData(hoje.minusDays(1), hoje))
        assertEquals("05/10/2026", rotuloData(LocalDate.of(2026, 10, 5), hoje))
    }
}
