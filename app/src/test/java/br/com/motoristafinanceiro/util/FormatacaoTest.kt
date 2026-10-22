package br.com.motoristafinanceiro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatacaoTest {
    @Test
    fun reais_viraTextoEvoltaParaCentavos() {
        assertEquals("12,50", 1250L.paraTextoReais())
        assertEquals("0,05", 5L.paraTextoReais())
        assertEquals("100,00", 10_000L.paraTextoReais())
        assertEquals(1250L, "12,50".reaisParaCentavos())
        assertEquals(1250L, 1250L.paraTextoReais().reaisParaCentavos())
    }

    @Test
    fun numero_semCasasQuandoInteiro() {
        assertEquals("50", 50.0.paraTextoNumero())
        assertEquals("50,5", 50.5.paraTextoNumero())
    }

    @Test
    fun minutos_viramHorasDecimais() {
        assertEquals("4", 240.minutosParaHorasTexto())
        assertEquals("6,5", 390.minutosParaHorasTexto())
        assertEquals("1,67", 100.minutosParaHorasTexto())
    }

    @Test
    fun minutosParaHorario_comZerosAEsquerda() {
        assertEquals("00:00", 0.minutosParaHorario())
        assertEquals("08:05", (8 * 60 + 5).minutosParaHorario())
        assertEquals("18:30", (18 * 60 + 30).minutosParaHorario())
        assertEquals("23:59", 1439.minutosParaHorario())
    }
}
