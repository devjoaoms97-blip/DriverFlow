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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val foto = byteArrayOf(9, 8, 7, 6, 5)

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private lateinit var configuracoes: Configuracoes
    private lateinit var comprovantes: ComprovanteStorage
    private lateinit var pastaFotos: File
    private lateinit var pastaTemp: File
    private lateinit var manager: BackupManager

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db)
        configuracoes = Configuracoes(
            PreferenceDataStoreFactory.create(scope = escopo, produceFile = { File(pasta.root, "config.preferences_pb") }),
        )
        pastaFotos = File(pasta.root, "comprovantes")
        pastaTemp = File(pasta.root, "restauracao")
        comprovantes = ComprovanteStorage(pastaFotos)
        manager = BackupManager(repo, configuracoes, comprovantes, pastaTemp)
    }

    @After
    fun encerrar() {
        db.close()
        escopo.cancel()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    private suspend fun instalarDadosDeExemplo() {
        repo.substituirTudo(dadosDeExemplo())
        configuracoes.salvarDepreciacaoPorKm(25)
        pastaFotos.mkdirs()
        File(pastaFotos, "comp_1.jpg").writeBytes(foto)
    }

    private suspend fun exportarParaBytes(): ByteArray {
        val saida = ByteArrayOutputStream()
        manager.exportar(saida)
        return saida.toByteArray()
    }

    private suspend fun estadoAtual() = repo.lerTudo(configuracoes.depreciacaoPorKmCentavos.first())

    private fun zipCom(vararg entradas: Pair<String, ByteArray>): ByteArray {
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { zip ->
            entradas.forEach { (nome, conteudo) ->
                zip.putNextEntry(ZipEntry(nome)); zip.write(conteudo); zip.closeEntry()
            }
        }
        return saida.toByteArray()
    }

    // ---- Exportar ----

    @Test
    fun exportar_devolveContagens() = roda {
        instalarDadosDeExemplo()

        val resumo = manager.exportar(ByteArrayOutputStream())

        assertEquals(2, resumo.ganhos)
        assertEquals(4, resumo.despesas)
        assertEquals(1, resumo.fotos)
        assertEquals(0, resumo.fotosAusentes)
    }

    @Test
    fun exportar_naoAlteraOsDados() = roda {
        instalarDadosDeExemplo()
        val antes = estadoAtual()

        exportarParaBytes()

        assertEquals(antes, estadoAtual())
        assertTrue(File(pastaFotos, "comp_1.jpg").exists())
    }

    // ---- Restaurar ----

    @Test
    fun exportarERestaurar_devolveTudoComoEstava() = roda {
        instalarDadosDeExemplo()
        val backup = exportarParaBytes()

        // Perde tudo (como ao reinstalar o app)
        repo.substituirTudo(DadosBackup())
        configuracoes.salvarDepreciacaoPorKm(99)
        File(pastaFotos, "comp_1.jpg").delete()

        val resumo = manager.restaurar(ByteArrayInputStream(backup))

        assertEquals(dadosDeExemplo(), estadoAtual())
        assertEquals(25L, configuracoes.depreciacaoPorKmCentavos.first())
        assertArrayEquals(foto, File(pastaFotos, "comp_1.jpg").readBytes())
        assertEquals(2, resumo.ganhos)
        assertEquals(1, resumo.fotos)
    }

    @Test
    fun restaurar_substituiTudoEApagaOQueNaoEstaNoBackup() = roda {
        instalarDadosDeExemplo()
        val backup = exportarParaBytes()

        repo.salvarGanho(Ganho(data = LocalDate.of(2026, 10, 20), plataforma = Plataforma.UBER, valorCentavos = 77_700))
        File(pastaFotos, "velha.jpg").writeBytes(foto)

        manager.restaurar(ByteArrayInputStream(backup))

        assertEquals(dadosDeExemplo(), estadoAtual())
        assertFalse(File(pastaFotos, "velha.jpg").exists())
    }

    @Test
    fun restaurar_arquivoInvalido_naoMexeEmNada() = roda {
        instalarDadosDeExemplo()
        val antes = estadoAtual()

        try {
            manager.restaurar(ByteArrayInputStream("não sou um backup".toByteArray()))
            fail("Deveria ter recusado")
        } catch (e: BackupInvalidoException) {
            // esperado
        }

        assertEquals(antes, estadoAtual())
        assertEquals(25L, configuracoes.depreciacaoPorKmCentavos.first())
        assertTrue(File(pastaFotos, "comp_1.jpg").exists())
    }

    @Test
    fun restaurar_dadosInvalidosDentroDoZip_naoMexemEmNada() = roda {
        instalarDadosDeExemplo()
        val antes = estadoAtual()
        val json = JSONObject(BackupJson.gerar(dadosDeExemplo(), 1L))
        json.getJSONArray("ganhos").getJSONObject(0).put("valorCentavos", -5)
        val zipRuim = zipCom("dados.json" to json.toString().toByteArray(), "comprovantes/comp_1.jpg" to byteArrayOf(0))

        try {
            manager.restaurar(ByteArrayInputStream(zipRuim))
            fail("Deveria ter recusado")
        } catch (e: BackupInvalidoException) {
            // esperado
        }

        assertEquals(antes, estadoAtual())
        assertArrayEquals(foto, File(pastaFotos, "comp_1.jpg").readBytes()) // a foto do zip ruim não sobrescreveu
    }

    @Test
    fun restaurar_fotoAusenteNoArquivo_limpaOCampoEConta() = roda {
        repo.substituirTudo(dadosDeExemplo()) // despesa 1 aponta para comp_1.jpg, mas o arquivo não existe no aparelho
        val backup = exportarParaBytes()
        repo.substituirTudo(DadosBackup())

        val resumo = manager.restaurar(ByteArrayInputStream(backup))

        assertEquals(1, resumo.fotosAusentes)
        assertEquals(0, resumo.fotos)
        assertNull(estadoAtual().despesas.first { it.id == 1L }.fotoUri)
    }

    @Test
    fun restaurar_mantemOsVinculosEOsIds() = roda {
        instalarDadosDeExemplo()
        val backup = exportarParaBytes()
        repo.substituirTudo(DadosBackup())

        manager.restaurar(ByteArrayInputStream(backup))

        // O vínculo despesa 2 <-> abastecimento 1 continua funcionando
        val abastecimento = repo.abastecimentoPorId(1)!!
        repo.excluirAbastecimento(abastecimento)
        val ids = estadoAtual().despesas.map { it.id }
        assertFalse(2L in ids)
        assertTrue(1L in ids && 3L in ids && 4L in ids)
    }

    @Test
    fun restaurar_novosLancamentosNaoColidemComOsRestaurados() = roda {
        instalarDadosDeExemplo()
        val backup = exportarParaBytes()
        repo.substituirTudo(DadosBackup())
        manager.restaurar(ByteArrayInputStream(backup))

        repo.salvarGanho(Ganho(data = LocalDate.of(2026, 10, 20), plataforma = Plataforma.UBER, valorCentavos = 1_000))

        val ganhos = estadoAtual().ganhos
        assertEquals(3, ganhos.size)
        assertEquals(3, ganhos.map { it.id }.toSet().size)
        assertTrue(ganhos.first { it.valorCentavos == 1_000L }.id > 2)
    }

    @Test
    fun restaurar_limpaAPastaTemporaria() = roda {
        instalarDadosDeExemplo()
        val backup = exportarParaBytes()

        manager.restaurar(ByteArrayInputStream(backup))

        assertFalse(pastaTemp.exists())
    }

    @Test
    fun restaurar_limpaAPastaTemporariaTambemQuandoFalha() = roda {
        try {
            manager.restaurar(ByteArrayInputStream("lixo".toByteArray()))
        } catch (e: BackupInvalidoException) {
            // esperado
        }

        assertFalse(pastaTemp.exists())
    }
}
