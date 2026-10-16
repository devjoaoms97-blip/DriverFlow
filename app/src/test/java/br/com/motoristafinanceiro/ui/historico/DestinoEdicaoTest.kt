package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DestinoEdicaoTest {
    private fun despesa(origem: OrigemDespesa, origemId: Long? = null) = Despesa(
        id = 5,
        data = LocalDate.of(2026, 10, 12),
        categoria = CategoriaDespesa.COMBUSTIVEL,
        valorCentavos = 1_000,
        origem = origem,
        origemId = origemId,
    )

    @Test
    fun despesaManual_editaADespesa() {
        assertEquals(DestinoEdicao.EditarDespesa(5), destinoEdicao(despesa(OrigemDespesa.MANUAL)))
    }

    @Test
    fun despesaDeAbastecimento_levaParaOAbastecimentoDeOrigem() {
        assertEquals(
            DestinoEdicao.EditarAbastecimento(7),
            destinoEdicao(despesa(OrigemDespesa.ABASTECIMENTO, origemId = 7)),
        )
    }

    @Test
    fun despesaDeAbastecimento_semOrigem_naoEditavel() {
        assertEquals(DestinoEdicao.SemEdicao, destinoEdicao(despesa(OrigemDespesa.ABASTECIMENTO, origemId = null)))
    }

    @Test
    fun despesaDeManutencao_levaParaAManutencaoDeOrigem() {
        assertEquals(
            DestinoEdicao.EditarManutencao(3),
            destinoEdicao(despesa(OrigemDespesa.MANUTENCAO, origemId = 3)),
        )
    }

    @Test
    fun despesaDeManutencao_semOrigem_naoEditavel() {
        assertEquals(DestinoEdicao.SemEdicao, destinoEdicao(despesa(OrigemDespesa.MANUTENCAO, origemId = null)))
    }
}
