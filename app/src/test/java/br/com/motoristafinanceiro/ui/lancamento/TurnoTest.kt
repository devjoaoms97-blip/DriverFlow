package br.com.motoristafinanceiro.ui.lancamento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnoTest {

    private fun h(hora: Int, minuto: Int = 0) = hora * 60 + minuto

    @Test
    fun duracao_noMesmoDia() = assertEquals(240, duracaoTurno(h(17), h(21)))

    @Test
    fun duracao_passandoDaMeiaNoite() = assertEquals(240, duracaoTurno(h(22), h(2)))

    @Test
    fun duracao_comMinutos() = assertEquals(390, duracaoTurno(h(6, 15), h(12, 45)))

    @Test
    fun duracao_semUmDosHorariosOuFimIgualAoInicio_ehNula() {
        assertNull(duracaoTurno(h(17), null))
        assertNull(duracaoTurno(null, h(21)))
        assertNull(duracaoTurno(h(17), h(17)))
    }

    @Test
    fun fimDoTurno_ehInicioMaisMinutos() {
        assertEquals(h(21), fimDoTurno(h(17), 240))
        assertEquals(h(2), fimDoTurno(h(22), 240)) // passa da meia-noite
        assertNull(fimDoTurno(h(17), 0)) // sem horas não há fim
        assertNull(fimDoTurno(null, 240))
    }

    @Test
    fun idaEVolta_salvarEEditarDevolveOsMesmosHorarios() {
        val minutos = duracaoTurno(h(22, 30), h(3, 15))!!
        assertEquals(h(3, 15), fimDoTurno(h(22, 30), minutos))
    }

    @Test
    fun estado_comInicioEFim_horasSaoADiferenca() {
        val e = estadoTurno(h(17), h(21))
        assertEquals(240, e.minutosOnline)
        assertEquals("4 h de turno", e.texto)
        assertFalse(e.erro)
    }

    @Test
    fun estado_inicioEFimSubstituemAsHorasDeUmLancamentoAntigo() =
        assertEquals(240, estadoTurno(h(17), h(21), minutosOriginais = 480).minutosOnline)

    @Test
    fun estado_soOInicio_salvaSemHorasEAvisa() {
        val e = estadoTurno(h(17), null)
        assertEquals(0, e.minutosOnline)
        assertFalse(e.erro)
        assertTrue(e.texto.contains("Sem o fim"))
    }

    @Test
    fun estado_soOFimOuFimIgualAoInicio_naoDeixaSalvar() {
        assertTrue(estadoTurno(null, h(21)).erro)
        assertTrue(estadoTurno(h(17), h(17)).erro)
    }

    @Test
    fun estado_lancamentoAntigoSemHorario_mantemAsHoras() {
        val e = estadoTurno(null, null, minutosOriginais = 390)
        assertEquals(390, e.minutosOnline)
        assertEquals("6,5 h online (lançado sem horário)", e.texto)
        assertFalse(e.erro)
    }

    @Test
    fun estado_novoSemHorario_semHorasESemTexto() {
        val e = estadoTurno(null, null)
        assertEquals(0, e.minutosOnline)
        assertEquals("", e.texto)
        assertFalse(e.erro)
    }
}
