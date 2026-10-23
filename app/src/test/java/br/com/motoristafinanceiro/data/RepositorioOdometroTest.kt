package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Plataforma
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioOdometroTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private val d10 = LocalDate.of(2026, 10, 10)

    @Before
    fun criarBanco() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db)
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    private suspend fun ganho(data: LocalDate, km: Double) =
        repo.salvarGanho(Ganho(data = data, plataforma = Plataforma.UBER, valorCentavos = 10_000, km = km))

    @Test
    fun semLeitura_ehNulo() = roda {
        ganho(d10, 100.0)
        assertNull(repo.odometro().first())
    }

    @Test
    fun somaOsGanhosDepoisDaLeitura_eAcompanhaNovosLancamentos() = roda {
        repo.registrarAbastecimento(Abastecimento(data = d10, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_000))
        ganho(d10, 50.0) // mesmo dia: fica de fora
        ganho(d10.plusDays(1), 120.0)
        assertEquals(10_120, repo.odometro().first()!!.km)

        ganho(d10.plusDays(2), 80.0)
        assertEquals(10_200, repo.odometro().first()!!.km)
    }

    @Test
    fun novaLeitura_zeraAEstimativa() = roda {
        repo.registrarAbastecimento(Abastecimento(data = d10, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_000))
        ganho(d10.plusDays(1), 300.0)
        repo.registrarAbastecimento(
            Abastecimento(data = d10.plusDays(2), litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_350)
        )

        val o = repo.odometro().first()!!
        assertEquals(10_350, o.km)
        assertEquals(0, o.kmEstimados)
    }

    @Test
    fun kmPessoaisDepoisDaLeitura_entramNoOdometro_eSaemAoExcluir() = roda {
        repo.registrarAbastecimento(Abastecimento(data = d10, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_000))
        ganho(d10.plusDays(1), 100.0)
        repo.salvarKmPessoal(KmPessoal(data = d10.plusDays(2), km = 40.0))
        assertEquals(10_140, repo.odometro().first()!!.km)

        repo.excluirKmPessoal(repo.kmPessoais().first().single())
        assertEquals(10_100, repo.odometro().first()!!.km)
    }

    @Test
    fun conferencia_comparaOdometroComOsKmLancados() = roda {
        repo.registrarAbastecimento(Abastecimento(data = d10, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_000))
        ganho(d10.plusDays(1), 300.0)
        repo.salvarKmPessoal(KmPessoal(data = d10.plusDays(2), km = 50.0))
        repo.registrarAbastecimento(
            Abastecimento(data = d10.plusDays(5), litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_500)
        )
        val c = repo.conferenciaKm().first()!!
        assertEquals(500, c.kmReais)
        assertEquals(300.0, c.kmTrabalho, 0.0)
        assertEquals(50.0, c.kmPessoal, 0.0)
        assertEquals(150, c.kmSemLancamento)
    }

    @Test
    fun kmPessoal_salvarEditarEExcluir() = roda {
        repo.salvarKmPessoal(KmPessoal(data = d10, km = 10.0))
        val salvo = repo.kmPessoais().first().single()
        repo.salvarKmPessoal(salvo.copy(km = 15.0, observacao = "Viagem"))
        assertEquals(KmPessoal(salvo.id, d10, 15.0, "Viagem"), repo.kmPessoalPorId(salvo.id))
        repo.excluirKmPessoal(salvo)
        assertNull(repo.kmPessoalPorId(salvo.id))
    }
}
