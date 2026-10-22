package br.com.motoristafinanceiro.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Margem lateral das telas. */
val MargemTela = 16.dp

/** Cartão do DriverFlow: superfície com borda fina. Com [onClick], o cartão inteiro é tocável. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    forma: Shape = MaterialTheme.shapes.large,
    cor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borda: Color? = MaterialTheme.colorScheme.outlineVariant,
    espacamentoInterno: PaddingValues = PaddingValues(20.dp),
    espacamento: Dp = 12.dp,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val contorno = borda?.let { BorderStroke(1.dp, it) }
    val corpo = @Composable {
        Column(
            Modifier.padding(espacamentoInterno),
            verticalArrangement = Arrangement.spacedBy(espacamento),
            content = conteudo,
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = forma, color = cor, border = contorno) { corpo() }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = forma, color = cor, border = contorno) { corpo() }
    }
}

/** Título da tela, com uma linha pequena acima (ex.: a data) e ações à direita. */
@Composable
fun TituloTela(
    texto: String,
    modifier: Modifier = Modifier,
    sobretitulo: String? = null,
    acoes: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            sobretitulo?.let { RotuloSecao(it) }
            Text(texto, style = MaterialTheme.typography.headlineSmall)
        }
        acoes()
    }
}

/** Cabeçalho das telas secundárias: botão de voltar (ou fechar), título e ações. */
@Composable
fun CabecalhoVoltar(
    titulo: String,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector = Icones.SetaVoltar,
    descricaoVoltar: String = "Voltar",
    acoes: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVoltar, modifier = Modifier.size(48.dp)) {
            Icon(icone, contentDescription = descricaoVoltar)
        }
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        acoes()
    }
}

/** Rótulo pequeno em caixa alta que separa seções. */
@Composable
fun RotuloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Botão redondo de ícone com borda (ex.: Ajustes no Início). */
@Composable
fun BotaoIcone(icone: ImageVector, descricao: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = descricao, modifier = Modifier.size(20.dp))
        }
    }
}

/** Rótulo pequeno e valor em destaque, num cartão médio. */
@Composable
fun Indicador(rotulo: String, valor: String, modifier: Modifier = Modifier, corValor: Color = Color.Unspecified) {
    Cartao(
        modifier,
        forma = MaterialTheme.shapes.medium,
        espacamentoInterno = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        espacamento = 4.dp,
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = MaterialTheme.typography.titleLarge,
            color = corValor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Rótulo pequeno e valor, sem cartão (dentro de um cartão maior). */
@Composable
fun ValorRotulado(rotulo: String, valor: String, modifier: Modifier = Modifier, corValor: Color = Color.Unspecified) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = corValor)
    }
}

/** Duas colunas iguais; uma linha por par de itens. */
@Composable
fun GradeDuasColunas(itens: List<@Composable (Modifier) -> Unit>, espaco: Dp = 10.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(espaco)) {
        itens.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(espaco)) {
                par.forEach { it(Modifier.weight(1f)) }
                if (par.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Seletor de opções lado a lado (Hoje/Semana/Mês, Ganho/Despesa). [corSelecionada] e
 * [corTextoSelecionado] permitem pintar a opção escolhida (verde no ganho, vermelho na despesa).
 */
@Composable
fun SeletorSegmentado(
    opcoes: List<String>,
    selecionado: Int,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    corSelecionada: (Int) -> Color = { Color.Unspecified },
    corTextoSelecionado: (Int) -> Color = { Color.Unspecified },
    icones: List<ImageVector?> = emptyList(),
    altura: Dp = 40.dp,
) {
    val esquema = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(esquema.surfaceContainer)
            .border(1.dp, esquema.outlineVariant, MaterialTheme.shapes.small)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opcoes.forEachIndexed { i, texto ->
            val ativo = i == selecionado
            val fundo = if (ativo) corSelecionada(i).takeOrElse(esquema.primary) else Color.Transparent
            val cor = if (ativo) corTextoSelecionado(i).takeOrElse(esquema.onPrimary) else esquema.onSurfaceVariant
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = altura)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fundo)
                    .selectable(selected = ativo, role = Role.Tab, onClick = { onSelecionar(i) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icones.getOrNull(i)?.let {
                    Icon(it, contentDescription = null, tint = cor, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                }
                Text(
                    texto,
                    color = cor,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                )
            }
        }
    }
}

private fun Color.takeOrElse(outra: Color) = if (this == Color.Unspecified) outra else this

/** Chip de escolha (plataforma, categoria, filtro). Escolhido: borda e texto na cor de destaque. */
@Composable
fun Chip(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    corDestaque: Color = MaterialTheme.colorScheme.primary,
    fundoDestaque: Color = MaterialTheme.colorScheme.primaryContainer,
    textoDestaque: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        selected = selecionado,
        onClick = onClick,
        shape = CircleShape,
        color = if (selecionado) fundoDestaque else Color.Transparent,
        border = BorderStroke(1.dp, if (selecionado) corDestaque else esquema.outlineVariant),
    ) {
        Box(Modifier.heightIn(min = 40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                color = if (selecionado) textoDestaque else esquema.onSurface,
            )
        }
    }
}

/** Selo pequeno de estado ("Atrasada", "Em 320 km"). */
@Composable
fun Selo(texto: String, cor: Color, fundo: Color) {
    Box(
        Modifier.clip(CircleShape).background(fundo).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = cor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
    }
}

/** Barra de progresso arredondada; [fracao] entre 0 e 1. */
@Composable
fun BarraProgresso(
    fracao: Float,
    cor: Color,
    modifier: Modifier = Modifier,
    altura: Dp = 8.dp,
    trilho: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Box(modifier.fillMaxWidth().height(altura).clip(CircleShape).background(trilho)) {
        val f = fracao.coerceIn(0f, 1f)
        if (f > 0f) Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(cor))
    }
}

/** Barra dividida em partes proporcionais (ganho × despesa, Uber × 99). Partes com peso 0 somem. */
@Composable
fun BarraDividida(partes: List<Pair<Float, Color>>, modifier: Modifier = Modifier, altura: Dp = 8.dp) {
    val visiveis = partes.filter { it.first > 0f }
    if (visiveis.isEmpty()) {
        BarraProgresso(0f, Color.Transparent, modifier, altura)
        return
    }
    Row(modifier.fillMaxWidth().height(altura), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        visiveis.forEach { (peso, cor) ->
            Box(Modifier.weight(peso).fillMaxHeight().clip(CircleShape).background(cor))
        }
    }
}

/** Quadradinho colorido com ícone ou sigla, no começo das linhas e dos avisos. */
@Composable
fun Marcador(
    fundo: Color,
    cor: Color,
    icone: ImageVector? = null,
    sigla: String? = null,
    tamanho: Dp = 40.dp,
) {
    Box(
        Modifier.size(tamanho).clip(RoundedCornerShape(12.dp)).background(fundo),
        contentAlignment = Alignment.Center,
    ) {
        if (icone != null) Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        else if (sigla != null) Text(sigla, color = cor, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
    }
}

/** Linha de aviso tocável: marcador, título, detalhe e seta. */
@Composable
fun LinhaAviso(
    titulo: String,
    detalhe: String,
    marcadorFundo: Color,
    marcadorCor: Color,
    icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fundo: Color = MaterialTheme.colorScheme.surfaceContainer,
    borda: Color? = MaterialTheme.colorScheme.outlineVariant,
    corTexto: Color = MaterialTheme.colorScheme.onSurface,
    corDetalhe: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = fundo,
        contentColor = corTexto,
        border = borda?.let { BorderStroke(1.dp, it) },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Marcador(marcadorFundo, marcadorCor, icone = icone)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
                if (detalhe.isNotBlank()) {
                    Text(detalhe, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = corDetalhe)
                }
            }
            Icon(Icones.Seta, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
