package br.com.motoristafinanceiro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DinheiroTest {
    @Test
    fun reaisParaCentavos_aceitaVirgulaEPonto() {
        assertEquals(1250L, "12,50".reaisParaCentavos())
        assertEquals(1250L, "12.50".reaisParaCentavos())
        assertEquals(1200L, "12".reaisParaCentavos())
    }

    @Test
    fun reaisParaCentavos_arredondaMeioParaCima() {
        assertEquals(101L, "1,005".reaisParaCentavos())
    }

    @Test
    fun reaisParaCentavos_textoInvalido_retornaNulo() {
        assertNull("".reaisParaCentavos())
        assertNull("abc".reaisParaCentavos())
        assertNull("1,2,3".reaisParaCentavos())
    }

    @Test
    fun formatarReais_usaFormatoBrasileiro() {
        val texto = 123_450L.formatarReais().replace(Regex("[\\u00a0\\u202f]"), " ")
        assertEquals("R$ 1.234,50", texto)
    }
}
