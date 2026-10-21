package br.com.motoristafinanceiro.ui.ajustes

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.notificacoes.NotificadorAndroid

/** Chave "Avisar sobre manutenção" em Ajustes, com o pedido de permissão do Android 13+. */
@Composable
fun SecaoNotificacoes(vm: AjustesViewModel) {
    val contexto = LocalContext.current
    val ativas by vm.notificacoesAtivas.collectAsStateWithLifecycle(initialValue = false)
    var sistemaPermite by remember { mutableStateOf(NotificadorAndroid.permitido(contexto)) }
    var negouPermissao by remember { mutableStateOf(false) }

    // Ao voltar das configurações do Android, confere de novo.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        sistemaPermite = NotificadorAndroid.permitido(contexto)
        if (sistemaPermite) negouPermissao = false
    }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        sistemaPermite = NotificadorAndroid.permitido(contexto)
        negouPermissao = !concedida
        // Se negou, a chave continua desligada e o aviso abaixo explica como liberar.
        vm.definirNotificacoes(concedida)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Avisar sobre manutenção", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Notificação quando uma manutenção ficar próxima (500 km) ou atrasada. " +
                        "O app só sabe o km pelos abastecimentos e manutenções que você registra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ativas,
                onCheckedChange = { quer ->
                    when {
                        !quer -> vm.definirNotificacoes(false)
                        precisaPedirPermissao(Build.VERSION.SDK_INT, sistemaPermite) ->
                            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> vm.definirNotificacoes(true)
                    }
                },
            )
        }

        if (estadoNotificacao(ativas, sistemaPermite) == EstadoNotificacao.SEM_PERMISSAO || negouPermissao) {
            Text(
                "As notificações do app estão bloqueadas no Android. Libere para receber os avisos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = {
                contexto.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                )
            }) { Text("Abrir configurações de notificação") }
        }
    }
}
