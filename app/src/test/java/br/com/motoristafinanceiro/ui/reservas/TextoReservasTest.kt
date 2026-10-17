package br.com.motoristafinanceiro.ui.reservas

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoReservasTest {
    @Test
    fun hoje() = assertEquals("Vence hoje", textoVencimento(0))

    @Test
    fun amanha() = assertEquals("Vence amanhã", textoVencimento(1))

    @Test
    fun emVariosDias() = assertEquals("Vence em 12 dias", textoVencimento(12))
}
