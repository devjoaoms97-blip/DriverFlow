package br.com.motoristafinanceiro.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.backup.FrequenciaBackup
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Backup automático: frequência, pasta escolhida uma vez, último backup e erro da última tentativa. */
@Composable
fun SecaoBackupAutomatico() {
    val vm: BackupAutomaticoViewModel = viewModel(factory = BackupAutomaticoViewModel.Factory)
    val frequencia by vm.frequencia.collectAsStateWithLifecycle(initialValue = FrequenciaBackup.DESLIGADO)
    val pasta by vm.pasta.collectAsStateWithLifecycle(initialValue = null)
    val ultimo by vm.ultimoBackupMs.collectAsStateWithLifecycle(initialValue = null)
    val erro by vm.erro.collectAsStateWithLifecycle(initialValue = null)
    // Frequência escolhida antes de haver pasta: vale quando a pasta for escolhida.
    var frequenciaPendente by remember { mutableStateOf<FrequenciaBackup?>(null) }

    val escolherPasta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.definirPasta(uri, frequenciaPendente)
        frequenciaPendente = null
    }
    val nomePasta by produceState<String?>(initialValue = null, pasta) {
        value = pasta?.let { uri -> withContext(Dispatchers.IO) { vm.nomeDaPasta(uri) } }
    }
    val opcoes = FrequenciaBackup.entries

    SecaoAjustes("Backup automático") {
        SeletorSegmentado(
            opcoes.map { it.rotulo },
            opcoes.indexOf(frequencia),
            { i ->
                val nova = opcoes[i]
                if (nova != FrequenciaBackup.DESLIGADO && pasta == null) {
                    frequenciaPendente = nova
                    escolherPasta.launch(null)
                } else {
                    vm.definirFrequencia(nova)
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Pasta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(textoPasta(pasta, nomePasta), style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = { escolherPasta.launch(null) }) { Text(if (pasta == null) "Escolher" else "Trocar") }
        }
        Text(textoUltimoBackup(ultimo), style = MaterialTheme.typography.bodyMedium)
        erro?.let { TextoDeApoio(it, erro = true) }
        if (pasta != null) {
            TextButton(onClick = vm::fazerAgora) { Text("Fazer backup agora nesta pasta") }
        }
        TextoDeApoio(
            "Guarda um arquivo por dia e mantém os 7 mais recentes; os backups que você exporta à mão nunca são " +
                "apagados. Para não perder nada se o celular sumir, prefira uma pasta que vá para a nuvem.",
        )
    }
}
