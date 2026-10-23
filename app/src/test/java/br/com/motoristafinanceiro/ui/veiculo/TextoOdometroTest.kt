package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.domain.ConferenciaKm
import br.com.motoristafinanceiro.domain.LeituraOdometro
import br.com.motoristafinanceiro.domain.Odometro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            "Última leitura: 48.500 km em 19/10 + 220 km lançados",
            detalheOdometro(Odometro(48_720, leitura)),
        )
        assertNull(detalheOdometro(Odometro(48_500, leitura)))
        assertNull(detalheOdometro(null))
    }

    private fun conferencia(kmFim: Int, trabalho: Double, pessoal: Double = 0.0) = ConferenciaKm(
        inicio = LeituraOdometro(10_000, LocalDate.of(2026, 9, 1)),
        fim = LeituraOdometro(kmFim, LocalDate.of(2026, 10, 1)),
        kmTrabalho = trabalho,
        kmPessoal = pessoal,
    )

    @Test
    fun conferencia_emDia_semAtencao() {
        val t = textoConferencia(conferencia(11_000, 900.0, 60.0))
        assertEquals("96% lançados", t.selo)
        assertFalse(t.atencao)
        assertEquals(
            "De 01/09 a 01/10 o odômetro andou 1.000 km e você lançou 960 km (60 km pessoais).",
            t.detalhe,
        )
    }

    @Test
    fun conferencia_kmEsquecidos_pedeAtencao() {
        val t = textoConferencia(conferencia(11_000, 700.0))
        assertTrue(t.atencao)
        assertEquals("70% lançados", t.selo)
        assertTrue(t.detalhe, t.detalhe.contains("300 km ficaram sem lançamento"))
    }

    @Test
    fun conferencia_lancouAMais_pedeAtencao() {
        val t = textoConferencia(conferencia(11_000, 1_200.0))
        assertTrue(t.atencao)
        assertTrue(t.detalhe, t.detalhe.contains("200 km a mais do que o odômetro mostra"))
    }
}
