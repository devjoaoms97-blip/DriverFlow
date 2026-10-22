package br.com.motoristafinanceiro.data.relatorio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.YearMonth

/**
 * O desenho do PDF (PdfDocument) é nativo do Android e não roda no PC, nem com Robolectric: ele é conferido no
 * celular (roteiro manual). O conteúdo do relatório é testado em RelatorioMesTest.
 */
class RelatorioPdfTest {
    private val outubro = YearMonth.of(2026, 10)

    @Test
    fun nomeETitulo() {
        assertEquals("driverflow-relatorio-2026-10.pdf", nomeArquivoRelatorio(outubro))
        assertEquals("Outubro de 2026", tituloDoMes(outubro))
    }

    @Test
    fun compartilhar_usaAPastaDeclaradaNoFileProvider() {
        // O Gradle roda com app/ como pasta de trabalho.
        val xml = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(xml.contains("<cache-path name=\"relatorios\" path=\"${ExportadorRelatorio.PASTA}/\" />"))
    }
}
