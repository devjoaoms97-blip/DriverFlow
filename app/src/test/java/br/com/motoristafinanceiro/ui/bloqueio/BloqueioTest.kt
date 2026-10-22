package br.com.motoristafinanceiro.ui.bloqueio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BloqueioTest {
    private val agora = 10_000_000L

    @Test
    fun desligado_nuncaBloqueia() = assertFalse(deveBloquearAoVoltar(false, agora - 60 * 60_000, agora))

    @Test
    fun voltaRapida_naoBloqueia() {
        // Girar a tela, tirar a foto do comprovante, olhar uma mensagem.
        assertFalse(deveBloquearAoVoltar(true, agora - 5_000, agora))
        assertFalse(deveBloquearAoVoltar(true, agora - TEMPO_FORA_PARA_BLOQUEAR_MS + 1, agora))
    }

    @Test
    fun depoisDeDoisMinutosFora_bloqueia() {
        assertTrue(deveBloquearAoVoltar(true, agora - TEMPO_FORA_PARA_BLOQUEAR_MS, agora))
        assertTrue(deveBloquearAoVoltar(true, agora - 60 * 60_000, agora))
    }

    @Test
    fun semTerSaido_naoBloqueia() = assertFalse(deveBloquearAoVoltar(true, null, agora))

    @Test
    fun mainActivity_cobreOAppComATelaDeBloqueioEUsaFragmentActivity() {
        // O Gradle roda com app/ como pasta de trabalho.
        val fonte = File("src/main/java/br/com/motoristafinanceiro/MainActivity.kt").readText()
        assertTrue(fonte.contains("class MainActivity : FragmentActivity()"))
        assertTrue(fonte.contains("TelaBloqueio("))
        assertTrue(fonte.contains("setKeepOnScreenCondition { lendoBloqueio }"))
    }
}
