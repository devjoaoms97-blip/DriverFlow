package br.com.motoristafinanceiro.ui.reservas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.util.formatarReais
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ReservasScreen(
    onEditar: (Long) -> Unit,
    vm: ReservasViewModel = viewModel(factory = ReservasViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Separar por dia", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.totalDiarioCentavos.formatarReais(),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Por semana")
                        Text(state.totalSemanalCentavos.formatarReais())
                    }
                    Text(
                        "Dividido por todos os dias do período, trabalhando ou não.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text(
                "Despesas fixas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (state.itens.isEmpty()) {
            item { Text("Toque em + para cadastrar IPVA, seguro, parcela do carro e outras despesas fixas.") }
        } else {
            items(state.itens, key = { it.reserva.id }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEditar(item.reserva.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.reserva.nome, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOf(
                                item.reserva.periodicidade.rotulo + " · " + item.reserva.valorCentavos.formatarReais(),
                                textoVencimento(item.diasParaVencer) + " (" + item.proximaOcorrencia.format(formatoData) + ")",
                            ).joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        item.diariaCentavos.formatarReais() + "\npor dia",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
