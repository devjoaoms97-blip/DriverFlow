package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.util.formatarKm

/** "Atrasada em 200 km", "Faltam 300 km" ou "No limite". */
fun AlertaManutencao.resumoTexto(): String = when {
    kmRestantes < 0 -> "Atrasada em " + (-kmRestantes).formatarKm()
    kmRestantes == 0 -> "No limite"
    else -> "Faltam " + kmRestantes.formatarKm()
}

/** Quanto do intervalo já foi rodado (0 a 1), para a barra do cartão de manutenção. Sem intervalo: cheia só se atrasada. */
fun AlertaManutencao.fracaoRodada(): Float {
    val intervalo = manutencao.intervaloKm?.takeIf { it > 0 }
        ?: return if (status == StatusAlerta.ATRASADA) 1f else 0f
    return ((intervalo - kmRestantes).toFloat() / intervalo).coerceIn(0f, 1f)
}

/** Texto do selo: "Atrasada", "Em 320 km", "No limite" ou "Em dia". */
fun AlertaManutencao.seloTexto(): String = when (status) {
    StatusAlerta.ATRASADA -> "Atrasada"
    StatusAlerta.PROXIMA -> if (kmRestantes > 0) "Em " + kmRestantes.formatarKm() else "No limite"
    StatusAlerta.EM_DIA -> "Em dia"
}

/** Linha abaixo da barra: "Faltam 8.400 km · a cada 40.000 km". */
fun AlertaManutencao.detalheTexto(): String =
    listOfNotNull(resumoTexto(), manutencao.intervaloKm?.let { "a cada " + it.formatarKm() }).joinToString(" · ")
