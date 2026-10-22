package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import java.time.LocalDate
import kotlin.math.roundToInt

/** Leitura real do painel do carro, registrada num abastecimento ou numa manutenção. */
data class LeituraOdometro(val km: Int, val data: LocalDate)

/**
 * Odômetro usado nos alertas: a última leitura real mais os km dos ganhos lançados depois dela.
 * [km] == [leitura].km quando não há ganho com km depois da leitura (nada estimado).
 */
data class Odometro(val km: Int, val leitura: LeituraOdometro) {
    /** Km somados pelos ganhos desde a leitura. */
    val kmEstimados: Int get() = km - leitura.km

    val estimado: Boolean get() = kmEstimados > 0
}

/** Maior odômetro registrado; em empate, a leitura mais recente. Null sem abastecimento nem manutenção. */
fun ultimaLeitura(abastecimentos: List<Abastecimento>, manutencoes: List<Manutencao>): LeituraOdometro? =
    (abastecimentos.map { LeituraOdometro(it.odometroKm, it.data) } + manutencoes.map { LeituraOdometro(it.odometroKm, it.data) })
        .maxWithOrNull(compareBy<LeituraOdometro> { it.km }.thenBy { it.data.toEpochDay() })

/**
 * Soma à última leitura os km dos ganhos dos dias **seguintes** a ela. Ganhos do mesmo dia ficam de fora,
 * porque não dá para saber se foram antes ou depois da leitura. Km rodados fora dos apps (pessoais) não
 * entram, então a estimativa tende a ficar abaixo do real: o alerta pode chegar um pouco tarde, nunca cedo.
 */
fun odometroEstimado(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    ganhos: List<Ganho>,
): Odometro? {
    val leitura = ultimaLeitura(abastecimentos, manutencoes) ?: return null
    val kmDepois = ganhos.filter { it.data > leitura.data }.sumOf { it.km.coerceAtLeast(0.0) }.roundToInt()
    return Odometro(leitura.km + kmDepois, leitura)
}
