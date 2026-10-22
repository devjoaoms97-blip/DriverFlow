package br.com.motoristafinanceiro.ui.veiculo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.domain.abastecimentoValido
import br.com.motoristafinanceiro.domain.precoPorLitroCentavos
import br.com.motoristafinanceiro.ui.componentes.BotaoExcluir
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.CampoValor
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar
import br.com.motoristafinanceiro.ui.componentes.LinhaChave
import br.com.motoristafinanceiro.ui.componentes.NaoEncontrado
import br.com.motoristafinanceiro.ui.componentes.TelaFormulario
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
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
        id != 0L && original == null -> NaoEncontrado("Abastecimento não encontrado", onConcluir)
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

    TelaFormulario(
        titulo = if (original == null) "Novo abastecimento" else "Editar abastecimento",
        novo = original == null,
        onVoltar = onConcluir,
        textoSalvar = if (original == null) "Salvar abastecimento" else "Salvar alterações",
        podeSalvar = valido,
        onSalvar = {
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
    ) {
        CampoValor("Valor total", valor, { valor = it }, MaterialTheme.colorScheme.primary)
        if (precoLitro != null && precoLitro > 0) {
            TextoDeApoio("≈ ${precoLitro.formatarReais()} por litro", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        CampoData(data, { data = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoCompacto("Litros", litros, { litros = it }, Modifier.weight(1f), dica = "ex.: 38,2", teclado = KeyboardType.Decimal)
            CampoCompacto("Odômetro (km)", odometro, { odometro = it }, Modifier.weight(1f), dica = "ex.: 48720", teclado = KeyboardType.Number)
        }
        Cartao(forma = MaterialTheme.shapes.small, espacamentoInterno = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            LinhaChave(
                "Tanque cheio",
                "O consumo (km/l) só é calculado entre abastecimentos com tanque cheio.",
                cheio,
            ) { cheio = it }
        }
        if (original != null) BotaoExcluir("Excluir abastecimento") { confirmarExclusao = true }
    }

    if (confirmarExclusao && original != null) {
        DialogoConfirmar(
            "Excluir abastecimento?",
            "A despesa de combustível gerada por ele também será excluída.",
            "Excluir",
            onConfirmar = { vm.excluir(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}
