package br.com.motoristafinanceiro.ui.navigation

import br.com.motoristafinanceiro.notificacoes.ABRIR_VEICULO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarraInferiorTest {

    @Test
    fun abas_naOrdemComOMaisNoMeio() {
        assertEquals(listOf("Início", "Histórico"), abasEsquerda.map { it.rotulo })
        assertEquals(listOf("Veículo", "Análises"), abasDireita.map { it.rotulo })
        assertEquals(listOf("inicio", "historico", "veiculo", "analises"), (abasEsquerda + abasDireita).map { it.rota })
    }

    @Test
    fun reservas_saiuDasAbasEAbrePeloInicio() {
        assertFalse((abasEsquerda + abasDireita).any { it.rota == "reservas" })
        assertTrue("reservas" in rotasSemBarra) // tela secundária, com voltar e sem barra
    }

    @Test
    fun formulariosEAjustes_semBarra() {
        listOf("novo", "ganho/{id}", "despesa/{id}", "abastecimento/{id}", "manutencao/{id}", "ajustes", "meta", "reserva/{id}")
            .forEach { assertTrue("$it deveria esconder a barra", it in rotasSemBarra) }
        (abasEsquerda + abasDireita).forEach { assertFalse(it.rota in rotasSemBarra) }
    }

    @Test
    fun notificacaoDeManutencao_abreUmaAbaQueExiste() {
        assertTrue((abasEsquerda + abasDireita).any { it.rota == ABRIR_VEICULO })
    }
}
