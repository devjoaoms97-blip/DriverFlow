package br.com.motoristafinanceiro.util

import java.util.Locale

private val ptBR = Locale("pt", "BR")

/** 13.333 -> "13,3 km/l" */
fun Double.formatarKmPorLitro(): String = String.format(ptBR, "%.1f km/l", this)

/** 10400 -> "10.400 km" */
fun Int.formatarKm(): String = String.format(ptBR, "%,d km", this)
