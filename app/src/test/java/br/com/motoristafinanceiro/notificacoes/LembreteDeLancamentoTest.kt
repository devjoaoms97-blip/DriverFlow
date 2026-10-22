package br.com.motoristafinanceiro.notificacoes

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.notificacoes.LembreteDeLancamento.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LembreteDeLancamentoTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private lateinit var configuracoes: Configuracoes
    private var avisos = 0
    private var sistemaPermite = true
    private lateinit var lembrete: LembreteDeLancamento
    private val hoje = LocalDate.of(2026, 10, 22)

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db)
        configuracoes = Configuracoes(
            PreferenceDataStoreFactory.create(scope = escopo, produceFile = { File(tmp.root, "config.preferences_pb") }),
        )
        lembrete = LembreteDeLancamento(repo, configuracoes) {
            if (sistemaPermite) avisos++
            sistemaPermite
        }
    }

    @After
    fun encerrar() {
        db.close()
        escopo.cancel()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    // ---- Horário ----

    @Test
    fun atraso_hojeQuandoOHorarioAindaNaoChegou() =
        assertEquals(Duration.ofHours(2), atrasoAteLembrete(LocalDateTime.of(2026, 10, 22, 20, 0), 22 * 60))

    @Test
    fun atraso_amanhaQuandoOHorarioJaPassou() =
        assertEquals(Duration.ofHours(23), atrasoAteLembrete(LocalDateTime.of(2026, 10, 22, 23, 0), 22 * 60))

    @Test
    fun atraso_noHorarioExato_ficaParaAmanha() =
        assertEquals(Duration.ofDays(1), atrasoAteLembrete(LocalDateTime.of(2026, 10, 22, 22, 0), 22 * 60))

    // ---- Regra ----

    @Test
    fun desligado_naoAvisa() = roda {
        assertEquals(Resultado.DESLIGADO, lembrete.executar(hoje))
        assertEquals(0, avisos)
    }

    @Test
    fun ligadoSemGanhoHoje_avisa() = roda {
        configuracoes.salvarLembrete(true, 22 * 60)
        repo.salvarGanho(Ganho(data = hoje.minusDays(1), plataforma = Plataforma.UBER, valorCentavos = 10_000)) // ontem não conta

        assertEquals(Resultado.AVISOU, lembrete.executar(hoje))
        assertEquals(1, avisos)
    }

    @Test
    fun comGanhoHoje_naoAvisa() = roda {
        configuracoes.salvarLembrete(true, 22 * 60)
        repo.salvarGanho(Ganho(data = hoje, plataforma = Plataforma.NOVE_NOVE, valorCentavos = 10_000))

        assertEquals(Resultado.JA_LANCOU, lembrete.executar(hoje))
        assertEquals(0, avisos)
    }

    @Test
    fun soDespesaHoje_aindaAvisa() = roda {
        configuracoes.salvarLembrete(true, 22 * 60)
        repo.registrarAbastecimento(Abastecimento(data = hoje, litros = 30.0, valorTotalCentavos = 18_000, odometroKm = 10_000))

        assertEquals(Resultado.AVISOU, lembrete.executar(hoje))
    }

    @Test
    fun semPermissao_naoEntregue() = roda {
        configuracoes.salvarLembrete(true, 22 * 60)
        sistemaPermite = false

        assertEquals(Resultado.NAO_ENTREGUE, lembrete.executar(hoje))
        assertEquals(0, avisos)
    }
}
