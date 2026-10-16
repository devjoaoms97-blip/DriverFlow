package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
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
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private val dia = LocalDate.of(2026, 10, 12)

    @Before
    fun criarBanco() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = Repositorio(db)
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    private fun ganho(data: LocalDate, valor: Long = 1_000) =
        Ganho(data = data, plataforma = Plataforma.UBER, valorCentavos = valor)

    private fun despesa(data: LocalDate, valor: Long = 1_000) =
        Despesa(data = data, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = valor)

    // ---------- Ganhos ----------

    @Test
    fun ganho_inserirEAtualizar() = roda {
        repo.salvarGanho(ganho(dia, 10_000))
        val salvo = repo.ganhos(dia, dia).first().single()
        assertTrue(salvo.id != 0L)

        repo.salvarGanho(salvo.copy(valorCentavos = 12_000))

        val depois = repo.ganhos(dia, dia).first()
        assertEquals(1, depois.size)
        assertEquals(12_000L, depois[0].valorCentavos)
    }

    @Test
    fun ganho_periodoIncluiLimites_eOrdenaDoMaisRecente() = roda {
        repo.salvarGanho(ganho(dia.minusDays(1)))
        repo.salvarGanho(ganho(dia))
        repo.salvarGanho(ganho(dia.plusDays(1)))

        assertEquals(listOf(dia), repo.ganhos(dia, dia).first().map { it.data })
        assertEquals(
            listOf(dia.plusDays(1), dia, dia.minusDays(1)),
            repo.ganhos(dia.minusDays(1), dia.plusDays(1)).first().map { it.data },
        )
    }

    @Test
    fun ganho_porId_existenteEInexistente() = roda {
        repo.salvarGanho(ganho(dia))
        val salvo = repo.ganhos(dia, dia).first().single()

        assertNotNull(repo.ganhoPorId(salvo.id))
        assertNull(repo.ganhoPorId(9_999))
    }

    @Test
    fun ganho_excluir() = roda {
        repo.salvarGanho(ganho(dia))
        repo.excluirGanho(repo.ganhos(dia, dia).first().single())

        assertTrue(repo.ganhos(dia, dia).first().isEmpty())
    }

    // ---------- Despesas ----------

    @Test
    fun despesa_inserirAtualizarEExcluir() = roda {
        repo.salvarDespesa(despesa(dia, 3_000))
        val salva = repo.despesas(dia, dia).first().single()
        assertEquals(OrigemDespesa.MANUAL, salva.origem)

        repo.salvarDespesa(salva.copy(categoria = CategoriaDespesa.MULTA, valorCentavos = 5_000))
        val editada = repo.despesaPorId(salva.id)!!
        assertEquals(CategoriaDespesa.MULTA, editada.categoria)
        assertEquals(5_000L, editada.valorCentavos)

        repo.excluirDespesa(editada)
        assertTrue(repo.despesas(dia, dia).first().isEmpty())
    }

    @Test
    fun despesa_porId_inexistente_retornaNulo() = roda {
        assertNull(repo.despesaPorId(9_999))
    }

    // ---------- Abastecimento ----------

    @Test
    fun abastecimento_geraDespesaDeCombustivelVinculada() = roda {
        repo.registrarAbastecimento(
            Abastecimento(data = dia, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000)
        )

        val abastecimento = repo.abastecimentos().first().single()
        val despesa = repo.despesas(dia, dia).first().single()
        assertEquals(CategoriaDespesa.COMBUSTIVEL, despesa.categoria)
        assertEquals(24_000L, despesa.valorCentavos)
        assertEquals(OrigemDespesa.ABASTECIMENTO, despesa.origem)
        assertEquals(abastecimento.id, despesa.origemId)
    }

    @Test
    fun abastecimento_excluir_removeSoADespesaVinculada() = roda {
        repo.salvarDespesa(despesa(dia, 2_000))
        repo.registrarAbastecimento(
            Abastecimento(data = dia, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 10_000)
        )

        repo.excluirAbastecimento(repo.abastecimentos().first().single())

        assertTrue(repo.abastecimentos().first().isEmpty())
        val restantes = repo.despesas(dia, dia).first()
        assertEquals(1, restantes.size)
        assertEquals(OrigemDespesa.MANUAL, restantes[0].origem)
    }

    @Test
    fun abastecimento_entraNoLucroUmaSoVez() = roda {
        repo.salvarGanho(Ganho(data = dia, plataforma = Plataforma.UBER, valorCentavos = 20_000, km = 100.0))
        val abastecimento = Abastecimento(data = dia, litros = 40.0, valorTotalCentavos = 6_000, odometroKm = 10_000)

        repo.registrarAbastecimento(abastecimento)
        val comAbastecimento = calcularEstatisticas(repo.ganhos(dia, dia).first(), repo.despesas(dia, dia).first())
        assertEquals(14_000L, comAbastecimento.lucroLiquidoCentavos)

        repo.excluirAbastecimento(repo.abastecimentos().first().single())
        val semAbastecimento = calcularEstatisticas(repo.ganhos(dia, dia).first(), repo.despesas(dia, dia).first())
        assertEquals(20_000L, semAbastecimento.lucroLiquidoCentavos)
    }

    // ---------- Manutenção ----------

    @Test
    fun manutencao_geraDespesaVinculada_eExcluirRemove() = roda {
        repo.registrarManutencao(
            Manutencao(tipo = TipoManutencao.OLEO, data = dia, odometroKm = 10_000, valorCentavos = 25_000, intervaloKm = 10_000)
        )

        val manutencao = repo.manutencoes().first().single()
        val despesa = repo.despesas(dia, dia).first().single()
        assertEquals(CategoriaDespesa.MANUTENCAO, despesa.categoria)
        assertEquals(TipoManutencao.OLEO.rotulo, despesa.observacao)
        assertEquals(OrigemDespesa.MANUTENCAO, despesa.origem)
        assertEquals(manutencao.id, despesa.origemId)

        repo.excluirManutencao(manutencao)
        assertTrue(repo.manutencoes().first().isEmpty())
        assertTrue(repo.despesas(dia, dia).first().isEmpty())
    }

    // ---------- Metas e reservas ----------

    @Test
    fun meta_salvarDuasVezes_mantemUmaSoComOUltimoValor() = roda {
        repo.salvarMeta(Meta("2026-10", 500_000))
        repo.salvarMeta(Meta("2026-10", 600_000))

        assertEquals(600_000L, repo.meta("2026-10").first()!!.valorAlvoLucroCentavos)
        assertNull(repo.meta("2026-11").first())
    }

    @Test
    fun reserva_salvarEAtualizar() = roda {
        repo.salvarReserva(
            ReservaDespesa(nome = "IPVA", valorCentavos = 365_000, periodicidade = Periodicidade.ANUAL, proximoVencimento = dia)
        )
        val salva = repo.reservas().first().single()

        repo.salvarReserva(salva.copy(valorCentavos = 400_000))

        val depois = repo.reservas().first()
        assertEquals(1, depois.size)
        assertEquals(400_000L, depois[0].valorCentavos)
    }
}
