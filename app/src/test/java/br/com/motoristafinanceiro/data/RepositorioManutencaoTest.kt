package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.odometroAtual
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioManutencaoTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private val d1 = LocalDate.of(2026, 10, 1)
    private val d2 = LocalDate.of(2026, 10, 10)

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

    private fun oleo(data: LocalDate = d1, valor: Long = 25_000, odometro: Int = 10_000, intervalo: Int? = 10_000) =
        Manutencao(tipo = TipoManutencao.OLEO, data = data, odometroKm = odometro, valorCentavos = valor, intervaloKm = intervalo)

    @Test
    fun atualizar_ajustaDataValorEDescricaoDaDespesaVinculada() = roda {
        repo.registrarManutencao(oleo())
        val original = repo.manutencoes().first().single()

        repo.atualizarManutencao(original.copy(tipo = TipoManutencao.FREIOS, data = d2, valorCentavos = 40_000))

        val despesas = repo.despesas(d1, d2).first()
        assertEquals(1, despesas.size)
        assertEquals(d2, despesas[0].data)
        assertEquals(40_000L, despesas[0].valorCentavos)
        assertEquals("Freios", despesas[0].observacao)
        assertEquals(OrigemDespesa.MANUTENCAO, despesas[0].origem)
        assertEquals(original.id, despesas[0].origemId)
    }

    @Test
    fun registrar_outroComObservacao_usaADescricaoNaDespesa() = roda {
        repo.registrarManutencao(
            Manutencao(tipo = TipoManutencao.OUTRO, data = d1, odometroKm = 10_000, valorCentavos = 30_000, observacao = "Bateria")
        )

        assertEquals("Bateria", repo.despesas(d1, d1).first().single().observacao)
    }

    @Test
    fun salvar_idZeroInsereComDespesa_idExistenteAtualiza() = roda {
        repo.salvarManutencao(oleo())
        assertEquals(1, repo.manutencoes().first().size)
        assertEquals(1, repo.despesas(d1, d1).first().size)

        val salva = repo.manutencoes().first().single()
        repo.salvarManutencao(salva.copy(valorCentavos = 27_000))

        assertEquals(1, repo.manutencoes().first().size)
        val despesas = repo.despesas(d1, d1).first()
        assertEquals(1, despesas.size)
        assertEquals(27_000L, despesas[0].valorCentavos)
    }

    @Test
    fun porId_existenteEInexistente() = roda {
        repo.registrarManutencao(oleo())
        val salva = repo.manutencoes().first().single()

        assertNotNull(repo.manutencaoPorId(salva.id))
        assertNull(repo.manutencaoPorId(9_999))
    }

    @Test
    fun atualizar_naoMexeNaDespesaDoAbastecimentoDoMesmoDiaEValor() = roda {
        repo.registrarAbastecimento(
            Abastecimento(data = d1, litros = 40.0, valorTotalCentavos = 25_000, odometroKm = 10_000)
        )
        repo.registrarManutencao(oleo(data = d1, valor = 25_000))
        val manutencao = repo.manutencoes().first().single()

        repo.atualizarManutencao(manutencao.copy(valorCentavos = 31_000))

        val despesas = repo.despesas(d1, d1).first()
        assertEquals(2, despesas.size)
        val combustivel = despesas.single { it.origem == OrigemDespesa.ABASTECIMENTO }
        assertEquals(25_000L, combustivel.valorCentavos)
        assertEquals(CategoriaDespesa.COMBUSTIVEL, combustivel.categoria)
        assertEquals(31_000L, despesas.single { it.origem == OrigemDespesa.MANUTENCAO }.valorCentavos)
    }

    @Test
    fun alertas_viaRepositorio() = roda {
        repo.registrarManutencao(oleo(odometro = 10_000, intervalo = 5_000))
        repo.registrarAbastecimento(
            Abastecimento(data = d2, litros = 40.0, valorTotalCentavos = 24_000, odometroKm = 14_800)
        )

        val manutencoes = repo.manutencoes().first()
        val odometro = odometroAtual(repo.abastecimentos().first(), manutencoes)!!
        val alerta = calcularAlertas(manutencoes, odometro).single()

        assertEquals(200, alerta.kmRestantes)
        assertEquals(StatusAlerta.PROXIMA, alerta.status)
    }
}
