package br.com.motoristafinanceiro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprovantesTest {

    @Test
    fun nomeDoArquivo_temFormatoEstavel_eNaoUsaAleatorioNegativo() {
        assertEquals("comprovante_1700000000000_42.jpg", novoNomeComprovante(1_700_000_000_000, 42))
        assertEquals("comprovante_1_0.jpg", novoNomeComprovante(1, -7))
    }

    @Test
    fun inSampleSize_reduzSemFicarMenorQueOLimite() {
        assertEquals(2, calcularInSampleSize(4_000, 3_000, 1_600)) // fica com 2000 px
        assertEquals(2, calcularInSampleSize(3_200, 2_400, 1_600)) // fica com 1600 px
        assertEquals(4, calcularInSampleSize(8_000, 6_000, 1_600)) // fica com 2000 px
    }

    @Test
    fun inSampleSize_imagemJaPequena_naoReduz() {
        assertEquals(1, calcularInSampleSize(1_600, 1_200, 1_600))
        assertEquals(1, calcularInSampleSize(800, 600, 1_600))
    }

    @Test
    fun inSampleSize_valoresInvalidos_voltamAUm() {
        assertEquals(1, calcularInSampleSize(0, 100, 1_600))
        assertEquals(1, calcularInSampleSize(100, -1, 1_600))
        assertEquals(1, calcularInSampleSize(4_000, 3_000, 0))
    }

    @Test
    fun inSampleSize_usaOMaiorLado_emFotoDeitadaOuEmPe() {
        assertEquals(calcularInSampleSize(4_000, 3_000, 1_600), calcularInSampleSize(3_000, 4_000, 1_600))
    }

    @Test
    fun rotacaoExif_mapeiaOsValoresDaCamera() {
        assertEquals(90, grausDeRotacao(6))
        assertEquals(180, grausDeRotacao(3))
        assertEquals(270, grausDeRotacao(8))
        assertEquals(0, grausDeRotacao(1))
        assertEquals(0, grausDeRotacao(0))
        assertEquals(0, grausDeRotacao(99))
    }

    @Test
    fun orfaos_soOsSemUsoEComIdadeSuficiente() {
        val agora = 10L * IDADE_MINIMA_ORFAO_MS
        val arquivos = listOf(
            InfoArquivo("usado.jpg", modificadoEmMs = 0),
            InfoArquivo("velho_sem_uso.jpg", modificadoEmMs = 0),
            InfoArquivo("recente_sem_uso.jpg", modificadoEmMs = agora - 1_000),
            InfoArquivo("no_limite.jpg", modificadoEmMs = agora - IDADE_MINIMA_ORFAO_MS),
        )

        val orfaos = arquivosOrfaos(arquivos, emUso = setOf("usado.jpg"), agoraMs = agora)

        assertEquals(listOf("velho_sem_uso.jpg", "no_limite.jpg"), orfaos)
    }

    @Test
    fun orfaos_semArquivos_listaVazia() {
        assertTrue(arquivosOrfaos(emptyList(), emptySet(), agoraMs = 1_000_000).isEmpty())
    }

    @Test
    fun fotoSubstituida_soQuandoMudouOuFoiRemovida() {
        assertEquals("a.jpg", fotoSubstituidaOuRemovida("a.jpg", "b.jpg")) // trocada
        assertEquals("a.jpg", fotoSubstituidaOuRemovida("a.jpg", null))    // removida
        assertNull(fotoSubstituidaOuRemovida("a.jpg", "a.jpg"))            // igual: nada a apagar
        assertNull(fotoSubstituidaOuRemovida(null, "b.jpg"))               // não havia foto antes
        assertNull(fotoSubstituidaOuRemovida(null, null))
    }
}
