package br.com.motoristafinanceiro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guarda a fonte Manrope, a escala de texto e os cantos do "Painel de bordo" (o Gradle roda com app/ como pasta de trabalho). */
class TipografiaTest {

    private val t = TipografiaDriverFlow
    private val todos = listOf(
        t.displayLarge, t.displayMedium, t.displaySmall,
        t.headlineLarge, t.headlineMedium, t.headlineSmall,
        t.titleLarge, t.titleMedium, t.titleSmall,
        t.bodyLarge, t.bodyMedium, t.bodySmall,
        t.labelLarge, t.labelMedium, t.labelSmall,
    )

    @Test
    fun arquivoDaFonte_existeEEhTrueType() {
        val fonte = File("src/main/res/font/manrope.ttf")
        assertTrue("manrope.ttf não existe", fonte.exists())
        assertEquals(listOf<Byte>(0, 1, 0, 0), fonte.readBytes().take(4)) // assinatura TrueType
    }

    @Test
    fun licencaDaFonte_vaiJuntoNoRepositorio() {
        assertTrue(File("../docs/licencas/Manrope-OFL.txt").readText().contains("SIL Open Font License"))
    }

    @Test
    fun todosOsEstilos_usamManropeComNumerosTabulares() {
        todos.forEach {
            assertEquals(Manrope, it.fontFamily)
            assertEquals(NUMEROS_TABULARES, it.fontFeatureSettings)
        }
    }

    @Test
    fun pesosDaFonte_cobremDoRegularAoExtraNegrito() = assertEquals(listOf(400, 500, 600, 700, 800), pesosManrope)

    @Test
    fun numeroPrincipal_grandeEExtraNegrito() {
        assertEquals(44f, t.displaySmall.fontSize.value)
        assertEquals(800, t.displaySmall.fontWeight?.weight)
        assertEquals(24f, t.headlineSmall.fontSize.value) // título das telas
    }

    @Test
    fun cantos_24NosCartoesGrandes18NosMedios14NosCampos() {
        assertEquals(RoundedCornerShape(24.dp), FormasDriverFlow.large)
        assertEquals(RoundedCornerShape(18.dp), FormasDriverFlow.medium)
        assertEquals(RoundedCornerShape(14.dp), FormasDriverFlow.small)
    }

    @Test
    fun tema_aplicaTipografiaEFormas() {
        val tema = File("src/main/java/br/com/motoristafinanceiro/ui/theme/Theme.kt").readText()
        assertTrue(tema.contains("typography = TipografiaDriverFlow"))
        assertTrue(tema.contains("shapes = FormasDriverFlow"))
    }
}
