package br.com.motoristafinanceiro.ui.ajustes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoNotificacaoTest {

    @Test
    fun desligada_independenteDaPermissao() {
        assertEquals(EstadoNotificacao.DESLIGADAS, estadoNotificacao(ativas = false, sistemaPermite = true))
        assertEquals(EstadoNotificacao.DESLIGADAS, estadoNotificacao(ativas = false, sistemaPermite = false))
    }

    @Test
    fun ligadaComPermissao_estaAtiva() {
        assertEquals(EstadoNotificacao.ATIVAS, estadoNotificacao(ativas = true, sistemaPermite = true))
    }

    @Test
    fun ligadaSemPermissao_pedeParaLiberar() {
        assertEquals(EstadoNotificacao.SEM_PERMISSAO, estadoNotificacao(ativas = true, sistemaPermite = false))
    }

    @Test
    fun pedePermissao_soNoAndroid13OuMaisNovo() {
        assertFalse(precisaPedirPermissao(sdkInt = 32, sistemaPermite = false))
        assertTrue(precisaPedirPermissao(sdkInt = 33, sistemaPermite = false))
        assertTrue(precisaPedirPermissao(sdkInt = 34, sistemaPermite = false))
    }

    @Test
    fun jaPermitido_naoPedeDeNovo() {
        assertFalse(precisaPedirPermissao(sdkInt = 34, sistemaPermite = true))
    }
}
