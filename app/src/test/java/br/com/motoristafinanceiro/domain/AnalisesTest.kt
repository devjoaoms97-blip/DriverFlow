package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AnalisesTest {
    // Outubro de 2026: dia 12 é segunda, 13 terça, 16 sexta, 19 segunda.
    private val seg12 = LocalDate.of(2026, 10, 12)
    private val ter13 = LocalDate.of(2026, 10, 13)
    private val sex16 = LocalDate.of(2026, 10, 16)
    private val seg19 = LocalDate.of(2026, 10, 19)

    private fun ganho(
        data: LocalDate,
        valor: Long,
        plataforma: Plataforma = Plataforma.UBER,
        gorjeta: Long = 0,
        km: Double = 0.0,
        minutos: Int = 0,
    ) = Ganho(
        data = data, plataforma = plataforma, valorCentavos = valor,
        gorjetaBonusCentavos = gorjeta, km = km, minutosOnline = minutos,
    )

    // ---- desempenhoDe ----

    @Test
    fun desempenho_somaGanhosComGorjeta() {
        val d = desempenhoDe(
            listOf(
                ganho(seg12, 10_000, gorjeta = 500, km = 50.0, minutos = 240),
                ganho(ter13, 6_000, Plataforma.NOVE_NOVE, km = 30.0, minutos = 120),
            ),
        )

        assertEquals(16_500L, d.ganhoBrutoCentavos)
        assertEquals(2, d.lancamentos)
        assertEquals(2_750L, d.ganhoPorHoraCentavos)         // 16500 / 6 h
        assertEquals(206L, d.ganhoPorKmCentavos)             // 16500 / 80 km = 206,25
        assertEquals(8_250L, d.mediaPorLancamentoCentavos)
    }

    @Test
    fun ganhoPorHora_ignoraLancamentosSemHoras() {
        val d = desempenhoDe(listOf(ganho(seg12, 10_000, minutos = 120), ganho(ter13, 5_000, minutos = 0)))

        assertEquals(15_000L, d.ganhoBrutoCentavos)
        assertEquals(5_000L, d.ganhoPorHoraCentavos) // 10000 / 2 h, e não 15000 / 2 h
    }

    @Test
    fun ganhoPorKm_ignoraLancamentosSemKm() {
        val d = desempenhoDe(listOf(ganho(seg12, 10_000, km = 50.0), ganho(ter13, 6_000, km = 0.0)))

        assertEquals(16_000L, d.ganhoBrutoCentavos)
        assertEquals(200L, d.ganhoPorKmCentavos)
    }

    @Test
    fun semGanhos_tudoZeradoEIndicadoresNulos() {
        val d = desempenhoDe(emptyList())

        assertEquals(0L, d.ganhoBrutoCentavos)
        assertEquals(0, d.lancamentos)
        assertNull(d.ganhoPorHoraCentavos)
        assertNull(d.ganhoPorKmCentavos)
        assertNull(d.mediaPorDiaCentavos)
        assertNull(d.mediaPorLancamentoCentavos)
    }

    @Test
    fun semHorasNemKm_indicadoresNulosMasGanhoContado() {
        val d = desempenhoDe(listOf(ganho(seg12, 10_000)))

        assertEquals(10_000L, d.ganhoBrutoCentavos)
        assertNull(d.ganhoPorHoraCentavos)
        assertNull(d.ganhoPorKmCentavos)
    }

    @Test
    fun diasTrabalhados_contaDatasDiferentes() {
        val d = desempenhoDe(
            listOf(ganho(seg12, 10_000), ganho(seg12, 4_000, Plataforma.NOVE_NOVE), ganho(ter13, 6_000)),
        )

        assertEquals(3, d.lancamentos)
        assertEquals(2, d.diasTrabalhados)
        assertEquals(10_000L, d.mediaPorDiaCentavos) // 20000 / 2 dias
    }

    // ---- analisarPorDiaDaSemana ----

    @Test
    fun porDiaDaSemana_sempreSeteDiasDeSegundaADomingo() {
        val dias = analisarPorDiaDaSemana(emptyList())

        assertEquals(7, dias.size)
        assertEquals(DayOfWeek.MONDAY, dias.first().diaSemana)
        assertEquals(DayOfWeek.SUNDAY, dias.last().diaSemana)
    }

    @Test
    fun porDiaDaSemana_agrupaPeloDiaDaSemanaDaData() {
        val dias = analisarPorDiaDaSemana(
            listOf(ganho(seg12, 10_000), ganho(seg19, 20_000), ganho(sex16, 5_000)),
        ).associateBy { it.diaSemana }

        val segunda = dias.getValue(DayOfWeek.MONDAY).desempenho
        assertEquals(30_000L, segunda.ganhoBrutoCentavos)
        assertEquals(2, segunda.diasTrabalhados)
        assertEquals(5_000L, dias.getValue(DayOfWeek.FRIDAY).desempenho.ganhoBrutoCentavos)
        assertEquals(0, dias.getValue(DayOfWeek.TUESDAY).desempenho.lancamentos)
    }

    // ---- analisarPorPlataforma ----

    @Test
    fun porPlataforma_soAsQueTemGanho_daQueMaisRendeuParaAMenos() {
        val lista = analisarPorPlataforma(
            listOf(
                ganho(seg12, 10_000, Plataforma.UBER),
                ganho(ter13, 5_000, Plataforma.UBER),
                ganho(ter13, 20_000, Plataforma.NOVE_NOVE),
            ),
        )

        assertEquals(listOf(Plataforma.NOVE_NOVE, Plataforma.UBER), lista.map { it.plataforma })
        assertEquals(20_000L, lista[0].desempenho.ganhoBrutoCentavos)
        assertEquals(15_000L, lista[1].desempenho.ganhoBrutoCentavos)
        assertEquals(2, lista[1].desempenho.lancamentos)
    }

    @Test
    fun porPlataforma_comparaGanhoPorHoraEPorKm() {
        val lista = analisarPorPlataforma(
            listOf(
                ganho(seg12, 12_000, Plataforma.UBER, km = 60.0, minutos = 240),   // 3000/h, 200/km
                ganho(seg12, 9_000, Plataforma.NOVE_NOVE, km = 30.0, minutos = 120), // 4500/h, 300/km
            ),
        ).associateBy { it.plataforma }

        assertEquals(3_000L, lista.getValue(Plataforma.UBER).desempenho.ganhoPorHoraCentavos)
        assertEquals(4_500L, lista.getValue(Plataforma.NOVE_NOVE).desempenho.ganhoPorHoraCentavos)
        assertEquals(200L, lista.getValue(Plataforma.UBER).desempenho.ganhoPorKmCentavos)
        assertEquals(300L, lista.getValue(Plataforma.NOVE_NOVE).desempenho.ganhoPorKmCentavos)
    }

    @Test
    fun porPlataforma_semGanhosListaVazia() {
        assertEquals(emptyList<DesempenhoPlataforma>(), analisarPorPlataforma(emptyList()))
    }

    // ---- melhorDiaPorHora ----

    @Test
    fun melhorDia_ehOComMaiorGanhoPorHora() {
        val dias = analisarPorDiaDaSemana(
            listOf(
                ganho(seg12, 10_000, minutos = 120), // 5000/h
                ganho(sex16, 6_000, minutos = 60),   // 6000/h
            ),
        )

        assertEquals(DayOfWeek.FRIDAY, melhorDiaPorHora(dias)?.diaSemana)
    }

    @Test
    fun melhorDia_precisaDeAoMenosDoisDiasComHoras() {
        val umSo = analisarPorDiaDaSemana(listOf(ganho(seg12, 10_000, minutos = 120)))
        val outroSemHoras = analisarPorDiaDaSemana(
            listOf(ganho(seg12, 10_000, minutos = 120), ganho(sex16, 6_000, minutos = 0)),
        )

        assertNull(melhorDiaPorHora(umSo))
        assertNull(melhorDiaPorHora(outroSemHoras)) // sexta não tem horas, então não entra na comparação
        assertNull(melhorDiaPorHora(analisarPorDiaDaSemana(emptyList())))
    }

    @Test
    fun melhorDia_empateFicaComODiaQueVemPrimeiroNaSemana() {
        val dias = analisarPorDiaDaSemana(
            listOf(ganho(sex16, 6_000, minutos = 60), ganho(seg12, 6_000, minutos = 60)),
        )

        assertEquals(DayOfWeek.MONDAY, melhorDiaPorHora(dias)?.diaSemana)
    }

    // ---- Período ----

    @Test
    fun periodo_inicioContaHojeNaJanela() {
        val hoje = LocalDate.of(2026, 10, 20)

        assertEquals(LocalDate.of(2026, 9, 21), PeriodoAnalise.ULTIMOS_30.inicio(hoje)) // 21/09 a 20/10 = 30 dias
        assertEquals(LocalDate.of(2026, 7, 23), PeriodoAnalise.ULTIMOS_90.inicio(hoje)) // 23/07 a 20/10 = 90 dias
        assertEquals(LocalDate.of(2000, 1, 1), PeriodoAnalise.TUDO.inicio(hoje))
    }
}
