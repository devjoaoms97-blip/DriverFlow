package br.com.motoristafinanceiro.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones de traço do DriverFlow (24×24), os mesmos do design. O Material Icons Core não tem carro
 * nem gráfico, por isso eles são desenhados aqui; a cor vem do `tint` do Icon.
 */
object Icones {
    val Inicio = icone("Inicio", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Historico = icone("Historico", "M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01")
    val Mais = icone("Mais", "M12 5v14M5 12h14", espessura = 2.4f)
    val Veiculo = icone("Veiculo", "M3 13l2-6h14l2 6v4H3zM5 17v2M19 17v2M7.5 13.5h.01M16.5 13.5h.01")
    val Analises = icone("Analises", "M5 20V11M11 20V5M17 20v-6M3 20h18")
    /** A engrenagem do Material: a de traço fino lembrava um sol ou uma lâmpada. */
    val Ajustes: ImageVector = Icons.Filled.Settings
    val Alerta = icone("Alerta", "M12 3 2 21h20zM12 10v5M12 18h.01", espessura = 2f)
    val Seta = icone("Seta", "m9 6 6 6-6 6", espessura = 2f)
    val SetaVoltar = icone("SetaVoltar", "m15 6-6 6 6 6", espessura = 2f)
    val Calendario = icone("Calendario", "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM3 10h18M8 3v4M16 3v4")
    val Combustivel = icone("Combustivel", "M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 21h12M4 10h10M14 8l4 3v7a1.5 1.5 0 0 0 3 0V9l-3-3")
    val Chave = icone("Chave", "M14.7 6.3a4 4 0 0 0 5 5L11 20a2.1 2.1 0 0 1-3-3zM14.7 6.3 18 3")
    val Fechar = icone("Fechar", "M6 6l12 12M18 6 6 18", espessura = 2f)
    val Ganho = icone("Ganho", "M7 17 17 7M8 7h9v9", espessura = 2.2f)
    val Despesa = icone("Despesa", "M7 7l10 10M17 8v9H8", espessura = 2.2f)
    val Documento = icone("Documento", "M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM14 3v5h5M9 13h6M9 17h6")
    val Cadeado = icone("Cadeado", "M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1zM8 11V7a4 4 0 0 1 8 0v4M12 15v2")
    val Clipe = icone("Clipe", "M21 11.5 12.5 20a5 5 0 0 1-7-7L14 4.5a3.3 3.3 0 0 1 4.7 4.7L10.2 17.7a1.7 1.7 0 0 1-2.4-2.4L15.5 7.6")
}

private fun icone(nome: String, caminho: String, espessura: Float = 1.8f): ImageVector =
    ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(caminho),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = espessura,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
