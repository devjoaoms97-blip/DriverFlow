package br.com.motoristafinanceiro.ui.abertura

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import br.com.motoristafinanceiro.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/** Mesmo azul-marinho da abertura do sistema e da animação, para a troca não piscar. */
private val FundoAbertura = Color(0xFF080E20)
private val CorDoNome = Color(0xFFF5FAFF)
private val CorDoLema = Color(0xFFABBFDB)

// Medidas do desenho original (tela de 1080 x 1920): posição do topo de cada texto e tamanho da letra.
private const val LARGURA_BASE = 1080f
private const val ALTURA_BASE = 1920f

/**
 * Abertura animada: o logo e a barrinha vêm do Lottie (res/raw/driverflow_splash.json) e o nome e o lema
 * são texto do Compose (o Lottie não consegue carregar fontes dentro do Compose), com os mesmos tempos.
 * Chama [aoTerminar] quando a animação acaba, quando o usuário toca para pular ou se ela não carregar
 * (nunca prende o usuário aqui).
 */
@Composable
fun AberturaAnimada(aoTerminar: () -> Unit) {
    val fim by rememberUpdatedState(aoTerminar)
    val composicao = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.driverflow_splash))
    val progresso by animateLottieCompositionAsState(composicao.value, iterations = 1)

    LaunchedEffect(composicao.isFailure) { if (composicao.isFailure) fim() }
    LaunchedEffect(progresso, composicao.value) { if (composicao.value != null && progresso >= 1f) fim() }

    Box(
        Modifier
            .fillMaxSize()
            .background(FundoAbertura)
            // Tocar pula a animação (sem efeito de toque)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fim() },
    ) {
        // Mesma proporção do desenho (9:16), centralizada: o texto acompanha o logo em qualquer tela.
        BoxWithConstraints(Modifier.align(Alignment.Center).aspectRatio(LARGURA_BASE / ALTURA_BASE)) {
            val escala = maxWidth.value / LARGURA_BASE
            val quadro = progresso * QUADROS_DA_ABERTURA

            LottieAnimation(composition = composicao.value, progress = { progresso }, modifier = Modifier.fillMaxSize())

            Text(
                text = "DriverFlow",
                color = CorDoNome.copy(alpha = opacidadeNoQuadro(quadro, 22f, 30f)),
                fontSize = (86f * escala).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offset(y = maxHeight * (1030f / ALTURA_BASE)),
            )
            Text(
                text = "Sua operação sob controle.",
                color = CorDoLema.copy(alpha = opacidadeNoQuadro(quadro, 31f, 39f)),
                fontSize = (35f * escala).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offset(y = maxHeight * (1150f / ALTURA_BASE)),
            )
        }
    }
}
