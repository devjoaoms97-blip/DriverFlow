package br.com.motoristafinanceiro.ui.lancamento

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.ProcessadorDeImagem
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.ui.componentes.CabecalhoVoltar
import br.com.motoristafinanceiro.ui.componentes.BarraSalvar
import br.com.motoristafinanceiro.ui.componentes.BotaoExcluir
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.CampoValor
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.CorpoFormulario
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar
import br.com.motoristafinanceiro.ui.componentes.Escolhas
import br.com.motoristafinanceiro.ui.componentes.NaoEncontrado
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.comprovante.ComprovanteDialog
import br.com.motoristafinanceiro.ui.comprovante.MiniaturaComprovante
import br.com.motoristafinanceiro.util.paraTextoNumero
import br.com.motoristafinanceiro.util.paraTextoReais
import br.com.motoristafinanceiro.util.reaisParaCentavos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
        tipo == TipoLancamento.GANHO && ganhoOriginal == null -> NaoEncontrado("Lançamento não encontrado", onConcluir)
        tipo == TipoLancamento.DESPESA && despesaOriginal == null -> NaoEncontrado("Lançamento não encontrado", onConcluir)
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
private fun FormGanho(vm: NovoLancamentoViewModel, original: Ganho?, onConcluir: () -> Unit, modifier: Modifier) {
    var data by remember { mutableStateOf(original?.data ?: LocalDate.now()) }
    var plataforma by remember { mutableStateOf(original?.plataforma ?: Plataforma.UBER) }
    var valor by remember { mutableStateOf(original?.valorCentavos?.paraTextoReais() ?: "") }
    var gorjeta by remember {
        mutableStateOf(original?.gorjetaBonusCentavos?.takeIf { it > 0 }?.paraTextoReais() ?: "")
    }
    var km by remember { mutableStateOf(original?.km?.takeIf { it > 0 }?.paraTextoNumero() ?: "") }
    var obs by remember { mutableStateOf(original?.observacao ?: "") }
    var inicioTurno by remember { mutableStateOf(original?.inicioTurnoMinutos) }
    var fimTurno by remember { mutableStateOf(original?.let { fimDoTurno(it.inicioTurnoMinutos, it.minutosOnline) }) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    val valorCentavos = valor.reaisParaCentavos()
    val turno = estadoTurno(inicioTurno, fimTurno, original?.minutosOnline ?: 0)
    val esquema = MaterialTheme.colorScheme

    Column(modifier) {
        CorpoFormulario(Modifier.weight(1f)) {
            if (original == null) {
                vm.ultimoGanho?.let { ultimo ->
                    val repeticao = repeticaoDe(ultimo)
                    BotaoRepetirUltimo(textoRepeticao(repeticao)) {
                        plataforma = repeticao.plataforma
                        inicioTurno = repeticao.inicio
                        fimTurno = repeticao.fim
                    }
                }
            }
            CampoValor("Valor bruto", valor, { valor = it }, esquema.secondary)
            Escolhas(
                "Plataforma", Plataforma.entries, plataforma, { it.rotulo }, { plataforma = it },
                esquema.secondary, esquema.secondaryContainer,
            )
            CampoData(data, { data = it })
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoHora("Início do turno", inicioTurno, { inicioTurno = it }, Modifier.weight(1f))
                    CampoHora("Fim do turno", fimTurno, { fimTurno = it }, Modifier.weight(1f))
                }
                if (turno.texto.isNotEmpty()) TextoDeApoio(turno.texto, erro = turno.erro)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoCompacto("Km rodados", km, { km = it }, Modifier.weight(1f), dica = "0", teclado = KeyboardType.Decimal)
                CampoCompacto("Gorjeta/bônus", gorjeta, { gorjeta = it }, Modifier.weight(1f), dica = "R$ 0,00", teclado = KeyboardType.Decimal)
            }
            CampoCompacto("Observação (opcional)", obs, { obs = it }, Modifier.fillMaxWidth(), umaLinha = false)
            if (original != null) BotaoExcluir("Excluir lançamento") { confirmarExclusao = true }
        }
        BarraSalvar(
            texto = if (original == null) "Salvar ganho" else "Salvar alterações",
            habilitado = valorCentavos != null && valorCentavos > 0 && !turno.erro,
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
                    minutosOnline = turno.minutosOnline,
                    observacao = obs.ifBlank { null },
                    inicioTurnoMinutos = inicioTurno,
                ),
                onConcluir,
            )
        }
    }

    if (confirmarExclusao && original != null) {
        DialogoConfirmar(
            "Excluir lançamento?", "Essa ação não pode ser desfeita.", "Excluir",
            onConfirmar = { vm.excluirGanho(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}

/** Atalho no topo do ganho novo: copia plataforma e horários do último lançamento. */
@Composable
private fun BotaoRepetirUltimo(detalhe: String, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = esquema.secondaryContainer,
        contentColor = esquema.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icones.Historico, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text("Repetir o último", style = MaterialTheme.typography.titleSmall)
                Text(detalhe, style = MaterialTheme.typography.bodySmall)
            }
        }
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
            if (original != null) BotaoExcluir("Excluir lançamento") { confirmarExclusao = true }
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
        DialogoConfirmar(
            "Excluir lançamento?", "Essa ação não pode ser desfeita.", "Excluir",
            onConfirmar = { vm.excluirDespesa(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}
