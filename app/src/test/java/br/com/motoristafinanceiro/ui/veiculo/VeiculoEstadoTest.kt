package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VeiculoEstadoTest {
    private val d1 = LocalDate.of(2026, 10, 1)
    private val d2 = LocalDate.of(2026, 10, 10)

    private val a1 = Abastecimento(id = 1, data = d1, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000)
    private val a2 = Abastecimento(id = 2, data = d2, litros = 30.0, valorTotalCentavos = 19_500, odometroKm = 10_400)

    @Test
    fun listaOrdenadaDoMaisRecenteParaOMaisAntigo() {
        val s = montarEstadoVeiculo(listOf(a1, a2), emptyList(), emptyList(), emptyList())
        assertEquals(listOf(2L, 1L), s.abastecimentos.map { it.id })
    }

    @Test
    fun mesmoDia_maiorOdometroPrimeiro() {
        val a3 = a2.copy(id = 3, odometroKm = 10_450)
        val s = montarEstadoVeiculo(listOf(a1, a2, a3), emptyList(), emptyList(), emptyList())
        assertEquals(listOf(3L, 2L, 1L), s.abastecimentos.map { it.id })
    }

    @Test
    fun resumo_usaGanhosEDespesasDoMesParaCustoPorKm() {
        val ganhos = listOf(Ganho(data = d2, plataforma = Plataforma.UBER, valorCentavos = 20_000, km = 100.0))
        val despesas = listOf(Despesa(data = d2, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000))

        val s = montarEstadoVeiculo(listOf(a1, a2), emptyList(), ganhos, despesas)

        assertEquals(60L, s.resumo.custoPorKmMesCentavos)
        assertEquals(10_400, s.resumo.odometroAtualKm)
        assertEquals(650L, s.resumo.precoUltimoLitroCentavos)
    }

    @Test
    fun semDados_estadoVazio() {
        val s = montarEstadoVeiculo(emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(s.abastecimentos.isEmpty())
        assertTrue(s.manutencoes.isEmpty())
        assertTrue(s.alertas.isEmpty())
        assertNull(s.resumo.consumoMedioKmPorLitro)
    }

    @Test
    fun manutencoes_maisRecentesPrimeiro() {
        val antiga = Manutencao(id = 1, tipo = TipoManutencao.OLEO, data = d1, odometroKm = 10_000, valorCentavos = 0)
        val recente = Manutencao(id = 2, tipo = TipoManutencao.PNEUS, data = d2, odometroKm = 10_300, valorCentavos = 0)

        val s = montarEstadoVeiculo(emptyList(), listOf(antiga, recente), emptyList(), emptyList())

        assertEquals(listOf(2L, 1L), s.manutencoes.map { it.id })
    }

    @Test
    fun alertas_usamOOdometroDosAbastecimentosEDasManutencoes() {
        val oleo = Manutencao(
            id = 1, tipo = TipoManutencao.OLEO, data = d1, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 400,
        )

        val s = montarEstadoVeiculo(listOf(a1, a2), listOf(oleo), emptyList(), emptyList())

        // vence em 10.400; odômetro atual (abastecimento) é 10.400 => 0 km restantes
        assertEquals(0, s.alertas.single().kmRestantes)
        assertEquals(StatusAlerta.PROXIMA, s.alertas.single().status)
    }

    @Test
    fun alertas_semOdometro_naoHaAlertas() {
        val s = montarEstadoVeiculo(emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(s.alertas.isEmpty())
    }

    @Test
    fun depreciacao_entraNoCustoPorKmDoMes() {
        val ganhos = listOf(Ganho(data = d2, plataforma = Plataforma.UBER, valorCentavos = 20_000, km = 100.0))
        val despesas = listOf(Despesa(data = d2, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000))

        val s = montarEstadoVeiculo(listOf(a1, a2), emptyList(), ganhos, despesas, depreciacaoPorKmCentavos = 25)

        assertEquals(85L, s.resumo.custoPorKmMesCentavos) // (6000 + 100 km x 25) / 100 km
        assertEquals(25L, s.depreciacaoPorKmCentavos)
    }

    @Test
    fun semDepreciacao_estadoPadraoZeroEMantemOCustoDeAntes() {
        val ganhos = listOf(Ganho(data = d2, plataforma = Plataforma.UBER, valorCentavos = 20_000, km = 100.0))
        val despesas = listOf(Despesa(data = d2, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 6_000))

        val s = montarEstadoVeiculo(listOf(a1, a2), emptyList(), ganhos, despesas)

        assertEquals(0L, s.depreciacaoPorKmCentavos)
        assertEquals(60L, s.resumo.custoPorKmMesCentavos)
    }
}
