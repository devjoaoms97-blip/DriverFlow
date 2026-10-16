package br.com.motoristafinanceiro.ui.veiculo

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.domain.ResumoVeiculo
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.domain.precoPorLitroCentavos
import br.com.motoristafinanceiro.util.formatarKm
import br.com.motoristafinanceiro.util.formatarKmPorLitro
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun VeiculoScreen(
    onEditarAbastecimento: (Long) -> Unit,
    onEditarManutencao: (Long) -> Unit,
    vm: VeiculoViewModel = viewModel(factory = VeiculoViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { CartaoResumo(state.resumo, state.depreciacaoPorKmCentavos) }

        if (state.alertas.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Alertas de manutenção", style = MaterialTheme.typography.titleMedium)
                        ListaAlertas(state.alertas)
                    }
                }
            }
        }

        item { Titulo("Manutenções") }
        if (state.manutencoes.isEmpty()) {
            item { Text("Toque em + e escolha Manutenção para registrar a primeira.") }
        } else {
            items(state.manutencoes, key = { "m${it.id}" }) { m ->
                LinhaManutencao(m) { onEditarManutencao(m.id) }
            }
        }

        item { Titulo("Abastecimentos") }
        if (state.abastecimentos.isEmpty()) {
            item { Text("Toque em + e escolha Abastecimento para registrar o primeiro.") }
        } else {
            items(state.abastecimentos, key = { "a${it.id}" }) { a ->
                LinhaAbastecimento(a) { onEditarAbastecimento(a.id) }
            }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun CartaoResumo(r: ResumoVeiculo, depreciacaoPorKmCentavos: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resumo do veículo", style = MaterialTheme.typography.titleMedium)
            Linha("Consumo médio", r.consumoMedioKmPorLitro?.formatarKmPorLitro() ?: "—")
            Linha("Odômetro atual", r.odometroAtualKm?.formatarKm() ?: "—")
            Linha("Último litro", r.precoUltimoLitroCentavos?.let { it.formatarReais() + "/L" } ?: "—")
            Linha("Custo por km (mês)", r.custoPorKmMesCentavos?.let { it.formatarReais() + "/km" } ?: "—")
            if (depreciacaoPorKmCentavos > 0) {
                Text(
                    "O custo por km inclui depreciação de " + depreciacaoPorKmCentavos.formatarReais() +
                        "/km (ajuste na engrenagem do Início).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (r.consumoMedioKmPorLitro == null) {
                Text(
                    "O consumo aparece depois de 2 abastecimentos com tanque cheio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun LinhaManutencao(m: Manutencao, onClick: () -> Unit) {
    val detalhes = listOfNotNull(
        m.data.format(formatoData),
        m.odometroKm.formatarKm(),
        m.intervaloKm?.let { "a cada " + it.formatarKm() },
    ).joinToString(" · ")

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(descricaoManutencao(m), style = MaterialTheme.typography.bodyLarge)
            Text(
                detalhes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(m.valorCentavos.formatarReais(), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinhaAbastecimento(a: Abastecimento, onClick: () -> Unit) {
    val preco = precoPorLitroCentavos(a)?.let { it.formatarReais() + "/L" }
    val detalhes = listOfNotNull(
        a.litros.paraTextoNumero() + " L",
        preco,
        a.odometroKm.formatarKm(),
    ).joinToString(" · ")

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                a.data.format(formatoData) + if (a.tanqueCheio) "" else " (parcial)",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                detalhes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(a.valorTotalCentavos.formatarReais(), style = MaterialTheme.typography.bodyLarge)
    }
}
