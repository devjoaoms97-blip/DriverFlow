package br.com.motoristafinanceiro.ui.reservas

import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReservasEstadoTest {
    private val hoje = LocalDate.of(2026, 10, 17)

    private val ipva = ReservaDespesa(
        id = 1, nome = "IPVA", valorCentavos = 365_000,
        periodicidade = Periodicidade.ANUAL, proximoVencimento = LocalDate.of(2027, 1, 10),
    )
    private val seguro = ReservaDespesa(
        id = 2, nome = "Seguro", valorCentavos = 90_000,
        periodicidade = Periodicidade.MENSAL, proximoVencimento = LocalDate.of(2026, 10, 20),
    )
    private val parcela = ReservaDespesa(
        id = 3, nome = "Parcela", valorCentavos = 60_000,
        periodicidade = Periodicidade.MENSAL, proximoVencimento = LocalDate.of(2026, 10, 5), // já passou
    )

    @Test
    fun ordenaPeloVencimentoMaisProximo_eAvancaDatasVencidas() {
        val s = montarEstadoReservas(listOf(ipva, seguro, parcela), hoje)

        assertEquals(listOf("Seguro", "Parcela", "IPVA"), s.itens.map { it.reserva.nome })
        assertEquals(LocalDate.of(2026, 11, 5), s.itens[1].proximaOcorrencia) // parcela avançou um mês
        assertEquals(3L, s.itens[0].diasParaVencer)
    }

    @Test
    fun somaAReservaDiariaEASemanal() {
        val s = montarEstadoReservas(listOf(ipva, seguro, parcela), hoje)

        assertEquals(1_000L, s.itens.single { it.reserva.id == 1L }.diariaCentavos) // 365000 / 365
        assertEquals(3_000L, s.itens.single { it.reserva.id == 2L }.diariaCentavos) // 90000 / 30
        assertEquals(2_000L, s.itens.single { it.reserva.id == 3L }.diariaCentavos) // 60000 / 30
        assertEquals(6_000L, s.totalDiarioCentavos)
        assertEquals(42_000L, s.totalSemanalCentavos)
    }

    @Test
    fun semReservas_totaisZerados() {
        val s = montarEstadoReservas(emptyList(), hoje)
        assertTrue(s.itens.isEmpty())
        assertEquals(0L, s.totalDiarioCentavos)
        assertEquals(0L, s.totalSemanalCentavos)
    }
}
