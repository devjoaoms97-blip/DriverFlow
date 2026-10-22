package br.com.motoristafinanceiro.ui.historico

import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Plataforma

/** Sigla no quadradinho verde das linhas de ganho. */
fun siglaPlataforma(p: Plataforma): String = when (p) {
    Plataforma.UBER -> "U"
    Plataforma.NOVE_NOVE -> "99"
    Plataforma.INDRIVE -> "iD"
    Plataforma.PARTICULAR -> "P"
    Plataforma.OUTRA -> "OU"
}

/** Sigla no quadradinho vermelho das linhas de despesa; uma diferente para cada categoria. */
fun siglaCategoria(c: CategoriaDespesa): String = when (c) {
    CategoriaDespesa.COMBUSTIVEL -> "CB"
    CategoriaDespesa.MANUTENCAO -> "MN"
    CategoriaDespesa.LAVAGEM -> "LV"
    CategoriaDespesa.ALUGUEL_CARRO -> "AC"
    CategoriaDespesa.FINANCIAMENTO -> "FI"
    CategoriaDespesa.SEGURO -> "SG"
    CategoriaDespesa.IPVA_LICENCIAMENTO -> "IP"
    CategoriaDespesa.MULTA -> "MU"
    CategoriaDespesa.ALIMENTACAO -> "AL"
    CategoriaDespesa.INTERNET_CELULAR -> "NT"
    CategoriaDespesa.OUTRA -> "OU"
}
