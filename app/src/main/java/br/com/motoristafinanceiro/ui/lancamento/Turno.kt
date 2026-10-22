package br.com.motoristafinanceiro.ui.lancamento

import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.domain.MINUTOS_NO_DIA
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.minutosParaHorario

/**
 * Duração do turno em minutos; passa da meia-noite normalmente (22:00 às 02:00 = 240).
 * Null sem os dois horários ou com fim igual ao início.
 */
fun duracaoTurno(inicio: Int?, fim: Int?): Int? {
    if (inicio == null || fim == null) return null
    return ((fim - inicio + MINUTOS_NO_DIA) % MINUTOS_NO_DIA).takeIf { it > 0 }
}

/** Fim do turno de um ganho já salvo: início + minutos (o fim não é guardado, é sempre essa soma). */
fun fimDoTurno(inicio: Int?, minutosOnline: Int): Int? =
    inicio?.takeIf { minutosOnline > 0 }?.let { (it + minutosOnline) % MINUTOS_NO_DIA }

/** O que o formulário do ganho diz sobre o turno e se dá para salvar. */
data class EstadoTurno(
    /** Minutos que vão para o ganho. */
    val minutosOnline: Int,
    /** Linha de apoio abaixo dos horários; vazia quando não há o que dizer. */
    val texto: String,
    /** true = o texto é um problema que impede salvar. */
    val erro: Boolean,
)

/**
 * Regra dos horários do ganho: com início e fim, as horas são a diferença. Lançamento antigo, com horas
 * mas sem horário, mantém as horas até que os dois horários sejam preenchidos.
 */
fun estadoTurno(inicio: Int?, fim: Int?, minutosOriginais: Int = 0): EstadoTurno {
    val duracao = duracaoTurno(inicio, fim)
    return when {
        duracao != null -> EstadoTurno(duracao, "${duracao.minutosParaHorasTexto()} h de turno", erro = false)
        inicio != null && fim != null -> EstadoTurno(0, "O fim tem que ser diferente do início.", erro = true)
        fim != null -> EstadoTurno(0, "Preencha também o início do turno.", erro = true)
        inicio != null -> EstadoTurno(0, "Sem o fim, o ganho fica sem horas (não entra no ganho por hora).", erro = false)
        minutosOriginais > 0 ->
            EstadoTurno(minutosOriginais, "${minutosOriginais.minutosParaHorasTexto()} h online (lançado sem horário)", erro = false)
        else -> EstadoTurno(0, "", erro = false)
    }
}

/** O que "Repetir o último" copia do ganho anterior: a plataforma e os horários (o valor muda todo dia). */
data class Repeticao(val plataforma: Plataforma, val inicio: Int?, val fim: Int?)

fun repeticaoDe(ultimo: Ganho): Repeticao =
    Repeticao(ultimo.plataforma, ultimo.inicioTurnoMinutos, fimDoTurno(ultimo.inicioTurnoMinutos, ultimo.minutosOnline))

/** "Uber · 17:00 às 21:00", ou só "Uber" quando o último não tinha horário. */
fun textoRepeticao(r: Repeticao): String {
    if (r.inicio == null) return r.plataforma.rotulo
    val fim = r.fim?.let { " às ${it.minutosParaHorario()}" } ?: ""
    return "${r.plataforma.rotulo} · ${r.inicio.minutosParaHorario()}$fim"
}
