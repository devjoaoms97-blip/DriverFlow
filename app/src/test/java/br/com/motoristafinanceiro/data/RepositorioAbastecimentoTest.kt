package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.domain.calcularConsumoMedio
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
class RepositorioAbastecimentoTest {

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

    private fun abastecimento(data: LocalDate = d1, litros: Double = 40.0, valor: Long = 24_000, odometro: Int = 10_000) =
        Abastecimento(data = data, litros = litros, valorTotalCentavos = valor, odometroKm = odometro)

    @Test
    fun atualizar_ajustaDataEValorDaDespesaVinculada() = roda {
        repo.registrarAbastecimento(abastecimento())
        val original = repo.abastecimentos().first().single()

        repo.atualizarAbastecimento(original.copy(data = d2, valorTotalCentavos = 30_000))

        val atualizado = repo.abastecimentos().first().single()
        assertEquals(30_000L, atualizado.valorTotalCentavos)
        assertEquals(d2, atualizado.data)

        val despesas = repo.despesas(d1, d2).first()
        assertEquals(1, despesas.size)
        assertEquals(d2, despesas[0].data)
        assertEquals(30_000L, despesas[0].valorCentavos)
        assertEquals(OrigemDespesa.ABASTECIMENTO, despesas[0].origem)
        assertEquals(atualizado.id, despesas[0].origemId)
    }

    @Test
    fun atualizar_naoMexeEmDespesaManualDoMesmoDiaEValor() = roda {
        repo.salvarDespesa(Despesa(data = d1, categoria = CategoriaDespesa.LAVAGEM, valorCentavos = 24_000))
        repo.registrarAbastecimento(abastecimento(data = d1, valor = 24_000))
        val original = repo.abastecimentos().first().single()

        repo.atualizarAbastecimento(original.copy(valorTotalCentavos = 30_000))

        val despesas = repo.despesas(d1, d1).first()
        assertEquals(2, despesas.size)
        val manual = despesas.single { it.origem == OrigemDespesa.MANUAL }
        assertEquals(24_000L, manual.valorCentavos)
        assertEquals(CategoriaDespesa.LAVAGEM, manual.categoria)
    }

    @Test
    fun salvar_idZeroInsereComDespesa_idExistenteAtualiza() = roda {
        repo.salvarAbastecimento(abastecimento())
        assertEquals(1, repo.abastecimentos().first().size)
        assertEquals(1, repo.despesas(d1, d1).first().size)

        val salvo = repo.abastecimentos().first().single()
        repo.salvarAbastecimento(salvo.copy(valorTotalCentavos = 26_000))

        assertEquals(1, repo.abastecimentos().first().size)
        val despesas = repo.despesas(d1, d1).first()
        assertEquals(1, despesas.size)
        assertEquals(26_000L, despesas[0].valorCentavos)
    }

    @Test
    fun porId_existenteEInexistente() = roda {
        repo.registrarAbastecimento(abastecimento())
        val salvo = repo.abastecimentos().first().single()

        assertNotNull(repo.abastecimentoPorId(salvo.id))
        assertNull(repo.abastecimentoPorId(9_999))
    }

    @Test
    fun consumoMedio_viaRepositorio() = roda {
        repo.registrarAbastecimento(abastecimento(d1, litros = 40.0, valor = 24_000, odometro = 10_000))
        repo.registrarAbastecimento(abastecimento(d2, litros = 30.0, valor = 19_500, odometro = 10_400))

        val consumo = calcularConsumoMedio(repo.abastecimentos().first())

        assertEquals(13.3333, consumo!!, 0.001)
    }
}
