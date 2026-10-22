package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import java.time.LocalDate

/** Conjunto de dados que usa todos os tipos, campos opcionais preenchidos e vazios, e os vínculos entre tabelas. */
internal fun dadosDeExemplo(): DadosBackup {
    val d12 = LocalDate.of(2026, 10, 12)
    val d13 = LocalDate.of(2026, 10, 13)
    return DadosBackup(
        ganhos = listOf(
            Ganho(
                id = 1, data = d12, plataforma = Plataforma.UBER, valorCentavos = 10_000,
                gorjetaBonusCentavos = 500, km = 50.5, minutosOnline = 240, observacao = "Aeroporto",
                inicioTurnoMinutos = 18 * 60 + 30,
            ),
            Ganho(id = 2, data = d13, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 6_000),
        ),
        despesas = listOf(
            Despesa(id = 1, data = d12, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 3_000, observacao = "Lava rápido", fotoUri = "comp_1.jpg"),
            Despesa(id = 2, data = d12, categoria = CategoriaDespesa.COMBUSTIVEL, valorCentavos = 24_000, origem = OrigemDespesa.ABASTECIMENTO, origemId = 1),
            Despesa(id = 3, data = d13, categoria = CategoriaDespesa.MANUTENCAO, valorCentavos = 25_000, observacao = "Troca de óleo", origem = OrigemDespesa.MANUTENCAO, origemId = 1),
            Despesa(id = 4, data = d13, categoria = CategoriaDespesa.SEGURO, valorCentavos = 1_000),
        ),
        abastecimentos = listOf(
            Abastecimento(id = 1, data = d12, litros = 40.5, valorTotalCentavos = 24_000, odometroKm = 10_000, tanqueCheio = true),
            Abastecimento(id = 2, data = d13, litros = 20.0, valorTotalCentavos = 12_000, odometroKm = 10_300, tanqueCheio = false),
        ),
        manutencoes = listOf(
            Manutencao(id = 1, tipo = TipoManutencao.OLEO, data = d13, odometroKm = 10_000, valorCentavos = 25_000, intervaloKm = 10_000, observacao = "Sintético"),
            Manutencao(id = 2, tipo = TipoManutencao.PNEUS, data = d13, odometroKm = 10_100, valorCentavos = 0, intervaloKm = null),
        ),
        reservas = listOf(
            ReservaDespesa(id = 1, nome = "IPVA", valorCentavos = 365_000, periodicidade = Periodicidade.ANUAL, proximoVencimento = LocalDate.of(2027, 1, 10)),
        ),
        metas = listOf(Meta("2026-09", 400_000), Meta("2026-10", 500_000)),
        depreciacaoPorKmCentavos = 25,
    )
}
