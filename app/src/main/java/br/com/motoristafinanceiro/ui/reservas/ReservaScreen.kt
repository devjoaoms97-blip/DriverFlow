package br.com.motoristafinanceiro.ui.reservas

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.domain.MODELOS_DE_RESERVA
import br.com.motoristafinanceiro.domain.reservaDiariaCentavos
import br.com.motoristafinanceiro.domain.reservaValida
import br.com.motoristafinanceiro.ui.lancamento.CampoData
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate

@Composable
fun ReservaScreen(id: Long, onConcluir: () -> Unit) {
    val vm: ReservaFormViewModel = viewModel(
        key = "reserva-$id",
        factory = ReservaFormViewModel.factory(id),
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
            Text("Reserva não encontrada")
            Button(onClick = onConcluir, modifier = Modifier.padding(top = 12.dp)) { Text("Voltar") }
        }
        else -> FormReserva(vm, original, onConcluir)
    }
}

@Composable
private fun FormReserva(vm: ReservaFormViewModel, original: ReservaDespesa?, onConcluir: () -> Unit) {
    var nome by remember { mutableStateOf(original?.nome ?: "") }
    var valor by remember { mutableStateOf(original?.valorCentavos?.paraTextoReais() ?: "") }
    var periodicidade by remember { mutableStateOf(original?.periodicidade ?: Periodicidade.ANUAL) }
    var vencimento by remember { mutableStateOf(original?.proximoVencimento ?: LocalDate.now()) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val valorCentavos = valor.reaisParaCentavos()
    val valido = reservaValida(nome, valorCentavos)
    val diaria = if (valido) {
        reservaDiariaCentavos(
            ReservaDespesa(
                nome = nome, valorCentavos = valorCentavos ?: 0,
                periodicidade = periodicidade, proximoVencimento = vencimento,
            )
        )
    } else {
        null
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (original == null) "Nova reserva" else "Editar reserva",
            style = MaterialTheme.typography.titleLarge,
        )

        if (original == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODELOS_DE_RESERVA.forEach { modelo ->
                    AssistChip(
                        onClick = {
                            nome = modelo.nome
                            periodicidade = modelo.periodicidade
                        },
                        label = { Text(modelo.nome) },
                    )
                }
            }
        }

        OutlinedTextField(nome, { nome = it }, label = { Text("Nome (ex.: IPVA, Seguro)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor de cada vencimento (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("Periodicidade", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Periodicidade.entries.forEach {
                FilterChip(selected = periodicidade == it, onClick = { periodicidade = it }, label = { Text(it.rotulo) })
            }
        }

        Text("Próximo vencimento", style = MaterialTheme.typography.labelLarge)
        CampoData(vencimento, { vencimento = it }, mostrarAtalhos = false)
        Text(
            "Se a data já passou, o app avança sozinho para o próximo vencimento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (diaria != null) {
            Text("≈ ${diaria.formatarReais()} por dia", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            enabled = valido,
            onClick = {
                val base = original ?: ReservaDespesa(
                    nome = "", valorCentavos = 0, periodicidade = periodicidade, proximoVencimento = vencimento,
                )
                vm.salvar(
                    base.copy(
                        nome = nome.trim(),
                        valorCentavos = valorCentavos ?: 0,
                        periodicidade = periodicidade,
                        proximoVencimento = vencimento,
                    ),
                    onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (original == null) "Salvar reserva" else "Salvar alterações") }

        if (original != null) {
            OutlinedButton(
                onClick = { confirmarExclusao = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Excluir reserva") }
        }
    }

    if (confirmarExclusao && original != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir reserva?") },
            text = { Text("Isso não apaga nenhuma despesa já lançada.") },
            confirmButton = { TextButton(onClick = { vm.excluir(original, onConcluir) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}
