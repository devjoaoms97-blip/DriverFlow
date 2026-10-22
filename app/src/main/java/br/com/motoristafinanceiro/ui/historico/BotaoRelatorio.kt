package br.com.motoristafinanceiro.ui.historico

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.com.motoristafinanceiro.data.relatorio.ExportadorRelatorio
import br.com.motoristafinanceiro.data.relatorio.nomeArquivoRelatorio
import br.com.motoristafinanceiro.data.relatorio.tituloDoMes
import br.com.motoristafinanceiro.domain.RelatorioMes
import br.com.motoristafinanceiro.ui.componentes.BotaoIcone
import br.com.motoristafinanceiro.ui.componentes.Icones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

/** Botão do relatório em PDF do mês mostrado no Histórico: salvar num arquivo ou compartilhar. */
@Composable
fun BotaoRelatorio(mes: YearMonth, carregar: suspend () -> RelatorioMes) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val exportador = remember(contexto) { ExportadorRelatorio(contexto) }
    var aberto by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    fun avisar(texto: String) = Toast.makeText(contexto, texto, Toast.LENGTH_LONG).show()

    val salvarComo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destino ->
        if (destino == null) return@rememberLauncherForActivityResult
        trabalhando = true
        escopo.launch {
            val certo = runCatching { withContext(Dispatchers.IO) { exportador.salvar(carregar(), destino) } }.isSuccess
            trabalhando = false
            avisar(if (certo) "Relatório salvo." else "Não foi possível salvar o relatório.")
        }
    }

    BotaoIcone(Icones.Documento, "Relatório do mês em PDF") { aberto = true }

    if (aberto) {
        AlertDialog(
            onDismissRequest = { aberto = false },
            title = { Text("Relatório de ${tituloDoMes(mes)}") },
            text = {
                Text(
                    "PDF com o resumo do mês (ganhos, despesas e lucro), os totais por plataforma e por categoria e " +
                        "a lista de lançamentos. Serve para comprovar renda.",
                )
            },
            confirmButton = {
                TextButton(enabled = !trabalhando, onClick = {
                    aberto = false
                    trabalhando = true
                    escopo.launch {
                        val pedido = runCatching { withContext(Dispatchers.IO) { exportador.intentDeCompartilhar(carregar()) } }
                        trabalhando = false
                        pedido.onSuccess { contexto.startActivity(it) }
                            .onFailure { avisar("Não foi possível gerar o relatório.") }
                    }
                }) { Text("Compartilhar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { aberto = false }) { Text("Cancelar") }
                    TextButton(enabled = !trabalhando, onClick = {
                        aberto = false
                        salvarComo.launch(nomeArquivoRelatorio(mes))
                    }) { Text("Salvar") }
                }
            },
        )
    }
}
