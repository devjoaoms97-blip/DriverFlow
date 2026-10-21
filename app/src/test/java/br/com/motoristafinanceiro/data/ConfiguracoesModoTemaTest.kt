package br.com.motoristafinanceiro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConfiguracoesModoTemaTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun encerrar() {
        escopo.cancel()
    }

    private fun criar() = Configuracoes(
        PreferenceDataStoreFactory.create(scope = escopo, produceFile = { File(pasta.root, "teste.preferences_pb") })
    )

    @Test
    fun modoTema_comecaEscuro() {
        runBlocking { assertEquals(ModoTema.ESCURO, criar().modoTema.first()) }
    }

    @Test
    fun modoTema_guardaCadaModo() {
        runBlocking {
            val c = criar()
            ModoTema.values().forEach { modo ->
                c.salvarModoTema(modo)
                assertEquals(modo, c.modoTema.first())
            }
        }
    }
}
