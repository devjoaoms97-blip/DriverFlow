package br.com.motoristafinanceiro.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ModoTemaTest {

    @Test
    fun padrao_eEscuro() = assertEquals(ModoTema.ESCURO, ModoTema.PADRAO)

    @Test
    fun deNome_reconheceOsTresModos() {
        ModoTema.values().forEach { assertEquals(it, ModoTema.deNome(it.name)) }
    }

    @Test
    fun deNome_ausenteOuDesconhecido_voltaParaOPadrao() {
        assertEquals(ModoTema.PADRAO, ModoTema.deNome(null))
        assertEquals(ModoTema.PADRAO, ModoTema.deNome(""))
        assertEquals(ModoTema.PADRAO, ModoTema.deNome("SEPIA"))
    }
}
