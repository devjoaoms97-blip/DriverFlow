package br.com.motoristafinanceiro.ui.veiculo

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.domain.abastecimentoValido
import br.com.motoristafinanceiro.domain.precoPorLitroCentavos
import br.com.motoristafinanceiro.ui.lancamento.CampoData
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoNumero
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate

@Composable
fun AbastecimentoScreen(id: Long, onConcluir: () -> Unit) {
    val vm: AbastecimentoViewModel = viewModel(
        key = "abastecimento-$id",
        factory = AbastecimentoViewModel.factory(id),
    )
    val original = vm.original

    when {
        vm.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        id != 0L && original == null -> Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Abastecimento não encontrado")
            Button(onClick = onConcluir, modifier = Modifier.padding(top = 12.dp)) { Text("Voltar") }
        }
        else -> FormAbastecimento(vm, original, onConcluir)
    }
}

@Composable
private fun FormAbastecimento(vm: AbastecimentoViewModel, original: Abastecimento?, onConcluir: () -> Unit) {
    var data by remember { mutableStateOf(original?.data ?: LocalDate.now()) }
    var litros by remember { mutableStateOf(original?.litros?.paraTextoNumero() ?: "") }
    var valor by remember { mutableStateOf(original?.valorTotalCentavos?.paraTextoReais() ?: "") }
    var odometro by remember { mutableStateOf(original?.odometroKm?.toString() ?: "") }
    var cheio by remember { mutableStateOf(original?.tanqueCheio ?: true) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val litrosNum = litros.replace(',', '.').toDoubleOrNull()
    val valorCentavos = valor.reaisParaCentavos()
    val odometroKm = odometro.toIntOrNull()
    val valido = abastecimentoValido(litrosNum, valorCentavos, odometroKm)
    val precoLitro =
        if (litrosNum != null && valorCentavos != null) precoPorLitroCentavos(valorCentavos, litrosNum) else null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (original == null) "Novo abastecimento" else "Editar abastecimento",
            style = MaterialTheme.typography.titleLarge,
        )
        CampoData(data, { data = it })

        OutlinedTextField(litros, { litros = it }, label = { Text("Litros") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor total (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (precoLitro != null && precoLitro > 0) {
            Text(
                "≈ ${precoLitro.formatarReais()} por litro",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(odometro, { odometro = it }, label = { Text("Odômetro (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tanque cheio")
                Text(
                    "O consumo (km/l) só é calculado entre abastecimentos com tanque cheio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = cheio, onCheckedChange = { cheio = it })
        }

        Button(
            enabled = valido,
            onClick = {
                val base = original ?: Abastecimento(data = data, litros = 0.0, valorTotalCentavos = 0, odometroKm = 0)
                vm.salvar(
                    base.copy(
                        data = data,
                        litros = litrosNum ?: 0.0,
                        valorTotalCentavos = valorCentavos ?: 0,
                        odometroKm = odometroKm ?: 0,
                        tanqueCheio = cheio,
                    ),
                    onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (original == null) "Salvar abastecimento" else "Salvar alterações") }

        if (original != null) {
            OutlinedButton(
                onClick = { confirmarExclusao = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Excluir abastecimento") }
        }
    }

    if (confirmarExclusao && original != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir abastecimento?") },
            text = { Text("A despesa de combustível gerada por ele também será excluída.") },
            confirmButton = { TextButton(onClick = { vm.excluir(original, onConcluir) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}
