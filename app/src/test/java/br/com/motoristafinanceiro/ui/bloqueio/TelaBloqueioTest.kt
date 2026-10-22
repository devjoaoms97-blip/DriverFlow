package br.com.motoristafinanceiro.ui.bloqueio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Regressão: com o bloqueio ligado, o pedido de digital cobria a abertura animada desde o primeiro quadro. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelaBloqueioTest {

    @get:Rule
    val tela = createComposeRule()

    @Test
    fun enquantoAAberturaToca_naoPedeADigital_eAoTerminarPedeUmaVez() {
        var pedidos = 0
        var abertura by mutableStateOf(true)
        tela.setContent { MotoristaTheme { TelaBloqueio(pedirAgora = !abertura, onDesbloquear = { pedidos++ }) } }
        tela.waitForIdle()
        assertEquals(0, pedidos)

        abertura = false
        tela.waitForIdle()
        assertEquals(1, pedidos)
    }

    @Test
    fun semAbertura_pedeADigitalAssimQueAparece() {
        var pedidos = 0
        tela.setContent { MotoristaTheme { TelaBloqueio(onDesbloquear = { pedidos++ }) } }
        tela.waitForIdle()
        assertEquals(1, pedidos)
    }

    @Test
    fun mainActivity_desenhaOBloqueioPorBaixoDaAbertura_eEsperaElaTerminar() {
        // O Gradle roda com app/ como pasta de trabalho.
        val fonte = File("src/main/java/br/com/motoristafinanceiro/MainActivity.kt").readText()
        assertTrue(fonte.contains("TelaBloqueio(pedirAgora = !abertura"))
        assertTrue(fonte.indexOf("TelaBloqueio(") < fonte.indexOf("AberturaAnimada("))
    }
}
