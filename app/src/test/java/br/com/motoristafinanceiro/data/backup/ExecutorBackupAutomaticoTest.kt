package br.com.motoristafinanceiro.data.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.ComprovanteStorage
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.backup.ExecutorBackupAutomatico.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate

/** Pasta de mentira, em memória; [quebrada] simula a pasta recusando gravar. */
private class PastaNaMemoria : PastaDeBackup {
    val arquivos = linkedMapOf<String, ByteArray>()
    var quebrada = false

    override fun criar(nome: String): OutputStream {
        if (quebrada) throw IOException("sem espaço")
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                arquivos[nome] = toByteArray()
            }
        }
    }

    override fun listar(): List<String> = arquivos.keys.toList()

    override fun apagar(nome: String) {
        arquivos.remove(nome)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExecutorBackupAutomaticoTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private lateinit var configuracoes: Configuracoes
    private lateinit var manager: BackupManager
    private val pasta = PastaNaMemoria()
    private lateinit var executor: ExecutorBackupAutomatico

    private val hoje = LocalDate.of(2026, 10, 22)
    private val agora = 1_792_000_000_000L
    private val dia = 24L * 60 * 60 * 1000

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db)
        configuracoes = Configuracoes(
            PreferenceDataStoreFactory.create(scope = escopo, produceFile = { File(tmp.root, "config.preferences_pb") }),
        )
        manager = BackupManager(repo, configuracoes, ComprovanteStorage(File(tmp.root, "comprovantes")), File(tmp.root, "restauracao"))
        executor = ExecutorBackupAutomatico(configuracoes, manager) { uri -> if (uri == "pasta://ok") pasta else null }
    }

    @After
    fun encerrar() {
        db.close()
        escopo.cancel()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    private suspend fun ligar(frequencia: FrequenciaBackup, uri: String? = "pasta://ok") {
        configuracoes.salvarFrequenciaBackup(frequencia)
        configuracoes.salvarPastaBackup(uri)
    }

    @Test
    fun desligado_naoFazNada() = roda {
        assertEquals(Resultado.DESLIGADO, executor.executar(agora, hoje))
        assertTrue(pasta.arquivos.isEmpty())
    }

    @Test
    fun diario_gravaUmBackupValidoERegistraAHora() = roda {
        repo.salvarGanho(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 24_890))
        ligar(FrequenciaBackup.DIARIO)

        assertEquals(Resultado.FEITO, executor.executar(agora, hoje))

        val bytes = pasta.arquivos.getValue("driverflow-auto-2026-10-22.zip")
        val lido = ArquivoBackup.ler(ByteArrayInputStream(bytes), File(tmp.root, "conferir"))
        assertEquals(24_890L, lido.dados.ganhos.single().valorCentavos)
        assertEquals(agora, configuracoes.ultimoBackupMs.first())
        assertNull(configuracoes.erroBackupAutomatico.first())
    }

    @Test
    fun antesDaHora_naoGravaDeNovo() = roda {
        ligar(FrequenciaBackup.SEMANAL)
        configuracoes.registrarBackupFeito(agora - 3 * dia)

        assertEquals(Resultado.AINDA_NAO, executor.executar(agora, hoje))
        assertTrue(pasta.arquivos.isEmpty())
    }

    @Test
    fun fazerAgora_ignoraAFrequencia() = roda {
        ligar(FrequenciaBackup.DESLIGADO)
        configuracoes.registrarBackupFeito(agora - dia / 2)

        assertEquals(Resultado.FEITO, executor.executar(agora, hoje, forcar = true))
        assertTrue("driverflow-auto-2026-10-22.zip" in pasta.arquivos)
    }

    @Test
    fun semPermissaoNaPasta_registraOErro() = roda {
        ligar(FrequenciaBackup.DIARIO, uri = "pasta://perdida")

        assertEquals(Resultado.SEM_PASTA, executor.executar(agora, hoje))
        assertTrue(configuracoes.erroBackupAutomatico.first()!!.contains("pasta"))
        assertNull(configuracoes.ultimoBackupMs.first())
    }

    @Test
    fun falhaAoGravar_registraOErroENaoContaComoFeito() = roda {
        ligar(FrequenciaBackup.DIARIO)
        pasta.quebrada = true

        assertEquals(Resultado.FALHOU, executor.executar(agora, hoje))
        assertTrue(configuracoes.erroBackupAutomatico.first()!!.contains("sem espaço"))
        assertNull(configuracoes.ultimoBackupMs.first())

        pasta.quebrada = false // na próxima tentativa dá certo e o erro some
        assertEquals(Resultado.FEITO, executor.executar(agora, hoje))
        assertNull(configuracoes.erroBackupAutomatico.first())
    }

    @Test
    fun mantemSoOsSeteAutomaticosMaisNovos_eNaoMexeNosManuais() = roda {
        ligar(FrequenciaBackup.DIARIO)
        (1..10).forEach { pasta.arquivos[nomeBackupAutomatico(LocalDate.of(2026, 10, it))] = byteArrayOf(1) }
        pasta.arquivos["motorista-financeiro-backup-2026-01-01.zip"] = byteArrayOf(1)

        executor.executar(agora, hoje)

        val automaticos = pasta.arquivos.keys.filter { it.startsWith(PREFIXO_BACKUP_AUTOMATICO) }.sorted()
        assertEquals(7, automaticos.size)
        assertEquals("driverflow-auto-2026-10-22.zip", automaticos.last())
        assertTrue("motorista-financeiro-backup-2026-01-01.zip" in pasta.arquivos)
    }
}
