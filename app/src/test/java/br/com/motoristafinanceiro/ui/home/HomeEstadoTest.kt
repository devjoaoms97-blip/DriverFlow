package br.com.motoristafinanceiro.ui.home

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeEstadoTest {
    private val hoje = LocalDate.of(2026, 10, 15)
    private val ontem = hoje.minusDays(1)

    private fun montar(
        ganhos: List<Ganho> = emptyList(),
        despesas: List<Despesa> = emptyList(),
        meta: Meta? = null,
        abastecimentos: List<Abastecimento> = emptyList(),
        manutencoes: List<Manutencao> = emptyList(),
        depreciacaoPorKmCentavos: Long = 0,
    ) = montarEstadoHome(ganhos, despesas, meta, abastecimentos, manutencoes, hoje, depreciacaoPorKmCentavos)

    @Test
    fun hojeFiltraSoODiaEMesSomaTudo() {
        val ganhos = listOf(
            Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000),
            Ganho(data = ontem, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 5_000),
        )
        val despesas = listOf(
            Despesa(data = hoje, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 3_000),
            Despesa(data = ontem, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 1_000),
        )

        val s = montar(ganhos, despesas)

        assertEquals(7_000L, s.hoje.lucroLiquidoCentavos)
        assertEquals(11_000L, s.mes.lucroLiquidoCentavos) // 15000 - 4000
    }

    @Test
    fun meta_passaOValorOuNulo() {
        assertEquals(500_000L, montar(meta = Meta("2026-10", 500_000)).metaCentavos)
        assertNull(montar(meta = null).metaCentavos)
    }

    @Test
    fun alertasUrgentes_soAtrasadasEProximas() {
        val oleo = Manutencao(tipo = TipoManutencao.OLEO, data = ontem, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 5_000)
        val pneus = Manutencao(tipo = TipoManutencao.PNEUS, data = ontem, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 40_000)
        val abastecimento = Abastecimento(data = hoje, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 15_200)

        val s = montar(abastecimentos = listOf(abastecimento), manutencoes = listOf(oleo, pneus))

        assertEquals(1, s.alertasUrgentes.size) // pneus está em dia e não aparece
        assertEquals(TipoManutencao.OLEO, s.alertasUrgentes[0].manutencao.tipo)
        assertEquals(StatusAlerta.ATRASADA, s.alertasUrgentes[0].status)
        assertEquals(-200, s.alertasUrgentes[0].kmRestantes)
    }

    @Test
    fun semDados_semAlertas() {
        assertTrue(montar().alertasUrgentes.isEmpty())
    }

    @Test
    fun depreciacao_eAplicadaEmHojeENoMes() {
        val ganhos = listOf(
            Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = 100.0),
            Ganho(data = ontem, plataforma = Plataforma.UBER, valorCentavos = 5_000, km = 50.0),
        )

        val s = montar(ganhos = ganhos, depreciacaoPorKmCentavos = 30)

        assertEquals(3_000L, s.hoje.depreciacaoCentavos)   // 100 km x 30
        assertEquals(4_500L, s.mes.depreciacaoCentavos)    // 150 km x 30
        assertEquals(7_000L, s.hoje.lucroAposDepreciacaoCentavos) // 10000 - 3000
        assertEquals(30L, s.hoje.custoPorKmCentavos)       // 3000 / 100 km
    }

    @Test
    fun depreciacao_naoMudaOLucroLiquido() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = 100.0))
        val despesas = listOf(Despesa(data = hoje, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 2_000))

        val com = montar(ganhos, despesas, depreciacaoPorKmCentavos = 30)
        val sem = montar(ganhos, despesas)

        assertEquals(sem.hoje.lucroLiquidoCentavos, com.hoje.lucroLiquidoCentavos)
        assertEquals(8_000L, com.hoje.lucroLiquidoCentavos)
        assertEquals(5_000L, com.hoje.lucroAposDepreciacaoCentavos)
    }

    @Test
    fun semDepreciacao_ficaZero() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = 100.0))
        assertEquals(0L, montar(ganhos = ganhos).mes.depreciacaoCentavos)
    }
}
