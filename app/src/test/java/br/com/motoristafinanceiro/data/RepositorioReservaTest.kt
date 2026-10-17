package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
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
class RepositorioReservaTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio

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

    private fun reserva(nome: String, venc: LocalDate, valor: Long = 365_000) = ReservaDespesa(
        nome = nome, valorCentavos = valor, periodicidade = Periodicidade.ANUAL, proximoVencimento = venc,
    )

    @Test
    fun salvarNova_geraId() = roda {
        repo.salvarReserva(reserva("IPVA", LocalDate.of(2027, 1, 10)))

        val salva = repo.reservas().first().single()
        assertTrue(salva.id != 0L)
        assertEquals("IPVA", salva.nome)
    }

    @Test
    fun salvarExistente_atualizaSemDuplicar() = roda {
        repo.salvarReserva(reserva("IPVA", LocalDate.of(2027, 1, 10)))
        val salva = repo.reservas().first().single()

        repo.salvarReserva(salva.copy(valorCentavos = 400_000, nome = "IPVA 2027"))

        val depois = repo.reservas().first()
        assertEquals(1, depois.size)
        assertEquals(400_000L, depois[0].valorCentavos)
        assertEquals("IPVA 2027", depois[0].nome)
    }

    @Test
    fun porId_existenteEInexistente() = roda {
        repo.salvarReserva(reserva("IPVA", LocalDate.of(2027, 1, 10)))
        val salva = repo.reservas().first().single()

        assertNotNull(repo.reservaPorId(salva.id))
        assertNull(repo.reservaPorId(9_999))
    }

    @Test
    fun excluir_removeSoAReservaEscolhida() = roda {
        repo.salvarReserva(reserva("IPVA", LocalDate.of(2027, 1, 10)))
        repo.salvarReserva(reserva("Seguro", LocalDate.of(2026, 12, 1)))
        val ipva = repo.reservas().first().single { it.nome == "IPVA" }

        repo.excluirReserva(ipva)

        assertEquals(listOf("Seguro"), repo.reservas().first().map { it.nome })
    }

    @Test
    fun lista_vemOrdenadaPeloVencimentoCadastrado() = roda {
        repo.salvarReserva(reserva("Depois", LocalDate.of(2027, 6, 1)))
        repo.salvarReserva(reserva("Primeiro", LocalDate.of(2026, 11, 1)))

        assertEquals(listOf("Primeiro", "Depois"), repo.reservas().first().map { it.nome })
    }
}
