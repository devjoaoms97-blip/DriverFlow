package br.com.motoristafinanceiro.ui.veiculo

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
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.intervaloSugeridoKm
import br.com.motoristafinanceiro.domain.manutencaoValida
import br.com.motoristafinanceiro.domain.parseIntervaloKm
import br.com.motoristafinanceiro.ui.lancamento.CampoData
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate

@Composable
fun ManutencaoScreen(id: Long, onConcluir: () -> Unit) {
    val vm: ManutencaoViewModel = viewModel(
        key = "manutencao-$id",
        factory = ManutencaoViewModel.factory(id),
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
            Text("Manutenção não encontrada")
            Button(onClick = onConcluir, modifier = Modifier.padding(top = 12.dp)) { Text("Voltar") }
        }
        else -> FormManutencao(vm, original, onConcluir)
    }
}

@Composable
private fun FormManutencao(vm: ManutencaoViewModel, original: Manutencao?, onConcluir: () -> Unit) {
    var tipo by remember { mutableStateOf(original?.tipo ?: TipoManutencao.OLEO) }
    var data by remember { mutableStateOf(original?.data ?: LocalDate.now()) }
    var odometro by remember { mutableStateOf(original?.odometroKm?.toString() ?: "") }
    var valor by remember { mutableStateOf(original?.valorCentavos?.paraTextoReais() ?: "") }
    var intervalo by remember {
        mutableStateOf(original?.intervaloKm?.toString() ?: intervaloSugeridoKm(TipoManutencao.OLEO)?.toString().orEmpty())
    }
    // Enquanto o usuário não mexer no intervalo, ele acompanha a sugestão do tipo escolhido.
    var intervaloEditado by remember { mutableStateOf(original != null) }
    var obs by remember { mutableStateOf(original?.observacao ?: "") }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val odometroKm = odometro.toIntOrNull()
    val valorCentavos = valor.reaisParaCentavos()
    val valido = manutencaoValida(odometroKm, valorCentavos, intervalo)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (original == null) "Nova manutenção" else "Editar manutenção",
            style = MaterialTheme.typography.titleLarge,
        )
        CampoData(data, { data = it })

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoManutencao.entries.forEach {
                FilterChip(
                    selected = tipo == it,
                    onClick = {
                        tipo = it
                        if (!intervaloEditado) intervalo = intervaloSugeridoKm(it)?.toString().orEmpty()
                    },
                    label = { Text(it.rotulo) },
                )
            }
        }

        OutlinedTextField(odometro, { odometro = it }, label = { Text("Odômetro na hora do serviço (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(intervalo, { intervalo = it; intervaloEditado = true },
            label = { Text("Intervalo até a próxima (km), opcional") },
            supportingText = { Text("Deixe vazio para não criar alerta.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(obs, { obs = it }, label = { Text("Observação (opcional)") },
            supportingText = {
                if (tipo == TipoManutencao.OUTRO) Text("Para \"Outro\", descreva o serviço aqui (ex.: Bateria).")
            },
            modifier = Modifier.fillMaxWidth())

        Button(
            enabled = valido,
            onClick = {
                val base = original ?: Manutencao(tipo = tipo, data = data, odometroKm = 0, valorCentavos = 0)
                vm.salvar(
                    base.copy(
                        tipo = tipo,
                        data = data,
                        odometroKm = odometroKm ?: 0,
                        valorCentavos = valorCentavos ?: 0,
                        intervaloKm = parseIntervaloKm(intervalo),
                        observacao = obs.ifBlank { null },
                    ),
                    onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (original == null) "Salvar manutenção" else "Salvar alterações") }

        if (original != null) {
            OutlinedButton(
                onClick = { confirmarExclusao = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Excluir manutenção") }
        }
    }

    if (confirmarExclusao && original != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir manutenção?") },
            text = { Text("A despesa gerada por ela também será excluída.") },
            confirmButton = { TextButton(onClick = { vm.excluir(original, onConcluir) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}
