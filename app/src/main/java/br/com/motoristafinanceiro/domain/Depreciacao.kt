package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.util.reaisParaCentavos

/** Teto de sanidade: R$ 10,00 por km já é irreal para qualquer carro. */
const val DEPRECIACAO_MAXIMA_POR_KM_CENTAVOS = 1_000L

/**
 * Converte o texto digitado em Ajustes ("0,25") em centavos por km.
 * Vazio vale 0 (sem depreciação). Retorna null para texto inválido, negativo ou acima do teto.
 */
fun parseDepreciacaoPorKm(texto: String): Long? {
    if (texto.isBlank()) return 0L
    val centavos = texto.reaisParaCentavos() ?: return null
    return centavos.takeIf { it in 0..DEPRECIACAO_MAXIMA_POR_KM_CENTAVOS }
}
