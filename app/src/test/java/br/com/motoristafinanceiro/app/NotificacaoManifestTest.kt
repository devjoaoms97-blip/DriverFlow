package br.com.motoristafinanceiro.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guarda o que as notificações de manutenção exigem do manifest e dos recursos. */
class NotificacaoManifestTest {

    private val manifest get() = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun manifest_declaraAPermissaoDeNotificacao() {
        assertTrue(manifest.contains("<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />"))
    }

    @Test
    fun iconesDasNotificacoes_existemESaoUmaCorSo() {
        listOf("ic_stat_manutencao", "ic_stat_lembrete").forEach { nome ->
            val icone = File("src/main/res/drawable/$nome.xml")
            assertTrue("$nome.xml não existe", icone.exists())
            assertTrue(icone.readText().contains("#FFFFFF"))
        }
    }
}
