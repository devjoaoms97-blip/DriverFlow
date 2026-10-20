package br.com.motoristafinanceiro.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TextoSemanaTest {
    @Test
    fun semanaNoMesmoMes() = assertEquals("12/10 a 18/10", rotuloSemana(LocalDate.of(2026, 10, 15)))

    @Test
    fun semanaQueViraOMes() = assertEquals("28/09 a 04/10", rotuloSemana(LocalDate.of(2026, 10, 2)))

    @Test
    fun domingoMostraASemanaQueTermina() = assertEquals("12/10 a 18/10", rotuloSemana(LocalDate.of(2026, 10, 18)))
}
