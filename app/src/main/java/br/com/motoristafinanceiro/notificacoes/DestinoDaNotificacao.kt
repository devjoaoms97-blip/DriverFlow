package br.com.motoristafinanceiro.notificacoes

/** Nome do extra do Intent da notificação que diz para qual tela o app deve ir ao abrir. */
const val EXTRA_ABRIR = "abrir"

/** Rota da aba Veículo (a mesma usada na navegação do app). */
const val ABRIR_VEICULO = "veiculo"

private val DESTINOS_PERMITIDOS = setOf(ABRIR_VEICULO)

/** O app é aberto por outros apps também: só aceita destinos conhecidos, ignora o resto. */
fun destinoPedido(extra: String?): String? = extra?.takeIf { it in DESTINOS_PERMITIDOS }
