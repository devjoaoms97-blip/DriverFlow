package br.com.motoristafinanceiro.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArquivoBackupTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val fotoBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    private fun pastaDeFotos(vararg nomes: String): File {
        val dir = pasta.newFolder("fotos")
        nomes.forEach { File(dir, it).writeBytes(fotoBytes) }
        return dir
    }

    private fun exportar(dados: DadosBackup, fotos: File): Pair<ByteArray, ResumoBackup> {
        val saida = ByteArrayOutputStream()
        val resumo = ArquivoBackup.escrever(saida, dados, { File(fotos, it) }, 1L)
        return saida.toByteArray() to resumo
    }

    private fun zipCom(vararg entradas: Pair<String, ByteArray>): ByteArray {
        val saida = ByteArrayOutputStream()
        ZipOutputStream(saida).use { zip ->
            entradas.forEach { (nome, conteudo) ->
                zip.putNextEntry(ZipEntry(nome))
                zip.write(conteudo)
                zip.closeEntry()
            }
        }
        return saida.toByteArray()
    }

    private fun assertInvalido(bytes: ByteArray) {
        try {
            ArquivoBackup.ler(ByteArrayInputStream(bytes), File(pasta.root, "temp"))
            fail("Deveria ter recusado o arquivo")
        } catch (e: BackupInvalidoException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    @Test
    fun escreverELer_devolveDadosEFotos() {
        val (bytes, resumo) = exportar(dadosDeExemplo(), pastaDeFotos("comp_1.jpg"))
        val temp = File(pasta.root, "temp")

        val lido = ArquivoBackup.ler(ByteArrayInputStream(bytes), temp)

        assertEquals(dadosDeExemplo(), lido.dados)
        assertEquals(setOf("comp_1.jpg"), lido.fotos)
        assertArrayEquals(fotoBytes, File(temp, "comp_1.jpg").readBytes())
        assertEquals(1, resumo.fotos)
        assertEquals(0, resumo.fotosAusentes)
    }

    @Test
    fun escrever_oArquivoTemDadosJsonEPastaComprovantes() {
        val (bytes, _) = exportar(dadosDeExemplo(), pastaDeFotos("comp_1.jpg"))
        val arquivo = File(pasta.root, "backup.zip").apply { writeBytes(bytes) }

        val nomes = ZipFile(arquivo).use { z -> z.entries().toList().map { it.name } }

        assertEquals(listOf("dados.json", "comprovantes/comp_1.jpg"), nomes)
    }

    @Test
    fun escrever_fotoReferenciadaQueNaoExisteNoAparelhoFicaDeForaEEContada() {
        val (bytes, resumo) = exportar(dadosDeExemplo(), pastaDeFotos()) // pasta vazia

        val lido = ArquivoBackup.ler(ByteArrayInputStream(bytes), File(pasta.root, "temp"))

        assertEquals(0, resumo.fotos)
        assertEquals(1, resumo.fotosAusentes)
        assertTrue(lido.fotos.isEmpty())
    }

    @Test
    fun escrever_soLevaFotosUsadasPorAlgumaDespesa() {
        val (bytes, resumo) = exportar(dadosDeExemplo(), pastaDeFotos("comp_1.jpg", "sobra.jpg"))

        val lido = ArquivoBackup.ler(ByteArrayInputStream(bytes), File(pasta.root, "temp"))

        assertEquals(setOf("comp_1.jpg"), lido.fotos)
        assertEquals(1, resumo.fotos)
    }

    @Test
    fun ler_arquivoSemDadosJsonEInvalido() {
        assertInvalido(zipCom("comprovantes/comp_1.jpg" to fotoBytes))
    }

    @Test
    fun ler_bytesQueNaoSaoZipSaoInvalidos() {
        assertInvalido("isto não é um zip".toByteArray())
        assertInvalido(ByteArray(0))
    }

    @Test
    fun ler_dadosJsonComConteudoInvalidoEInvalido() {
        assertInvalido(zipCom("dados.json" to "{\"formato\":\"outro\"}".toByteArray()))
    }

    @Test
    fun ler_ignoraEntradasComCaminhoSuspeitoEArquivosEstranhos() {
        val json = BackupJson.gerar(DadosBackup(), 1L).toByteArray()
        val bytes = zipCom(
            "dados.json" to json,
            "comprovantes/ok.jpg" to fotoBytes,
            "comprovantes/../fuga.jpg" to fotoBytes,
            "comprovantes/sub/x.jpg" to fotoBytes,
            "leia-me.txt" to fotoBytes,
        )
        val temp = File(pasta.root, "temp")

        val lido = ArquivoBackup.ler(ByteArrayInputStream(bytes), temp)

        assertEquals(setOf("ok.jpg"), lido.fotos)
        assertFalse(File(pasta.root, "fuga.jpg").exists())
        assertFalse(File(temp, "x.jpg").exists())
        assertFalse(File(temp, "leia-me.txt").exists())
    }

    @Test
    fun ler_naoAlteraArquivosForaDaPastaTemporaria() {
        val sentinela = File(pasta.root, "intocado.txt").apply { writeText("ok") }
        val bytes = zipCom(
            "dados.json" to BackupJson.gerar(DadosBackup(), 1L).toByteArray(),
            "comprovantes/../intocado.txt" to "sobrescrito".toByteArray(),
        )

        ArquivoBackup.ler(ByteArrayInputStream(bytes), File(pasta.root, "temp"))

        assertEquals("ok", sentinela.readText())
    }
}
