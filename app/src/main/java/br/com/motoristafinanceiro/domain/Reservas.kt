package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.ReservaDespesa
import kotlin.math.ceil

/** Quanto separar por dia para cobrir uma despesa recorrente (valor ÷ dias do período). */
fun reservaDiariaCentavos(reserva: ReservaDespesa): Long =
    ceil(reserva.valorCentavos.toDouble() / reserva.periodicidade.dias).toLong()

fun reservaDiariaTotalCentavos(reservas: List<ReservaDespesa>): Long =
    reservas.sumOf { reservaDiariaCentavos(it) }
