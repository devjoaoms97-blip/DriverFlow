package br.com.motoristafinanceiro.ui.meta

import br.com.motoristafinanceiro.domain.ProgressoMeta
import br.com.motoristafinanceiro.util.formatarReais
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Frase principal do card de meta: quanto falta por dia, ou o aviso de meta batida. */
fun textoFaltaPorDia(p: ProgressoMeta): String = when {
    p.atingida && p.excedenteCentavos > 0 -> "Meta batida! Você passou ${p.excedenteCentavos.formatarReais()} da meta."
    p.atingida -> "Meta batida!"
    p.diasRestantes == 1 -> "Último dia do mês: faltam ${p.faltaCentavos.formatarReais()}."
    else -> "Faltam ${p.faltaPorDiaCentavos.formatarReais()} por dia."
}

/** Linha de apoio abaixo da frase principal. Vazia quando a meta já foi batida. */
fun textoDiasRestantes(p: ProgressoMeta): String = when {
    p.atingida -> ""
    p.diasRestantes == 1 -> "Hoje é o último dia."
    else -> "Faltam ${p.faltaCentavos.formatarReais()} em ${p.diasRestantes} dias (contando hoje)."
}

/** "setembro" (nome do mês em minúsculas, como no meio de uma frase). */
fun nomeDoMes(mes: YearMonth): String = mes.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))

/** Botão de repetir a meta: "Repetir a de setembro: R$ 5.000,00". */
fun textoRepetirMeta(mesAnterior: YearMonth, centavos: Long): String =
    "Repetir a de ${nomeDoMes(mesAnterior)}: ${centavos.formatarReais()}"
