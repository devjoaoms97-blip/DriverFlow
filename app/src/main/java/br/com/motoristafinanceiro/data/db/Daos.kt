package br.com.motoristafinanceiro.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.KmPessoal
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GanhoDao {
    @Insert suspend fun inserir(ganho: Ganho): Long
    @Update suspend fun atualizar(ganho: Ganho)
    @Delete suspend fun excluir(ganho: Ganho)

    @Query("SELECT * FROM ganhos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Ganho?

    @Query("SELECT * FROM ganhos WHERE data BETWEEN :inicio AND :fim ORDER BY data DESC, id DESC")
    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<Ganho>>

    @Query("SELECT * FROM ganhos ORDER BY id")
    suspend fun listarTodos(): List<Ganho>

    /** O de data mais recente; no mesmo dia, o lançado por último. */
    @Query("SELECT * FROM ganhos ORDER BY data DESC, id DESC LIMIT 1")
    suspend fun maisRecente(): Ganho?

    @Query("DELETE FROM ganhos")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<Ganho>)
}

@Dao
interface DespesaDao {
    @Insert suspend fun inserir(despesa: Despesa): Long
    @Update suspend fun atualizar(despesa: Despesa)
    @Delete suspend fun excluir(despesa: Despesa)

    @Query("SELECT * FROM despesas WHERE id = :id")
    suspend fun buscarPorId(id: Long): Despesa?

    /** Nomes dos arquivos de comprovante que alguma despesa usa. */
    @Query("SELECT fotoUri FROM despesas WHERE fotoUri IS NOT NULL")
    suspend fun fotosEmUso(): List<String>

    @Query("DELETE FROM despesas WHERE origem = :origem AND origemId = :origemId")
    suspend fun excluirPorOrigem(origem: OrigemDespesa, origemId: Long)

    /** Mantém a despesa gerada (abastecimento/manutenção) em sincronia com o registro de origem. */
    @Query(
        "UPDATE despesas SET data = :data, valorCentavos = :valorCentavos, observacao = :observacao " +
            "WHERE origem = :origem AND origemId = :origemId"
    )
    suspend fun atualizarPorOrigem(
        origem: OrigemDespesa,
        origemId: Long,
        data: LocalDate,
        valorCentavos: Long,
        observacao: String?,
    )

    @Query("SELECT * FROM despesas WHERE data BETWEEN :inicio AND :fim ORDER BY data DESC, id DESC")
    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<Despesa>>

    @Query("SELECT * FROM despesas ORDER BY id")
    suspend fun listarTodos(): List<Despesa>

    @Query("DELETE FROM despesas")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<Despesa>)
}

@Dao
interface AbastecimentoDao {
    @Insert suspend fun inserir(abastecimento: Abastecimento): Long
    @Update suspend fun atualizar(abastecimento: Abastecimento)
    @Delete suspend fun excluir(abastecimento: Abastecimento)

    @Query("SELECT * FROM abastecimentos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Abastecimento?

    @Query("SELECT * FROM abastecimentos ORDER BY odometroKm ASC")
    fun observarTodos(): Flow<List<Abastecimento>>

    @Query("SELECT * FROM abastecimentos ORDER BY id")
    suspend fun listarTodos(): List<Abastecimento>

    @Query("DELETE FROM abastecimentos")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<Abastecimento>)
}

@Dao
interface ManutencaoDao {
    @Insert suspend fun inserir(manutencao: Manutencao): Long
    @Update suspend fun atualizar(manutencao: Manutencao)
    @Delete suspend fun excluir(manutencao: Manutencao)

    @Query("SELECT * FROM manutencoes WHERE id = :id")
    suspend fun buscarPorId(id: Long): Manutencao?

    @Query("SELECT * FROM manutencoes ORDER BY odometroKm DESC")
    fun observarTodas(): Flow<List<Manutencao>>

    @Query("SELECT * FROM manutencoes ORDER BY id")
    suspend fun listarTodos(): List<Manutencao>

    @Query("DELETE FROM manutencoes")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<Manutencao>)
}

@Dao
interface ReservaDao {
    @Upsert suspend fun salvar(reserva: ReservaDespesa): Long
    @Delete suspend fun excluir(reserva: ReservaDespesa)

    @Query("SELECT * FROM reservas WHERE id = :id")
    suspend fun buscarPorId(id: Long): ReservaDespesa?

    @Query("SELECT * FROM reservas ORDER BY proximoVencimento ASC")
    fun observarTodas(): Flow<List<ReservaDespesa>>

    @Query("SELECT * FROM reservas ORDER BY id")
    suspend fun listarTodos(): List<ReservaDespesa>

    @Query("DELETE FROM reservas")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<ReservaDespesa>)
}

@Dao
interface MetaDao {
    @Upsert suspend fun salvar(meta: Meta)

    @Query("SELECT * FROM metas WHERE mes = :mes")
    fun observar(mes: String): Flow<Meta?>

    @Query("DELETE FROM metas WHERE mes = :mes")
    suspend fun excluir(mes: String)

    @Query("SELECT * FROM metas ORDER BY mes")
    suspend fun listarTodos(): List<Meta>

    @Query("DELETE FROM metas")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<Meta>)
}

@Dao
interface KmPessoalDao {
    @Upsert suspend fun salvar(km: KmPessoal): Long
    @Delete suspend fun excluir(km: KmPessoal)

    @Query("SELECT * FROM km_pessoais WHERE id = :id")
    suspend fun buscarPorId(id: Long): KmPessoal?

    @Query("SELECT * FROM km_pessoais WHERE data BETWEEN :inicio AND :fim ORDER BY data DESC, id DESC")
    fun observarPorPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<KmPessoal>>

    @Query("SELECT * FROM km_pessoais ORDER BY id")
    suspend fun listarTodos(): List<KmPessoal>

    @Query("DELETE FROM km_pessoais")
    suspend fun apagarTodos()

    @Insert suspend fun inserirTodos(lista: List<KmPessoal>)
}
