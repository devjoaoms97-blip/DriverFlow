package br.com.motoristafinanceiro.ui.lancamento

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import br.com.motoristafinanceiro.data.ProcessadorDeImagem
import br.com.motoristafinanceiro.ui.comprovante.ComprovanteDialog
import br.com.motoristafinanceiro.ui.comprovante.MiniaturaComprovante
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.paraTextoNumero
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Tela única para novo lançamento (tipo = null, com abas) e para editar/excluir
 * um ganho ou uma despesa existente (tipo + id).
 */
@Composable
fun NovoLancamentoScreen(tipo: TipoLancamento?, id: Long, onConcluir: () -> Unit) {
    val vm: NovoLancamentoViewModel = viewModel(
        key = "lancamento-$tipo-$id",
        factory = NovoLancamentoViewModel.factory(tipo, id),
    )
    val ganhoOriginal = vm.ganhoOriginal
    val despesaOriginal = vm.despesaOriginal

    when {
        vm.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        tipo == TipoLancamento.GANHO && ganhoOriginal == null -> NaoEncontrado(onConcluir)
        tipo == TipoLancamento.DESPESA && despesaOriginal == null -> NaoEncontrado(onConcluir)
        tipo == TipoLancamento.GANHO -> Column {
            Cabecalho("Editar ganho")
            FormGanho(vm, ganhoOriginal, onConcluir)
        }
        tipo == TipoLancamento.DESPESA -> Column {
            Cabecalho("Editar despesa")
            FormDespesa(vm, despesaOriginal, onConcluir)
        }
        else -> NovoComAbas(vm, onConcluir)
    }
}

@Composable
private fun NovoComAbas(vm: NovoLancamentoViewModel, onConcluir: () -> Unit) {
    var aba by remember { mutableIntStateOf(0) }
    Column {
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Ganho") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Despesa") })
        }
        when (aba) {
            0 -> FormGanho(vm, null, onConcluir)
            else -> FormDespesa(vm, null, onConcluir)
        }
    }
}

@Composable
private fun Cabecalho(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
    )
}

@Composable
private fun NaoEncontrado(onVoltar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Lançamento não encontrado")
        Button(onClick = onVoltar, modifier = Modifier.padding(top = 12.dp)) { Text("Voltar") }
    }
}

private val teclaNumerica = KeyboardOptions(keyboardType = KeyboardType.Decimal)

@Composable
private fun FormGanho(vm: NovoLancamentoViewModel, original: Ganho?, onConcluir: () -> Unit) {
    var data by remember { mutableStateOf(original?.data ?: LocalDate.now()) }
    var plataforma by remember { mutableStateOf(original?.plataforma ?: Plataforma.UBER) }
    var valor by remember { mutableStateOf(original?.valorCentavos?.paraTextoReais() ?: "") }
    var gorjeta by remember {
        mutableStateOf(original?.gorjetaBonusCentavos?.takeIf { it > 0 }?.paraTextoReais() ?: "")
    }
    var km by remember { mutableStateOf(original?.km?.takeIf { it > 0 }?.paraTextoNumero() ?: "") }
    var horas by remember {
        mutableStateOf(original?.minutosOnline?.takeIf { it > 0 }?.minutosParaHorasTexto() ?: "")
    }
    var obs by remember { mutableStateOf(original?.observacao ?: "") }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val valorCentavos = valor.reaisParaCentavos()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CampoData(data, { data = it })
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
        OutlinedTextField(obs, { obs = it }, label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth())

        Button(
            enabled = valorCentavos != null && valorCentavos > 0,
            onClick = {
                val base = original ?: Ganho(data = data, plataforma = plataforma, valorCentavos = 0)
                vm.salvarGanho(
                    base.copy(
                        data = data,
                        plataforma = plataforma,
                        valorCentavos = valorCentavos ?: 0,
                        gorjetaBonusCentavos = (gorjeta.reaisParaCentavos() ?: 0).coerceAtLeast(0),
                        km = (km.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        minutosOnline = ((horas.replace(',', '.').toDoubleOrNull() ?: 0.0) * 60)
                            .roundToInt().coerceAtLeast(0),
                        observacao = obs.ifBlank { null },
                    ),
                    onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (original == null) "Salvar ganho" else "Salvar alterações") }

        if (original != null) BotaoExcluir { confirmarExclusao = true }
    }

    if (confirmarExclusao && original != null) {
        DialogoExcluir(
            onConfirmar = { vm.excluirGanho(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}

@Composable
private fun FormDespesa(vm: NovoLancamentoViewModel, original: Despesa?, onConcluir: () -> Unit) {
    // rememberSaveable: os campos sobrevivem a girar a tela e a abrir o app da câmera.
    var data by rememberSaveable { mutableStateOf(original?.data ?: LocalDate.now()) }
    var categoria by rememberSaveable { mutableStateOf(original?.categoria ?: CategoriaDespesa.COMBUSTIVEL) }
    var valor by rememberSaveable { mutableStateOf(original?.valorCentavos?.paraTextoReais() ?: "") }
    var obs by rememberSaveable { mutableStateOf(original?.observacao ?: "") }
    var fotoNome by rememberSaveable { mutableStateOf(original?.fotoUri) }
    var fotoPendente by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var verFoto by remember { mutableStateOf(false) }
    var processandoFoto by remember { mutableStateOf(false) }
    var erroFoto by remember { mutableStateOf<String?>(null) }

    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tirou ->
        val nome = fotoPendente
        fotoPendente = null
        if (nome != null) {
            if (tirou) {
                processandoFoto = true
                escopo.launch {
                    val certo = withContext(Dispatchers.IO) { ProcessadorDeImagem.otimizar(vm.comprovantes.arquivo(nome)) }
                    processandoFoto = false
                    if (certo) fotoNome = nome else erroFoto = "Não foi possível usar essa foto."
                }
            } else {
                escopo.launch(Dispatchers.IO) { vm.comprovantes.excluir(nome) }
            }
        }
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processandoFoto = true
            escopo.launch {
                val nome = withContext(Dispatchers.IO) { vm.comprovantes.importar(contexto.contentResolver, uri) }
                processandoFoto = false
                if (nome != null) fotoNome = nome else erroFoto = "Não foi possível usar essa foto."
            }
        }
    }

    val valorCentavos = valor.reaisParaCentavos()
    val nomeDaFoto = fotoNome

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CampoData(data, { data = it })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoriaDespesa.entries.forEach {
                FilterChip(selected = categoria == it, onClick = { categoria = it }, label = { Text(it.rotulo) })
            }
        }
        OutlinedTextField(valor, { valor = it }, label = { Text("Valor (R$)") },
            keyboardOptions = teclaNumerica, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(obs, { obs = it }, label = { Text("Observação (opcional)") },
            modifier = Modifier.fillMaxWidth())

        Text("Comprovante", style = MaterialTheme.typography.labelLarge)
        if (nomeDaFoto != null) {
            MiniaturaComprovante(
                arquivo = vm.comprovantes.arquivo(nomeDaFoto),
                ladoMax = 600,
                modifier = Modifier.fillMaxWidth().height(180.dp).clickable { verFoto = true },
            )
            Text(
                "Toque na foto para ampliar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (processandoFoto) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp))
                Text("Preparando a foto...")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = !processandoFoto,
                onClick = {
                    erroFoto = null
                    val arquivo = vm.comprovantes.novoArquivo()
                    fotoPendente = arquivo.name
                    try {
                        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
                        camera.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        fotoPendente = null
                        arquivo.delete()
                        erroFoto = "Nenhum app de câmera encontrado."
                    }
                },
            ) { Text("Tirar foto") }
            OutlinedButton(
                enabled = !processandoFoto,
                onClick = {
                    erroFoto = null
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) { Text("Galeria") }
            if (nomeDaFoto != null) {
                TextButton(onClick = { fotoNome = null }) { Text("Remover") }
            }
        }
        erroFoto?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(
            enabled = valorCentavos != null && valorCentavos > 0 && !processandoFoto,
            onClick = {
                val base = original ?: Despesa(data = data, categoria = categoria, valorCentavos = 0)
                vm.salvarDespesa(
                    base.copy(
                        data = data,
                        categoria = categoria,
                        valorCentavos = valorCentavos ?: 0,
                        observacao = obs.ifBlank { null },
                        fotoUri = fotoNome,
                    ),
                    onConcluir,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (original == null) "Salvar despesa" else "Salvar alterações") }

        if (original != null) BotaoExcluir { confirmarExclusao = true }
    }

    if (verFoto && nomeDaFoto != null) {
        ComprovanteDialog(vm.comprovantes.arquivo(nomeDaFoto)) { verFoto = false }
    }

    if (confirmarExclusao && original != null) {
        DialogoExcluir(
            onConfirmar = { vm.excluirDespesa(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}

@Composable
private fun BotaoExcluir(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) { Text("Excluir lançamento") }
}

@Composable
private fun DialogoExcluir(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Excluir lançamento?") },
        text = { Text("Essa ação não pode ser desfeita.") },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
