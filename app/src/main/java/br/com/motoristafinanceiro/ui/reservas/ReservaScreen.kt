package br.com.motoristafinanceiro.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.domain.MODELOS_DE_RESERVA
import br.com.motoristafinanceiro.domain.reservaDiariaCentavos
import br.com.motoristafinanceiro.domain.reservaValida
import br.com.motoristafinanceiro.ui.componentes.BotaoExcluir
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.CampoValor
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar
import br.com.motoristafinanceiro.ui.componentes.Escolhas
import br.com.motoristafinanceiro.ui.componentes.NaoEncontrado
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.TelaFormulario
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
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
        id != 0L && original == null -> NaoEncontrado("Reserva não encontrada", onConcluir)
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

    TelaFormulario(
        titulo = if (original == null) "Nova reserva" else "Editar reserva",
        novo = original == null,
        onVoltar = onConcluir,
        textoSalvar = if (original == null) "Salvar reserva" else "Salvar alterações",
        podeSalvar = valido,
        onSalvar = {
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
    ) {
        CampoValor("Valor de cada vencimento", valor, { valor = it }, MaterialTheme.colorScheme.primary)
        if (diaria != null) {
            TextoDeApoio("≈ ${diaria.formatarReais()} por dia", modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (original == null) {
            // Atalhos: tocar preenche o nome e a periodicidade comuns daquela despesa.
            Escolhas(
                "Atalhos", MODELOS_DE_RESERVA, MODELOS_DE_RESERVA.firstOrNull { it.nome == nome }, { it.nome },
                {
                    nome = it.nome
                    periodicidade = it.periodicidade
                },
            )
        }
        CampoCompacto("Nome (ex.: IPVA, Seguro)", nome, { nome = it }, Modifier.fillMaxWidth())

        Escolhas("Periodicidade", Periodicidade.entries, periodicidade, { it.rotulo }, { periodicidade = it })

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RotuloSecao("Próximo vencimento")
            CampoData(vencimento, { vencimento = it }, mostrarAtalhos = false)
            TextoDeApoio("Se a data já passou, o app avança sozinho para o próximo vencimento.")
        }

        if (original != null) BotaoExcluir("Excluir reserva") { confirmarExclusao = true }
    }

    if (confirmarExclusao && original != null) {
        DialogoConfirmar(
            "Excluir reserva?",
            "Isso não apaga nenhuma despesa já lançada.",
            "Excluir",
            onConfirmar = { vm.excluir(original, onConcluir) },
            onCancelar = { confirmarExclusao = false },
        )
    }
}
