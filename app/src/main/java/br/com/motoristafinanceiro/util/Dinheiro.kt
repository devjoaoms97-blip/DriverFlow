package br.com.motoristafinanceiro.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val localeBR = Locale("pt", "BR")

fun Long.formatarReais(): String = NumberFormat.getCurrencyInstance(localeBR).format(this / 100.0)

/** Converte texto digitado ("12,50" ou "12.50") em centavos. Retorna null se inválido. */
fun String.reaisParaCentavos(): Long? =
    trim().replace(',', '.').toBigDecimalOrNull()
        ?.multiply(BigDecimal(100))
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toLong()
