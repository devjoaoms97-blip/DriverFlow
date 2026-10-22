package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiglasHistoricoTest {

    @Test
    fun plataformas_siglasCurtasEDiferentes() {
        val siglas = Plataforma.entries.map { siglaPlataforma(it) }
        assertEquals("U", siglaPlataforma(Plataforma.UBER))
        assertEquals("99", siglaPlataforma(Plataforma.NOVE_NOVE))
        assertEquals(siglas.size, siglas.toSet().size)
        assertTrue(siglas.all { it.length in 1..2 })
    }

    @Test
    fun categorias_umaSiglaDiferenteParaCada() {
        val siglas = CategoriaDespesa.entries.map { siglaCategoria(it) }
        assertEquals("CB", siglaCategoria(CategoriaDespesa.COMBUSTIVEL))
        assertEquals(siglas.size, siglas.toSet().size)
        assertTrue(siglas.all { it.length == 2 })
    }

    @Test
    fun saldoDoDia_comSinal() {
        assertEquals("+ " + 18_640L.formatarReais(), textoSaldo(18_640))
        assertEquals("− " + 2_000L.formatarReais(), textoSaldo(-2_000))
        assertEquals("+ " + 0L.formatarReais(), textoSaldo(0))
    }
}
