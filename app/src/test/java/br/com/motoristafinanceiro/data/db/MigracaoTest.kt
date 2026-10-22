package br.com.motoristafinanceiro.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Plataforma
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Migração do banco com dados de verdade: cria o banco da versão 1 exatamente como o Room criava
 * (SQL do schema exportado em app/schemas), grava dados nele e abre com o app atual. O Room confere
 * a estrutura depois da migração, então uma coluna errada faz o teste falhar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigracaoTest {

    private val nome = "migracao-teste.db"
    private lateinit var contexto: Context
    private var db: AppDatabase? = null

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        contexto.deleteDatabase(nome)
    }

    @After
    fun limpar() {
        db?.close()
        contexto.deleteDatabase(nome)
    }

    private fun schema(versao: Int): JSONObject =
        JSONObject(File("schemas/br.com.motoristafinanceiro.data.db.AppDatabase/$versao.json").readText())
            .getJSONObject("database")

    /** Cria o arquivo do banco na [versao] com o SQL do schema exportado e roda [dados] nele. */
    private fun criarBancoNaVersao(versao: Int, dados: (SQLiteDatabase) -> Unit) {
        val s = schema(versao)
        val arquivo = contexto.getDatabasePath(nome).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(arquivo, null).use { banco ->
            val entidades = s.getJSONArray("entities")
            for (i in 0 until entidades.length()) {
                val e = entidades.getJSONObject(i)
                val tabela = e.getString("tableName")
                banco.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", tabela))
                val indices = e.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    banco.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tabela))
                }
            }
            val setup = s.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) banco.execSQL(setup.getString(i))
            banco.version = versao
            dados(banco)
        }
    }

    private fun abrirComOAppAtual(): AppDatabase =
        Room.databaseBuilder(contexto, AppDatabase::class.java, nome)
            .addMigrations(*MIGRACOES)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

    @Test
    fun v1ParaAtual_mantemOsGanhosESemHorarioDeInicio() = runBlocking {
        val dia = LocalDate.of(2026, 10, 20)
        criarBancoNaVersao(1) {
            it.execSQL(
                "INSERT INTO ganhos (id, data, plataforma, valorCentavos, gorjetaBonusCentavos, km, minutosOnline, observacao) " +
                    "VALUES (7, ${dia.toEpochDay()}, 'UBER', 24890, 1200, 120.0, 480, 'Aeroporto')"
            )
            it.execSQL("INSERT INTO metas (mes, valorAlvoLucroCentavos) VALUES ('2026-10', 500000)")
        }

        val repo = Repositorio(abrirComOAppAtual())
        val ganho = repo.ganhos(dia, dia).first().single()

        assertEquals(7L, ganho.id)
        assertEquals(Plataforma.UBER, ganho.plataforma)
        assertEquals(26_090L, ganho.totalCentavos)
        assertEquals(480, ganho.minutosOnline)
        assertEquals("Aeroporto", ganho.observacao)
        assertNull(ganho.inicioTurnoMinutos)
        assertEquals(500_000L, repo.meta("2026-10").first()!!.valorAlvoLucroCentavos)

        // Depois da migração, o campo novo grava e lê normalmente.
        repo.salvarGanho(ganho.copy(inicioTurnoMinutos = 18 * 60))
        assertEquals(18 * 60, repo.ganhos(dia, dia).first().single().inicioTurnoMinutos)
    }

    @Test
    fun migracoes_levamDaVersao1AteAAtual() {
        // A versão atual é a do schema exportado mais novo (o Room gera um arquivo por versão a cada build).
        val atual = File("schemas/br.com.motoristafinanceiro.data.db.AppDatabase").listFiles()!!
            .mapNotNull { it.nameWithoutExtension.toIntOrNull() }.max()
        assertEquals(atual, schema(atual).getInt("version"))
        var versao = 1
        while (versao < atual) {
            val proxima = MIGRACOES.singleOrNull { it.startVersion == versao }
                ?: error("falta a migração que sai da versão $versao")
            versao = proxima.endVersion
        }
        assertEquals(atual, versao)
    }
}
