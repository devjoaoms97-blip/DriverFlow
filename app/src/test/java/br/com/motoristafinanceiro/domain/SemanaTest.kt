package br.com.motoristafinanceiro.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SemanaTest {
    // 12/10/2026 é segunda-feira; 18/10/2026 é domingo.
    private val segunda = LocalDate.of(2026, 10, 12)
    private val domingo = LocalDate.of(2026, 10, 18)

    @Test
    fun segundaEhOInicioDaPropriaSemana() {
        assertEquals(segunda, inicioDaSemana(segunda))
    }

    @Test
    fun domingoPertenceASemanaQueComecouNaSegundaAnterior() {
        assertEquals(segunda, inicioDaSemana(domingo))
        assertEquals(domingo, fimDaSemana(domingo))
    }

    @Test
    fun meioDaSemana() {
        val quinta = LocalDate.of(2026, 10, 15)
        assertEquals(segunda, inicioDaSemana(quinta))
        assertEquals(domingo, fimDaSemana(quinta))
    }

    @Test
    fun semanaQueViraOMes() {
        // 30/09/2026 é quarta: a semana vai de 28/09 a 04/10
        val quarta = LocalDate.of(2026, 9, 30)
        assertEquals(LocalDate.of(2026, 9, 28), inicioDaSemana(quarta))
        assertEquals(LocalDate.of(2026, 10, 4), fimDaSemana(quarta))
    }

    @Test
    fun semanaQueViraOAno() {
        // 01/01/2027 é sexta: a semana vai de 28/12/2026 a 03/01/2027
        val sexta = LocalDate.of(2027, 1, 1)
        assertEquals(LocalDate.of(2026, 12, 28), inicioDaSemana(sexta))
        assertEquals(LocalDate.of(2027, 1, 3), fimDaSemana(sexta))
    }
}
