package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Manutencao
import java.time.LocalDate
import kotlin.math.roundToInt

/** Leitura real do painel do carro, registrada num abastecimento ou numa manutenção. */
data class LeituraOdometro(val km: Int, val data: LocalDate)

/**
 * Odômetro usado nos alertas: a última leitura real mais os km dos ganhos e dos km pessoais lançados depois dela.
 * [km] == [leitura].km quando não há km lançado depois da leitura (nada estimado).
 */
data class Odometro(val km: Int, val leitura: LeituraOdometro) {
    /** Km somados pelos ganhos e km pessoais desde a leitura. */
    val kmEstimados: Int get() = km - leitura.km

    val estimado: Boolean get() = kmEstimados > 0
}

private fun leituras(abastecimentos: List<Abastecimento>, manutencoes: List<Manutencao>): List<LeituraOdometro> =
    abastecimentos.map { LeituraOdometro(it.odometroKm, it.data) } + manutencoes.map { LeituraOdometro(it.odometroKm, it.data) }

/** Maior odômetro registrado; em empate, a leitura mais recente. Null sem abastecimento nem manutenção. */
fun ultimaLeitura(abastecimentos: List<Abastecimento>, manutencoes: List<Manutencao>): LeituraOdometro? =
    leituras(abastecimentos, manutencoes)
        .maxWithOrNull(compareBy<LeituraOdometro> { it.km }.thenBy { it.data.toEpochDay() })

/**
 * Soma à última leitura os km dos ganhos e dos km pessoais dos dias **seguintes** a ela. Os do mesmo dia ficam
 * de fora, porque não dá para saber se foram antes ou depois da leitura. Km que não foram lançados não entram,
 * então a estimativa tende a ficar abaixo do real: o alerta pode chegar um pouco tarde, nunca cedo.
 */
fun odometroEstimado(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    ganhos: List<Ganho>,
    kmPessoais: List<KmPessoal> = emptyList(),
): Odometro? {
    val leitura = ultimaLeitura(abastecimentos, manutencoes) ?: return null
    val kmDepois = ganhos.filter { it.data > leitura.data }.sumOf { it.km.coerceAtLeast(0.0) } +
        kmPessoais.filter { it.data > leitura.data }.sumOf { it.km.coerceAtLeast(0.0) }
    return Odometro(leitura.km + kmDepois.roundToInt(), leitura)
}

/** Quantos dias antes da última leitura a conferência procura a leitura de partida. */
const val DIAS_CONFERENCIA_KM = 90

/** Abaixo disso entre as duas leituras, a conferência não aparece (pouco km para comparar). */
const val KM_MINIMO_CONFERENCIA = 100

/**
 * Km que o carro rodou de verdade (diferença do odômetro entre duas leituras) × km lançados no mesmo intervalo
 * (ganhos + pessoais). A diferença são km sem lançamento: deslocamentos esquecidos, que deixam o custo por km
 * mais alto do que é, ou um odômetro digitado errado.
 */
data class ConferenciaKm(
    val inicio: LeituraOdometro,
    val fim: LeituraOdometro,
    val kmTrabalho: Double,
    val kmPessoal: Double,
) {
    val kmReais: Int get() = fim.km - inicio.km
    val kmLancados: Double get() = kmTrabalho + kmPessoal

    /** Positivo = rodou mais do que lançou; negativo = lançou mais do que o odômetro mostra. */
    val kmSemLancamento: Int get() = kmReais - kmLancados.roundToInt()

    /** Quanto dos km reais foi lançado, de 0 em diante (pode passar de 100). */
    val percentualLancado: Int get() = (kmLancados * 100 / kmReais).roundToInt()
}

/**
 * Compara a última leitura com a mais antiga dos [DIAS_CONFERENCIA_KM] dias anteriores a ela. Os km lançados são
 * os dos dias depois da leitura de partida até o dia da última, inclusive (mesma regra do odômetro estimado:
 * o lançado no dia de uma leitura conta como feito antes dela). Null sem duas leituras em dias diferentes ou com
 * menos de [KM_MINIMO_CONFERENCIA] km entre elas.
 */
fun conferirKm(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    ganhos: List<Ganho>,
    kmPessoais: List<KmPessoal>,
): ConferenciaKm? {
    val fim = ultimaLeitura(abastecimentos, manutencoes) ?: return null
    val limite = fim.data.minusDays(DIAS_CONFERENCIA_KM.toLong())
    val inicio = leituras(abastecimentos, manutencoes)
        .filter { it.data >= limite && it.data < fim.data && it.km < fim.km }
        .minWithOrNull(compareBy<LeituraOdometro> { it.data.toEpochDay() }.thenBy { it.km })
        ?: return null
    if (fim.km - inicio.km < KM_MINIMO_CONFERENCIA) return null
    val noIntervalo = { data: LocalDate -> data > inicio.data && data <= fim.data }
    return ConferenciaKm(
        inicio = inicio,
        fim = fim,
        kmTrabalho = ganhos.filter { noIntervalo(it.data) }.sumOf { it.km.coerceAtLeast(0.0) },
        kmPessoal = kmPessoais.filter { noIntervalo(it.data) }.sumOf { it.km.coerceAtLeast(0.0) },
    )
}
