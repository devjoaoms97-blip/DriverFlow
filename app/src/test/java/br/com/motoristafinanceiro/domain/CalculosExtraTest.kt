package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculosExtraTest {
    private val dia = LocalDate.of(2026, 10, 12)

    @Test
    fun estatisticas_comDepreciacao_ajustaLucroECustoPorKm() {
        val ganhos = listOf(Ganho(data = dia, plataforma = Plataforma.UBER, valorCentavos = 20_000, km = 100.0, minutosOnline = 360))
        val despesas = listOf(Despesa(data = dia, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000))

        val e = calcularEstatisticas(ganhos, despesas, depreciacaoPorKmCentavos = 30)

        assertEquals(3_000L, e.depreciacaoCentavos)
        assertEquals(14_000L, e.lucroLiquidoCentavos)
        assertEquals(11_000L, e.lucroAposDepreciacaoCentavos)
        assertEquals(90L, e.custoPorKmCentavos) // (6000 + 3000) / 100 km
    }

    @Test
    fun estatisticas_gorjetaEntraNoGanhoBruto() {
        val ganhos = listOf(Ganho(data = dia, plataforma = Plataforma.UBER, valorCentavos = 10_000, gorjetaBonusCentavos = 500))
        assertEquals(10_500L, calcularEstatisticas(ganhos, emptyList()).ganhoBrutoCentavos)
    }

    @Test
    fun consumoMedio_variosIntervalos_ordenaPorOdometro() {
        val embaralhado = listOf(
            Abastecimento(data = dia, litros = 50.0, valorTotalCentavos = 1, odometroKm = 1_900),
            Abastecimento(data = dia, litros = 40.0, valorTotalCentavos = 1, odometroKm = 1_000),
            Abastecimento(data = dia, litros = 30.0, valorTotalCentavos = 1, odometroKm = 1_400),
        )
        // 400 km / 30 L + 500 km / 50 L => 900 km / 80 L = 11,25
        assertEquals(11.25, calcularConsumoMedio(embaralhado)!!, 0.0001)
    }

    @Test
    fun consumoMedio_listaVazia_retornaNulo() {
        assertNull(calcularConsumoMedio(emptyList()))
    }

    @Test
    fun kmRestantes_calculaFaltaEAtraso() {
        val oleo = Manutencao(tipo = TipoManutencao.OLEO, data = dia, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 5_000)
        assertEquals(800, kmRestantes(oleo, odometroAtual = 14_200))
        assertEquals(-500, kmRestantes(oleo, odometroAtual = 15_500))
    }

    @Test
    fun kmRestantes_semIntervalo_retornaNulo() {
        val semAlerta = Manutencao(tipo = TipoManutencao.OUTRO, data = dia, odometroKm = 10_000, valorCentavos = 0)
        assertNull(kmRestantes(semAlerta, 12_000))
    }

    @Test
    fun reservaDiaria_arredondaParaCima() {
        val r = ReservaDespesa(nome = "Seguro", valorCentavos = 10_000, periodicidade = Periodicidade.MENSAL, proximoVencimento = dia)
        assertEquals(334L, reservaDiariaCentavos(r)) // 10000 / 30 = 333,33
    }

    @Test
    fun reservaDiariaTotal_somaTodas() {
        val reservas = listOf(
            ReservaDespesa(nome = "IPVA", valorCentavos = 365_000, periodicidade = Periodicidade.ANUAL, proximoVencimento = dia),
            ReservaDespesa(nome = "Parcela", valorCentavos = 90_000, periodicidade = Periodicidade.MENSAL, proximoVencimento = dia),
        )
        assertEquals(4_000L, reservaDiariaTotalCentavos(reservas)) // 1000 + 3000
    }
}
