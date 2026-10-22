package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

internal val teclaNumerica = KeyboardOptions(keyboardType = KeyboardType.Decimal)

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

/** Valor principal do lançamento, grande e centralizado, na cor do tipo (verde no ganho, vermelho na despesa). */
@Composable
internal fun CampoValor(rotulo: String, valor: String, onChange: (String) -> Unit, cor: Color) {
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
            keyboardOptions = teclaNumerica,
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

/** Campo compacto: rótulo pequeno em cima e o texto embaixo, num cartão (km, horas, gorjeta, observação). */
@Composable
internal fun CampoCompacto(
    rotulo: String,
    valor: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dica: String = "",
    numerico: Boolean = false,
    umaLinha: Boolean = true,
) {
    val esquema = MaterialTheme.colorScheme
    val estilo = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, color = esquema.onSurface)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = esquema.surfaceContainer,
        border = BorderStroke(1.dp, esquema.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant, maxLines = 1)
            BasicTextField(
                value = valor,
                onValueChange = onChange,
                singleLine = umaLinha,
                textStyle = estilo,
                keyboardOptions = if (numerico) teclaNumerica else KeyboardOptions.Default,
                cursorBrush = SolidColor(esquema.primary),
                decorationBox = { campo ->
                    Box {
                        if (valor.isEmpty() && dica.isNotEmpty()) {
                            Text(dica, style = estilo.copy(color = esquema.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 15.sp))
                        }
                        campo()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Faixa fixa no rodapé com o botão de salvar, sempre ao alcance do polegar. */
@Composable
internal fun BarraSalvar(texto: String, habilitado: Boolean, cor: Color, corTexto: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                onClick = onClick,
                enabled = habilitado,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = cor, contentColor = corTexto),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(texto, style = MaterialTheme.typography.titleMedium) }
        }
    }
}
