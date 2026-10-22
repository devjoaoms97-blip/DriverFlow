package br.com.motoristafinanceiro.ui.veiculo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import br.com.motoristafinanceiro.ui.componentes.BotaoExcluir
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.CampoValor
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar
import br.com.motoristafinanceiro.ui.componentes.Escolhas
import br.com.motoristafinanceiro.ui.componentes.NaoEncontrado
import br.com.motoristafinanceiro.ui.componentes.TelaFormulario
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
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
        id != 0L && original == null -> NaoEncontrado("Manutenção não encontrada", onConcluir)
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

    TelaFormulario(
        titulo = if (original == null) "Nova manutenção" else "Editar manutenção",
        novo = original == null,
        onVoltar = onConcluir,
        textoSalvar = if (original == null) "Salvar manutenção" else "Salvar alterações",
        podeSalvar = valido,
        onSalvar = {
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
    ) {
        CampoValor("Valor do serviço", valor, { valor = it }, MaterialTheme.colorScheme.primary)
        Escolhas(
            "Serviço", TipoManutencao.entries, tipo, { it.rotulo },
            {
                tipo = it
                if (!intervaloEditado) intervalo = intervaloSugeridoKm(it)?.toString().orEmpty()
            },
        )
        CampoData(data, { data = it })
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoCompacto(
                    "Odômetro no serviço", odometro, { odometro = it }, Modifier.weight(1f),
                    dica = "ex.: 48720", teclado = KeyboardType.Number,
                )
                CampoCompacto(
                    "Próxima a cada (km)", intervalo, { intervalo = it; intervaloEditado = true }, Modifier.weight(1f),
                    dica = "opcional", teclado = KeyboardType.Number,
                )
            }
            TextoDeApoio("Deixe o intervalo vazio para não criar alerta.")
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoCompacto("Observação (opcional)", obs, { obs = it }, Modifier.fillMaxWidth(), umaLinha = false)
            if (tipo == TipoManutencao.OUTRO) TextoDeApoio("Para \"Outro\", descreva o serviço aqui (ex.: Bateria).")
        }
        if (original != null) BotaoExcluir("Excluir manutenção") { confirmarExclusao = true }
    }

    if (confirmarExclusao && original != null) {
        DialogoConfirmar(
            "Excluir manutenção?",
            "A despesa gerada por ela também será excluída.",
            "Excluir",
            onConfirmar = { vm.excluir(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}
