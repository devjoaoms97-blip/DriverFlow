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
    fun iconeDaNotificacao_existeEEhUmaCorSo() {
        val icone = File("src/main/res/drawable/ic_stat_manutencao.xml")
        assertTrue(icone.exists())
        assertTrue(icone.readText().contains("#FFFFFF"))
    }
}
