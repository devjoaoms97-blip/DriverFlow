package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao

/**
 * Consumo médio (km/l) pelo método do tanque cheio: entre dois abastecimentos
 * com tanque cheio, os litros consumidos são a soma dos abastecimentos posteriores
 * ao primeiro até o segundo (inclusive). Retorna null se não houver dados suficientes.
 */
fun calcularConsumoMedio(abastecimentos: List<Abastecimento>): Double? {
    val ordenados = abastecimentos.sortedBy { it.odometroKm }
    val cheios = ordenados.indices.filter { ordenados[it].tanqueCheio }
    if (cheios.size < 2) return null

    var kmTotal = 0
    var litros = 0.0
    for (i in 1 until cheios.size) {
        val ini = cheios[i - 1]
        val fim = cheios[i]
        kmTotal += ordenados[fim].odometroKm - ordenados[ini].odometroKm
        litros += ordenados.subList(ini + 1, fim + 1).sumOf { it.litros }
    }
    return if (litros > 0) kmTotal / litros else null
}

/** Km restantes até a próxima troca/revisão. Negativo = atrasado. Null = sem alerta configurado. */
fun kmRestantes(manutencao: Manutencao, odometroAtual: Int): Int? =
    manutencao.intervaloKm?.let { manutencao.odometroKm + it - odometroAtual }
