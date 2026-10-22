package br.com.motoristafinanceiro.ui.lancamento

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.ProcessadorDeImagem
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.ui.componentes.CabecalhoVoltar
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.Chip
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.comprovante.ComprovanteDialog
import br.com.motoristafinanceiro.ui.comprovante.MiniaturaComprovante
import br.com.motoristafinanceiro.util.minutosParaHorasTexto
import br.com.motoristafinanceiro.util.paraTextoNumero
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Tela única para novo lançamento (tipo = null, com seletor Ganho/Despesa) e para editar/excluir
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
        tipo == TipoLancamento.GANHO -> Column(Modifier.fillMaxSize()) {
            CabecalhoVoltar("Editar ganho", onConcluir)
            FormGanho(vm, ganhoOriginal, onConcluir, Modifier.weight(1f))
        }
        tipo == TipoLancamento.DESPESA -> Column(Modifier.fillMaxSize()) {
            CabecalhoVoltar("Editar despesa", onConcluir)
            FormDespesa(vm, despesaOriginal, onConcluir, Modifier.weight(1f))
        }
        else -> NovoComSeletor(vm, onConcluir)
    }
}

@Composable
private fun NovoComSeletor(vm: NovoLancamentoViewModel, onConcluir: () -> Unit) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    val esquema = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        CabecalhoVoltar("Novo lançamento", onConcluir, icone = Icones.Fechar, descricaoVoltar = "Fechar")
        SeletorSegmentado(
            opcoes = listOf("Ganho", "Despesa"),
            selecionado = aba,
            onSelecionar = { aba = it },
            corSelecionada = { if (it == 0) esquema.secondary else esquema.error },
            corTextoSelecionado = { if (it == 0) esquema.onSecondary else esquema.onError },
            icones = listOf(Icones.Ganho, Icones.Despesa),
            altura = 44.dp,
            modifier = Modifier.padding(horizontal = MargemTela, vertical = 8.dp),
        )
        when (aba) {
            0 -> FormGanho(vm, null, onConcluir, Modifier.weight(1f))
            else -> FormDespesa(vm, null, onConcluir, Modifier.weight(1f))
        }
    }
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

/** Área que rola com os campos; o botão de salvar fica fora dela, fixo no rodapé. */
@Composable
private fun CorpoFormulario(modifier: Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = MargemTela, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = conteudo,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Escolhas(
    titulo: String,
    opcoes: List<T>,
    escolhida: T,
    rotulo: (T) -> String,
    onEscolher: (T) -> Unit,
    destaque: Color,
    fundoDestaque: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RotuloSecao(titulo)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            opcoes.forEach {
                Chip(rotulo(it), it == escolhida, { onEscolher(it) }, destaque, fundoDestaque, destaque)
            }
        }
    }
}

@Composable
private fun FormGanho(vm: NovoLancamentoViewModel, original: Ganho?, onConcluir: () -> Unit, modifier: Modifier) {
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
    val esquema = MaterialTheme.colorScheme

    Column(modifier) {
        CorpoFormulario(Modifier.weight(1f)) {
            CampoValor("Valor bruto", valor, { valor = it }, esquema.secondary)
            Escolhas(
                "Plataforma", Plataforma.entries, plataforma, { it.rotulo }, { plataforma = it },
                esquema.secondary, esquema.secondaryContainer,
            )
            CampoData(data, { data = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoCompacto("Km rodados", km, { km = it }, Modifier.weight(1f), dica = "0", numerico = true)
                CampoCompacto("Horas online", horas, { horas = it }, Modifier.weight(1f), dica = "ex.: 6,5", numerico = true)
                CampoCompacto("Gorjeta/bônus", gorjeta, { gorjeta = it }, Modifier.weight(1f), dica = "R$ 0,00", numerico = true)
            }
            CampoCompacto("Observação (opcional)", obs, { obs = it }, Modifier.fillMaxWidth(), umaLinha = false)
            if (original != null) BotaoExcluir { confirmarExclusao = true }
        }
        BarraSalvar(
            texto = if (original == null) "Salvar ganho" else "Salvar alterações",
            habilitado = valorCentavos != null && valorCentavos > 0,
            cor = esquema.secondary,
            corTexto = esquema.onSecondary,
        ) {
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
        }
    }

    if (confirmarExclusao && original != null) {
        DialogoExcluir(
            onConfirmar = { vm.excluirGanho(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}

@Composable
private fun FormDespesa(vm: NovoLancamentoViewModel, original: Despesa?, onConcluir: () -> Unit, modifier: Modifier) {
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
    val esquema = MaterialTheme.colorScheme

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

    Column(modifier) {
        CorpoFormulario(Modifier.weight(1f)) {
            CampoValor("Valor da despesa", valor, { valor = it }, esquema.error)
            Escolhas(
                "Categoria", CategoriaDespesa.entries, categoria, { it.rotulo }, { categoria = it },
                esquema.error, esquema.errorContainer,
            )
            CampoData(data, { data = it })

            Cartao(forma = MaterialTheme.shapes.small, espacamentoInterno = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icones.Clipe, contentDescription = null, tint = esquema.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Text("Comprovante", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (nomeDaFoto != null) {
                        TextButton(onClick = { fotoNome = null }) { Text("Remover") }
                    }
                }
                if (nomeDaFoto != null) {
                    MiniaturaComprovante(
                        arquivo = vm.comprovantes.arquivo(nomeDaFoto),
                        ladoMax = 600,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { verFoto = true },
                    )
                    Text(
                        "Toque na foto para ampliar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = esquema.onSurfaceVariant,
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
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, esquema.outlineVariant),
                        modifier = Modifier.weight(1f),
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
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, esquema.outlineVariant),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            erroFoto = null
                            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    ) { Text("Galeria") }
                }
                erroFoto?.let { Text(it, color = esquema.error, style = MaterialTheme.typography.bodySmall) }
            }

            CampoCompacto("Observação (opcional)", obs, { obs = it }, Modifier.fillMaxWidth(), umaLinha = false)
            if (original != null) BotaoExcluir { confirmarExclusao = true }
        }
        BarraSalvar(
            texto = if (original == null) "Salvar despesa" else "Salvar alterações",
            habilitado = valorCentavos != null && valorCentavos > 0 && !processandoFoto,
            cor = esquema.error,
            corTexto = esquema.onError,
        ) {
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
        }
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
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Excluir lançamento", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
    }
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
