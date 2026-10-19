package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioComprovanteTest {

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

    private fun despesa(foto: String?) =
        Despesa(data = dia, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 1_000, fotoUri = foto)

    @Test
    fun fotoDaDespesa_eSalvaELida() = roda {
        repo.salvarDespesa(despesa("a.jpg"))

        val salva = repo.despesas(dia, dia).first().single()
        assertEquals("a.jpg", repo.despesaPorId(salva.id)!!.fotoUri)
    }

    @Test
    fun comprovantesEmUso_ignoraDespesasSemFoto() = roda {
        repo.salvarDespesa(despesa("a.jpg"))
        repo.salvarDespesa(despesa(null))
        repo.salvarDespesa(despesa("b.jpg"))

        assertEquals(listOf("a.jpg", "b.jpg"), repo.nomesDeComprovantesEmUso().sorted())
    }

    @Test
    fun removerAFoto_tiraOArquivoDaListaDeEmUso() = roda {
        repo.salvarDespesa(despesa("a.jpg"))
        val salva = repo.despesas(dia, dia).first().single()

        repo.salvarDespesa(salva.copy(fotoUri = null))

        assertTrue(repo.nomesDeComprovantesEmUso().isEmpty())
    }

    @Test
    fun excluirADespesa_tiraOArquivoDaListaDeEmUso() = roda {
        repo.salvarDespesa(despesa("a.jpg"))
        val salva = repo.despesas(dia, dia).first().single()

        repo.excluirDespesa(salva)

        assertTrue(repo.nomesDeComprovantesEmUso().isEmpty())
    }
}
