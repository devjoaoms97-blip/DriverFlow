package br.com.motoristafinanceiro.leitor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guarda o que o leitor de corridas exige do manifest e dos recursos. O Gradle roda com app/ como pasta de trabalho. */
class LeitorManifestTest {
    private val manifest get() = File("src/main/AndroidManifest.xml").readText()
    private val configuracao get() = File("src/main/res/xml/leitor_corridas.xml").readText()

    @Test
    fun servico_declaradoEProtegidoPelaPermissaoDeAcessibilidade() {
        assertTrue(manifest.contains("android:name=\".leitor.LeitorDeCorridasService\""))
        assertTrue(manifest.contains("android:permission=\"android.permission.BIND_ACCESSIBILITY_SERVICE\""))
        assertTrue(manifest.contains("android:resource=\"@xml/leitor_corridas\""))
        // Não pede a permissão de sobrepor: a janela é a de acessibilidade.
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
    }

    @Test
    fun configuracao_soOuveUberE99_eLeAsJanelas() {
        LeitorDeCorridasService.PACOTES.forEach { assertTrue(it, configuracao.contains(it)) }
        assertTrue(configuracao.contains("android:canRetrieveWindowContent=\"true\""))
        assertTrue(configuracao.contains("flagRetrieveInteractiveWindows"))
        // Não pede para executar gestos: o leitor nunca toca no outro app.
        assertFalse(configuracao.contains("canPerformGestures"))
    }

    @Test
    fun gravacao_compartilhaPelaPastaDeclaradaNoFileProvider() {
        val xml = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(xml.contains("<files-path name=\"leitor\" path=\"${GravadorDeTela.PASTA}/\" />"))
    }
}
