package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReservasTest {
    private val hoje = LocalDate.of(2026, 10, 17)

    private fun reserva(venc: LocalDate, periodicidade: Periodicidade = Periodicidade.MENSAL) =
        ReservaDespesa(nome = "Teste", valorCentavos = 30_000, periodicidade = periodicidade, proximoVencimento = venc)

    @Test
    fun mesesDoPeriodo_porPeriodicidade() {
        assertEquals(1, mesesDoPeriodo(Periodicidade.MENSAL))
        assertEquals(3, mesesDoPeriodo(Periodicidade.TRIMESTRAL))
        assertEquals(6, mesesDoPeriodo(Periodicidade.SEMESTRAL))
        assertEquals(12, mesesDoPeriodo(Periodicidade.ANUAL))
    }

    @Test
    fun proximaOcorrencia_dataFuturaOuHoje_ficaComoCadastrada() {
        val futura = LocalDate.of(2026, 12, 1)
        assertEquals(futura, proximaOcorrencia(reserva(futura), hoje))
        assertEquals(hoje, proximaOcorrencia(reserva(hoje), hoje))
    }

    @Test
    fun proximaOcorrencia_mensalVencida_avancaMesAMes() {
        // 15/08 -> 15/09 -> 15/10 (já passou, hoje é 17/10) -> 15/11
        assertEquals(LocalDate.of(2026, 11, 15), proximaOcorrencia(reserva(LocalDate.of(2026, 8, 15)), hoje))
    }

    @Test
    fun proximaOcorrencia_fimDeMes_naoEscorregaODia() {
        val trinta1 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), proximaOcorrencia(reserva(trinta1), LocalDate.of(2026, 2, 10)))
        // contando a partir de 31/01: +2 meses é 31/03 (e não 28/03)
        assertEquals(LocalDate.of(2026, 3, 31), proximaOcorrencia(reserva(trinta1), LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun proximaOcorrencia_anualVencida_vaiParaOProximoAno() {
        val r = reserva(LocalDate.of(2025, 3, 10), Periodicidade.ANUAL)
        assertEquals(LocalDate.of(2027, 3, 10), proximaOcorrencia(r, hoje))
    }

    @Test
    fun proximaOcorrencia_trimestral() {
        val r = reserva(LocalDate.of(2026, 1, 20), Periodicidade.TRIMESTRAL)
        // 20/01 -> 20/04 -> 20/07 -> 20/10 (ainda não passou: hoje é 17/10)
        assertEquals(LocalDate.of(2026, 10, 20), proximaOcorrencia(r, hoje))
    }

    @Test
    fun diasParaVencer_contaDeHojeAteAProximaOcorrencia() {
        assertEquals(3L, diasParaVencer(reserva(LocalDate.of(2026, 10, 20)), hoje))
        assertEquals(0L, diasParaVencer(reserva(hoje), hoje))
        assertEquals(29L, diasParaVencer(reserva(LocalDate.of(2026, 8, 15)), hoje)) // próxima: 15/11
    }

    @Test
    fun reservaValida_exigeNomeEValorPositivo() {
        assertTrue(reservaValida("IPVA", 365_000))
        assertFalse(reservaValida("", 365_000))
        assertFalse(reservaValida("   ", 365_000))
        assertFalse(reservaValida("IPVA", null))
        assertFalse(reservaValida("IPVA", 0))
        assertFalse(reservaValida("IPVA", -1))
    }

    @Test
    fun modelos_incluemIpvaAnualEParcelaMensal() {
        assertEquals(Periodicidade.ANUAL, MODELOS_DE_RESERVA.single { it.nome == "IPVA" }.periodicidade)
        assertEquals(Periodicidade.MENSAL, MODELOS_DE_RESERVA.single { it.nome == "Parcela do carro" }.periodicidade)
    }
}
