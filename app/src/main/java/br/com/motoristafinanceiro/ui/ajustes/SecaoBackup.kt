package br.com.motoristafinanceiro.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Exportar e restaurar o backup completo (arquivo .zip). [aoRestaurar] avisa a tela para recarregar os ajustes. */
@Composable
fun SecaoBackup(aoRestaurar: () -> Unit) {
    val vm: BackupViewModel = viewModel(factory = BackupViewModel.Factory)
    val estado = vm.estado
    val trabalhando = estado is EstadoBackup.Trabalhando
    var confirmandoRestauracao by remember { mutableStateOf(false) }

    val criarArquivo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        vm.exportar(uri)
    }
    val abrirArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        vm.restaurar(uri, aoRestaurar)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Backup", style = MaterialTheme.typography.titleMedium)
        Text(
            "Guarda lançamentos, veículo, reservas, metas, depreciação e as fotos dos comprovantes em um arquivo " +
                ".zip. Escolha onde salvar (Google Drive, computador...). Sem backup, desinstalar o app ou perder " +
                "o celular apaga tudo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { criarArquivo.launch(vm.nomeSugerido) },
            enabled = !trabalhando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Exportar backup") }

        OutlinedButton(
            onClick = { confirmandoRestauracao = true },
            enabled = !trabalhando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Restaurar backup") }

        when (estado) {
            is EstadoBackup.Trabalhando -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(estado.texto)
            }
            is EstadoBackup.Resultado -> Text(
                estado.texto,
                color = if (estado.erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            EstadoBackup.Ocioso -> Unit
        }
    }

    if (confirmandoRestauracao) {
        AlertDialog(
            onDismissRequest = { confirmandoRestauracao = false },
            title = { Text("Restaurar backup?") },
            text = {
                Text(
                    "Isso substitui TODOS os dados atuais (lançamentos, veículo, reservas, metas, depreciação e " +
                        "fotos) pelos do arquivo. Se quiser guardar o que existe hoje, cancele e exporte um backup antes.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoRestauracao = false
                    abrirArquivo.launch(arrayOf("*/*"))
                }) { Text("Escolher arquivo") }
            },
            dismissButton = { TextButton(onClick = { confirmandoRestauracao = false }) { Text("Cancelar") } },
        )
    }
}
