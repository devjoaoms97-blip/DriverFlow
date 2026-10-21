package br.com.motoristafinanceiro.ui.theme

import br.com.motoristafinanceiro.data.ModoTema
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoDoTemaTest {

    @Test
    fun escuro_ignoraOModoDoCelular() {
        assertTrue(temaEscuro(ModoTema.ESCURO, sistemaEscuro = false))
        assertTrue(temaEscuro(ModoTema.ESCURO, sistemaEscuro = true))
    }

    @Test
    fun claro_ignoraOModoDoCelular() {
        assertFalse(temaEscuro(ModoTema.CLARO, sistemaEscuro = false))
        assertFalse(temaEscuro(ModoTema.CLARO, sistemaEscuro = true))
    }

    @Test
    fun automatico_segueOCelular() {
        assertTrue(temaEscuro(ModoTema.AUTOMATICO, sistemaEscuro = true))
        assertFalse(temaEscuro(ModoTema.AUTOMATICO, sistemaEscuro = false))
    }
}
