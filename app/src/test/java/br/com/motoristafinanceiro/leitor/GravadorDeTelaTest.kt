package br.com.motoristafinanceiro.leitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GravadorDeTelaTest {
    @get:Rule
    val pasta = TemporaryFolder()

    private val card = listOf("TextView: R$ 14,50", "TextView: 6 min (2,1 km)")
    private fun gravador(limite: Long = GravadorDeTela.LIMITE_PADRAO_BYTES) =
        GravadorDeTela(File(pasta.root, GravadorDeTela.PASTA), limite)

    @Test
    fun gravaATelaComPacoteEHora() {
        val g = gravador()
        assertTrue(g.registrar(0L, "com.ubercab.driver", card))
        val texto = g.arquivo.readText()
        assertTrue(texto.startsWith("=== "))
        assertTrue(texto.contains("com.ubercab.driver"))
        assertTrue(texto.contains("TextView: R$ 14,50\nTextView: 6 min (2,1 km)\n"))
    }

    @Test
    fun pulaTelaRepetidaETelaSemValorNemKm() {
        val g = gravador()
        assertTrue(g.registrar(0L, "p", card))
        assertFalse(g.registrar(1L, "p", card))
        assertFalse(g.registrar(2L, "p", listOf("Você está online")))
        assertTrue(g.registrar(3L, "p", listOf("Siga por 2 km")))
    }

    @Test
    fun paraNoLimiteDeTamanho() {
        val g = gravador(limite = 10)
        assertTrue(g.registrar(0L, "p", card))
        assertFalse(g.registrar(1L, "p", listOf("R$ 9,00")))
    }

    @Test
    fun apagar_zeraEPermiteGravarDeNovo() {
        val g = gravador()
        g.registrar(0L, "p", card)
        g.apagar()
        assertEquals(0L, g.tamanhoBytes())
        assertTrue(g.registrar(1L, "p", card))
    }
}
