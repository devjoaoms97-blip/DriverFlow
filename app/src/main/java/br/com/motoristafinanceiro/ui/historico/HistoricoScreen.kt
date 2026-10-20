package br.com.motoristafinanceiro.ui.historico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
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

@Composable
fun HistoricoScreen(
    onEditarGanho: (Long) -> Unit,
    onEditarDespesa: (Long) -> Unit,
    onEditarAbastecimento: (Long) -> Unit,
    onEditarManutencao: (Long) -> Unit,
    vm: HistoricoViewModel = viewModel(factory = HistoricoViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = vm::mesAnterior) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
            }
            Text(state.mes.rotulo(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = vm::proximoMes, enabled = state.podeAvancar) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês")
            }
        }

        Text(
            "Lucro líquido do mês: ${state.resumo.lucroLiquidoCentavos.formatarReais()}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FiltroHistorico.entries.forEach {
                FilterChip(
                    selected = state.filtro == it,
                    onClick = { vm.filtrar(it) },
                    label = { Text(it.rotulo) },
                )
            }
        }

        // Refinos do tipo escolhido: plataforma para Ganhos, categoria para Despesas.
        when (state.filtro) {
            FiltroHistorico.GANHOS -> LinhaDeChips {
                FilterChip(
                    selected = state.plataforma == null,
                    onClick = { vm.filtrarPlataforma(null) },
                    label = { Text("Todas") },
                )
                Plataforma.entries.forEach {
                    FilterChip(
                        selected = state.plataforma == it,
                        onClick = { vm.filtrarPlataforma(it) },
                        label = { Text(it.rotulo) },
                    )
                }
            }
            FiltroHistorico.DESPESAS -> LinhaDeChips {
                FilterChip(
                    selected = state.categoria == null,
                    onClick = { vm.filtrarCategoria(null) },
                    label = { Text("Todas") },
                )
                CategoriaDespesa.entries.forEach {
                    FilterChip(
                        selected = state.categoria == it,
                        onClick = { vm.filtrarCategoria(it) },
                        label = { Text(it.rotulo) },
                    )
                }
            }
            FiltroHistorico.TODOS -> Unit
        }

        state.resumoFiltro?.let {
            Text(
                textoResumoFiltro(it),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (state.dias.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum lançamento neste período")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                state.dias.forEach { dia ->
                    item(key = "dia-${dia.data}") { CabecalhoDia(dia) }
                    items(dia.itens, key = { it.chave }) { item ->
                        when (item) {
                            is ItemHistorico.DeGanho ->
                                LinhaGanho(item.ganho) { onEditarGanho(item.ganho.id) }
                            is ItemHistorico.DeDespesa ->
                                LinhaDespesa(item.despesa) { destino ->
                                    when (destino) {
                                        is DestinoEdicao.EditarDespesa -> onEditarDespesa(destino.id)
                                        is DestinoEdicao.EditarAbastecimento -> onEditarAbastecimento(destino.id)
                                        is DestinoEdicao.EditarManutencao -> onEditarManutencao(destino.id)
                                        DestinoEdicao.SemEdicao -> Unit
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

/** Chips em uma linha que rola para o lado quando não cabem na tela. */
@Composable
private fun LinhaDeChips(conteudo: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { conteudo() }
}

@Composable
private fun CabecalhoDia(dia: DiaHistorico) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            dia.data.rotuloDia(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            dia.saldoCentavos.formatarReais(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LinhaGanho(ganho: Ganho, onClick: () -> Unit) {
    val detalhes = listOfNotNull(
        ganho.km.takeIf { it > 0 }?.let { "${it.paraTextoNumero()} km" },
        ganho.minutosOnline.takeIf { it > 0 }?.let { "${it.minutosParaHorasTexto()} h" },
        ganho.observacao?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    LinhaBase(
        titulo = ganho.plataforma.rotulo,
        subtitulo = detalhes,
        valor = "+" + ganho.totalCentavos.formatarReais(),
        cor = MaterialTheme.colorScheme.primary,
        onClick = onClick,
    )
}

@Composable
private fun LinhaDespesa(despesa: Despesa, onEditar: (DestinoEdicao) -> Unit) {
    val destino = destinoEdicao(despesa)
    val detalhes = detalhesDespesa(despesa)
    val aoClicar: (() -> Unit)? =
        if (destino is DestinoEdicao.SemEdicao) null else ({ onEditar(destino) })

    LinhaBase(
        titulo = despesa.categoria.rotulo,
        subtitulo = detalhes,
        valor = "−" + despesa.valorCentavos.formatarReais(),
        cor = MaterialTheme.colorScheme.error,
        onClick = aoClicar,
    )
}

@Composable
private fun LinhaBase(
    titulo: String,
    subtitulo: String,
    valor: String,
    cor: Color,
    onClick: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (subtitulo.isNotBlank()) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = cor)
    }
}
