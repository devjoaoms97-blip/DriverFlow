package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class KmPessoalECorridasTest {
    private val d1 = LocalDate.of(2026, 10, 1)

    private fun ganho(
        valor: Long = 10_000,
        km: Double = 0.0,
        minutos: Int = 0,
        corridas: Int? = null,
        data: LocalDate = d1,
    ) = Ganho(data = data, plataforma = Plataforma.UBER, valorCentavos = valor, km = km, minutosOnline = minutos, corridas = corridas)

    private fun despesa(valor: Long) = Despesa(data = d1, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = valor)

    private fun abastecimento(data: LocalDate, km: Int) =
        Abastecimento(data = data, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = km)

    // ---- Corridas ----

    @Test
    fun corridas_soContamOsGanhosQueInformam() {
        val r = resumoCorridas(listOf(ganho(valor = 20_000, minutos = 300, corridas = 10), ganho(valor = 50_000)))
        assertEquals(10, r.corridas)
        assertEquals(2_000L, r.ganhoPorCorridaCentavos) // o ganho sem corridas não entra na divisão
        assertEquals(30.0, r.minutosPorCorrida!!, 0.001)
    }

    @Test
    fun corridas_tempoMedioSoComHoras() {
        val r = resumoCorridas(listOf(ganho(valor = 10_000, minutos = 120, corridas = 4), ganho(valor = 10_000, corridas = 6)))
        assertEquals(10, r.corridas)
        assertEquals(2_000L, r.ganhoPorCorridaCentavos)
        assertEquals(30.0, r.minutosPorCorrida!!, 0.001) // 120 min ÷ as 4 corridas que têm horas
    }

    @Test
    fun corridas_semNenhuma_ehNulo() {
        val r = resumoCorridas(listOf(ganho(corridas = 0), ganho()))
        assertEquals(0, r.corridas)
        assertNull(r.ganhoPorCorridaCentavos)
        assertNull(r.minutosPorCorrida)
    }

    @Test
    fun estatisticasEDesempenho_trazemAsCorridas() {
        val ganhos = listOf(ganho(valor = 30_000, corridas = 12))
        assertEquals(2_500L, calcularEstatisticas(ganhos, emptyList()).corridas.ganhoPorCorridaCentavos)
        assertEquals(2_500L, desempenhoDe(ganhos).corridas.ganhoPorCorridaCentavos)
    }

    // ---- Km pessoal no custo por km ----

    @Test
    fun kmPessoal_entraNoDivisorDoCustoPorKm() {
        val e = calcularEstatisticas(
            listOf(ganho(km = 300.0)),
            listOf(despesa(40_000)),
            kmPessoais = listOf(KmPessoal(data = d1, km = 100.0)),
        )
        assertEquals(100L, e.custoPorKmCentavos) // R$ 400 ÷ 400 km
        assertEquals(10_000L, e.custoUsoPessoalCentavos) // 100 km × R$ 1,00
        assertEquals(400.0, e.kmDoCarro, 0.0)
        assertEquals(300.0, e.kmTotal, 0.0) // ganho por km continua só com os km de trabalho
    }

    @Test
    fun kmPessoal_depreciacaoEntraNoCustoMasNaoNoLucro() {
        val e = calcularEstatisticas(
            listOf(ganho(valor = 50_000, km = 100.0)),
            listOf(despesa(10_000)),
            depreciacaoPorKmCentavos = 20,
            kmPessoais = listOf(KmPessoal(data = d1, km = 100.0)),
        )
        assertEquals(2_000L, e.depreciacaoCentavos) // só os 100 km de trabalho
        assertEquals(2_000L, e.depreciacaoPessoalCentavos)
        assertEquals(38_000L, e.lucroAposDepreciacaoCentavos)
        assertEquals(70L, e.custoPorKmCentavos) // (100 + 20 + 20) ÷ 200 km
    }

    @Test
    fun semKmPessoal_custoPorKmNaoMuda_eCustoPessoalEhNulo() {
        val e = calcularEstatisticas(listOf(ganho(km = 200.0)), listOf(despesa(20_000)))
        assertEquals(100L, e.custoPorKmCentavos)
        assertNull(e.custoUsoPessoalCentavos)
    }

    // ---- Odômetro ----

    @Test
    fun odometroEstimado_somaKmPessoaisDepoisDaLeitura() {
        val o = odometroEstimado(
            listOf(abastecimento(d1, 10_000)),
            emptyList(),
            listOf(ganho(km = 100.0, data = d1.plusDays(1))),
            listOf(KmPessoal(data = d1, km = 30.0), KmPessoal(data = d1.plusDays(2), km = 25.0)),
        )!!
        assertEquals(10_125, o.km) // o pessoal do dia da leitura fica de fora
    }

    // ---- Conferência ----

    @Test
    fun conferencia_usaAMaisAntigaDos90DiasEOsLancadosDepoisDela() {
        val fim = d1.plusDays(60)
        val c = conferirKm(
            listOf(abastecimento(d1.minusDays(100), 8_000), abastecimento(d1, 10_000), abastecimento(fim, 12_000)),
            listOf(Manutencao(tipo = TipoManutencao.OLEO, data = d1.plusDays(30), odometroKm = 11_000, valorCentavos = 0)),
            listOf(ganho(km = 999.0, data = d1), ganho(km = 1_500.0, data = d1.plusDays(10)), ganho(km = 200.0, data = fim)),
            listOf(KmPessoal(data = d1.plusDays(20), km = 100.0)),
        )!!
        assertEquals(LeituraOdometro(10_000, d1), c.inicio) // a de 100 dias antes fica fora da janela
        assertEquals(2_000, c.kmReais)
        assertEquals(1_700.0, c.kmTrabalho, 0.0) // o ganho do dia da leitura de partida fica de fora
        assertEquals(100.0, c.kmPessoal, 0.0)
        assertEquals(200, c.kmSemLancamento)
        assertEquals(90, c.percentualLancado)
    }

    @Test
    fun conferencia_lancouMaisQueOOdometro_diferencaNegativa() {
        val c = conferirKm(
            listOf(abastecimento(d1, 10_000), abastecimento(d1.plusDays(7), 10_500)),
            emptyList(),
            listOf(ganho(km = 600.0, data = d1.plusDays(3))),
            emptyList(),
        )!!
        assertEquals(-100, c.kmSemLancamento)
        assertEquals(120, c.percentualLancado)
    }

    @Test
    fun conferencia_semDuasLeiturasOuComPoucoKm_ehNula() {
        assertNull(conferirKm(listOf(abastecimento(d1, 10_000)), emptyList(), emptyList(), emptyList()))
        // Duas leituras no mesmo dia não formam intervalo.
        assertNull(conferirKm(listOf(abastecimento(d1, 10_000), abastecimento(d1, 10_400)), emptyList(), emptyList(), emptyList()))
        // Menos de 100 km entre elas.
        assertNull(
            conferirKm(listOf(abastecimento(d1, 10_000), abastecimento(d1.plusDays(2), 10_080)), emptyList(), emptyList(), emptyList())
        )
    }
}
