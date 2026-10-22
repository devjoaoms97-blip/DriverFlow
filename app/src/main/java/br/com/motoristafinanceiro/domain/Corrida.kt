package br.com.motoristafinanceiro.domain

import kotlin.math.roundToLong

/**
 * Uma oferta de corrida lida do card da Uber/99. Os campos que o card não mostrou ficam null.
 * Nome do passageiro e endereços não entram aqui: só os números da conta.
 */
data class OfertaCorrida(
    val valorCentavos: Long,
    val kmAteBusca: Double? = null,
    val minAteBusca: Int? = null,
    val kmViagem: Double? = null,
    val minViagem: Int? = null,
    val nota: Double? = null,
) {
    /** km do deslocamento até o passageiro + km da viagem (o carro roda os dois). */
    val kmTotal: Double get() = (kmAteBusca ?: 0.0) + (kmViagem ?: 0.0)

    /** null quando o card não mostrou nenhum tempo. */
    val minTotal: Int? get() = if (minAteBusca == null && minViagem == null) null else (minAteBusca ?: 0) + (minViagem ?: 0)
}

/** Um trecho "N min (X km)" do card. */
private data class Trecho(val km: Double, val min: Int?, val linha: String)

private val PRECO = Regex("""(?<![+\d])R\$\s?(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{2}))?(?![\d,.]|\s?/)""")
private val KM = Regex("""(\d+(?:[.,]\d+)?)\s?km\b""", RegexOption.IGNORE_CASE)
private val METROS = Regex("""(?<![.,\d])(\d+)\s?m(?![a-zA-Zà-ú])""")
private val MINUTOS = Regex("""(?:(\d+)\s?h\s?)?(\d+)\s?min""", RegexOption.IGNORE_CASE)
private val NOTA = Regex("""(?<![\d,.])([1-5][.,]\d{1,2})(?![\d,.])""")
private val PALAVRAS_VIAGEM = Regex("""viagem|destino|total""", RegexOption.IGNORE_CASE)

/**
 * Lê uma oferta dos textos visíveis na tela, na ordem em que aparecem.
 *
 * Regras (ajustadas conforme as gravações de cards reais):
 * - valor: o primeiro "R$ 12,50" que não seja um acréscimo ("+R$ 3,00") nem uma taxa ("R$ 1,50/km");
 * - trechos: cada linha com distância (km ou metros) é um trecho, com o tempo da mesma linha, se houver;
 *   o primeiro trecho é até o passageiro e o segundo é a viagem; um trecho só conta como a viagem;
 * - nota: número de 1,00 a 5,00 numa linha com ★ (ou sozinho na linha).
 *
 * Devolve null quando não há valor e distância: aí a tela não é uma oferta.
 */
fun lerOferta(textos: List<String>): OfertaCorrida? {
    val linhas = textos.map { it.replace(' ', ' ').trim() }.filter { it.isNotEmpty() }
    val valor = linhas.firstNotNullOfOrNull { precoEmCentavos(it) } ?: return null
    val trechos = linhas.mapNotNull { trechoDaLinha(it) }
    if (trechos.isEmpty()) return null

    val (busca, viagem) = when {
        trechos.size >= 2 -> trechos[0] to trechos[1]
        else -> null to trechos[0]
    }
    return OfertaCorrida(
        valorCentavos = valor,
        kmAteBusca = busca?.km,
        minAteBusca = busca?.min,
        kmViagem = viagem.km,
        minViagem = viagem.min,
        nota = linhas.firstNotNullOfOrNull { notaDaLinha(it) },
    )
}

private fun precoEmCentavos(linha: String): Long? {
    val m = PRECO.find(linha) ?: return null
    val reais = m.groupValues[1].replace(".", "").toLongOrNull() ?: return null
    val centavos = m.groupValues[2].ifEmpty { "0" }.toLong()
    return reais * 100 + centavos
}

private fun trechoDaLinha(linha: String): Trecho? {
    if (linha.contains("R$")) return null // "R$ 1,50/km" é taxa, não distância
    val km = KM.find(linha)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        ?: METROS.find(linha)?.groupValues?.get(1)?.toDoubleOrNull()?.div(1000)
        ?: return null
    val min = MINUTOS.find(linha)?.let { m ->
        (m.groupValues[1].toIntOrNull() ?: 0) * 60 + m.groupValues[2].toInt()
    }
    return Trecho(km, min, linha)
}

private fun notaDaLinha(linha: String): Double? {
    val comEstrela = linha.contains('★') || linha.contains('⭐')
    val sozinha = linha.matches(Regex("""[1-5][.,]\d{1,2}"""))
    if (!comEstrela && !sozinha) return null
    if (linha.contains("R$") || KM.containsMatchIn(linha)) return null
    return NOTA.find(linha)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 1.0..5.0 }
}

/** Faixas do R$/km escolhidas pelo motorista: verde a partir de [bomPorKmCentavos], amarelo a partir de [minimoPorKmCentavos]. */
data class LimitesCorrida(val bomPorKmCentavos: Long = 200, val minimoPorKmCentavos: Long = 150) {
    val validos: Boolean get() = minimoPorKmCentavos in 1..bomPorKmCentavos
}

enum class CorOferta { VERDE, AMARELO, VERMELHO }

data class AvaliacaoCorrida(
    val oferta: OfertaCorrida,
    /** Valor ÷ (km até o passageiro + km da viagem); null sem distância. */
    val porKmCentavos: Long?,
    /** Valor ÷ (tempo até o passageiro + tempo da viagem); null sem tempo. */
    val porHoraCentavos: Long?,
    /** Valor − custo por km × km total; null sem custo por km conhecido. */
    val lucroCentavos: Long?,
    val cor: CorOferta,
)

fun avaliarOferta(oferta: OfertaCorrida, custoPorKmCentavos: Long?, limites: LimitesCorrida): AvaliacaoCorrida {
    val km = oferta.kmTotal
    val porKm = if (km > 0) (oferta.valorCentavos / km).roundToLong() else null
    val minutos = oferta.minTotal
    val porHora = if (minutos != null && minutos > 0) (oferta.valorCentavos * 60.0 / minutos).roundToLong() else null
    val lucro = custoPorKmCentavos?.let { oferta.valorCentavos - (it * km).roundToLong() }
    val cor = when {
        porKm == null -> CorOferta.AMARELO
        porKm >= limites.bomPorKmCentavos -> CorOferta.VERDE
        porKm >= limites.minimoPorKmCentavos -> CorOferta.AMARELO
        else -> CorOferta.VERMELHO
    }
    return AvaliacaoCorrida(oferta, porKm, porHora, lucro, cor)
}
