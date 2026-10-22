package br.com.motoristafinanceiro.data.relatorio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import br.com.motoristafinanceiro.R
import br.com.motoristafinanceiro.domain.LinhaRelatorio
import br.com.motoristafinanceiro.domain.RelatorioMes
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val ptBR = Locale("pt", "BR")
private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoDia = DateTimeFormatter.ofPattern("dd/MM")

/** "Outubro de 2026". */
fun tituloDoMes(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.FULL, ptBR).replaceFirstChar { it.uppercase() } + " de " + mes.year

/** "driverflow-relatorio-2026-10.pdf". */
fun nomeArquivoRelatorio(mes: YearMonth): String = "driverflow-relatorio-$mes.pdf"

/**
 * Desenha o relatório do mês em PDF (A4, em pontos). Primeira página: título, resumo, ganhos por plataforma e
 * despesas por categoria; depois, a lista de lançamentos, quebrando página quando não cabe.
 */
class RelatorioPdf(private val context: Context) {

    private val largura = 595
    private val altura = 842
    private val margem = 48f
    private val alturaLinha = 18f

    private val fonte: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull() ?: Typeface.SANS_SERIF
    private val negrito = Typeface.create(fonte, Typeface.BOLD)

    private fun pincel(tamanho: Float, forte: Boolean = false, cor: Int = PRETO) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (forte) negrito else fonte
        textSize = tamanho
        color = cor
    }

    private val titulo = pincel(20f, forte = true)
    private val subtitulo = pincel(11f, cor = CINZA)
    private val secao = pincel(12f, forte = true)
    private val texto = pincel(10f)
    private val textoForte = pincel(10f, forte = true)
    private val rodape = pincel(8f, cor = CINZA)
    private val linhaCinza = Paint().apply { color = CINZA_CLARO; strokeWidth = 0.8f }

    fun escrever(relatorio: RelatorioMes, saida: OutputStream, geradoEm: LocalDate = LocalDate.now()) {
        val doc = PdfDocument()
        try {
            val restantes = ArrayDeque(relatorio.linhas)
            var numero = 1
            do {
                val pagina = doc.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
                val c = pagina.canvas
                var y = margem + 10f
                if (numero == 1) {
                    y = cabecalho(c, relatorio, y)
                    y = resumo(c, relatorio, y)
                }
                // Quantas linhas cabem do ponto atual até o rodapé (o resumo da 1ª página ocupa espaço variável).
                val cabem = linhasQueCabem(y)
                val daPagina = List(minOf(cabem, restantes.size)) { restantes.removeFirst() }
                listaDeLancamentos(c, relatorio, daPagina, y, continuacao = numero > 1)
                c.drawText(
                    "Gerado em ${geradoEm.format(formatoData)} pelo app DriverFlow, com os valores lançados pelo próprio motorista.",
                    margem, altura - margem / 2, rodape,
                )
                doc.finishPage(pagina)
                numero++
            } while (restantes.isNotEmpty())
            doc.writeTo(saida)
        } finally {
            doc.close()
        }
    }

    /** Linhas de lançamento que cabem a partir de [y] (descontando o título da lista e o rodapé); pelo menos 1. */
    private fun linhasQueCabem(y: Float): Int =
        ((altura - margem - y - TITULO_DA_LISTA) / alturaLinha).toInt().coerceAtLeast(1)

    private fun cabecalho(c: Canvas, r: RelatorioMes, inicio: Float): Float {
        var y = inicio
        c.drawText("Relatório de ganhos e despesas", margem, y, titulo)
        y += 18f
        c.drawText("${tituloDoMes(r.mes)} · motorista de aplicativo", margem, y, subtitulo)
        y += 14f
        c.drawLine(margem, y, largura - margem, y, linhaCinza)
        return y + 22f
    }

    private fun resumo(c: Canvas, r: RelatorioMes, inicio: Float): Float {
        var y = inicio
        val t = r.totais
        c.drawText("Resumo do mês", margem, y, secao)
        y += 20f
        val itens = listOf(
            "Ganho bruto" to t.ganhoBrutoCentavos.formatarReais(),
            "Despesas" to t.despesasCentavos.formatarReais(),
            "Lucro líquido" to t.lucroLiquidoCentavos.formatarReais(),
            "Dias trabalhados" to r.diasTrabalhados.toString(),
            "Km rodados" to (t.kmTotal.takeIf { it > 0 }?.let { "${it.paraTextoNumero()} km" } ?: "—"),
            "Horas online" to (t.minutosOnline.takeIf { it > 0 }?.let { "${it.minutosParaHorasTexto()} h" } ?: "—"),
        )
        itens.forEach { (rotulo, valor) ->
            c.drawText(rotulo, margem, y, texto)
            direita(c, valor, y, if (rotulo == "Lucro líquido") textoForte else texto)
            y += alturaLinha
        }
        y += 10f
        y = tabela(c, "Ganhos por plataforma", r.porPlataforma.map { it.first.rotulo to it.second }, y)
        y = tabela(c, "Despesas por categoria", r.porCategoria.map { it.first.rotulo to it.second }, y)
        return y
    }

    private fun tabela(c: Canvas, nome: String, itens: List<Pair<String, Long>>, inicio: Float): Float {
        if (itens.isEmpty()) return inicio
        var y = inicio
        c.drawText(nome, margem, y, secao)
        y += 20f
        itens.forEach { (rotulo, centavos) ->
            c.drawText(rotulo, margem, y, texto)
            direita(c, centavos.formatarReais(), y, texto)
            y += alturaLinha
        }
        return y + 10f
    }

    private fun listaDeLancamentos(
        c: Canvas,
        r: RelatorioMes,
        linhas: List<LinhaRelatorio>,
        inicio: Float,
        continuacao: Boolean,
    ): Float {
        var y = inicio
        c.drawText(if (continuacao) "Lançamentos (continuação)" else "Lançamentos", margem, y, secao)
        y += 8f
        c.drawLine(margem, y, largura - margem, y, linhaCinza)
        y += 16f
        if (r.vazio) {
            c.drawText("Nenhum lançamento neste mês.", margem, y, texto)
            return y + alturaLinha
        }
        linhas.forEach { l ->
            c.drawText(l.data.format(formatoDia), margem, y, texto)
            c.drawText(l.descricao, margem + 56f, y, texto)
            val valor = (if (l.valorCentavos >= 0) "+ " else "− ") + abs(l.valorCentavos).formatarReais()
            direita(c, valor, y, texto)
            y += alturaLinha
        }
        return y
    }

    private fun direita(c: Canvas, valor: String, y: Float, p: Paint) {
        c.drawText(valor, largura - margem - p.measureText(valor), y, p)
    }

    companion object {
        /** Espaço do título "Lançamentos" e da linha embaixo dele. */
        private const val TITULO_DA_LISTA = 24f

        private const val PRETO = 0xFF0B1226.toInt()
        private const val CINZA = 0xFF44516B.toInt()
        private const val CINZA_CLARO = 0xFFC5CEE0.toInt()
    }
}
