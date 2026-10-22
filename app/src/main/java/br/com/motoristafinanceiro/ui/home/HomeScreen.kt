package br.com.motoristafinanceiro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.ProgressoMeta
import br.com.motoristafinanceiro.ui.componentes.BarraDividida
import br.com.motoristafinanceiro.ui.componentes.BarraProgresso
import br.com.motoristafinanceiro.ui.componentes.BotaoIcone
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.GradeDuasColunas
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.Indicador
import br.com.motoristafinanceiro.ui.componentes.LinhaAviso
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.componentes.TituloTela
import br.com.motoristafinanceiro.ui.componentes.ValorRotulado
import br.com.motoristafinanceiro.ui.meta.textoDiasRestantes
import br.com.motoristafinanceiro.ui.meta.textoFaltaPorDia
import br.com.motoristafinanceiro.ui.meta.textoRepetirMeta
import br.com.motoristafinanceiro.util.formatarReais
import java.time.LocalDate

@Composable
fun HomeScreen(
    onAbrirAjustes: () -> Unit,
    onAbrirMeta: () -> Unit,
    onAbrirVeiculo: () -> Unit,
    onAbrirReservas: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var periodo by rememberSaveable { mutableIntStateOf(0) }
    val estatisticas = when (periodo) {
        0 -> state.hoje
        1 -> state.semana
        else -> state.mes
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = MargemTela, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TituloTela("Início", sobretitulo = rotuloDataInicio(LocalDate.now())) {
            BotaoIcone(Icones.Ajustes, "Ajustes", onAbrirAjustes)
        }

        SeletorSegmentado(periodosInicio, periodo, { periodo = it })

        CartaoLucro(tituloLucroPeriodo(periodo, state.rotuloSemana), estatisticas)

        GradeDuasColunas(
            listOf(
                { m -> Indicador("Ganho por km", estatisticas.ganhoPorKmCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Ganho por hora", estatisticas.ganhoPorHoraCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Custo por km", estatisticas.custoPorKmCentavos?.formatarReais() ?: "—", m) },
                { m -> Indicador("Rodado", textoRodado(estatisticas), m) },
            )
        )

        textoAtencaoManutencao(state.alertasUrgentes)?.let { aviso ->
            val esquema = MaterialTheme.colorScheme
            LinhaAviso(
                titulo = aviso.titulo,
                detalhe = aviso.detalhe,
                marcadorFundo = esquema.tertiary,
                marcadorCor = esquema.onTertiary,
                icone = Icones.Alerta,
                onClick = onAbrirVeiculo,
                fundo = esquema.tertiaryContainer,
                borda = null,
                corTexto = esquema.onTertiaryContainer,
                corDetalhe = esquema.onTertiaryContainer,
            )
        }

        CartaoMeta(
            state.progressoMeta,
            onAbrirMeta,
            repetir = state.metaParaRepetirCentavos?.let { valor ->
                textoRepetirMeta(vm.mesAtual.minusMonths(1), valor) to { vm.repetirMetaAnterior(valor) }
            },
        )

        val reservas = textoAvisoReservas(state.reservas)
        LinhaAviso(
            titulo = reservas.titulo,
            detalhe = reservas.detalhe,
            marcadorFundo = MaterialTheme.colorScheme.primaryContainer,
            marcadorCor = MaterialTheme.colorScheme.onPrimaryContainer,
            icone = Icones.Calendario,
            onClick = onAbrirReservas,
        )
    }
}

@Composable
private fun CartaoLucro(titulo: String, e: Estatisticas) {
    val esquema = MaterialTheme.colorScheme
    Cartao(espacamento = 14.dp) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = esquema.onSurfaceVariant)
        Text(e.lucroLiquidoCentavos.formatarReais(), style = MaterialTheme.typography.displaySmall)
        BarraDividida(
            listOf(
                e.lucroLiquidoCentavos.coerceAtLeast(0).toFloat() to esquema.secondary,
                e.despesasCentavos.toFloat() to esquema.error,
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ValorRotulado("Ganho bruto", "+ " + e.ganhoBrutoCentavos.formatarReais(), Modifier.weight(1f), esquema.secondary)
            ValorRotulado("Despesas", "− " + e.despesasCentavos.formatarReais(), Modifier.weight(1f), esquema.error)
        }
        if (e.depreciacaoCentavos > 0) {
            HorizontalDivider(color = esquema.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ValorRotulado("Depreciação estimada", e.depreciacaoCentavos.formatarReais(), Modifier.weight(1f))
                ValorRotulado("Após depreciação", e.lucroAposDepreciacaoCentavos.formatarReais(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
/** [repetir]: texto e ação do botão que copia a meta do mês anterior (só quando o mês ainda não tem meta). */
private fun CartaoMeta(progresso: ProgressoMeta?, onAbrirMeta: () -> Unit, repetir: Pair<String, () -> Unit>?) {
    val esquema = MaterialTheme.colorScheme
    Cartao {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Meta do mês", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (progresso != null) {
                Text("${progresso.percentual}%", style = MaterialTheme.typography.titleSmall, color = esquema.primary)
            }
        }
        if (progresso == null) {
            Text(
                "Defina quanto você quer lucrar neste mês e veja quanto falta por dia.",
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurfaceVariant,
            )
            if (repetir != null) {
                Button(onClick = repetir.second, modifier = Modifier.fillMaxWidth()) { Text(repetir.first) }
                TextButton(onClick = onAbrirMeta, modifier = Modifier.fillMaxWidth()) { Text("Definir outro valor") }
            } else {
                Button(onClick = onAbrirMeta, modifier = Modifier.fillMaxWidth()) { Text("Definir meta") }
            }
        } else {
            BarraProgresso(progresso.fracao, esquema.primary, altura = 10.dp)
            Text(
                "${progresso.lucroCentavos.formatarReais()} de ${progresso.metaCentavos.formatarReais()}",
                style = MaterialTheme.typography.bodyMedium,
                color = esquema.onSurfaceVariant,
            )
            HorizontalDivider(color = esquema.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(textoFaltaPorDia(progresso), style = MaterialTheme.typography.titleMedium)
                    textoDiasRestantes(progresso).takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onAbrirMeta) { Text("Editar") }
            }
        }
    }
}
