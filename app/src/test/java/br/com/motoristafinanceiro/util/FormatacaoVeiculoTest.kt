package br.com.motoristafinanceiro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatacaoVeiculoTest {
    @Test
    fun kmPorLitro_umaCasaDecimal() {
        assertEquals("13,3 km/l", 13.3333.formatarKmPorLitro())
        assertEquals("12,5 km/l", 12.5.formatarKmPorLitro())
    }

    @Test
    fun km_comSeparadorDeMilhar() {
        assertEquals("10.400 km", 10_400.formatarKm())
        assertEquals("950 km", 950.formatarKm())
    }
}
