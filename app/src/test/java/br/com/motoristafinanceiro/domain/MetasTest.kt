package br.com.motoristafinanceiro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MetasTest {

    // ---- parseMetaMensal ----

    @Test
    fun parse_valorInteiroEComVirgula() {
        assertEquals(500_000L, parseMetaMensal("5000"))
        assertEquals(500_050L, parseMetaMensal("5000,50"))
        assertEquals(500_050L, parseMetaMensal("5000.50"))
    }

    @Test
    fun parse_vazioInvalidoZeroENegativoSaoNulos() {
        assertNull(parseMetaMensal(""))
        assertNull(parseMetaMensal("   "))
        assertNull(parseMetaMensal("abc"))
        assertNull(parseMetaMensal("0"))
        assertNull(parseMetaMensal("-100"))
    }

    @Test
    fun parse_respeitaOTeto() {
        assertEquals(META_MAXIMA_CENTAVOS, parseMetaMensal("100000"))
        assertNull(parseMetaMensal("100000,01"))
    }

    // ---- calcularProgressoMeta ----

    @Test
    fun faltaPorDia_contaHojeComoDiaRestante() {
        // 15/10/2026: outubro tem 31 dias, restam 17 (15..31)
        val p = calcularProgressoMeta(500_000, 150_000, LocalDate.of(2026, 10, 15))

        assertEquals(17, p.diasRestantes)
        assertEquals(350_000L, p.faltaCentavos)
        assertEquals(20_589L, p.faltaPorDiaCentavos) // 350000 / 17 = 20588,2 -> arredonda para cima
    }

    @Test
    fun faltaPorDia_divisaoExataNaoArredonda() {
        // 20/10/2026: restam 12 dias; falta 120000 -> 10000 por dia exatos
        val p = calcularProgressoMeta(200_000, 80_000, LocalDate.of(2026, 10, 20))

        assertEquals(12, p.diasRestantes)
        assertEquals(10_000L, p.faltaPorDiaCentavos)
    }

    @Test
    fun faltaPorDia_arredondaParaCimaParaNaoFicarAquemDaMeta() {
        val p = calcularProgressoMeta(100_001, 0, LocalDate.of(2026, 10, 22)) // restam 10 dias

        assertEquals(10_001L, p.faltaPorDiaCentavos)
        assertTrue(p.faltaPorDiaCentavos * p.diasRestantes >= p.faltaCentavos)
    }

    @Test
    fun primeiroEUltimoDiaDoMes() {
        assertEquals(31, calcularProgressoMeta(1_000, 0, LocalDate.of(2026, 10, 1)).diasRestantes)
        assertEquals(1, calcularProgressoMeta(1_000, 0, LocalDate.of(2026, 10, 31)).diasRestantes)
    }

    @Test
    fun mesesCurtosEBissexto() {
        assertEquals(1, calcularProgressoMeta(1_000, 0, LocalDate.of(2026, 2, 28)).diasRestantes)
        assertEquals(2, calcularProgressoMeta(1_000, 0, LocalDate.of(2028, 2, 28)).diasRestantes) // 2028 é bissexto
        assertEquals(30, calcularProgressoMeta(1_000, 0, LocalDate.of(2026, 11, 1)).diasRestantes)
    }

    @Test
    fun ultimoDia_faltaPorDiaEhTudoQueFalta() {
        val p = calcularProgressoMeta(500_000, 480_000, LocalDate.of(2026, 10, 31))

        assertEquals(20_000L, p.faltaCentavos)
        assertEquals(20_000L, p.faltaPorDiaCentavos)
    }

    @Test
    fun metaBatida_naoFaltaNada() {
        val p = calcularProgressoMeta(500_000, 500_000, LocalDate.of(2026, 10, 15))

        assertTrue(p.atingida)
        assertEquals(0L, p.faltaCentavos)
        assertEquals(0L, p.faltaPorDiaCentavos)
        assertEquals(0L, p.excedenteCentavos)
        assertEquals(100, p.percentual)
    }

    @Test
    fun metaSuperada_mostraExcedenteEPercentualAcimaDe100() {
        val p = calcularProgressoMeta(500_000, 560_000, LocalDate.of(2026, 10, 15))

        assertTrue(p.atingida)
        assertEquals(60_000L, p.excedenteCentavos)
        assertEquals(112, p.percentual)
        assertEquals(1f, p.fracao, 0.0001f) // a barra nunca passa de 100%
    }

    @Test
    fun lucroNegativo_barraZeradaEFaltaMaiorQueAMeta() {
        val p = calcularProgressoMeta(500_000, -20_000, LocalDate.of(2026, 10, 15))

        assertFalse(p.atingida)
        assertEquals(0f, p.fracao, 0.0001f)
        assertEquals(0, p.percentual)
        assertEquals(520_000L, p.faltaCentavos)
    }

    @Test
    fun percentualEFracao() {
        val p = calcularProgressoMeta(500_000, 125_000, LocalDate.of(2026, 10, 15))

        assertEquals(25, p.percentual)
        assertEquals(0.25f, p.fracao, 0.0001f)
    }

    @Test
    fun percentual_arredondaParaBaixo() {
        // 99,99% não pode aparecer como 100%
        assertEquals(99, calcularProgressoMeta(100_000, 99_999, LocalDate.of(2026, 10, 15)).percentual)
    }

    // ---- metaPorDiaDoMesCentavos ----

    @Test
    fun metaPorDiaDoMes_divideEntreOsDiasDoMes() {
        assertEquals(10_000L, metaPorDiaDoMesCentavos(310_000, YearMonth.of(2026, 10))) // 31 dias
        assertEquals(10_000L, metaPorDiaDoMesCentavos(300_000, YearMonth.of(2026, 11))) // 30 dias
        assertEquals(10_000L, metaPorDiaDoMesCentavos(280_000, YearMonth.of(2026, 2)))  // 28 dias
    }

    @Test
    fun metaPorDiaDoMes_arredondaAoCentavoMaisProximo() {
        assertEquals(16_129L, metaPorDiaDoMesCentavos(500_000, YearMonth.of(2026, 10))) // 16129,03
    }

    @Test
    fun repetir_soQuandoOMesNaoTemMetaEOAnteriorTinha() {
        assertEquals(500_000L, metaParaRepetirCentavos(null, 500_000))
        assertEquals(500_000L, metaParaRepetirCentavos(0, 500_000)) // meta zerada conta como sem meta
        assertNull(metaParaRepetirCentavos(400_000, 500_000)) // o mês já tem meta
        assertNull(metaParaRepetirCentavos(null, null)) // o anterior não tinha
        assertNull(metaParaRepetirCentavos(null, META_MAXIMA_CENTAVOS + 1)) // fora do limite
    }

    @Test
    fun metaDoDia_faltaAteOntemDivididaPelosDiasQueRestam() {
        // 21/10: restam 11 dias (contando hoje). Lucro até ontem R$ 3.000; meta R$ 5.000 → R$ 2.000 ÷ 11.
        val m = calcularMetaDoDia(500_000, lucroMesCentavos = 300_000 + 5_000, lucroHojeCentavos = 5_000, hoje = LocalDate.of(2026, 10, 21))!!
        assertEquals(18_182L, m.metaCentavos) // 18.181,8... arredondado para cima
        assertEquals(5_000L, m.lucroHojeCentavos)
        assertFalse(m.batida)
        assertEquals(13_182L, m.faltaCentavos)
    }

    @Test
    fun metaDoDia_naoMudaComOQueSeGanhaHoje() {
        val dia = LocalDate.of(2026, 10, 21)
        val cedo = calcularMetaDoDia(500_000, 300_000, 0, dia)!!
        val tarde = calcularMetaDoDia(500_000, 320_000, 20_000, dia)!!
        assertEquals(cedo.metaCentavos, tarde.metaCentavos)
        assertTrue(tarde.batida)
        assertEquals(20_000L - tarde.metaCentavos, tarde.excedenteCentavos)
    }

    @Test
    fun metaDoDia_ultimoDiaDoMes_ehTudoOQueFalta() =
        assertEquals(40_000L, calcularMetaDoDia(500_000, 460_000, 0, LocalDate.of(2026, 10, 31))!!.metaCentavos)

    @Test
    fun metaDoDia_mesJaBatidoAteOntem_ehNula() =
        assertNull(calcularMetaDoDia(500_000, 520_000, 10_000, LocalDate.of(2026, 10, 21)))

    @Test
    fun metaDoDia_prejuizoHoje_barraVazia() {
        val m = calcularMetaDoDia(500_000, 290_000, -10_000, LocalDate.of(2026, 10, 21))!!
        assertEquals(0f, m.fracao, 0f)
        assertFalse(m.batida)
    }
}
