package br.com.motoristafinanceiro.ui.meta

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.META_MAXIMA_CENTAVOS
import br.com.motoristafinanceiro.domain.metaParaRepetirCentavos
import br.com.motoristafinanceiro.domain.metaPorDiaDoMesCentavos
import br.com.motoristafinanceiro.domain.parseMetaMensal
import br.com.motoristafinanceiro.ui.componentes.BotaoExcluir
import br.com.motoristafinanceiro.ui.componentes.CampoValor
import br.com.motoristafinanceiro.ui.componentes.Chip
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar
import br.com.motoristafinanceiro.ui.componentes.Indicador
import br.com.motoristafinanceiro.ui.componentes.TelaFormulario
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoReais
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Marcador para distinguir "ainda carregando" de "carregou e não há meta". */
private data class MetaCarregada(val centavos: Long?, val anterior: Long?)

@Composable
fun MetaScreen(onVoltar: () -> Unit) {
    val vm: MetaViewModel = viewModel(factory = MetaViewModel.Factory)
    val carregada by produceState<MetaCarregada?>(initialValue = null) { value = MetaCarregada(vm.metaAtual(), vm.metaAnterior()) }
    val atual = carregada

    if (atual == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        FormMeta(vm, atual.centavos, metaParaRepetirCentavos(atual.centavos, atual.anterior), onVoltar)
    }
}

@Composable
private fun FormMeta(vm: MetaViewModel, metaInicial: Long?, paraRepetir: Long?, onVoltar: () -> Unit) {
    var texto by remember { mutableStateOf(metaInicial?.paraTextoReais() ?: "") }
    var confirmarRemocao by remember { mutableStateOf(false) }
    val centavos = parseMetaMensal(texto)
    val nomeDoMes = vm.mes.nomeParaTitulo()
    val invalido = texto.isNotBlank() && centavos == null

    TelaFormulario(
        titulo = "Meta de $nomeDoMes",
        novo = false,
        onVoltar = onVoltar,
        textoSalvar = "Salvar meta",
        podeSalvar = centavos != null,
        onSalvar = { centavos?.let { vm.salvar(it, onVoltar) } },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CampoValor(
                "Meta de lucro líquido",
                texto,
                { texto = it },
                if (invalido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            TextoDeApoio(
                if (invalido) {
                    "Valor inválido. Use algo como 5000 ou 5000,00 (máximo ${META_MAXIMA_CENTAVOS.formatarReais()})."
                } else {
                    "Quanto você quer ter de lucro no mês, já descontadas as despesas."
                },
                erro = invalido,
            )
        }

        if (paraRepetir != null) {
            // Atalho: preenche o campo com a meta do mês anterior (ainda é preciso salvar).
            Chip(
                textoRepetirMeta(vm.mes.minusMonths(1), paraRepetir),
                selecionado = centavos == paraRepetir,
                onClick = { texto = paraRepetir.paraTextoReais() },
            )
        }

        if (centavos != null) {
            Indicador(
                "Por dia, se o mês todo (${vm.mes.lengthOfMonth()} dias) rendesse igual",
                metaPorDiaDoMesCentavos(centavos, vm.mes).formatarReais(),
            )
        }

        Text(
            "O progresso usa o lucro líquido do mês (ganhos menos despesas). No Início você vê quanto " +
                "falta para bater a meta e quanto precisa lucrar por dia até o fim do mês, contando hoje.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (metaInicial != null) BotaoExcluir("Remover meta") { confirmarRemocao = true }
    }

    if (confirmarRemocao) {
        DialogoConfirmar(
            "Remover a meta de $nomeDoMes?",
            "Os lançamentos não são afetados. Você pode definir a meta de novo quando quiser.",
            "Remover",
            onConfirmar = { vm.remover(onVoltar) },
            onCancelar = { confirmarRemocao = false },
        )
    }
}

private fun YearMonth.nomeParaTitulo(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))} de $year"
