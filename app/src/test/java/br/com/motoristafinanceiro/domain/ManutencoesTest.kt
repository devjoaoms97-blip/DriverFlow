package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ManutencoesTest {
    private val dia = LocalDate.of(2026, 10, 12)

    private fun manut(
        tipo: TipoManutencao,
        odometro: Int,
        intervalo: Int? = null,
        obs: String? = null,
        data: LocalDate = dia,
    ) = Manutencao(tipo = tipo, data = data, odometroKm = odometro, valorCentavos = 0, intervaloKm = intervalo, observacao = obs)

    @Test
    fun status_fronteiras() {
        assertEquals(StatusAlerta.ATRASADA, statusAlerta(-1))
        assertEquals(StatusAlerta.PROXIMA, statusAlerta(0))
        assertEquals(StatusAlerta.PROXIMA, statusAlerta(LIMIAR_PROXIMA_KM))
        assertEquals(StatusAlerta.EM_DIA, statusAlerta(LIMIAR_PROXIMA_KM + 1))
    }

    @Test
    fun alertas_calculaKmRestantesEOrdenaPorUrgencia() {
        val lista = listOf(
            manut(TipoManutencao.PNEUS, odometro = 5_000, intervalo = 40_000),  // vence em 45.000
            manut(TipoManutencao.OLEO, odometro = 10_000, intervalo = 10_000),  // vence em 20.000
        )

        val alertas = calcularAlertas(lista, odometroAtualKm = 19_700)

        assertEquals(listOf(TipoManutencao.OLEO, TipoManutencao.PNEUS), alertas.map { it.manutencao.tipo })
        assertEquals(300, alertas[0].kmRestantes)
        assertEquals(StatusAlerta.PROXIMA, alertas[0].status)
        assertEquals(25_300, alertas[1].kmRestantes)
        assertEquals(StatusAlerta.EM_DIA, alertas[1].status)
    }

    @Test
    fun alertas_atrasada() {
        val alertas = calcularAlertas(listOf(manut(TipoManutencao.OLEO, 10_000, 5_000)), 15_500)

        assertEquals(-500, alertas.single().kmRestantes)
        assertEquals(StatusAlerta.ATRASADA, alertas.single().status)
    }

    @Test
    fun alertas_usaSoORegistroMaisRecenteDeCadaServico() {
        val lista = listOf(
            manut(TipoManutencao.OLEO, odometro = 1_000, intervalo = 10_000),
            manut(TipoManutencao.OLEO, odometro = 12_000, intervalo = 10_000),
        )

        val alertas = calcularAlertas(lista, 15_000)

        assertEquals(1, alertas.size)
        assertEquals(7_000, alertas[0].kmRestantes) // 12.000 + 10.000 - 15.000
    }

    @Test
    fun alertas_registroMaisRecenteSemIntervalo_naoGeraAlerta() {
        val lista = listOf(
            manut(TipoManutencao.OLEO, odometro = 1_000, intervalo = 10_000),
            manut(TipoManutencao.OLEO, odometro = 12_000, intervalo = null),
        )

        assertTrue(calcularAlertas(lista, 15_000).isEmpty())
    }

    @Test
    fun alertas_outro_agrupaPorDescricaoSemDiferenciarMaiusculas() {
        val lista = listOf(
            manut(TipoManutencao.OUTRO, 10_000, 20_000, obs = "Filtro de ar"),
            manut(TipoManutencao.OUTRO, 12_000, 20_000, obs = "filtro de ar "),
            manut(TipoManutencao.OUTRO, 11_000, 30_000, obs = "Bateria"),
        )

        val alertas = calcularAlertas(lista, 15_000)

        assertEquals(2, alertas.size)
        assertEquals(setOf("Filtro de ar", "Bateria").map { it.lowercase() }.toSet(),
            alertas.map { descricaoManutencao(it.manutencao).trim().lowercase() }.toSet())
    }

    @Test
    fun alertas_semManutencoes_listaVazia() {
        assertTrue(calcularAlertas(emptyList(), 10_000).isEmpty())
    }

    @Test
    fun descricao_outroUsaObservacao_demaisUsamOTipo() {
        assertEquals("Bateria", descricaoManutencao(manut(TipoManutencao.OUTRO, 1, obs = " Bateria ")))
        assertEquals("Outro", descricaoManutencao(manut(TipoManutencao.OUTRO, 1)))
        assertEquals("Troca de óleo", descricaoManutencao(manut(TipoManutencao.OLEO, 1, obs = "Sintético")))
    }

    @Test
    fun intervaloSugerido_porTipo() {
        assertEquals(10_000, intervaloSugeridoKm(TipoManutencao.OLEO))
        assertEquals(40_000, intervaloSugeridoKm(TipoManutencao.PNEUS))
        assertNull(intervaloSugeridoKm(TipoManutencao.OUTRO))
    }

    @Test
    fun intervalo_vazioEhValido_preenchidoPrecisaSerPositivo() {
        assertTrue(intervaloValido(""))
        assertTrue(intervaloValido("  "))
        assertTrue(intervaloValido("10000"))
        assertFalse(intervaloValido("0"))
        assertFalse(intervaloValido("-5"))
        assertFalse(intervaloValido("abc"))
        assertEquals(10_000, parseIntervaloKm(" 10000 "))
        assertNull(parseIntervaloKm(""))
    }

    @Test
    fun manutencaoValida_exigeOdometroEValorNaoNegativo() {
        assertTrue(manutencaoValida(10_000, 25_000, "10000"))
        assertTrue(manutencaoValida(10_000, 0, "")) // garantia
        assertFalse(manutencaoValida(null, 25_000, ""))
        assertFalse(manutencaoValida(0, 25_000, ""))
        assertFalse(manutencaoValida(10_000, null, ""))
        assertFalse(manutencaoValida(10_000, -1, ""))
        assertFalse(manutencaoValida(10_000, 25_000, "abc"))
    }
}
