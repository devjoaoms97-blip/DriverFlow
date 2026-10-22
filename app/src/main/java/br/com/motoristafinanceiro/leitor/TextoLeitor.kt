package br.com.motoristafinanceiro.leitor

import br.com.motoristafinanceiro.domain.AvaliacaoCorrida
import br.com.motoristafinanceiro.domain.LimitesCorrida
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.util.Locale

private val ptBR = Locale("pt", "BR")

private fun Double.km(): String = String.format(ptBR, "%.1f km", this)

/** Linha grande da janela: "R$ 1,85/km" (ou o valor, se o card não mostrou distância). */
fun tituloAvaliacao(a: AvaliacaoCorrida): String =
    a.porKmCentavos?.let { "${it.formatarReais()}/km" } ?: a.oferta.valorCentavos.formatarReais()

/** "R$ 12,50 · R$ 42,00/h · lucro ≈ R$ 9,80" */
fun linhaValores(a: AvaliacaoCorrida): String = listOfNotNull(
    a.oferta.valorCentavos.formatarReais(),
    a.porHoraCentavos?.let { "${it.formatarReais()}/h" },
    a.lucroCentavos?.let { "lucro ≈ ${it.formatarReais()}" },
).joinToString(" · ")

/** "6,7 km no total (1,2 km até o passageiro) · 22 min · ★ 4,9" */
fun linhaPercurso(a: AvaliacaoCorrida): String {
    val o = a.oferta
    val km = buildString {
        append(o.kmTotal.km()).append(" no total")
        o.kmAteBusca?.let { append(" (").append(it.km()).append(" até o passageiro)") }
    }
    return listOfNotNull(
        km,
        o.minTotal?.let { "$it min" },
        o.nota?.let { String.format(ptBR, "★ %.2f", it).trimEnd('0').trimEnd(',') },
    ).joinToString(" · ")
}

/** Lê os dois campos de Ajustes; null se algum for inválido ou o amarelo passar do verde. */
fun limitesDosCampos(bom: String, minimo: String): LimitesCorrida? {
    val b = bom.reaisParaCentavos() ?: return null
    val m = minimo.reaisParaCentavos() ?: return null
    return LimitesCorrida(b, m).takeIf { it.validos }
}

/** "Gravação: 12 KB" */
fun textoTamanhoGravacao(bytes: Long): String = when {
    bytes <= 0 -> "Nada gravado ainda."
    bytes < 1024 -> "Gravação: $bytes bytes"
    bytes < 1024 * 1024 -> "Gravação: ${bytes / 1024} KB"
    else -> String.format(ptBR, "Gravação: %.1f MB", bytes / (1024.0 * 1024.0))
}

/**
 * Se o serviço aparece na lista de acessibilidade ligada do Android ("pacote/classe:pacote/classe").
 * [nomes] = as formas do nome do componente (completa e curta, "pacote/.Classe").
 */
fun servicoLigado(listaDoSistema: String?, vararg nomes: String): Boolean =
    listaDoSistema.orEmpty().split(':').any { item -> nomes.any { it.equals(item.trim(), ignoreCase = true) } }
