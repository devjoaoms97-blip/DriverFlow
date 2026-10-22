package br.com.motoristafinanceiro.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.domain.parseDepreciacaoPorKm
import br.com.motoristafinanceiro.ui.componentes.BotaoPrincipal
import br.com.motoristafinanceiro.ui.componentes.CabecalhoVoltar
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoReais

@Composable
fun AjustesScreen(onVoltar: () -> Unit) {
    val vm: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)
    // Sobe depois de restaurar um backup, para o campo mostrar a depreciação restaurada.
    var versao by remember { mutableStateOf(0) }
    val inicial by produceState<Long?>(initialValue = null, versao) { value = vm.depreciacaoAtual() }
    val valorInicial = inicial

    if (valorInicial == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        key(valorInicial) { FormAjustes(vm, valorInicial, onVoltar, aoRestaurar = { versao++ }) }
    }
}

@Composable
private fun FormAjustes(vm: AjustesViewModel, valorInicial: Long, onVoltar: () -> Unit, aoRestaurar: () -> Unit) {
    var texto by remember { mutableStateOf(if (valorInicial > 0) valorInicial.paraTextoReais() else "") }
    val centavos = parseDepreciacaoPorKm(texto)

    Column(Modifier.fillMaxSize()) {
        CabecalhoVoltar("Ajustes", onVoltar)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = MargemTela, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SecaoAjustes("Depreciação") {
                CampoCompacto(
                    "Depreciação por km (R$)", texto, { texto = it }, Modifier.fillMaxWidth(),
                    dica = "ex.: 0,25", teclado = KeyboardType.Decimal, erro = centavos == null,
                )
                TextoDeApoio(
                    when {
                        centavos == null -> "Valor inválido. Use algo como 0,25."
                        centavos > 0 -> "≈ ${(centavos * 100).formatarReais()} a cada 100 km rodados"
                        else -> "Deixe vazio ou 0 para ignorar."
                    },
                    erro = centavos == null,
                )
                Text(
                    "É quanto o carro perde de valor e desgasta a cada km rodado. Esse valor entra no custo por km " +
                        "e no \"lucro após depreciação\" do Início, sem alterar o lucro líquido (que só considera o " +
                        "dinheiro que saiu do bolso).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BotaoPrincipal("Salvar depreciação", habilitado = centavos != null) {
                    vm.salvarDepreciacao(centavos ?: 0L, onVoltar)
                }
            }
            SecaoAparencia(vm)
            SecaoSeguranca(vm)
            SecaoNotificacoes(vm)
            SecaoBackup(aoRestaurar)
            SecaoBackupAutomatico()
        }
    }
}

/** Uma seção de Ajustes: rótulo em caixa alta e o conteúdo num cartão. */
@Composable
internal fun SecaoAjustes(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RotuloSecao(titulo, Modifier.padding(horizontal = 4.dp))
        Cartao(espacamento = 12.dp, conteudo = conteudo)
    }
}
