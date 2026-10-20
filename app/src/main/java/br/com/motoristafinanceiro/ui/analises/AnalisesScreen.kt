package br.com.motoristafinanceiro.ui.analises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.Desempenho
import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.domain.DesempenhoPlataforma
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import br.com.motoristafinanceiro.util.formatarReais

@Composable
fun AnalisesScreen(vm: AnalisesViewModel = viewModel(factory = AnalisesViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Análises", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodoAnalise.values().forEach {
                FilterChip(
                    selected = state.periodo == it,
                    onClick = { vm.escolherPeriodo(it) },
                    label = { Text(it.rotulo) },
                )
            }
        }

        if (!state.temDados) {
            Text("Nenhum ganho lançado neste período.")
            return@Column
        }

        CartaoTotal(state.total)

        state.melhorDia?.let {
            Text(textoMelhorDia(it), style = MaterialTheme.typography.titleSmall)
        }
        CartaoDiasDaSemana(state.dias)

        Text("Por plataforma", style = MaterialTheme.typography.titleMedium)
        state.plataformas.forEach { CartaoPlataforma(it) }

        Text(
            "Ganho bruto (sem descontar despesas). O ganho por hora considera só os lançamentos com horas " +
                "informadas, e o ganho por km só os que têm km. Com poucos dias lançados, a comparação " +
                "pode enganar: veja quantos dias cada linha tem.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartaoTotal(d: Desempenho) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resumo do período", style = MaterialTheme.typography.titleMedium)
            Linha("Ganho bruto", d.ganhoBrutoCentavos.formatarReais())
            Linha("Dias trabalhados", d.diasTrabalhados.toString())
            Linha("Média por dia trabalhado", d.mediaPorDiaCentavos?.formatarReais() ?: "—")
            Linha("Ganho por hora", d.ganhoPorHoraCentavos?.formatarReais() ?: "—")
            Linha("Ganho por km", d.ganhoPorKmCentavos?.formatarReais() ?: "—")
        }
    }
}

@Composable
private fun CartaoDiasDaSemana(dias: List<DesempenhoDia>) {
    val maximo = dias.mapNotNull { it.desempenho.ganhoPorHoraCentavos }.maxOrNull()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ganho por hora por dia da semana", style = MaterialTheme.typography.titleMedium)
            dias.forEach { d ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(rotuloDiaSemana(d.diaSemana), style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(
                        progress = { fracaoDaBarra(d.desempenho.ganhoPorHoraCentavos, maximo) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        textoDesempenhoDia(d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoPlataforma(p: DesempenhoPlataforma) {
    val d = p.desempenho
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(p.plataforma.rotulo, style = MaterialTheme.typography.titleMedium)
            Linha("Ganho bruto", d.ganhoBrutoCentavos.formatarReais())
            Linha("Lançamentos", d.lancamentos.toString())
            Linha("Média por lançamento", d.mediaPorLancamentoCentavos?.formatarReais() ?: "—")
            Linha("Ganho por hora", d.ganhoPorHoraCentavos?.formatarReais() ?: "—")
            Linha("Ganho por km", d.ganhoPorKmCentavos?.formatarReais() ?: "—")
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
