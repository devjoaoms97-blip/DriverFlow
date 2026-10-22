package br.com.motoristafinanceiro.ui.ajustes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.data.ModoTema
import br.com.motoristafinanceiro.ui.componentes.SeletorSegmentado

/** Escolha da aparência do app: Automático (segue o celular), Claro ou Escuro. */
@Composable
fun SecaoAparencia(vm: AjustesViewModel) {
    val modo by vm.modoTema.collectAsStateWithLifecycle(initialValue = ModoTema.PADRAO)
    val opcoes = ModoTema.values().toList()

    SecaoAjustes("Aparência") {
        SeletorSegmentado(opcoes.map { it.rotulo }, opcoes.indexOf(modo), { vm.definirModoTema(opcoes[it]) })
        Text(
            "Automático segue o modo claro ou escuro do celular.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
