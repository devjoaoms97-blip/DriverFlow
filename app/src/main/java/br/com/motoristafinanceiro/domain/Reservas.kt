package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Quanto separar por dia para cobrir uma despesa recorrente (valor ÷ dias do período). */
fun reservaDiariaCentavos(reserva: ReservaDespesa): Long =
    ceil(reserva.valorCentavos.toDouble() / reserva.periodicidade.dias).toLong()

fun reservaDiariaTotalCentavos(reservas: List<ReservaDespesa>): Long =
    reservas.sumOf { reservaDiariaCentavos(it) }

/** Meses entre duas ocorrências da despesa. */
fun mesesDoPeriodo(periodicidade: Periodicidade): Int = when (periodicidade) {
    Periodicidade.MENSAL -> 1
    Periodicidade.TRIMESTRAL -> 3
    Periodicidade.SEMESTRAL -> 6
    Periodicidade.ANUAL -> 12
}

/**
 * Próxima data de vencimento a partir de [hoje]. Se a data cadastrada já passou, avança de período em
 * período (sempre contando a partir da data original, para não "escorregar" o dia em meses curtos).
 */
fun proximaOcorrencia(reserva: ReservaDespesa, hoje: LocalDate): LocalDate {
    val passo = mesesDoPeriodo(reserva.periodicidade).toLong()
    var vezes = 0L
    var data = reserva.proximoVencimento
    while (data.isBefore(hoje)) {
        vezes += 1
        data = reserva.proximoVencimento.plusMonths(vezes * passo)
    }
    return data
}

fun diasParaVencer(reserva: ReservaDespesa, hoje: LocalDate): Long =
    ChronoUnit.DAYS.between(hoje, proximaOcorrencia(reserva, hoje))

fun reservaValida(nome: String, valorCentavos: Long?): Boolean =
    nome.isNotBlank() && valorCentavos != null && valorCentavos > 0

/** Atalhos do formulário: preenchem nome e periodicidade. */
data class ModeloReserva(val nome: String, val periodicidade: Periodicidade)

val MODELOS_DE_RESERVA = listOf(
    ModeloReserva("IPVA", Periodicidade.ANUAL),
    ModeloReserva("Licenciamento", Periodicidade.ANUAL),
    ModeloReserva("Seguro", Periodicidade.ANUAL),
    ModeloReserva("Parcela do carro", Periodicidade.MENSAL),
    ModeloReserva("Aluguel do carro", Periodicidade.MENSAL),
)
