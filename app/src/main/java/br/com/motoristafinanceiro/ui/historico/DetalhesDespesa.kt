package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.OrigemDespesa

/** Linha de detalhes da despesa no histórico: observação, origem e se tem comprovante. */
internal fun detalhesDespesa(despesa: Despesa): String {
    val origem = when (despesa.origem) {
        OrigemDespesa.MANUAL -> null
        OrigemDespesa.ABASTECIMENTO -> "Abastecimento"
        OrigemDespesa.MANUTENCAO -> "Manutenção"
    }
    return listOfNotNull(
        despesa.observacao?.takeIf { it.isNotBlank() },
        origem,
        if (despesa.fotoUri != null) "Com comprovante" else null,
    ).joinToString(" · ")
}
