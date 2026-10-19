package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DetalhesDespesaTest {
    private fun despesa(
        obs: String? = null,
        origem: OrigemDespesa = OrigemDespesa.MANUAL,
        foto: String? = null,
    ) = Despesa(
        data = LocalDate.of(2026, 10, 12),
        categoria = CategoriaDespesa.LAVAGEM,
        valorCentavos = 1_000,
        observacao = obs,
        origem = origem,
        fotoUri = foto,
    )

    @Test
    fun manualSemNada_ficaVazio() = assertEquals("", detalhesDespesa(despesa()))

    @Test
    fun observacaoEComprovante() =
        assertEquals("Posto X · Com comprovante", detalhesDespesa(despesa(obs = "Posto X", foto = "a.jpg")))

    @Test
    fun observacaoEmBranco_ehIgnorada() = assertEquals("", detalhesDespesa(despesa(obs = "   ")))

    @Test
    fun abastecimento() =
        assertEquals("Abastecimento", detalhesDespesa(despesa(origem = OrigemDespesa.ABASTECIMENTO)))

    @Test
    fun manutencaoComDescricao() =
        assertEquals("Troca de óleo · Manutenção",
            detalhesDespesa(despesa(obs = "Troca de óleo", origem = OrigemDespesa.MANUTENCAO)))
}
