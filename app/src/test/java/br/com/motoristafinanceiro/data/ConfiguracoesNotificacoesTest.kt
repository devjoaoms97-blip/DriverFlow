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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConfiguracoesNotificacoesTest {

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
    fun avisoDeManutencao_comecaLigado() {
        runBlocking { assertTrue(criar().notificacoesManutencaoAtivas.first()) }
    }

    @Test
    fun permissaoPerguntada_comecaFalsaEFicaMarcada() {
        runBlocking {
            val c = criar()
            assertFalse(c.permissaoNotificacaoPerguntada.first())
            c.marcarPermissaoNotificacaoPerguntada()
            assertTrue(c.permissaoNotificacaoPerguntada.first())
        }
    }

    @Test
    fun avisoDeManutencao_ligaEDesliga() {
        runBlocking {
            val c = criar()
            c.salvarNotificacoesManutencao(false)
            assertFalse(c.notificacoesManutencaoAtivas.first())
            c.salvarNotificacoesManutencao(true)
            assertTrue(c.notificacoesManutencaoAtivas.first())
        }
    }

    @Test
    fun alertasNotificados_comecamVazios() {
        runBlocking { assertTrue(criar().alertasNotificados.first().isEmpty()) }
    }

    @Test
    fun alertasNotificados_salvamESubstituem() {
        runBlocking {
            val c = criar()
            c.salvarAlertasNotificados(setOf("1:PROXIMA", "2:ATRASADA"))
            assertEquals(setOf("1:PROXIMA", "2:ATRASADA"), c.alertasNotificados.first())
            c.salvarAlertasNotificados(setOf("2:ATRASADA"))
            assertEquals(setOf("2:ATRASADA"), c.alertasNotificados.first())
        }
    }

    @Test
    fun depreciacaoENotificacoes_naoSeInterferem() {
        runBlocking {
            val c = criar()
            c.salvarDepreciacaoPorKm(25)
            c.salvarNotificacoesManutencao(true)
            assertEquals(25L, c.depreciacaoPorKmCentavos.first())
            assertTrue(c.notificacoesManutencaoAtivas.first())
        }
    }
}
