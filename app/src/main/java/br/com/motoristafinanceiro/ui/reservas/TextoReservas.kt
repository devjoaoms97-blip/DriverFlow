package br.com.motoristafinanceiro.ui.reservas

/** "Vence hoje", "Vence amanhã" ou "Vence em 12 dias". */
fun textoVencimento(diasParaVencer: Long): String = when (diasParaVencer) {
    0L -> "Vence hoje"
    1L -> "Vence amanhã"
    else -> "Vence em $diasParaVencer dias"
}
