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
import br.com.motoristafinanceiro.domain.DesempenhoFaixa
import br.com.motoristafinanceiro.domain.DesempenhoPlataforma
import br.com.motoristafinanceiro.domain.MESES_NA_EVOLUCAO
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import br.com.motoristafinanceiro.domain.ResumoMes
import br.com.motoristafinanceiro.domain.melhorMes
import br.com.motoristafinanceiro.ui.componentes.BarraDividida
import br.com.motoristafinanceiro.ui.componentes.BarraProgresso
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.GradeDuasColunas
import br.com.motoristafinanceiro.ui.componentes.Indicador
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.componentes.TituloTela
import br.com.motoristafinanceiro.util.formatarReais
import java.time.YearMonth
import kotlin.math.abs

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
            // A evolução não depende do período: mostra mesmo assim, se houver meses com lançamento.
            if (state.evolucao.any { it.temLancamentos }) CartaoEvolucao(state.evolucao)
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

        CartaoFaixas(state.faixas, state.melhorFaixa, state.ganhosSemHorario)

        if (state.plataformas.isNotEmpty()) CartaoPlataformas(state.plataformas)

        if (state.evolucao.any { it.temLancamentos }) CartaoEvolucao(state.evolucao)

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

/** Lucro dos últimos meses em barras (prejuízo em vermelho), com o R$/h embaixo; o mês atual em destaque. */
@Composable
private fun CartaoEvolucao(meses: List<ResumoMes>) {
    val esquema = MaterialTheme.colorScheme
    val atual = YearMonth.now()
    val maximo = meses.maxOf { abs(it.lucroCentavos) }.coerceAtLeast(1)
    Cartao(espacamento = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Evolução mês a mês", style = MaterialTheme.typography.titleMedium)
            Text(
                melhorMes(meses, atual)?.let { textoMelhorMes(it) } ?: "Lucro líquido dos últimos $MESES_NA_EVOLUCAO meses",
                style = MaterialTheme.typography.bodySmall,
                color = esquema.onSurfaceVariant,
            )
        }
        val descricao = meses.joinToString("; ") { "${siglaMes(it.mes)}: ${it.lucroCentavos.formatarReais()}" }
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = descricao },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            meses.forEach { m ->
                val ehAtual = m.mes == atual
                val prejuizo = m.lucroCentavos < 0
                val fracao = (abs(m.lucroCentavos).toFloat() / maximo).coerceIn(0f, 1f)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (m.temLancamentos) rotuloBarraLucro(m.lucroCentavos) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (prejuizo) esquema.error else if (ehAtual) esquema.onSurface else esquema.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Box(Modifier.height(alturaBarras).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (fracao > 0f) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(alturaBarras * fracao)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(
                                        when {
                                            prejuizo -> esquema.error
                                            ehAtual -> esquema.primary
                                            else -> esquema.surfaceContainerHighest
                                        }
                                    ),
                            )
                        }
                    }
                    Text(
                        siglaMes(m.mes) + if (ehAtual) "*" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = esquema.onSurfaceVariant,
                    )
                    Text(rotuloPorHoraMes(m.ganhoPorHoraCentavos), style = MaterialTheme.typography.labelSmall, color = esquema.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        Text(
            "* mês em andamento. Embaixo de cada mês, o ganho por hora.",
            style = MaterialTheme.typography.bodySmall,
            color = esquema.onSurfaceVariant,
        )
    }
}

/** Ganho por hora em cada faixa do dia (madrugada, manhã, tarde, noite), com barra relativa à melhor. */
@Composable
private fun CartaoFaixas(faixas: List<DesempenhoFaixa>, melhor: DesempenhoFaixa?, semHorario: Int) {
    val esquema = MaterialTheme.colorScheme
    val maximo = faixas.mapNotNull { it.ganhoPorHoraCentavos }.maxOrNull()
    Cartao(espacamento = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Ganho por hora por horário", style = MaterialTheme.typography.titleMedium)
            melhor?.let {
                Text(textoMelhorFaixa(it), style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
            }
        }
        if (maximo == null) {
            Text(
                "Informe o início do turno ao lançar um ganho para ver em que horário você ganha mais.",
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurfaceVariant,
            )
        } else {
            faixas.forEach { f ->
                val destaque = melhor != null && f.faixa == melhor.faixa
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.faixa.rotulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(intervaloFaixa(f.faixa), style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant)
                    }
                    BarraProgresso(
                        fracaoDaBarra(f.ganhoPorHoraCentavos, maximo),
                        if (destaque) esquema.primary else esquema.outline,
                    )
                    Text(textoFaixa(f), style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
                }
            }
        }
        textoSemHorario(semHorario).takeIf { it.isNotEmpty() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
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
