package br.com.motoristafinanceiro.notificacoes

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.notificacoes.VerificadorDeManutencao.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerificadorDeManutencaoTest {

    @get:Rule
    val pasta = TemporaryFolder()

    private val escopo = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase
    private lateinit var repo: Repositorio
    private lateinit var configuracoes: Configuracoes
    private lateinit var verificador: VerificadorDeManutencao

    private val entregues = mutableListOf<TextoNotificacao>()
    private var limpezas = 0
    private var sistemaPermite = true
    private val hoje = LocalDate.of(2026, 10, 20)

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repositorio(db)
        configuracoes = Configuracoes(
            PreferenceDataStoreFactory.create(scope = escopo, produceFile = { File(pasta.root, "teste.preferences_pb") })
        )
        verificador = VerificadorDeManutencao(
            repositorio = repo,
            configuracoes = configuracoes,
            notificar = { texto -> if (sistemaPermite) entregues.add(texto) else false },
            limpar = { limpezas++ },
        )
    }

    @After
    fun encerrar() {
        db.close()
        escopo.cancel()
    }

    private fun roda(bloco: suspend () -> Unit) = runBlocking { bloco() }

    private suspend fun trocaDeOleo(odometro: Int) = repo.registrarManutencao(
        Manutencao(tipo = TipoManutencao.OLEO, data = hoje, odometroKm = odometro, valorCentavos = 25_000, intervaloKm = 10_000)
    )

    private suspend fun abastecerNo(odometro: Int) = repo.registrarAbastecimento(
        Abastecimento(data = hoje, litros = 30.0, valorTotalCentavos = 19_500, odometroKm = odometro)
    )

    @Test
    fun desligado_naoNotificaELimpaAExistente() = roda {
        configuracoes.salvarNotificacoesManutencao(false)
        trocaDeOleo(10_000)
        abastecerNo(19_700) // próxima

        assertEquals(Resultado.DESLIGADO, verificador.verificar())
        assertTrue(entregues.isEmpty())
        assertEquals(1, limpezas)
        assertTrue(configuracoes.alertasNotificados.first().isEmpty())
    }

    @Test
    fun ligadoSemNenhumDado_naoFazNada() = roda {
        configuracoes.salvarNotificacoesManutencao(true)

        assertEquals(Resultado.NADA_NOVO, verificador.verificar())
        assertTrue(entregues.isEmpty())
    }

    @Test
    fun proxima_notificaUmaVezENaoRepete() = roda {
        configuracoes.salvarNotificacoesManutencao(true)
        trocaDeOleo(10_000)
        abastecerNo(19_700)

        assertEquals(Resultado.NOTIFICOU, verificador.verificar())
        assertEquals("Manutenção próxima", entregues.single().titulo)

        assertEquals(Resultado.NADA_NOVO, verificador.verificar())
        assertEquals(1, entregues.size)
    }

    @Test
    fun proximaQueViraAtrasada_notificaDeNovoComTextoAtualizado() = roda {
        configuracoes.salvarNotificacoesManutencao(true)
        trocaDeOleo(10_000)
        abastecerNo(19_700)
        verificador.verificar()

        abastecerNo(20_200)
        assertEquals(Resultado.NOTIFICOU, verificador.verificar())
        assertEquals(2, entregues.size)
        assertEquals("Manutenção atrasada", entregues.last().titulo)
        assertEquals("Troca de óleo — Atrasada em 200 km", entregues.last().resumo)
    }

    @Test
    fun semPermissao_naoMarcaComoAvisadoEAvisaQuandoLiberar() = roda {
        configuracoes.salvarNotificacoesManutencao(true)
        trocaDeOleo(10_000)
        abastecerNo(19_700)

        sistemaPermite = false
        assertEquals(Resultado.NAO_ENTREGUE, verificador.verificar())
        assertTrue(entregues.isEmpty())
        assertTrue(configuracoes.alertasNotificados.first().isEmpty())

        sistemaPermite = true
        assertEquals(Resultado.NOTIFICOU, verificador.verificar())
        assertEquals(1, entregues.size)
    }

    @Test
    fun alertaResolvido_limpaANotificacaoEEsqueceOAviso() = roda {
        configuracoes.salvarNotificacoesManutencao(true)
        trocaDeOleo(10_000)
        abastecerNo(20_200)
        assertEquals(Resultado.NOTIFICOU, verificador.verificar())
        val limpezasAntes = limpezas

        trocaDeOleo(20_300) // trocou o óleo: plano novo, em dia

        assertEquals(Resultado.NADA_NOVO, verificador.verificar())
        assertEquals(limpezasAntes + 1, limpezas)
        assertTrue(configuracoes.alertasNotificados.first().isEmpty())
        assertEquals(1, entregues.size)
    }
}
