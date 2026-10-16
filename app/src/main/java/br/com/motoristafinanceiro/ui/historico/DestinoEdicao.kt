package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.OrigemDespesa

/** Para onde levar o toque numa despesa do histórico. */
sealed interface DestinoEdicao {
    data class EditarDespesa(val id: Long) : DestinoEdicao
    data class EditarAbastecimento(val id: Long) : DestinoEdicao
    data object SemEdicao : DestinoEdicao
}

internal fun destinoEdicao(despesa: Despesa): DestinoEdicao = when (despesa.origem) {
    OrigemDespesa.MANUAL -> DestinoEdicao.EditarDespesa(despesa.id)
    OrigemDespesa.ABASTECIMENTO ->
        despesa.origemId?.let { DestinoEdicao.EditarAbastecimento(it) } ?: DestinoEdicao.SemEdicao
    // Manutenção ganha edição na Etapa 4.
    OrigemDespesa.MANUTENCAO -> DestinoEdicao.SemEdicao
}
