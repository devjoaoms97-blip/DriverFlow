package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao

/** Até esta distância (km) a manutenção é considerada "próxima". */
const val LIMIAR_PROXIMA_KM = 500

enum class StatusAlerta { ATRASADA, PROXIMA, EM_DIA }

data class AlertaManutencao(
    /** Último registro daquele serviço, que define o plano atual. */
    val manutencao: Manutencao,
    /** Negativo = atrasada. */
    val kmRestantes: Int,
    val status: StatusAlerta,
)

fun statusAlerta(kmRestantes: Int): StatusAlerta = when {
    kmRestantes < 0 -> StatusAlerta.ATRASADA
    kmRestantes <= LIMIAR_PROXIMA_KM -> StatusAlerta.PROXIMA
    else -> StatusAlerta.EM_DIA
}

/** Nome exibido: para "Outro" com observação, usa o que o usuário descreveu. */
fun descricaoManutencao(m: Manutencao): String =
    if (m.tipo == TipoManutencao.OUTRO && !m.observacao.isNullOrBlank()) m.observacao.trim() else m.tipo.rotulo

/** Intervalo típico em km, usado só como sugestão no formulário. */
fun intervaloSugeridoKm(tipo: TipoManutencao): Int? = when (tipo) {
    TipoManutencao.OLEO -> 10_000
    TipoManutencao.PNEUS -> 40_000
    TipoManutencao.FREIOS -> 30_000
    TipoManutencao.CORREIA -> 60_000
    TipoManutencao.ALINHAMENTO -> 10_000
    TipoManutencao.REVISAO -> 10_000
    TipoManutencao.OUTRO -> null
}

private fun chaveDoServico(m: Manutencao): String =
    if (m.tipo == TipoManutencao.OUTRO) "OUTRO:" + m.observacao.orEmpty().trim().lowercase() else m.tipo.name

/**
 * Um alerta por serviço (tipo; para "Outro", por descrição), calculado a partir do registro mais
 * recente dele. Sem intervalo no registro mais recente, não há alerta. Mais urgentes primeiro.
 */
fun calcularAlertas(manutencoes: List<Manutencao>, odometroAtualKm: Int): List<AlertaManutencao> =
    manutencoes.groupBy { chaveDoServico(it) }.values
        .mapNotNull { grupo ->
            val ultima = grupo.maxWithOrNull(
                compareBy<Manutencao> { it.odometroKm }.thenBy { it.data.toEpochDay() }
            ) ?: return@mapNotNull null
            val restantes = kmRestantes(ultima, odometroAtualKm) ?: return@mapNotNull null
            AlertaManutencao(ultima, restantes, statusAlerta(restantes))
        }
        .sortedBy { it.kmRestantes }

fun parseIntervaloKm(texto: String): Int? = texto.trim().toIntOrNull()?.takeIf { it > 0 }

/** Vazio é válido (sem alerta); preenchido precisa ser um inteiro positivo. */
fun intervaloValido(texto: String): Boolean = texto.isBlank() || parseIntervaloKm(texto) != null

/** Valor 0 é permitido (serviço na garantia). */
fun manutencaoValida(odometroKm: Int?, valorCentavos: Long?, intervaloTexto: String): Boolean =
    odometroKm != null && odometroKm > 0 &&
        valorCentavos != null && valorCentavos >= 0 &&
        intervaloValido(intervaloTexto)
