package br.com.motoristafinanceiro.notificacoes

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.statusAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TextoNotificacaoTest {

    private fun alerta(tipo: TipoManutencao, restantes: Int, observacao: String? = null) = AlertaManutencao(
        manutencao = Manutencao(
            tipo = tipo, data = LocalDate.of(2026, 10, 1), odometroKm = 10_000, valorCentavos = 0, observacao = observacao,
        ),
        kmRestantes = restantes,
        status = statusAlerta(restantes),
    )

    @Test
    fun semUrgentes_naoHaNotificacao() = assertNull(montarTextoNotificacao(emptyList()))

    @Test
    fun umaAtrasada() {
        val t = montarTextoNotificacao(listOf(alerta(TipoManutencao.OLEO, -200)))!!
        assertEquals("Manutenção atrasada", t.titulo)
        assertEquals("Troca de óleo — Atrasada em 200 km", t.resumo)
        assertEquals(t.resumo, t.detalhes)
    }

    @Test
    fun umaProxima() {
        val t = montarTextoNotificacao(listOf(alerta(TipoManutencao.FREIOS, 300)))!!
        assertEquals("Manutenção próxima", t.titulo)
        assertEquals("Freios — Faltam 300 km", t.resumo)
    }

    @Test
    fun varias_maisUrgentePrimeiro() {
        val t = montarTextoNotificacao(
            listOf(alerta(TipoManutencao.OLEO, 300), alerta(TipoManutencao.PNEUS, -100))
        )!!
        assertEquals("2 manutenções precisam de atenção", t.titulo)
        assertEquals("Pneus — Atrasada em 100 km e mais 1", t.resumo)
        assertEquals("Pneus — Atrasada em 100 km\nTroca de óleo — Faltam 300 km", t.detalhes)
    }

    @Test
    fun outroComObservacao_usaADescricaoDoUsuario() {
        val t = montarTextoNotificacao(listOf(alerta(TipoManutencao.OUTRO, 100, observacao = "Bateria")))!!
        assertEquals("Bateria — Faltam 100 km", t.resumo)
    }
}
