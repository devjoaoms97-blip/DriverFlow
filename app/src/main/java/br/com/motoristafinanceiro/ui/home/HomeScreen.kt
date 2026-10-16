package br.com.motoristafinanceiro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.ui.veiculo.ListaAlertas
import br.com.motoristafinanceiro.util.formatarReais

@Composable
fun HomeScreen(
    onAbrirAjustes: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Início", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onAbrirAjustes) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
            }
        }

        if (state.alertasUrgentes.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Atenção: manutenção", style = MaterialTheme.typography.titleMedium)
                    ListaAlertas(state.alertasUrgentes)
                }
            }
        }

        CartaoResumo("Hoje", state.hoje)
        CartaoResumo("Este mês", state.mes)

        state.metaCentavos?.takeIf { it > 0 }?.let { meta ->
            val lucro = state.mes.lucroLiquidoCentavos
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meta do mês", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { (lucro.toFloat() / meta).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${lucro.formatarReais()} de ${meta.formatarReais()}")
                }
            }
        }
    }
}

@Composable
private fun CartaoResumo(titulo: String, e: Estatisticas) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                e.lucroLiquidoCentavos.formatarReais(),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text("lucro líquido", style = MaterialTheme.typography.labelMedium)
            Linha("Ganho bruto", e.ganhoBrutoCentavos.formatarReais())
            Linha("Despesas", e.despesasCentavos.formatarReais())
            Linha("Ganho por km", e.ganhoPorKmCentavos?.formatarReais() ?: "—")
            Linha("Ganho por hora", e.ganhoPorHoraCentavos?.formatarReais() ?: "—")
            Linha("Custo por km", e.custoPorKmCentavos?.formatarReais() ?: "—")
            if (e.depreciacaoCentavos > 0) {
                Linha("Depreciação estimada", e.depreciacaoCentavos.formatarReais())
                Linha("Lucro após depreciação", e.lucroAposDepreciacaoCentavos.formatarReais())
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo)
        Text(valor)
    }
}
