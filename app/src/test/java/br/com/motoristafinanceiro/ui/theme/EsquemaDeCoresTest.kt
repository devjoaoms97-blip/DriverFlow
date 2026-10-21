package br.com.motoristafinanceiro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EsquemaDeCoresTest {

    private val c = EsquemaDriverFlow

    private fun contraste(a: Color, b: Color): Float {
        val (claro, escuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05f) / (escuro + 0.05f)
    }

    private fun confereContraste(c: ColorScheme) {
        val pares = mapOf(
            "onBackground/background" to (c.onBackground to c.background),
            "onSurface/surface" to (c.onSurface to c.surface),
            "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
            "onPrimary/primary" to (c.onPrimary to c.primary),
            "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
            "onSecondary/secondary" to (c.onSecondary to c.secondary),
            "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
            "onTertiary/tertiary" to (c.onTertiary to c.tertiary),
            "onTertiaryContainer/tertiaryContainer" to (c.onTertiaryContainer to c.tertiaryContainer),
            "onError/error" to (c.onError to c.error),
            "onErrorContainer/errorContainer" to (c.onErrorContainer to c.errorContainer),
            // textos coloridos sobre o fundo e sobre os cartões
            "primary/background" to (c.primary to c.background),
            "secondary/background" to (c.secondary to c.background),
            "tertiary/background" to (c.tertiary to c.background),
            "error/background" to (c.error to c.background),
            "onSurface/cartão" to (c.onSurface to c.surfaceContainerHighest),
            "onSurfaceVariant/cartão" to (c.onSurfaceVariant to c.surfaceContainerHighest),
            "primary/cartão" to (c.primary to c.surfaceContainerHighest),
            "secondary/cartão" to (c.secondary to c.surfaceContainerHighest),
            "error/cartão" to (c.error to c.surfaceContainerHighest),
            "tertiary/cartão" to (c.tertiary to c.surfaceContainerHighest),
        )
        pares.forEach { (nome, par) ->
            val razao = contraste(par.first, par.second)
            assertTrue("$nome: contraste ${"%.1f".format(razao)} abaixo de 4,5", razao >= 4.5f)
        }
    }

    @Test
    fun fundoDoApp_eOMesmoDaAbertura() {
        assertEquals(Color(0xFF080E20), c.background)
        assertTrue(File("src/main/res/values/themes.xml").readText().contains("windowSplashScreenBackground\">#080E20<"))
    }

    @Test
    fun paresTextoEFundo_temContrasteMinimoDe4e5_noTemaEscuro() = confereContraste(EsquemaDriverFlow)

    @Test
    fun paresTextoEFundo_temContrasteMinimoDe4e5_noTemaClaro() = confereContraste(EsquemaClaroDriverFlow)

    @Test
    fun temaClaroTemFundoClaro_eOEscuroTemFundoEscuro() {
        assertTrue(EsquemaClaroDriverFlow.background.luminance() > 0.8f)
        assertTrue(EsquemaDriverFlow.background.luminance() < 0.05f)
    }

    @Test
    fun ganhosEDespesas_temCoresDiferentes_nosDoisTemas() {
        // Ganho usa secondary (verde) e despesa usa error (vermelho) no Histórico.
        listOf(EsquemaDriverFlow, EsquemaClaroDriverFlow).forEach {
            assertNotEquals(it.secondary, it.error)
            assertNotEquals(it.secondary, it.primary)
        }
    }

    @Test
    fun temaXml_eEscuroESemCoresDinamicas() {
        val xml = File("src/main/res/values/themes.xml").readText()
        assertTrue(xml.contains("name=\"Theme.MotoristaFinanceiro\" parent=\"android:Theme.Material.NoActionBar\""))
        assertTrue(xml.contains("android:windowBackground\">#080E20<"))
        val tema = File("src/main/java/br/com/motoristafinanceiro/ui/theme/Theme.kt").readText()
        assertFalse("Cores dinâmicas fariam o app mudar de cor conforme o papel de parede", tema.contains("dynamic"))
    }
}
