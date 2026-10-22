package br.com.motoristafinanceiro.ui.historico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.Chip
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.Marcador
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.TituloTela
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.minutosParaHorario
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import java.time.format.TextStyle as DiaTextStyle

private val ptBR = Locale("pt", "BR")
private val formatoDia = DateTimeFormatter.ofPattern("EEEE, dd/MM", ptBR)

private fun YearMonth.rotulo(): String =
    month.getDisplayName(DiaTextStyle.FULL, ptBR).replaceFirstChar { it.uppercase() } + " de " + year

private fun LocalDate.rotuloDia(): String {
    val hoje = LocalDate.now()
    return when (this) {
        hoje -> "Hoje"
        hoje.minusDays(1) -> "Ontem"
        else -> format(formatoDia).replaceFirstChar { it.uppercase() }
    }
}

/** "+ R$ 186,40" ou "− R$ 20,00" (saldo do dia). */
internal fun textoSaldo(centavos: Long): String =
    (if (centavos >= 0) "+ " else "− ") + abs(centavos).formatarReais()

@Composable
fun HistoricoScreen(
    onEditarGanho: (Long) -> Unit,
    onEditarDespesa: (Long) -> Unit,
    onEditarAbastecimento: (Long) -> Unit,
    onEditarManutencao: (Long) -> Unit,
    vm: HistoricoViewModel = viewModel(factory = HistoricoViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val esquema = MaterialTheme.colorScheme

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MargemTela, end = MargemTela, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "titulo") { TituloTela("Histórico") }

        item(key = "mes") {
            Cartao(forma = MaterialTheme.shapes.medium, espacamentoInterno = PaddingValues(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::mesAnterior) {
                        Icon(Icones.SetaVoltar, contentDescription = "Mês anterior")
                    }
                    Text(
                        state.mes.rotulo(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = vm::proximoMes, enabled = state.podeAvancar) {
                        Icon(Icones.Seta, contentDescription = "Próximo mês")
                    }
                }
            }
        }

        item(key = "resumo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalMes("Ganhos", state.resumo.ganhoBrutoCentavos.formatarReais(), esquema.secondary, Modifier.weight(1f))
                TotalMes("Despesas", state.resumo.despesasCentavos.formatarReais(), esquema.error, Modifier.weight(1f))
                TotalMes(
                    "Líquido",
                    state.resumo.lucroLiquidoCentavos.formatarReais(),
                    esquema.onPrimaryContainer,
                    Modifier.weight(1f),
                    destaque = true,
                )
            }
        }

        item(key = "filtros") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaDeChips {
                    FiltroHistorico.entries.forEach {
                        Chip(it.rotulo, state.filtro == it, { vm.filtrar(it) })
                    }
                }
                // Refinos do tipo escolhido: plataforma para Ganhos, categoria para Despesas.
                when (state.filtro) {
                    FiltroHistorico.GANHOS -> LinhaDeChips {
                        Chip("Todas", state.plataforma == null, { vm.filtrarPlataforma(null) })
                        Plataforma.entries.forEach {
                            Chip(it.rotulo, state.plataforma == it, { vm.filtrarPlataforma(it) })
                        }
                    }
                    FiltroHistorico.DESPESAS -> LinhaDeChips {
                        Chip("Todas", state.categoria == null, { vm.filtrarCategoria(null) })
                        CategoriaDespesa.entries.forEach {
                            Chip(it.rotulo, state.categoria == it, { vm.filtrarCategoria(it) })
                        }
                    }
                    FiltroHistorico.TODOS -> Unit
                }
                state.resumoFiltro?.let {
                    Text(textoResumoFiltro(it), style = MaterialTheme.typography.bodyMedium, color = esquema.onSurfaceVariant)
                }
            }
        }

        if (state.dias.isEmpty()) {
            item(key = "vazio") {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum lançamento neste período", color = esquema.onSurfaceVariant)
                }
            }
        } else {
            state.dias.forEach { dia ->
                item(key = "dia-${dia.data}") {
                    Dia(
                        dia,
                        onEditarGanho = onEditarGanho,
                        onEditarDespesa = { destino ->
                            when (destino) {
                                is DestinoEdicao.EditarDespesa -> onEditarDespesa(destino.id)
                                is DestinoEdicao.EditarAbastecimento -> onEditarAbastecimento(destino.id)
                                is DestinoEdicao.EditarManutencao -> onEditarManutencao(destino.id)
                                DestinoEdicao.SemEdicao -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Total do mês no topo; [destaque] pinta o cartão do líquido de azul. */
@Composable
private fun TotalMes(rotulo: String, valor: String, cor: Color, modifier: Modifier, destaque: Boolean = false) {
    val esquema = MaterialTheme.colorScheme
    Cartao(
        modifier,
        forma = MaterialTheme.shapes.medium,
        cor = if (destaque) esquema.primaryContainer else esquema.surfaceContainer,
        borda = if (destaque) null else esquema.outlineVariant,
        espacamentoInterno = PaddingValues(12.dp),
        espacamento = 4.dp,
    ) {
        Text(
            rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = if (destaque) esquema.onPrimaryContainer else esquema.onSurfaceVariant,
        )
        Text(
            valor,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = cor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Chips em uma linha que rola para o lado quando não cabem na tela. */
@Composable
private fun LinhaDeChips(conteudo: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { conteudo() }
}

@Composable
private fun Dia(dia: DiaHistorico, onEditarGanho: (Long) -> Unit, onEditarDespesa: (DestinoEdicao) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RotuloSecao(dia.data.rotuloDia(), Modifier.weight(1f))
            Text(textoSaldo(dia.saldoCentavos), style = MaterialTheme.typography.titleSmall)
        }
        Cartao(forma = MaterialTheme.shapes.medium, espacamentoInterno = PaddingValues(0.dp), espacamento = 0.dp) {
            dia.itens.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                when (item) {
                    is ItemHistorico.DeGanho -> LinhaGanho(item.ganho) { onEditarGanho(item.ganho.id) }
                    is ItemHistorico.DeDespesa -> LinhaDespesa(item.despesa, onEditarDespesa)
                }
            }
        }
    }
}

@Composable
private fun LinhaGanho(ganho: Ganho, onClick: () -> Unit) {
    val detalhes = listOfNotNull(
        ganho.km.takeIf { it > 0 }?.let { "${it.paraTextoNumero()} km" },
        ganho.minutosOnline.takeIf { it > 0 }?.let { "${it.minutosParaHorasTexto()} h" },
        ganho.inicioTurnoMinutos?.let { "desde ${it.minutosParaHorario()}" },
        ganho.observacao?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    LinhaBase(
        sigla = siglaPlataforma(ganho.plataforma),
        titulo = ganho.plataforma.rotulo,
        subtitulo = detalhes,
        valor = "+ " + ganho.totalCentavos.formatarReais(),
        cor = MaterialTheme.colorScheme.secondary, // verde: dinheiro que entrou
        fundoSigla = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
    )
}

@Composable
private fun LinhaDespesa(despesa: Despesa, onEditar: (DestinoEdicao) -> Unit) {
    val destino = destinoEdicao(despesa)
    val aoClicar: (() -> Unit)? =
        if (destino is DestinoEdicao.SemEdicao) null else ({ onEditar(destino) })

    LinhaBase(
        sigla = siglaCategoria(despesa.categoria),
        titulo = despesa.categoria.rotulo,
        subtitulo = detalhesDespesa(despesa),
        valor = "− " + despesa.valorCentavos.formatarReais(),
        cor = MaterialTheme.colorScheme.error,
        fundoSigla = MaterialTheme.colorScheme.errorContainer,
        onClick = aoClicar,
    )
}

@Composable
private fun LinhaBase(
    sigla: String,
    titulo: String,
    subtitulo: String,
    valor: String,
    cor: Color,
    fundoSigla: Color,
    onClick: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Marcador(fundoSigla, cor, sigla = sigla)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            if (subtitulo.isNotBlank()) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(valor, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cor)
    }
}
