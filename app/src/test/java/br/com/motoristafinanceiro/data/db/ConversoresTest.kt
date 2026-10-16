package br.com.motoristafinanceiro.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConversoresTest {
    private val c = Conversores()
    private val dia = LocalDate.of(2026, 10, 12)

    @Test
    fun data_ida_e_volta() {
        assertEquals(dia.toEpochDay(), c.dataParaLong(dia))
        assertEquals(dia, c.longParaData(c.dataParaLong(dia)))
    }

    @Test
    fun nulo_continua_nulo() {
        assertNull(c.dataParaLong(null))
        assertNull(c.longParaData(null))
    }
}
