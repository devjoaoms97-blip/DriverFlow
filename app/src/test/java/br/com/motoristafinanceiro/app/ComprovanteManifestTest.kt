package br.com.motoristafinanceiro.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guarda a configuração que a câmera exige (FileProvider e consulta de apps de câmera).
 * O Gradle roda os testes de unidade com a pasta do módulo (app/) como diretório de trabalho.
 */
class ComprovanteManifestTest {

    private val manifest get() = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun manifest_declaraOFileProvider() {
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("\${applicationId}.fileprovider"))
        assertTrue(manifest.contains("android:grantUriPermissions=\"true\""))
        assertTrue(manifest.contains("android:exported=\"false\""))
        assertTrue(manifest.contains("@xml/file_paths"))
    }

    @Test
    fun manifest_consultaOsAppsDeCamera() {
        assertTrue(manifest.contains("android.media.action.IMAGE_CAPTURE"))
    }

    @Test
    fun manifest_naoPedePermissaoDeCamera() {
        // Pedir CAMERA no manifest obrigaria o app a ter a permissão para usar a câmera do sistema.
        assertTrue(!manifest.contains("android.permission.CAMERA"))
    }

    @Test
    fun filePaths_apontaParaAPastaDosComprovantes() {
        val xml = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(xml.contains("files-path"))
        assertTrue(xml.contains("comprovantes/"))
    }
}
