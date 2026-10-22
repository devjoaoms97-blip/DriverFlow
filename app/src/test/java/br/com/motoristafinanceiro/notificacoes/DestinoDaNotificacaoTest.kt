package br.com.motoristafinanceiro.notificacoes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinoDaNotificacaoTest {

    @Test
    fun veiculo_eAceito() {
        assertEquals("veiculo", destinoPedido(ABRIR_VEICULO))
    }

    @Test
    fun semExtra_naoPedeNada() = assertNull(destinoPedido(null))

    @Test
    fun destinoDesconhecido_eIgnorado() {
        // O app pode ser aberto por outros apps: nada de navegar para rotas arbitrárias.
        assertNull(destinoPedido("ajustes"))
        assertNull(destinoPedido("reserva/0"))
        assertNull(destinoPedido(""))
    }

    @Test
    fun novoLancamento_eAceito() = assertEquals("novo", destinoPedido(ABRIR_NOVO_LANCAMENTO))
}
