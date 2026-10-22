package br.com.motoristafinanceiro.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.ui.componentes.BotaoPrincipal
import br.com.motoristafinanceiro.ui.componentes.DialogoConfirmar

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

    SecaoAjustes("Backup") {
        Text(
            "Guarda lançamentos, veículo, reservas, metas, depreciação e as fotos dos comprovantes em um arquivo " +
                ".zip. Escolha onde salvar (Google Drive, computador...). Sem backup, desinstalar o app ou perder " +
                "o celular apaga tudo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BotaoPrincipal("Exportar backup", habilitado = !trabalhando) { criarArquivo.launch(vm.nomeSugerido) }

        OutlinedButton(
            onClick = { confirmandoRestauracao = true },
            enabled = !trabalhando,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Restaurar backup", style = MaterialTheme.typography.titleSmall) }

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
        DialogoConfirmar(
            "Restaurar backup?",
            "Isso substitui TODOS os dados atuais (lançamentos, veículo, reservas, metas, depreciação e " +
                "fotos) pelos do arquivo. Se quiser guardar o que existe hoje, cancele e exporte um backup antes.",
            "Escolher arquivo",
            onConfirmar = {
                confirmandoRestauracao = false
                abrirArquivo.launch(arrayOf("*/*"))
            },
            onCancelar = { confirmandoRestauracao = false },
        )
    }
}
