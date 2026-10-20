package br.com.motoristafinanceiro.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ComprovanteStorageTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var pasta: File
    private lateinit var storage: ComprovanteStorage

    @Before
    fun preparar() {
        pasta = File(temp.root, "comprovantes") // ainda não existe: o storage precisa criar
        storage = ComprovanteStorage(pasta)
    }

    @Test
    fun novoArquivo_criaAPastaEUmArquivoUnico() {
        val a = storage.novoArquivo()
        val b = storage.novoArquivo()

        assertTrue(a.isFile)
        assertTrue(b.isFile)
        assertNotEquals(a.name, b.name)
        assertEquals(pasta, a.parentFile)
        assertTrue(a.name.startsWith("comprovante_") && a.name.endsWith(".jpg"))
    }

    @Test
    fun arquivo_ignoraCaminhosNoNome() {
        assertEquals(pasta, storage.arquivo("../../segredo.jpg").parentFile)
        assertEquals("segredo.jpg", storage.arquivo("../../segredo.jpg").name)
        assertEquals(File(pasta, "foto.jpg"), storage.arquivo("foto.jpg"))
    }

    @Test
    fun excluir_apagaOArquivo() {
        val arquivo = storage.novoArquivo()
        storage.excluir(arquivo.name)
        assertFalse(arquivo.exists())
    }

    @Test
    fun excluir_nuloVazioOuInexistente_naoFazNada() {
        val arquivo = storage.novoArquivo()

        storage.excluir(null)
        storage.excluir("")
        storage.excluir("   ")
        storage.excluir("nao_existe.jpg")

        assertTrue(arquivo.exists())
        assertTrue(pasta.isDirectory) // a pasta nunca é apagada por engano
    }

    @Test
    fun salvarEntrada_copiaOsBytes() {
        val conteudo = byteArrayOf(1, 2, 3, 4, 5)

        val arquivo = storage.salvarEntrada(ByteArrayInputStream(conteudo))

        assertArrayEquals(conteudo, arquivo.readBytes())
        assertEquals(pasta, arquivo.parentFile)
    }

    @Test
    fun limparOrfaos_apagaSoOsAntigosSemUso() {
        val agora = 1_000_000_000_000L
        val doisDias = 2L * 24 * 60 * 60 * 1000

        val usado = storage.novoArquivo().also { it.setLastModified(agora - doisDias) }
        val antigoSemUso = storage.novoArquivo().also { it.setLastModified(agora - doisDias) }
        val recenteSemUso = storage.novoArquivo().also { it.setLastModified(agora - 1_000) }

        val apagados = storage.limparOrfaos(emUso = setOf(usado.name), agoraMs = agora)

        assertEquals(1, apagados)
        assertTrue(usado.exists())
        assertFalse(antigoSemUso.exists())
        assertTrue(recenteSemUso.exists())
    }

    @Test
    fun limparOrfaos_semPasta_devolveZero() {
        assertEquals(0, storage.limparOrfaos(emUso = emptySet()))
    }

    // ---- Restauração de backup ----

    @Test
    fun restaurar_copiaParaAPastaComOMesmoNome() {
        val origem = File(temp.root, "origem.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        storage.restaurar(origem, "comp_1.jpg")

        assertArrayEquals(byteArrayOf(1, 2, 3), File(pasta, "comp_1.jpg").readBytes())
        assertTrue(origem.exists()) // copia, não move
    }

    @Test
    fun restaurar_substituiArquivoComMesmoNome() {
        pasta.mkdirs()
        File(pasta, "comp_1.jpg").writeBytes(byteArrayOf(9))
        val origem = File(temp.root, "origem.jpg").apply { writeBytes(byteArrayOf(1, 2)) }

        storage.restaurar(origem, "comp_1.jpg")

        assertArrayEquals(byteArrayOf(1, 2), File(pasta, "comp_1.jpg").readBytes())
    }

    @Test
    fun restaurar_ignoraCaminhoNoNome() {
        val origem = File(temp.root, "origem.jpg").apply { writeBytes(byteArrayOf(1)) }

        storage.restaurar(origem, "../fuga.jpg")

        assertTrue(File(pasta, "fuga.jpg").exists())
        assertFalse(File(temp.root, "fuga.jpg").exists())
    }

    @Test
    fun manterSomente_apagaOQueNaoEstaNaLista() {
        pasta.mkdirs()
        File(pasta, "a.jpg").writeBytes(byteArrayOf(1))
        File(pasta, "b.jpg").writeBytes(byteArrayOf(2))
        File(pasta, "c.jpg").writeBytes(byteArrayOf(3))

        val apagados = storage.manterSomente(setOf("b.jpg"))

        assertEquals(2, apagados)
        assertFalse(File(pasta, "a.jpg").exists())
        assertTrue(File(pasta, "b.jpg").exists())
        assertFalse(File(pasta, "c.jpg").exists())
    }

    @Test
    fun manterSomente_pastaInexistenteNaoQuebra() {
        assertEquals(0, storage.manterSomente(emptySet()))
    }
}
