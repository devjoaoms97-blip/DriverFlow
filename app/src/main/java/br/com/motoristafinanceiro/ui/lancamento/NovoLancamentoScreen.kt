package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate

@Composable
fun NovoLancamentoScreen(
    onConcluir: () -> Unit,
    vm: NovoLancamentoViewModel = viewModel(factory = NovoLancamentoViewModel.Factory),
) {
    var aba by remember { mutableIntStateOf(0) }

    Column {
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Ganho") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Despesa") })
        }
        // TODO: seletor de data (hoje por padrão) e edição de lançamentos existentes.
        when (aba) {
            0 -> FormGanho(vm, onConcluir)
            else -> FormDespesa(vm, onConcluir)
        }
    }
}

private val teclaNumerica = KeyboardOptions(keyboardType = KeyboardType.Decimal)

@Composable
private fun FormGanho(vm: NovoLancamentoViewModel, onConcluir: () -> Unit) {
    var plataforma by remember { mutableStateOf(Plataforma.UBER) }
    var valor by remember { mutableStateOf("") }
    var gorjeta by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var horas by remember { mutableStateOf("") }

    val valorCentavos = valor.reaisParaCentavos()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Plataforma.entries.forEach {
                FilterChip(selected = plataforma == it, onClick = { plataforma = it }, label = { Text(it.rotulo) })
            }
        }
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor bruto (R$)") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(gorjeta, { gorjeta = it }, label = { Text("Gorjeta/bônus (R$)") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(km, { km = it }, label = { Text("Km rodados") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(horas, { horas = it }, label = { Text("Horas online (ex.: 6,5)") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())

        Button(
            enabled = valorCentavos != null && valorCentavos > 0,
            onClick = {
                vm.salvarGanho(
                    data = LocalDate.now(),
                    plataforma = plataforma,
                    valorCentavos = valorCentavos ?: 0,
                    gorjetaBonusCentavos = gorjeta.reaisParaCentavos() ?: 0,
                    km = km.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    minutosOnline = ((horas.replace(',', '.').toDoubleOrNull() ?: 0.0) * 60).toInt(),
                    aoConcluir = onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar ganho") }
    }
}

@Composable
private fun FormDespesa(vm: NovoLancamentoViewModel, onConcluir: () -> Unit) {
    var categoria by remember { mutableStateOf(CategoriaDespesa.COMBUSTIVEL) }
    var valor by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    val valorCentavos = valor.reaisParaCentavos()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoriaDespesa.entries.forEach {
                FilterChip(selected = categoria == it, onClick = { categoria = it }, label = { Text(it.rotulo) })
            }
        }
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor (R$)") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(obs, { obs = it }, label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth())

        // TODO: anexar foto do comprovante.
        Button(
            enabled = valorCentavos != null && valorCentavos > 0,
            onClick = {
                vm.salvarDespesa(
                    data = LocalDate.now(),
                    categoria = categoria,
                    valorCentavos = valorCentavos ?: 0,
                    observacao = obs.ifBlank { null },
                    aoConcluir = onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Salvar despesa") }
    }
}
