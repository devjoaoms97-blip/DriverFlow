package br.com.motoristafinanceiro.ui.reservas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.ui.componentes.CabecalhoVoltar
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.ValorRotulado
import br.com.motoristafinanceiro.util.formatarReais
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Despesas fixas. Abre pelo atalho do Início; o + do cabeçalho cadastra uma nova (id 0). */
@Composable
fun ReservasScreen(
    onEditar: (Long) -> Unit,
    onVoltar: () -> Unit,
    vm: ReservasViewModel = viewModel(factory = ReservasViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val esquema = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize()) {
        CabecalhoVoltar("Reservas", onVoltar) {
            IconButton(onClick = { onEditar(0L) }) {
                Icon(Icones.Mais, contentDescription = "Nova reserva")
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MargemTela, end = MargemTela, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "total") {
                Cartao(espacamento = 14.dp) {
                    Text("Separar por dia", style = MaterialTheme.typography.titleSmall, color = esquema.onSurfaceVariant)
                    Text(state.totalDiarioCentavos.formatarReais(), style = MaterialTheme.typography.displaySmall)
                    HorizontalDivider(color = esquema.outlineVariant)
                    ValorRotulado("Por semana", state.totalSemanalCentavos.formatarReais())
                    Text(
                        "Dividido por todos os dias do período, trabalhando ou não.",
                        style = MaterialTheme.typography.bodySmall,
                        color = esquema.onSurfaceVariant,
                    )
                }
            }

            item(key = "rotulo") { RotuloSecao("Despesas fixas", Modifier.padding(horizontal = 4.dp)) }

            item(key = "lista") {
                if (state.itens.isEmpty()) {
                    Text(
                        "Toque em + para cadastrar IPVA, seguro, parcela do carro e outras despesas fixas.",
                        color = esquema.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                } else {
                    Cartao(forma = MaterialTheme.shapes.medium, espacamentoInterno = PaddingValues(0.dp), espacamento = 0.dp) {
                        state.itens.forEachIndexed { i, item ->
                            if (i > 0) HorizontalDivider(color = esquema.outlineVariant.copy(alpha = 0.6f))
                            LinhaReserva(item) { onEditar(item.reserva.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaReserva(item: ItemReserva, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.reserva.nome, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(
                item.reserva.periodicidade.rotulo + " · " + item.reserva.valorCentavos.formatarReais(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = esquema.onSurfaceVariant,
            )
            Text(
                textoVencimento(item.diasParaVencer) + " (" + item.proximaOcorrencia.format(formatoData) + ")",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (item.diasParaVencer <= 7) esquema.tertiary else esquema.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.diariaCentavos.formatarReais(), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text("por dia", style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant, textAlign = TextAlign.End)
        }
    }
}
