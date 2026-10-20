package br.com.motoristafinanceiro.ui.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.META_MAXIMA_CENTAVOS
import br.com.motoristafinanceiro.domain.metaPorDiaDoMesCentavos
import br.com.motoristafinanceiro.domain.parseMetaMensal
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoReais
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Marcador para distinguir "ainda carregando" de "carregou e não há meta". */
private data class MetaCarregada(val centavos: Long?)

@Composable
fun MetaScreen(onVoltar: () -> Unit) {
    val vm: MetaViewModel = viewModel(factory = MetaViewModel.Factory)
    val carregada by produceState<MetaCarregada?>(initialValue = null) { value = MetaCarregada(vm.metaAtual()) }
    val atual = carregada

    if (atual == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        FormMeta(vm, atual.centavos, onVoltar)
    }
}

@Composable
private fun FormMeta(vm: MetaViewModel, metaInicial: Long?, onVoltar: () -> Unit) {
    var texto by remember { mutableStateOf(metaInicial?.paraTextoReais() ?: "") }
    var confirmarRemocao by remember { mutableStateOf(false) }
    val centavos = parseMetaMensal(texto)
    val nomeDoMes = vm.mes.nomeParaTitulo()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Meta de $nomeDoMes", style = MaterialTheme.typography.titleLarge)
        }

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Meta de lucro líquido (R$)") },
            supportingText = {
                Text(
                    if (texto.isNotBlank() && centavos == null) {
                        "Valor inválido. Use algo como 5000 ou 5000,00 (máximo ${META_MAXIMA_CENTAVOS.formatarReais()})."
                    } else {
                        "Quanto você quer ter de lucro no mês, já descontadas as despesas."
                    },
                )
            },
            isError = texto.isNotBlank() && centavos == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (centavos != null) {
            Text(
                "≈ ${metaPorDiaDoMesCentavos(centavos, vm.mes).formatarReais()} por dia, " +
                    "se o mês todo (${vm.mes.lengthOfMonth()} dias) rendesse igual.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "O progresso usa o lucro líquido do mês (ganhos menos despesas). No Início você vê quanto " +
                "falta para bater a meta e quanto precisa lucrar por dia até o fim do mês, contando hoje.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            enabled = centavos != null,
            onClick = { centavos?.let { vm.salvar(it, onVoltar) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar") }

        if (metaInicial != null) {
            OutlinedButton(onClick = { confirmarRemocao = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Remover meta")
            }
        }
    }

    if (confirmarRemocao) {
        AlertDialog(
            onDismissRequest = { confirmarRemocao = false },
            title = { Text("Remover a meta de $nomeDoMes?") },
            text = { Text("Os lançamentos não são afetados. Você pode definir a meta de novo quando quiser.") },
            confirmButton = { TextButton(onClick = { vm.remover(onVoltar) }) { Text("Remover") } },
            dismissButton = { TextButton(onClick = { confirmarRemocao = false }) { Text("Cancelar") } },
        )
    }
}

private fun YearMonth.nomeParaTitulo(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))} de $year"
