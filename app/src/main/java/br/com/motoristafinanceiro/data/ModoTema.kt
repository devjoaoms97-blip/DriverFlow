package br.com.motoristafinanceiro.data

/** Aparência do app, escolhida em Ajustes. */
enum class ModoTema(val rotulo: String) {
    AUTOMATICO("Automático"),
    CLARO("Claro"),
    ESCURO("Escuro"),
    ;

    companion object {
        /** Escuro, como o app sempre foi e como a abertura animada. */
        val PADRAO = ESCURO

        /** Valor guardado ausente ou desconhecido (ex.: de uma versão futura) volta para o padrão. */
        fun deNome(nome: String?): ModoTema = values().firstOrNull { it.name == nome } ?: PADRAO
    }
}
