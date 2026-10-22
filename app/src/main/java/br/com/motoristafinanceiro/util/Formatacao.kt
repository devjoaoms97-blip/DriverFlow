package br.com.motoristafinanceiro.util

import java.util.Locale
import kotlin.math.abs

private val ptBR = Locale("pt", "BR")

/** 1250 -> "12,50" (para preencher campos de texto na edição). */
fun Long.paraTextoReais(): String {
    val sinal = if (this < 0) "-" else ""
    val v = abs(this)
    return String.format(ptBR, "%s%d,%02d", sinal, v / 100, v % 100)
}

/** 50.0 -> "50"; 50.5 -> "50,5". */
fun Double.paraTextoNumero(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')

/** 390 -> "6,5"; 240 -> "4". */
fun Int.minutosParaHorasTexto(): String {
    if (this % 60 == 0) return (this / 60).toString()
    return String.format(ptBR, "%.2f", this / 60.0).trimEnd('0').trimEnd(',')
}

/** Minutos desde a meia-noite → "08:05", "18:30". */
fun Int.minutosParaHorario(): String = String.format(ptBR, "%02d:%02d", this / 60, this % 60)
