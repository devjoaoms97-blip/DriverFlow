package br.com.motoristafinanceiro.ui.ajustes

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.parseDepreciacaoPorKm
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoReais

@Composable
fun AjustesScreen(onVoltar: () -> Unit) {
    val vm: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)
    // Sobe depois de restaurar um backup, para o campo mostrar a depreciação restaurada.
    var versao by remember { mutableStateOf(0) }
    val inicial by produceState<Long?>(initialValue = null, versao) { value = vm.depreciacaoAtual() }
    val valorInicial = inicial

    if (valorInicial == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        key(valorInicial) { FormAjustes(vm, valorInicial, onVoltar, aoRestaurar = { versao++ }) }
    }
}

@Composable
private fun FormAjustes(vm: AjustesViewModel, valorInicial: Long, onVoltar: () -> Unit, aoRestaurar: () -> Unit) {
    var texto by remember { mutableStateOf(if (valorInicial > 0) valorInicial.paraTextoReais() else "") }
    val centavos = parseDepreciacaoPorKm(texto)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        }

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Depreciação por km (R$)") },
            supportingText = {
                Text(
                    if (centavos == null) "Valor inválido. Use algo como 0,25."
                    else "Ex.: 0,25. Deixe vazio ou 0 para ignorar."
                )
            },
            isError = centavos == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (centavos != null && centavos > 0) {
            Text(
                "≈ ${(centavos * 100).formatarReais()} a cada 100 km rodados",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "É quanto o carro perde de valor e desgasta a cada km rodado. Esse valor entra no custo por km " +
                "e no \"lucro após depreciação\" do Início, sem alterar o lucro líquido (que só considera o " +
                "dinheiro que saiu do bolso).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            enabled = centavos != null,
            onClick = { vm.salvarDepreciacao(centavos ?: 0L, onVoltar) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SecaoAparencia(vm)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SecaoNotificacoes(vm)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SecaoBackup(aoRestaurar)
    }
}
