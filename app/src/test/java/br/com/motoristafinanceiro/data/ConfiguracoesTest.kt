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

class ConfiguracoesTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun encerrar() {
        escopo.cancel()
    }

    private fun criar(): Configuracoes {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = escopo,
            produceFile = { File(pasta.root, "teste.preferences_pb") },
        )
        return Configuracoes(dataStore)
    }

    @Test
    fun depreciacao_padraoEhZero() {
        runBlocking { assertEquals(0L, criar().depreciacaoPorKmCentavos.first()) }
    }

    @Test
    fun depreciacao_salvaELe() {
        runBlocking {
            val configuracoes = criar()
            configuracoes.salvarDepreciacaoPorKm(25)
            assertEquals(25L, configuracoes.depreciacaoPorKmCentavos.first())
        }
    }

    @Test
    fun depreciacao_sobrescreveOValorAnterior() {
        runBlocking {
            val configuracoes = criar()
            configuracoes.salvarDepreciacaoPorKm(25)
            configuracoes.salvarDepreciacaoPorKm(40)
            assertEquals(40L, configuracoes.depreciacaoPorKmCentavos.first())
        }
    }

    @Test
    fun depreciacao_negativaViraZero() {
        runBlocking {
            val configuracoes = criar()
            configuracoes.salvarDepreciacaoPorKm(-5)
            assertEquals(0L, configuracoes.depreciacaoPorKmCentavos.first())
        }
    }
}
