package br.com.motoristafinanceiro.ui.analises

import br.com.motoristafinanceiro.domain.Desempenho
import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.domain.DesempenhoFaixa
import br.com.motoristafinanceiro.domain.FaixaHorario
import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class TextoAnalisesTest {

    @Test
    fun rotulosDosDiasDaSemana() {
        assertEquals(
            listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"),
            DayOfWeek.values().map { rotuloDiaSemana(it) },
        )
    }

    @Test
    fun textoDias_singularEPlural() {
        assertEquals("1 dia", textoDias(1))
        assertEquals("5 dias", textoDias(5))
    }

    @Test
    fun desempenhoDia_semLancamentos() {
        assertEquals("Sem lançamentos", textoDesempenhoDia(DesempenhoDia(DayOfWeek.MONDAY, Desempenho())))
    }

    @Test
    fun desempenhoDia_comHoras() {
        val d = Desempenho(
            ganhoBrutoCentavos = 90_000, lancamentos = 3, diasTrabalhados = 3,
            ganhoComHorasCentavos = 90_000, minutosOnline = 1_800, // 30 h -> R$ 30,00/h
        )

        assertEquals(
            "${3_000L.formatarReais()}/h · 3 dias · média ${30_000L.formatarReais()}/dia",
            textoDesempenhoDia(DesempenhoDia(DayOfWeek.FRIDAY, d)),
        )
    }

    @Test
    fun desempenhoDia_semHorasInformadas() {
        val d = Desempenho(ganhoBrutoCentavos = 10_000, lancamentos = 1, diasTrabalhados = 1)

        assertEquals(
            "sem horas · 1 dia · média ${10_000L.formatarReais()}/dia",
            textoDesempenhoDia(DesempenhoDia(DayOfWeek.TUESDAY, d)),
        )
    }

    @Test
    fun melhorDia() {
        val d = Desempenho(
            ganhoBrutoCentavos = 6_000, lancamentos = 1, diasTrabalhados = 1,
            ganhoComHorasCentavos = 6_000, minutosOnline = 60,
        )

        assertEquals(
            "Melhor dia por hora: Sexta (${6_000L.formatarReais()}/h)",
            textoMelhorDia(DesempenhoDia(DayOfWeek.FRIDAY, d)),
        )
    }

    @Test
    fun fracaoDaBarra_proporcionalAoMaior() {
        assertEquals(0.5f, fracaoDaBarra(50, 100), 0.0001f)
        assertEquals(1f, fracaoDaBarra(100, 100), 0.0001f)
    }

    @Test
    fun fracaoDaBarra_semDadoOuSemMaiorFicaZerada() {
        assertEquals(0f, fracaoDaBarra(null, 100), 0.0001f)
        assertEquals(0f, fracaoDaBarra(50, null), 0.0001f)
        assertEquals(0f, fracaoDaBarra(50, 0), 0.0001f)
        assertEquals(0f, fracaoDaBarra(0, 100), 0.0001f)
    }

    @Test
    fun siglasDosDias_tresLetras() {
        assertEquals(
            listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"),
            DayOfWeek.values().map { siglaDiaSemana(it) },
        )
    }

    @Test
    fun rotuloDaBarra_reaisInteirosArredondados() {
        assertEquals("38", rotuloBarraPorHora(3_750))
        assertEquals("37", rotuloBarraPorHora(3_749))
        assertEquals("", rotuloBarraPorHora(null))
        assertEquals("", rotuloBarraPorHora(0))
    }

    @Test
    fun percentuais_somamCem() {
        assertEquals(listOf(68, 32), percentuais(listOf(296_000, 142_000)))
        assertEquals(listOf(34, 33, 33), percentuais(listOf(1, 1, 1)))
        assertEquals(listOf(100), percentuais(listOf(500)))
        assertEquals(100, percentuais(listOf(7, 11, 13, 17)).sum())
    }

    @Test
    fun percentuais_totalZero_tudoZero() = assertEquals(listOf(0, 0), percentuais(listOf(0, 0)))

    @Test
    fun intervaloDasFaixas() {
        assertEquals(
            listOf("00h–06h", "06h–12h", "12h–18h", "18h–24h"),
            FaixaHorario.entries.map { intervaloFaixa(it) },
        )
    }

    @Test
    fun textoDaFaixa_porHoraETurnos() {
        assertEquals("${3_200L.formatarReais()}/h · 5 turnos", textoFaixa(DesempenhoFaixa(FaixaHorario.NOITE, 16_000, 300, 5)))
        assertEquals("${3_000L.formatarReais()}/h · 1 turno", textoFaixa(DesempenhoFaixa(FaixaHorario.TARDE, 3_000, 60, 1)))
        assertEquals("Sem turnos", textoFaixa(DesempenhoFaixa(FaixaHorario.MADRUGADA)))
    }

    @Test
    fun melhorFaixa_eAvisoDosSemHorario() {
        assertEquals(
            "Melhor horário: noite (${3_200L.formatarReais()}/h)",
            textoMelhorFaixa(DesempenhoFaixa(FaixaHorario.NOITE, 16_000, 300, 5)),
        )
        assertEquals("", textoSemHorario(0))
        assertEquals("1 lançamento sem horário de início (ou sem horas) não entra aqui.", textoSemHorario(1))
        assertEquals("3 lançamentos sem horário de início (ou sem horas) não entram aqui.", textoSemHorario(3))
    }
}
