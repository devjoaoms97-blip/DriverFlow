package br.com.motoristafinanceiro.ui.meta

import br.com.motoristafinanceiro.domain.calcularProgressoMeta
import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TextoMetaTest {
    private val meta = 500_000L

    @Test
    fun emAndamento_mostraQuantoFaltaPorDia() {
        val p = calcularProgressoMeta(meta, 150_000, LocalDate.of(2026, 10, 15))

        assertEquals("Faltam ${20_589L.formatarReais()} por dia.", textoFaltaPorDia(p))
        assertEquals("Faltam ${350_000L.formatarReais()} em 17 dias (contando hoje).", textoDiasRestantes(p))
    }

    @Test
    fun ultimoDia_mostraOTotalQueFalta() {
        val p = calcularProgressoMeta(meta, 480_000, LocalDate.of(2026, 10, 31))

        assertEquals("Último dia do mês: faltam ${20_000L.formatarReais()}.", textoFaltaPorDia(p))
        assertEquals("Hoje é o último dia.", textoDiasRestantes(p))
    }

    @Test
    fun metaExata_avisaSemExcedente() {
        val p = calcularProgressoMeta(meta, meta, LocalDate.of(2026, 10, 15))

        assertEquals("Meta batida!", textoFaltaPorDia(p))
        assertEquals("", textoDiasRestantes(p))
    }

    @Test
    fun metaSuperada_mostraQuantoPassou() {
        val p = calcularProgressoMeta(meta, 560_000, LocalDate.of(2026, 10, 15))

        assertEquals("Meta batida! Você passou ${60_000L.formatarReais()} da meta.", textoFaltaPorDia(p))
        assertTrue(textoDiasRestantes(p).isEmpty())
    }

    @Test
    fun nomeDoMes_emMinusculas() = assertEquals("setembro", nomeDoMes(YearMonth.of(2026, 9)))

    @Test
    fun botaoDeRepetir_comMesEValor() = assertEquals(
        "Repetir a de setembro: ${500_000L.formatarReais()}",
        textoRepetirMeta(YearMonth.of(2026, 9), 500_000),
    )
}
