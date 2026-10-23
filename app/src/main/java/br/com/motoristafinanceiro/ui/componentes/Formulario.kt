package br.com.motoristafinanceiro.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Mostra "R$ " antes do que foi digitado, sem fazer parte do texto do campo. */
internal object PrefixoReais : VisualTransformation {
    private const val PREFIXO = "R$ "

    override fun filter(text: AnnotatedString) = TransformedText(
        AnnotatedString(PREFIXO) + text,
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + PREFIXO.length
            override fun transformedToOriginal(offset: Int) = (offset - PREFIXO.length).coerceIn(0, text.length)
        },
    )
}

/**
 * Campo vazio fica sem o prefixo, para o "R$ 0,00" de exemplo não aparecer por cima do "R$ " do campo;
 * com algo digitado, o "R$ " aparece na frente.
 */
internal fun transformacaoDoValor(valor: String): VisualTransformation =
    if (valor.isEmpty()) VisualTransformation.None else PrefixoReais

/** Valor principal do formulário, grande e centralizado, na cor do tipo (verde no ganho, vermelho na despesa...). */
@Composable
fun CampoValor(rotulo: String, valor: String, onChange: (String) -> Unit, cor: Color) {
    val estilo = MaterialTheme.typography.displaySmall.copy(color = cor, textAlign = TextAlign.Center)
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(rotulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = valor,
            onValueChange = onChange,
            singleLine = true,
            textStyle = estilo,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = transformacaoDoValor(valor),
            cursorBrush = SolidColor(cor),
            decorationBox = { campo ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (valor.isEmpty()) Text("R$ 0,00", style = estilo.copy(color = cor.copy(alpha = 0.4f)))
                    campo()
                }
            },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = rotulo },
        )
    }
}

/**
 * Campo compacto: rótulo pequeno em cima e o texto embaixo, num cartão (km, horas, odômetro, observação...).
 * [teclado] escolhe o teclado numérico (Decimal aceita vírgula; Number, só inteiros). Com [erro], a borda
 * fica vermelha.
 */
@Composable
fun CampoCompacto(
    rotulo: String,
    valor: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dica: String = "",
    teclado: KeyboardType = KeyboardType.Text,
    umaLinha: Boolean = true,
    erro: Boolean = false,
) {
    val esquema = MaterialTheme.colorScheme
    val estilo = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, color = esquema.onSurface)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = esquema.surfaceContainer,
        border = BorderStroke(1.dp, if (erro) esquema.error else esquema.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = if (erro) esquema.error else esquema.onSurfaceVariant,
                maxLines = 1,
            )
            BasicTextField(
                value = valor,
                onValueChange = onChange,
                singleLine = umaLinha,
                textStyle = estilo,
                keyboardOptions = KeyboardOptions(keyboardType = teclado),
                cursorBrush = SolidColor(esquema.primary),
                decorationBox = { campo ->
                    Box {
                        if (valor.isEmpty() && dica.isNotEmpty()) {
                            Text(dica, style = estilo.copy(color = esquema.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 15.sp))
                        }
                        campo()
                    }
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = rotulo },
            )
        }
    }
}

/** Texto pequeno de apoio abaixo de um campo; vermelho quando é um erro. */
@Composable
fun TextoDeApoio(texto: String, erro: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** Área que rola com os campos; o botão de salvar fica fora dela, fixo no rodapé. */
@Composable
fun CorpoFormulario(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = MargemTela, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = conteudo,
    )
}

/**
 * Esqueleto das telas de formulário: cabeçalho (X para fechar no cadastro, seta no editar), campos que
 * rolam e o botão de salvar fixo no rodapé.
 */
@Composable
fun TelaFormulario(
    titulo: String,
    novo: Boolean,
    onVoltar: () -> Unit,
    textoSalvar: String,
    podeSalvar: Boolean,
    onSalvar: () -> Unit,
    corSalvar: Color = MaterialTheme.colorScheme.primary,
    corTextoSalvar: Color = MaterialTheme.colorScheme.onPrimary,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (novo) {
            CabecalhoVoltar(titulo, onVoltar, icone = Icones.Fechar, descricaoVoltar = "Fechar")
        } else {
            CabecalhoVoltar(titulo, onVoltar)
        }
        CorpoFormulario(Modifier.weight(1f), conteudo)
        BarraSalvar(textoSalvar, podeSalvar, corSalvar, corTextoSalvar, onSalvar)
    }
}

/** Botões de escolha (plataforma, categoria, tipo de serviço...) que quebram linha quando não cabem. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Escolhas(
    titulo: String,
    opcoes: List<T>,
    escolhida: T?,
    rotulo: (T) -> String,
    onEscolher: (T) -> Unit,
    destaque: Color = MaterialTheme.colorScheme.primary,
    fundoDestaque: Color = MaterialTheme.colorScheme.primaryContainer,
    textoDestaque: Color = if (destaque == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimaryContainer else destaque,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (titulo.isNotEmpty()) RotuloSecao(titulo)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach {
                Chip(rotulo(it), it == escolhida, { onEscolher(it) }, destaque, fundoDestaque, textoDestaque)
            }
        }
    }
}

/** Faixa fixa no rodapé com o botão de salvar, sempre ao alcance do polegar. */
@Composable
fun BarraSalvar(texto: String, habilitado: Boolean, cor: Color, corTexto: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            BotaoPrincipal(texto, habilitado, cor, corTexto, onClick)
        }
    }
}

/** Botão cheio e alto (56 dp), usado no rodapé dos formulários e nas ações principais de Ajustes. */
@Composable
fun BotaoPrincipal(
    texto: String,
    habilitado: Boolean = true,
    cor: Color = MaterialTheme.colorScheme.primary,
    corTexto: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = cor, contentColor = corTexto),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) { Text(texto, style = MaterialTheme.typography.titleMedium) }
}

/** Ação destrutiva no fim dos campos (Excluir, Remover), em texto vermelho. */
@Composable
fun BotaoExcluir(texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
}

/** Confirmação de exclusão com o botão de confirmar em vermelho. */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(textoConfirmar, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** Registro que não existe mais (ex.: apagado por um backup restaurado). */
@Composable
fun NaoEncontrado(texto: String, onVoltar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(MargemTela),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onVoltar) { Text("Voltar") }
    }
}

/** Linha com título, explicação e uma chave liga/desliga, dentro de um cartão. */
@Composable
fun LinhaChave(titulo: String, descricao: String, ligada: Boolean, onMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = ligada, onCheckedChange = onMudar)
    }
}
