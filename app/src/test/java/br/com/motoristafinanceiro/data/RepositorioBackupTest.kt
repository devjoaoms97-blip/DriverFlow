package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.backup.DadosBackup
import br.com.motoristafinanceiro.data.backup.dadosDeExemplo
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioBackupTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio

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

    @Test
    fun lerTudo_bancoVazio() = roda {
        assertEquals(DadosBackup(depreciacaoPorKmCentavos = 7), repo.lerTudo(7))
    }

    @Test
    fun substituirTudoELerTudo_mantemIdsEDados() = roda {
        repo.substituirTudo(dadosDeExemplo())

        assertEquals(dadosDeExemplo(), repo.lerTudo(25))
    }

    @Test
    fun substituirTudo_apagaOQueExistia() = roda {
        repo.salvarGanho(Ganho(data = LocalDate.of(2026, 10, 1), plataforma = Plataforma.UBER, valorCentavos = 5_000))

        repo.substituirTudo(dadosDeExemplo())

        assertEquals(2, repo.lerTudo(0).ganhos.size)
        assertTrue(repo.lerTudo(0).ganhos.none { it.valorCentavos == 5_000L })
    }

    @Test
    fun substituirTudo_comDadosVaziosLimpaTudo() = roda {
        repo.substituirTudo(dadosDeExemplo())

        repo.substituirTudo(DadosBackup())

        assertEquals(DadosBackup(), repo.lerTudo(0))
    }

    @Test
    fun substituirTudo_falhaNoMeio_desfazTudoEMantemOsDadosAntigos() = roda {
        repo.substituirTudo(dadosDeExemplo())
        val antes = repo.lerTudo(0)
        val g = antes.ganhos[0]
        val comIdRepetido = DadosBackup(ganhos = listOf(g, g.copy(valorCentavos = 1)))

        try {
            repo.substituirTudo(comIdRepetido)
            fail("Deveria ter falhado por id repetido")
        } catch (e: Exception) {
            // esperado: a transação é desfeita
        }

        assertEquals(antes, repo.lerTudo(0))
    }
}
