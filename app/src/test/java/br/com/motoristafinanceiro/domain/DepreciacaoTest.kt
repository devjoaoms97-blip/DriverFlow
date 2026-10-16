package br.com.motoristafinanceiro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DepreciacaoTest {
    @Test
    fun valorNormal_viraCentavos() {
        assertEquals(25L, parseDepreciacaoPorKm("0,25"))
        assertEquals(25L, parseDepreciacaoPorKm("0.25"))
        assertEquals(100L, parseDepreciacaoPorKm("1"))
    }

    @Test
    fun vazioOuZero_significaSemDepreciacao() {
        assertEquals(0L, parseDepreciacaoPorKm(""))
        assertEquals(0L, parseDepreciacaoPorKm("   "))
        assertEquals(0L, parseDepreciacaoPorKm("0"))
    }

    @Test
    fun textoInvalidoOuNegativo_retornaNulo() {
        assertNull(parseDepreciacaoPorKm("abc"))
        assertNull(parseDepreciacaoPorKm("1,2,3"))
        assertNull(parseDepreciacaoPorKm("-0,10"))
    }

    @Test
    fun acimaDoTeto_retornaNulo() {
        assertEquals(DEPRECIACAO_MAXIMA_POR_KM_CENTAVOS, parseDepreciacaoPorKm("10"))
        assertNull(parseDepreciacaoPorKm("10,01"))
        assertNull(parseDepreciacaoPorKm("11"))
    }

    @Test
    fun fracaoDeCentavo_arredondaMeioParaCima() {
        assertEquals(26L, parseDepreciacaoPorKm("0,255"))
    }
}
