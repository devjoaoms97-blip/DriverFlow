package br.com.motoristafinanceiro.ui.abertura

/** Quadros da animação (res/raw/driverflow_splash.json: "op"). O texto da abertura é do Compose e segue estes tempos. */
const val QUADROS_DA_ABERTURA = 75f

/** 0 antes de [inicio], 1 depois de [fim] e a transição linear no meio (tudo em quadros). */
fun opacidadeNoQuadro(quadro: Float, inicio: Float, fim: Float): Float =
    ((quadro - inicio) / (fim - inicio)).coerceIn(0f, 1f)
