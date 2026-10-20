package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Meta
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioMetaTest {

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

    @Test
    fun semMeta_retornaNulo() = roda {
        assertNull(repo.meta("2026-10").first())
    }

    @Test
    fun salvarELer() = roda {
        repo.salvarMeta(Meta("2026-10", 500_000))

        assertEquals(500_000L, repo.meta("2026-10").first()?.valorAlvoLucroCentavos)
    }

    @Test
    fun salvarDeNovo_substituiSemDuplicar() = roda {
        repo.salvarMeta(Meta("2026-10", 500_000))
        repo.salvarMeta(Meta("2026-10", 650_000))

        assertEquals(650_000L, repo.meta("2026-10").first()?.valorAlvoLucroCentavos)
    }

    @Test
    fun cadaMesTemSuaMeta() = roda {
        repo.salvarMeta(Meta("2026-10", 500_000))
        repo.salvarMeta(Meta("2026-11", 400_000))

        assertEquals(500_000L, repo.meta("2026-10").first()?.valorAlvoLucroCentavos)
        assertEquals(400_000L, repo.meta("2026-11").first()?.valorAlvoLucroCentavos)
    }

    @Test
    fun excluir_removeSoAqueleMes() = roda {
        repo.salvarMeta(Meta("2026-10", 500_000))
        repo.salvarMeta(Meta("2026-11", 400_000))

        repo.excluirMeta("2026-10")

        assertNull(repo.meta("2026-10").first())
        assertEquals(400_000L, repo.meta("2026-11").first()?.valorAlvoLucroCentavos)
    }

    @Test
    fun excluirMesSemMeta_naoQuebra() = roda {
        repo.excluirMeta("2026-10")

        assertNull(repo.meta("2026-10").first())
    }
}
