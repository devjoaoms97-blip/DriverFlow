package br.com.motoristafinanceiro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.motoristafinanceiro.ui.componentes.Icones

internal data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

/** Abas da barra inferior, nesta ordem; o + fica no meio, entre Histórico e Veículo. */
internal val abasEsquerda = listOf(
    Aba("inicio", "Início", Icones.Inicio),
    Aba("historico", "Histórico", Icones.Historico),
)
internal val abasDireita = listOf(
    Aba("veiculo", "Veículo", Icones.Veiculo),
    Aba("analises", "Análises", Icones.Analises),
)

/** Barra inferior do "Painel de bordo": 4 abas e o botão + no centro, ao alcance do polegar. */
@Composable
fun BarraInferior(
    rotaAtual: String?,
    onAbrir: (String) -> Unit,
    onNovo: () -> Unit,
    descricaoNovo: String,
) {
    val esquema = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(esquema.surfaceContainer).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = esquema.outlineVariant)
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            abasEsquerda.forEach { ItemAba(it, rotaAtual == it.rota, onAbrir, Modifier.weight(1f)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onNovo,
                    shape = CircleShape,
                    color = esquema.primary,
                    contentColor = esquema.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(56.dp).offset(y = (-14).dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icones.Mais, contentDescription = descricaoNovo, modifier = Modifier.size(26.dp))
                    }
                }
            }
            abasDireita.forEach { ItemAba(it, rotaAtual == it.rota, onAbrir, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ItemAba(aba: Aba, ativa: Boolean, onAbrir: (String) -> Unit, modifier: Modifier) {
    val cor = if (ativa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .fillMaxHeight()
            .selectable(selected = ativa, role = Role.Tab, onClick = { onAbrir(aba.rota) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(aba.icone, contentDescription = null, tint = cor, modifier = Modifier.size(24.dp))
        Text(
            aba.rotulo,
            color = cor,
            style = if (ativa) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.labelMedium,
        )
    }
}
