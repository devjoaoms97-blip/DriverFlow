package br.com.motoristafinanceiro.ui.bloqueio

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.motoristafinanceiro.ui.componentes.BotaoPrincipal
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela

/** Tempo fora do app a partir do qual ele pede a digital de novo (girar a tela ou tirar foto não bloqueia). */
const val TEMPO_FORA_PARA_BLOQUEAR_MS = 2 * 60 * 1000L

/** Se o app deve se bloquear ao voltar: só com o bloqueio ligado e depois de [TEMPO_FORA_PARA_BLOQUEAR_MS] fora. */
fun deveBloquearAoVoltar(ativo: Boolean, saiuEmMs: Long?, agoraMs: Long): Boolean =
    ativo && saiuEmMs != null && agoraMs - saiuEmMs >= TEMPO_FORA_PARA_BLOQUEAR_MS

/** Digital, rosto ou o PIN/padrão/senha do próprio celular. */
private const val AUTENTICADORES = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** Se o celular tem como confirmar quem é (precisa de um bloqueio de tela configurado). */
fun podeAutenticar(contexto: Context): Boolean =
    BiometricManager.from(contexto).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

/** Abre o pedido de digital/PIN do Android. [aoFalhar] recebe a mensagem (inclui "cancelado pelo usuário"). */
fun pedirAutenticacao(
    activity: FragmentActivity,
    titulo: String,
    aoConseguir: () -> Unit,
    aoFalhar: (String) -> Unit = {},
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = aoConseguir()
            override fun onAuthenticationError(codigo: Int, mensagem: CharSequence) = aoFalhar(mensagem.toString())
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(titulo)
        .setSubtitle("Use a digital ou o bloqueio de tela do celular")
        .setAllowedAuthenticators(AUTENTICADORES)
        .build()
    prompt.authenticate(info)
}

/**
 * Tela que cobre o app enquanto está bloqueado. Pede a digital assim que [pedirAgora] fica verdadeiro
 * (falso enquanto a abertura animada toca: o pedido do Android é uma janela por cima de tudo e a esconderia).
 */
@Composable
fun TelaBloqueio(pedirAgora: Boolean = true, onDesbloquear: () -> Unit) {
    LaunchedEffect(pedirAgora) { if (pedirAgora) onDesbloquear() }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(MargemTela),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icones.Cadeado, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text("DriverFlow bloqueado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Seus dados estão protegidos. Use a digital ou o bloqueio de tela do celular para entrar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BotaoPrincipal("Desbloquear", onClick = onDesbloquear)
    }
}
