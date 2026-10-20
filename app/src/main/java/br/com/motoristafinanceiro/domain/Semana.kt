package br.com.motoristafinanceiro.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A semana do app vai de segunda a domingo. Segunda é o início dela mesma. */
fun inicioDaSemana(data: LocalDate): LocalDate =
    data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Domingo da semana de [data]. */
fun fimDaSemana(data: LocalDate): LocalDate = inicioDaSemana(data).plusDays(6)
