package br.com.motoristafinanceiro.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.data.ModoTema

/** Escolha da aparência do app: Automático (segue o celular), Claro ou Escuro. */
@Composable
fun SecaoAparencia(vm: AjustesViewModel) {
    val modo by vm.modoTema.collectAsStateWithLifecycle(initialValue = ModoTema.PADRAO)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aparência", style = MaterialTheme.typography.titleMedium)
        Text(
            "Automático segue o modo claro ou escuro do celular.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModoTema.values().forEach { opcao ->
                FilterChip(
                    selected = modo == opcao,
                    onClick = { vm.definirModoTema(opcao) },
                    label = { Text(opcao.rotulo) },
                )
            }
        }
    }
}
