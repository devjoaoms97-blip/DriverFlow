package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VeiculoTest {
    private val d1 = LocalDate.of(2026, 10, 1)
    private val d2 = LocalDate.of(2026, 10, 10)

    private val a1 = Abastecimento(id = 1, data = d1, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000)
    private val a2 = Abastecimento(id = 2, data = d2, litros = 30.0, valorTotalCentavos = 19_500, odometroKm = 10_400)

    @Test
    fun precoPorLitro_divideValorPelosLitros() {
        assertEquals(600L, precoPorLitroCentavos(a1))
        assertEquals(650L, precoPorLitroCentavos(a2))
    }

    @Test
    fun precoPorLitro_semLitros_retornaNulo() {
        assertNull(precoPorLitroCentavos(10_000, 0.0))
    }

    @Test
    fun odometroAtual_eOMaiorEntreAbastecimentosEManutencoes() {
        val manutencao = Manutencao(tipo = TipoManutencao.OLEO, data = d2, odometroKm = 10_600, valorCentavos = 0)

        assertEquals(10_400, odometroAtual(listOf(a1, a2)))
        assertEquals(10_600, odometroAtual(listOf(a1, a2), listOf(manutencao)))
        assertNull(odometroAtual(emptyList()))
    }

    @Test
    fun ultimoAbastecimento_maisRecentePorData_empateGanhaMaiorOdometro() {
        assertEquals(2L, ultimoAbastecimento(listOf(a1, a2))!!.id)

        val mesmoDiaMaisKm = a2.copy(id = 3, odometroKm = 10_450)
        assertEquals(3L, ultimoAbastecimento(listOf(a2, mesmoDiaMaisKm))!!.id)
        assertNull(ultimoAbastecimento(emptyList()))
    }

    @Test
    fun resumo_juntaConsumoOdometroPrecoECustoPorKm() {
        val mes = Estatisticas(despesasCentavos = 6_000, kmTotal = 100.0)

        val r = calcularResumoVeiculo(listOf(a1, a2), emptyList(), mes)

        assertEquals(13.3333, r.consumoMedioKmPorLitro!!, 0.001) // 400 km / 30 L
        assertEquals(10_400, r.odometroAtualKm)
        assertEquals(650L, r.precoUltimoLitroCentavos)
        assertEquals(60L, r.custoPorKmMesCentavos)
    }

    @Test
    fun resumo_semDados_tudoNulo() {
        val r = calcularResumoVeiculo(emptyList(), emptyList(), Estatisticas())

        assertNull(r.consumoMedioKmPorLitro)
        assertNull(r.odometroAtualKm)
        assertNull(r.precoUltimoLitroCentavos)
        assertNull(r.custoPorKmMesCentavos)
    }

    @Test
    fun abastecimentoValido_exigeTudoPositivo() {
        assertTrue(abastecimentoValido(40.0, 24_000, 10_000))
        assertFalse(abastecimentoValido(null, 24_000, 10_000))
        assertFalse(abastecimentoValido(0.0, 24_000, 10_000))
        assertFalse(abastecimentoValido(40.0, null, 10_000))
        assertFalse(abastecimentoValido(40.0, 0, 10_000))
        assertFalse(abastecimentoValido(40.0, 24_000, null))
        assertFalse(abastecimentoValido(40.0, 24_000, 0))
    }
}
