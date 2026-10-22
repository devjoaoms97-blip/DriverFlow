package br.com.motoristafinanceiro.leitor

import br.com.motoristafinanceiro.domain.AvaliacaoCorrida
import br.com.motoristafinanceiro.domain.CorOferta
import br.com.motoristafinanceiro.domain.LimitesCorrida
import br.com.motoristafinanceiro.domain.OfertaCorrida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextoLeitorTest {
    private val nbsp = " "

    private val completa = AvaliacaoCorrida(
        oferta = OfertaCorrida(1_450, kmAteBusca = 2.1, minAteBusca = 6, kmViagem = 8.4, minViagem = 18, nota = 4.9),
        porKmCentavos = 138,
        porHoraCentavos = 3_625,
        lucroCentavos = 610,
        cor = CorOferta.VERMELHO,
    )

    @Test
    fun titulo_porKm_ouOValorSemDistancia() {
        assertEquals("R$${nbsp}1,38/km", tituloAvaliacao(completa))
        assertEquals("R$${nbsp}14,50", tituloAvaliacao(completa.copy(porKmCentavos = null)))
    }

    @Test
    fun linhas_completas() {
        assertEquals("R$${nbsp}14,50 · R$${nbsp}36,25/h · lucro ≈ R$${nbsp}6,10", linhaValores(completa))
        assertEquals("10,5 km no total (2,1 km até o passageiro) · 24 min · ★ 4,9", linhaPercurso(completa))
    }

    @Test
    fun linhas_soComOQueOCardMostrou() {
        val simples = completa.copy(oferta = OfertaCorrida(900, kmViagem = 4.5, nota = 5.0), porHoraCentavos = null, lucroCentavos = null)
        assertEquals("R$${nbsp}9,00", linhaValores(simples))
        assertEquals("4,5 km no total · ★ 5", linhaPercurso(simples))
    }

    @Test
    fun limitesDosCampos_validaEConverte() {
        assertEquals(LimitesCorrida(220, 160), limitesDosCampos("2,20", "1.60"))
        assertNull(limitesDosCampos("1,50", "2,00")) // amarelo acima do verde
        assertNull(limitesDosCampos("", "1,50"))
        assertNull(limitesDosCampos("2,00", "0"))
    }

    @Test
    fun tamanhoDaGravacao() {
        assertEquals("Nada gravado ainda.", textoTamanhoGravacao(0))
        assertEquals("Gravação: 500 bytes", textoTamanhoGravacao(500))
        assertEquals("Gravação: 12 KB", textoTamanhoGravacao(12 * 1024 + 100))
        assertEquals("Gravação: 2,5 MB", textoTamanhoGravacao((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun servicoLigado_procuraNaListaDoAndroid() {
        val nome = "br.com.motoristafinanceiro/br.com.motoristafinanceiro.leitor.LeitorDeCorridasService"
        val curto = "br.com.motoristafinanceiro/.leitor.LeitorDeCorridasService"
        assertTrue(servicoLigado("com.outro/.Servico:$nome", nome, curto))
        assertTrue(servicoLigado(curto, nome, curto))
        assertFalse(servicoLigado("com.outro/.Servico", nome, curto))
        assertFalse(servicoLigado(null, nome, curto))
    }

    @Test
    fun coresDaJanela_saoDiferentes() {
        assertEquals(3, CorOferta.entries.map { corDeFundo(it) }.toSet().size)
    }
}
