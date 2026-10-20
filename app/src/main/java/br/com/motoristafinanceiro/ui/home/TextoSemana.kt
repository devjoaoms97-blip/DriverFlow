package br.com.motoristafinanceiro.ui.home

import br.com.motoristafinanceiro.domain.fimDaSemana
import br.com.motoristafinanceiro.domain.inicioDaSemana
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")

/** "12/10 a 18/10": a semana (segunda a domingo) em que [data] cai. */
fun rotuloSemana(data: LocalDate): String =
    "${inicioDaSemana(data).format(formatoDiaMes)} a ${fimDaSemana(data).format(formatoDiaMes)}"
