package br.com.motoristafinanceiro.ui.componentes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class FormularioTest {

    @Test
    fun prefixoReais_mostraRSAntesDoValor() {
        val r = PrefixoReais.filter(AnnotatedString("248,90"))
        assertEquals("R$ 248,90", r.text.text)
    }

    @Test
    fun prefixoReais_cursorPulaOPrefixo() {
        val texto = "248,90"
        val m = PrefixoReais.filter(AnnotatedString(texto)).offsetMapping
        assertEquals(3, m.originalToTransformed(0))
        assertEquals(3 + texto.length, m.originalToTransformed(texto.length))
        assertEquals(0, m.transformedToOriginal(0)) // tocar no "R$" põe o cursor no começo
        assertEquals(0, m.transformedToOriginal(2))
        assertEquals(texto.length, m.transformedToOriginal(3 + texto.length))
    }

    @Test
    fun campoVazio_semPrefixo_paraNaoSobreporOExemplo() {
        // Bug visto no celular: "R$ " do campo e "R$ 0,00" do exemplo apareciam um por cima do outro.
        assertEquals(VisualTransformation.None, transformacaoDoValor(""))
        assertEquals(PrefixoReais, transformacaoDoValor("2"))
    }
}
