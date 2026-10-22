package br.com.motoristafinanceiro.ui.home

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.LeituraOdometro
import br.com.motoristafinanceiro.domain.Odometro
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

    // ---- Meta mensal: progresso e quanto falta por dia ----

    @Test
    fun semMeta_semProgresso() {
        assertNull(montar().progressoMeta)
        assertNull(montar(meta = Meta("2026-10", 0)).progressoMeta) // meta zerada equivale a sem meta
    }

    @Test
    fun meta_progressoUsaLucroLiquidoDoMesEDiasRestantes() {
        val ganhos = listOf(
            Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 200_000),
            Ganho(data = ontem, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 100_000),
        )
        val despesas = listOf(Despesa(data = ontem, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 50_000))

        val p = montar(ganhos, despesas, meta = Meta("2026-10", 500_000)).progressoMeta!!

        assertEquals(250_000L, p.lucroCentavos) // 300000 - 50000
        assertEquals(250_000L, p.faltaCentavos)
        assertEquals(17, p.diasRestantes)       // 15/10: dias 15 a 31
        assertEquals(14_706L, p.faltaPorDiaCentavos) // 250000 / 17 = 14705,9
    }

    @Test
    fun meta_depreciacaoNaoEntraNoProgresso() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 100_000, km = 100.0))

        val com = montar(ganhos, meta = Meta("2026-10", 500_000), depreciacaoPorKmCentavos = 30).progressoMeta!!
        val sem = montar(ganhos, meta = Meta("2026-10", 500_000)).progressoMeta!!

        assertEquals(sem.lucroCentavos, com.lucroCentavos)
    }

    @Test
    fun meta_batida() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 600_000))

        val p = montar(ganhos, meta = Meta("2026-10", 500_000)).progressoMeta!!

        assertTrue(p.atingida)
        assertEquals(0L, p.faltaPorDiaCentavos)
    }

    // ---- Semana (segunda a domingo) ----
    // hoje = 15/10/2026 (quinta): a semana vai de 12/10 (segunda) a 18/10 (domingo)

    @Test
    fun semana_somaSoOsDiasDaSemana() {
        val ganhos = listOf(
            Ganho(data = LocalDate.of(2026, 10, 11), plataforma = Plataforma.UBER, valorCentavos = 90_000), // domingo anterior
            Ganho(data = LocalDate.of(2026, 10, 12), plataforma = Plataforma.UBER, valorCentavos = 10_000), // segunda
            Ganho(data = hoje, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 5_000),
        )
        val despesas = listOf(Despesa(data = ontem, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 2_000))

        val s = montar(ganhos, despesas)
        val comSemana = montarEstadoHome(
            ganhos, despesas, null, emptyList(), emptyList(), hoje,
            ganhosSemana = ganhos, despesasSemana = despesas,
        )

        assertEquals(13_000L, comSemana.semana.lucroLiquidoCentavos) // 10000 + 5000 - 2000; o domingo anterior fica de fora
        assertEquals(0L, s.semana.lucroLiquidoCentavos)               // sem dados de semana, vem zerado
        assertEquals("12/10 a 18/10", comSemana.rotuloSemana)
    }

    @Test
    fun semana_incluiDiasDoMesAnteriorQuandoAVirada() {
        val hojeNoInicioDoMes = LocalDate.of(2026, 10, 2) // sexta; semana de 28/09 a 04/10
        val ganhosDoMes = listOf(Ganho(data = hojeNoInicioDoMes, plataforma = Plataforma.UBER, valorCentavos = 4_000))
        val ganhosDaSemana = listOf(
            Ganho(data = LocalDate.of(2026, 9, 29), plataforma = Plataforma.UBER, valorCentavos = 6_000),
            Ganho(data = hojeNoInicioDoMes, plataforma = Plataforma.UBER, valorCentavos = 4_000),
        )

        val s = montarEstadoHome(
            ganhosDoMes, emptyList(), null, emptyList(), emptyList(), hojeNoInicioDoMes,
            ganhosSemana = ganhosDaSemana,
        )

        assertEquals(10_000L, s.semana.lucroLiquidoCentavos) // 6000 de setembro + 4000 de outubro
        assertEquals(4_000L, s.mes.lucroLiquidoCentavos)     // o mês continua só com outubro
    }

    @Test
    fun semana_aplicaDepreciacaoPorKm() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = 100.0))

        val s = montarEstadoHome(
            ganhos, emptyList(), null, emptyList(), emptyList(), hoje,
            depreciacaoPorKmCentavos = 30, ganhosSemana = ganhos,
        )

        assertEquals(3_000L, s.semana.depreciacaoCentavos)
        assertEquals(10_000L, s.semana.lucroLiquidoCentavos)
    }

    @Test
    fun reservas_totalDiarioEProximaParaOAtalhoDoInicio() {
        val seguro = ReservaDespesa(
            id = 1, nome = "Seguro", valorCentavos = 90_000,
            periodicidade = Periodicidade.MENSAL, proximoVencimento = hoje.plusDays(3),
        )
        val ipva = ReservaDespesa(
            id = 2, nome = "IPVA", valorCentavos = 365_000,
            periodicidade = Periodicidade.ANUAL, proximoVencimento = hoje.plusDays(90),
        )

        val s = montarEstadoHome(
            emptyList(), emptyList(), null, emptyList(), emptyList(), hoje,
            reservas = listOf(ipva, seguro),
        )

        assertEquals(4_000L, s.reservas.totalDiarioCentavos) // 3.000 (seguro) + 1.000 (IPVA)
        assertEquals("Seguro", s.reservas.itens.first().reserva.nome)
    }

    @Test
    fun reservas_semCadastro_estadoVazio() {
        val s = montar()
        assertTrue(s.reservas.itens.isEmpty())
        assertEquals(0L, s.reservas.totalDiarioCentavos)
    }

    @Test
    fun alertas_usamOOdometroEstimadoQuandoHa() {
        val oleo = Manutencao(tipo = TipoManutencao.OLEO, data = ontem, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 5_000)

        val semEstimativa = montar(manutencoes = listOf(oleo))
        val comEstimativa = montarEstadoHome(
            emptyList(), emptyList(), null, emptyList(), listOf(oleo), hoje,
            odometroEstimado = Odometro(14_800, LeituraOdometro(10_000, ontem)),
        )

        assertTrue(semEstimativa.alertasUrgentes.isEmpty()) // 10.000 km: faltam 5.000
        assertEquals(StatusAlerta.PROXIMA, comEstimativa.alertasUrgentes.single().status) // faltam 200
    }

    @Test
    fun metaParaRepetir_soSemMetaNoMesEComMetaNoAnterior() {
        val anterior = Meta("2026-09", 500_000)

        val semMeta = montarEstadoHome(emptyList(), emptyList(), null, emptyList(), emptyList(), hoje, metaMesAnterior = anterior)
        val comMeta = montarEstadoHome(
            emptyList(), emptyList(), Meta("2026-10", 400_000), emptyList(), emptyList(), hoje, metaMesAnterior = anterior,
        )

        assertEquals(500_000L, semMeta.metaParaRepetirCentavos)
        assertNull(comMeta.metaParaRepetirCentavos)
        assertNull(montar().metaParaRepetirCentavos)
    }

    @Test
    fun avisoBackup_comDadosESemBackup_eSomeDepoisDoBackup() {
        val ganhos = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 10_000))

        val semBackup = montarEstadoHome(ganhos, emptyList(), null, emptyList(), emptyList(), hoje)
        val comBackup = montarEstadoHome(ganhos, emptyList(), null, emptyList(), emptyList(), hoje, ultimoBackup = hoje)

        assertEquals("Você ainda não fez backup", semBackup.avisoBackup?.titulo)
        assertNull(comBackup.avisoBackup)
        assertNull(montar().avisoBackup) // sem nada lançado, nada a guardar
    }

    @Test
    fun metaDoDia_soComMetaNoMes() {
        val ganhosHoje = listOf(Ganho(data = hoje, plataforma = Plataforma.UBER, valorCentavos = 5_000))
        assertNull(montar(ganhos = ganhosHoje).metaDoDia)

        val m = montar(ganhos = ganhosHoje, meta = Meta("2026-10", 170_000)).metaDoDia!!
        assertEquals(10_000L, m.metaCentavos) // 15/10: restam 17 dias; R$ 1.700 ÷ 17
        assertEquals(5_000L, m.lucroHojeCentavos)
    }
}
