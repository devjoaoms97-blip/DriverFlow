package br.com.motoristafinanceiro.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guarda o nome DriverFlow, as imagens do ícone e a tela de abertura (o Gradle roda com app/ como pasta de trabalho). */
class IdentidadeVisualTest {

    private fun arquivo(caminho: String) = File(caminho)

    @Test
    fun nomeDoApp_eDriverFlow() {
        assertTrue(arquivo("src/main/res/values/strings.xml").readText().contains("<string name=\"app_name\">DriverFlow</string>"))
    }

    @Test
    fun imagensDoIcone_existemESaoPng() {
        val assinaturaPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        listOf("ic_launcher_arte", "ic_launcher_arte_mono").forEach { nome ->
            val f = arquivo("src/main/res/drawable-nodpi/$nome.png")
            assertTrue("$nome.png não existe", f.exists())
            assertEquals("$nome.png não é PNG", assinaturaPng.toList(), f.readBytes().take(4))
        }
    }

    @Test
    fun camadasDoIcone_usamAsImagensNovas() {
        assertTrue(arquivo("src/main/res/drawable/ic_launcher_foreground.xml").readText().contains("@drawable/ic_launcher_arte\""))
        assertTrue(arquivo("src/main/res/drawable/ic_launcher_monochrome.xml").readText().contains("@drawable/ic_launcher_arte_mono"))
    }

    @Test
    fun telaDeAbertura_temTemaPropioEVoltaAoTemaDoApp() {
        val temas = arquivo("src/main/res/values/themes.xml").readText()
        assertTrue(temas.contains("name=\"Theme.MotoristaFinanceiro.Splash\" parent=\"Theme.SplashScreen\""))
        assertTrue(temas.contains("windowSplashScreenBackground\">#080E20<"))
        assertTrue(temas.contains("windowSplashScreenAnimatedIcon\">@drawable/splash_vazio"))
        assertTrue(temas.contains("postSplashScreenTheme\">@style/Theme.MotoristaFinanceiro<"))
        assertTrue(arquivo("src/main/AndroidManifest.xml").readText().contains("android:theme=\"@style/Theme.MotoristaFinanceiro.Splash\""))
    }

    @Test
    fun aberturaAnimada_usaAAnimacaoEFicaPorCimaDoApp() {
        val abertura = arquivo("src/main/java/br/com/motoristafinanceiro/ui/abertura/AberturaAnimada.kt").readText()
        assertTrue(abertura.contains("R.raw.driverflow_splash"))
        assertTrue(arquivo("src/main/res/raw/driverflow_splash.json").exists())
        assertTrue(arquivo("src/main/res/drawable/splash_vazio.xml").exists())
        assertTrue(arquivo("src/main/java/br/com/motoristafinanceiro/MainActivity.kt").readText().contains("AberturaAnimada("))
        assertTrue(abertura.contains("\"DriverFlow\"") && abertura.contains("Sua operação sob controle."))
    }

    @Test
    fun mainActivity_instalaAAberturaAntesDoSuperOnCreate() {
        val fonte = arquivo("src/main/java/br/com/motoristafinanceiro/MainActivity.kt").readText()
        val splash = fonte.indexOf("installSplashScreen()")
        val superCreate = fonte.indexOf("super.onCreate(")
        assertTrue("installSplashScreen() ausente", splash >= 0)
        assertTrue("installSplashScreen() precisa vir antes de super.onCreate()", splash < superCreate)
    }
}
