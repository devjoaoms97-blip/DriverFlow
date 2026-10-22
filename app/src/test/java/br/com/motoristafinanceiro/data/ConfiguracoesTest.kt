package br.com.motoristafinanceiro.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import br.com.motoristafinanceiro.data.backup.FrequenciaBackup
import br.com.motoristafinanceiro.domain.LimitesCorrida
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

    @Test
    fun backupAutomatico_padraoDesligadoSemPastaENuncaFeito() {
        runBlocking {
            val c = criar()
            assertEquals(FrequenciaBackup.DESLIGADO, c.frequenciaBackup.first())
            assertEquals(null, c.pastaBackup.first())
            assertEquals(null, c.ultimoBackupMs.first())
            assertEquals(null, c.erroBackupAutomatico.first())
        }
    }

    @Test
    fun backupAutomatico_salvaFrequenciaPastaEHora_eBackupFeitoApagaOErro() {
        runBlocking {
            val c = criar()
            c.salvarFrequenciaBackup(FrequenciaBackup.SEMANAL)
            c.salvarPastaBackup("content://pasta")
            c.registrarErroBackupAutomatico("falhou")
            c.registrarBackupFeito(123L)

            assertEquals(FrequenciaBackup.SEMANAL, c.frequenciaBackup.first())
            assertEquals("content://pasta", c.pastaBackup.first())
            assertEquals(123L, c.ultimoBackupMs.first())
            assertEquals(null, c.erroBackupAutomatico.first())

            c.salvarPastaBackup(null)
            assertEquals(null, c.pastaBackup.first())
        }
    }

    @Test
    fun bloqueio_padraoDesligado_eSalva() {
        runBlocking {
            val c = criar()
            assertEquals(false, c.bloqueioAtivo.first())
            c.salvarBloqueio(true)
            assertEquals(true, c.bloqueioAtivo.first())
        }
    }

    @Test
    fun lembrete_padraoDesligadoAs22h_eSalva() {
        runBlocking {
            val c = criar()
            assertEquals(false, c.lembreteAtivo.first())
            assertEquals(22 * 60, c.lembreteHorarioMinutos.first())
            c.salvarLembrete(true, 21 * 60 + 30)
            assertEquals(true, c.lembreteAtivo.first())
            assertEquals(21 * 60 + 30, c.lembreteHorarioMinutos.first())
        }
    }

    @Test
    fun limitesCorrida_padraoDoisEUmECinquenta_eSalva() {
        runBlocking {
            val configuracoes = criar()
            assertEquals(LimitesCorrida(200, 150), configuracoes.limitesCorrida.first())
            configuracoes.salvarLimitesCorrida(LimitesCorrida(250, 180))
            assertEquals(LimitesCorrida(250, 180), configuracoes.limitesCorrida.first())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun limitesCorrida_recusaAmareloAcimaDoVerde() {
        runBlocking { criar().salvarLimitesCorrida(LimitesCorrida(150, 200)) }
    }

    @Test
    fun leitorGravando_padraoDesligado_eSalva() {
        runBlocking {
            val configuracoes = criar()
            assertEquals(false, configuracoes.leitorGravando.first())
            configuracoes.salvarLeitorGravando(true)
            assertEquals(true, configuracoes.leitorGravando.first())
        }
    }
}
