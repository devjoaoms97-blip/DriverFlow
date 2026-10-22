package br.com.motoristafinanceiro.ui.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.ui.bloqueio.pedirAutenticacao
import br.com.motoristafinanceiro.ui.bloqueio.podeAutenticar
import br.com.motoristafinanceiro.ui.componentes.LinhaChave
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio

/** Chave "Bloquear o app": ao ligar, pede a digital uma vez para confirmar que funciona neste celular. */
@Composable
fun SecaoSeguranca(vm: AjustesViewModel) {
    val contexto = LocalContext.current
    val ativo by vm.bloqueioAtivo.collectAsStateWithLifecycle(initialValue = false)
    var aviso by remember { mutableStateOf<String?>(null) }

    SecaoAjustes("Segurança") {
        LinhaChave(
            "Bloquear o app",
            "Pede a digital ou o bloqueio de tela do celular ao abrir e ao voltar depois de 2 minutos fora.",
            ativo,
        ) { quer ->
            aviso = null
            when {
                !quer -> vm.definirBloqueio(false)
                !podeAutenticar(contexto) ->
                    aviso = "Configure um bloqueio de tela (PIN, padrão, senha ou digital) no celular para usar esta opção."
                else -> pedirAutenticacao(
                    contexto as FragmentActivity,
                    "Confirmar o bloqueio do app",
                    aoConseguir = { vm.definirBloqueio(true) },
                    aoFalhar = { aviso = "O bloqueio não foi ligado: $it" },
                )
            }
        }
        aviso?.let { TextoDeApoio(it, erro = true) }
    }
}
