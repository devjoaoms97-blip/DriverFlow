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
}

@Dao
interface DespesaDao {
    @Insert suspend fun inserir(despesa: Despesa): Long
    @Update suspend fun atualizar(despesa: Despesa)
    @Delete suspend fun excluir(despesa: Despesa)

    @Query("SELECT * FROM despesas WHERE id = :id")
    suspend fun buscarPorId(id: Long): Despesa?

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
}

@Dao
interface ReservaDao {
    @Upsert suspend fun salvar(reserva: ReservaDespesa): Long
    @Delete suspend fun excluir(reserva: ReservaDespesa)

    @Query("SELECT * FROM reservas WHERE id = :id")
    suspend fun buscarPorId(id: Long): ReservaDespesa?

    @Query("SELECT * FROM reservas ORDER BY proximoVencimento ASC")
    fun observarTodas(): Flow<List<ReservaDespesa>>
}

@Dao
interface MetaDao {
    @Upsert suspend fun salvar(meta: Meta)

    @Query("SELECT * FROM metas WHERE mes = :mes")
    fun observar(mes: String): Flow<Meta?>
}
