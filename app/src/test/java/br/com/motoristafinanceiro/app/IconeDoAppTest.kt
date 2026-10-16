package br.com.motoristafinanceiro.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guarda contra a remoção acidental do ícone. O Gradle roda os testes de unidade com a pasta
 * do módulo (app/) como diretório de trabalho.
 */
class IconeDoAppTest {

    @Test
    fun manifest_referenciaOIcone() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun iconeAdaptativo_temFundoPrimeiroPlanoEVersaoMonocromatica() {
        listOf("ic_launcher", "ic_launcher_round").forEach { nome ->
            val xml = File("src/main/res/mipmap-anydpi-v26/$nome.xml").readText()
            assertTrue("$nome sem fundo", xml.contains("@drawable/ic_launcher_background"))
            assertTrue("$nome sem primeiro plano", xml.contains("@drawable/ic_launcher_foreground"))
            assertTrue("$nome sem versão monocromática", xml.contains("@drawable/ic_launcher_monochrome"))
        }
    }

    @Test
    fun recursosDoIcone_existem() {
        listOf("ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome").forEach { nome ->
            assertTrue("$nome.xml não existe", File("src/main/res/drawable/$nome.xml").exists())
        }
    }
}
