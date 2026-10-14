package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculosTest {
    private val hoje = LocalDate.of(2026, 10, 12)

    @Test
    fun estatisticas_calculaLucroEIndicadores() {
        val ganhos = listOf(
            Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 20_000, gorjetaBonusCentavos = 1_000, km = 100.0, minutosOnline = 360),
            Ganho(data = hoje, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 9_000, km = 50.0, minutosOnline = 180),
        )
        val despesas = listOf(Despesa(data = hoje, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000))

        val e = calcularEstatisticas(ganhos, despesas)

        assertEquals(30_000, e.ganhoBrutoCentavos)
        assertEquals(24_000, e.lucroLiquidoCentavos)
        assertEquals(200L, e.ganhoPorKmCentavos)      // R$ 2,00/km
        assertEquals(3_333L, e.ganhoPorHoraCentavos)  // 30000 / 9h
        assertEquals(40L, e.custoPorKmCentavos)       // R$ 0,40/km
    }

    @Test
    fun estatisticas_semKmNemHoras_retornaNulo() {
        val e = calcularEstatisticas(emptyList(), emptyList())
        assertNull(e.ganhoPorKmCentavos)
        assertNull(e.ganhoPorHoraCentavos)
    }

    @Test
    fun consumoMedio_metodoTanqueCheio() {
        val abastecimentos = listOf(
            Abastecimento(data = hoje, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000),
            Abastecimento(data = hoje, litros = 20.0, valorTotalCentavos = 12_000, odometroKm = 10_300, tanqueCheio = false),
            Abastecimento(data = hoje, litros = 20.0, valorTotalCentavos = 12_000, odometroKm = 10_500),
        )
        // 500 km rodados, 40 litros consumidos (20 parcial + 20 cheio) => 12,5 km/l
        assertEquals(12.5, calcularConsumoMedio(abastecimentos)!!, 0.001)
    }

    @Test
    fun consumoMedio_comUmSoAbastecimento_retornaNulo() {
        val unico = listOf(Abastecimento(data = hoje, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000))
        assertNull(calcularConsumoMedio(unico))
    }

    @Test
    fun reservaDiaria_anualDividePor365() {
        val ipva = ReservaDespesa(nome = "IPVA", valorCentavos = 365_000, periodicidade = Periodicidade.ANUAL, proximoVencimento = hoje)
        assertEquals(1_000L, reservaDiariaCentavos(ipva))
    }
}
