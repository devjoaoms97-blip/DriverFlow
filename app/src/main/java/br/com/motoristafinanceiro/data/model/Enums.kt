package br.com.motoristafinanceiro.data.model

enum class Plataforma(val rotulo: String) {
    UBER("Uber"),
    NOVE_NOVE("99"),
    INDRIVE("InDrive"),
    PARTICULAR("Particular"),
    OUTRA("Outra"),
}

enum class CategoriaDespesa(val rotulo: String) {
    COMBUSTIVEL("Combustível"),
    MANUTENCAO("Manutenção"),
    LAVAGEM("Lavagem"),
    ALUGUEL_CARRO("Aluguel do carro"),
    FINANCIAMENTO("Financiamento"),
    SEGURO("Seguro"),
    IPVA_LICENCIAMENTO("IPVA/Licenciamento"),
    MULTA("Multas"),
    ALIMENTACAO("Alimentação"),
    INTERNET_CELULAR("Internet/Celular"),
    OUTRA("Outra"),
}

/** Indica quem gerou a despesa, para evitar lançamento duplicado. */
enum class OrigemDespesa { MANUAL, ABASTECIMENTO, MANUTENCAO }

enum class TipoManutencao(val rotulo: String) {
    OLEO("Troca de óleo"),
    PNEUS("Pneus"),
    FREIOS("Freios"),
    CORREIA("Correia dentada"),
    ALINHAMENTO("Alinhamento/Balanceamento"),
    REVISAO("Revisão"),
    OUTRO("Outro"),
}

enum class Periodicidade(val rotulo: String, val dias: Int) {
    MENSAL("Mensal", 30),
    TRIMESTRAL("Trimestral", 91),
    SEMESTRAL("Semestral", 182),
    ANUAL("Anual", 365),
}
