package br.com.motoristafinanceiro.ui.analises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.domain.DesempenhoPlataforma
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import br.com.motoristafinanceiro.ui.componentes.BarraDividida
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.GradeDuasColunas
import br.com.motoristafinanceiro.ui.componentes.Indicador
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.componentes.TituloTela
import br.com.motoristafinanceiro.util.formatarReais

/** Altura máxima das barras do gráfico por dia da semana. */
private val alturaBarras = 120.dp

@Composable
fun AnalisesScreen(vm: AnalisesViewModel = viewModel(factory = AnalisesViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val periodos = PeriodoAnalise.entries

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = MargemTela, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TituloTela("Análises")

        SeletorSegmentado(periodos.map { it.rotulo }, periodos.indexOf(state.periodo), { vm.escolherPeriodo(periodos[it]) })

        if (!state.temDados) {
            Text("Nenhum ganho lançado neste período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        val t = state.total
        GradeDuasColunas(
            listOf(
                { m -> Indicador("Ganho bruto", t.ganhoBrutoCentavos.formatarReais(), m) },
                { m -> Indicador("Dias trabalhados", t.diasTrabalhados.toString(), m) },
                { m -> Indicador("Média por dia", t.mediaPorDiaCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Ganho por hora", t.ganhoPorHoraCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Ganho por km", t.ganhoPorKmCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Lançamentos", t.lancamentos.toString(), m) },
            )
        )

        CartaoDiasDaSemana(state.dias, state.melhorDia)

        if (state.plataformas.isNotEmpty()) CartaoPlataformas(state.plataformas)

        Text(
            "Ganho bruto (sem descontar despesas). O ganho por hora considera só os lançamentos com horas " +
                "informadas, e o ganho por km só os que têm km. Com poucos dias lançados, a comparação " +
                "pode enganar: veja quantos dias cada dia da semana tem.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CartaoDiasDaSemana(dias: List<DesempenhoDia>, melhor: DesempenhoDia?) {
    val esquema = MaterialTheme.colorScheme
    val maximo = dias.mapNotNull { it.desempenho.ganhoPorHoraCentavos }.maxOrNull()
    Cartao(espacamento = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Ganho por hora na semana", style = MaterialTheme.typography.titleMedium)
            melhor?.let {
                Text(textoMelhorDia(it), style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
            }
        }
        val descricao = dias.joinToString("; ") { rotuloDiaSemana(it.diaSemana) + ": " + textoDesempenhoDia(it) }
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = descricao },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            dias.forEach { d ->
                val valor = d.desempenho.ganhoPorHoraCentavos
                val destaque = melhor != null && d.diaSemana == melhor.diaSemana
                val fracao = fracaoDaBarra(valor, maximo)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        rotuloBarraPorHora(valor),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (destaque) esquema.onSurface else esquema.onSurfaceVariant,
                    )
                    Box(Modifier.height(alturaBarras).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (fracao > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(alturaBarras * fracao)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(if (destaque) esquema.primary else esquema.surfaceContainerHighest),
                            )
                        }
                    }
                    Text(
                        siglaDiaSemana(d.diaSemana),
                        style = MaterialTheme.typography.labelMedium,
                        color = esquema.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        HorizontalDivider(color = esquema.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            dias.forEach { d ->
                Row(Modifier.fillMaxWidth()) {
                    Text(rotuloDiaSemana(d.diaSemana), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.3f))
                    Text(
                        textoDesempenhoDia(d),
                        style = MaterialTheme.typography.bodySmall,
                        color = esquema.onSurfaceVariant,
                        modifier = Modifier.weight(0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoPlataformas(plataformas: List<DesempenhoPlataforma>) {
    val esquema = MaterialTheme.colorScheme
    val cores = listOf(esquema.primary, esquema.tertiary, esquema.secondary, esquema.error, esquema.outline)
    val corDe = { i: Int -> cores[i % cores.size] }
    val partes = percentuais(plataformas.map { it.desempenho.ganhoBrutoCentavos })
    Cartao(espacamento = 14.dp) {
        Text("Por plataforma", style = MaterialTheme.typography.titleMedium)
        BarraDividida(
            plataformas.mapIndexed { i, p -> p.desempenho.ganhoBrutoCentavos.toFloat() to corDe(i) },
            altura = 12.dp,
        )
        plataformas.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = esquema.outlineVariant.copy(alpha = 0.6f))
            LinhaPlataforma(p, partes[i], corDe(i))
        }
    }
}

@Composable
private fun LinhaPlataforma(p: DesempenhoPlataforma, percentual: Int, cor: Color) {
    val d = p.desempenho
    val esquema = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(cor))
            Text("${p.plataforma.rotulo} · $percentual%", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(d.ganhoBrutoCentavos.formatarReais(), style = MaterialTheme.typography.titleSmall)
        }
        Text(
            listOf(
                d.ganhoPorKmCentavos?.let { it.formatarReais() + "/km" } ?: "sem km",
                d.ganhoPorHoraCentavos?.let { it.formatarReais() + "/h" } ?: "sem horas",
                if (d.lancamentos == 1) "1 lançamento" else "${d.lancamentos} lançamentos",
                d.mediaPorLancamentoCentavos?.let { "média " + it.formatarReais() },
            ).filterNotNull().joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = esquema.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp),
        )
    }
}
