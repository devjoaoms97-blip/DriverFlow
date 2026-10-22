package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.domain.LeituraOdometro
import br.com.motoristafinanceiro.domain.Odometro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TextoOdometroTest {
    private val leitura = LeituraOdometro(48_500, LocalDate.of(2026, 10, 19))

    @Test
    fun rotulo_estimadoOuAtual() {
        assertEquals("Odômetro estimado", rotuloOdometro(Odometro(48_720, leitura)))
        assertEquals("Odômetro atual", rotuloOdometro(Odometro(48_500, leitura)))
        assertEquals("Odômetro atual", rotuloOdometro(null))
    }

    @Test
    fun detalhe_mostraALeituraEOsKmDosGanhos() {
        assertEquals(
            "Última leitura: 48.500 km em 19/10 + 220 km dos ganhos",
            detalheOdometro(Odometro(48_720, leitura)),
        )
        assertNull(detalheOdometro(Odometro(48_500, leitura)))
        assertNull(detalheOdometro(null))
    }
}
