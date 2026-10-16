package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao
import kotlin.math.roundToLong

data class ResumoVeiculo(
    /** km/l pelo método do tanque cheio; null sem 2 abastecimentos com tanque cheio. */
    val consumoMedioKmPorLitro: Double?,
    /** Maior odômetro entre abastecimentos e manutenções. */
    val odometroAtualKm: Int?,
    val precoUltimoLitroCentavos: Long?,
    /** (despesas do mês) ÷ (km rodados no mês). */
    val custoPorKmMesCentavos: Long?,
)

fun precoPorLitroCentavos(valorTotalCentavos: Long, litros: Double): Long? =
    if (litros > 0) (valorTotalCentavos / litros).roundToLong() else null

fun precoPorLitroCentavos(a: Abastecimento): Long? = precoPorLitroCentavos(a.valorTotalCentavos, a.litros)

fun odometroAtual(abastecimentos: List<Abastecimento>, manutencoes: List<Manutencao> = emptyList()): Int? =
    (abastecimentos.map { it.odometroKm } + manutencoes.map { it.odometroKm }).maxOrNull()

/** Mais recente por data; em empate, o de maior odômetro. */
fun ultimoAbastecimento(abastecimentos: List<Abastecimento>): Abastecimento? =
    abastecimentos.maxWithOrNull(compareBy<Abastecimento> { it.data.toEpochDay() }.thenBy { it.odometroKm })

fun calcularResumoVeiculo(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    estatisticasMes: Estatisticas,
): ResumoVeiculo = ResumoVeiculo(
    consumoMedioKmPorLitro = calcularConsumoMedio(abastecimentos),
    odometroAtualKm = odometroAtual(abastecimentos, manutencoes),
    precoUltimoLitroCentavos = ultimoAbastecimento(abastecimentos)?.let { precoPorLitroCentavos(it) },
    custoPorKmMesCentavos = estatisticasMes.custoPorKmCentavos,
)

fun abastecimentoValido(litros: Double?, valorCentavos: Long?, odometroKm: Int?): Boolean =
    litros != null && litros > 0 &&
        valorCentavos != null && valorCentavos > 0 &&
        odometroKm != null && odometroKm > 0
