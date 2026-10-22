package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OdometroTest {
    private val d10 = LocalDate.of(2026, 10, 10)
    private val d12 = LocalDate.of(2026, 10, 12)

    private fun abastecimento(data: LocalDate, km: Int) =
        Abastecimento(data = data, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = km)

    private fun ganho(data: LocalDate, km: Double) =
        Ganho(data = data, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = km)

    @Test
    fun semLeitura_ehNulo() {
        assertNull(ultimaLeitura(emptyList(), emptyList()))
        assertNull(odometroEstimado(emptyList(), emptyList(), listOf(ganho(d12, 100.0))))
    }

    @Test
    fun ultimaLeitura_maiorOdometroEntreAbastecimentosEManutencoes() {
        val manutencao = Manutencao(tipo = TipoManutencao.OLEO, data = d12, odometroKm = 10_600, valorCentavos = 0)
        assertEquals(
            LeituraOdometro(10_600, d12),
            ultimaLeitura(listOf(abastecimento(d10, 10_400)), listOf(manutencao)),
        )
    }

    @Test
    fun ultimaLeitura_empateNoKm_ficaComAMaisRecente() {
        assertEquals(
            LeituraOdometro(10_400, d12),
            ultimaLeitura(listOf(abastecimento(d10, 10_400), abastecimento(d12, 10_400)), emptyList()),
        )
    }

    @Test
    fun somaSoOsKmDosGanhosDosDiasSeguintesALeitura() {
        val o = odometroEstimado(
            listOf(abastecimento(d10, 10_000)),
            emptyList(),
            listOf(
                ganho(d10.minusDays(1), 500.0), // antes da leitura: já está no odômetro
                ganho(d10, 80.0), // mesmo dia: não dá para saber se foi antes ou depois
                ganho(d12, 120.4),
                ganho(d12.plusDays(1), 99.8),
            ),
        )!!
        assertEquals(10_220, o.km) // 120,4 + 99,8 = 220,2 → 220
        assertEquals(LeituraOdometro(10_000, d10), o.leitura)
        assertEquals(220, o.kmEstimados)
        assertTrue(o.estimado)
    }

    @Test
    fun semGanhosDepois_naoEhEstimado() {
        val o = odometroEstimado(listOf(abastecimento(d10, 10_000)), emptyList(), listOf(ganho(d12, 0.0)))!!
        assertEquals(10_000, o.km)
        assertFalse(o.estimado)
    }

    @Test
    fun alerta_andaComOsKmDosGanhos() {
        val oleo = Manutencao(tipo = TipoManutencao.OLEO, data = d10, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 1_000)
        val o = odometroEstimado(emptyList(), listOf(oleo), listOf(ganho(d12, 700.0)))!!
        val alerta = calcularAlertas(listOf(oleo), o.km).single()
        assertEquals(300, alerta.kmRestantes)
        assertEquals(StatusAlerta.PROXIMA, alerta.status)
    }
}
