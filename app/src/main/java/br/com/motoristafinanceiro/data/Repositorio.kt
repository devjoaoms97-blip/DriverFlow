package br.com.motoristafinanceiro.data

import androidx.room.withTransaction
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Ponto único de acesso aos dados. A UI nunca fala direto com os DAOs. */
class Repositorio(private val db: AppDatabase) {

    // ---- Ganhos ----
    fun ganhos(inicio: LocalDate, fim: LocalDate): Flow<List<Ganho>> =
        db.ganhoDao().observarPorPeriodo(inicio, fim)

    suspend fun ganhoPorId(id: Long): Ganho? = db.ganhoDao().buscarPorId(id)

    suspend fun salvarGanho(ganho: Ganho) {
        if (ganho.id == 0L) db.ganhoDao().inserir(ganho) else db.ganhoDao().atualizar(ganho)
    }

    suspend fun excluirGanho(ganho: Ganho) = db.ganhoDao().excluir(ganho)

    // ---- Despesas ----
    fun despesas(inicio: LocalDate, fim: LocalDate): Flow<List<Despesa>> =
        db.despesaDao().observarPorPeriodo(inicio, fim)

    suspend fun despesaPorId(id: Long): Despesa? = db.despesaDao().buscarPorId(id)

    suspend fun salvarDespesa(despesa: Despesa) {
        if (despesa.id == 0L) db.despesaDao().inserir(despesa) else db.despesaDao().atualizar(despesa)
    }

    suspend fun excluirDespesa(despesa: Despesa) = db.despesaDao().excluir(despesa)

    // ---- Abastecimento (gera a despesa de combustível automaticamente) ----
    fun abastecimentos(): Flow<List<Abastecimento>> = db.abastecimentoDao().observarTodos()

    suspend fun registrarAbastecimento(a: Abastecimento) = db.withTransaction {
        val id = db.abastecimentoDao().inserir(a)
        db.despesaDao().inserir(
            Despesa(
                data = a.data,
                categoria = CategoriaDespesa.COMBUSTIVEL,
                valorCentavos = a.valorTotalCentavos,
                origem = OrigemDespesa.ABASTECIMENTO,
                origemId = id,
            )
        )
    }

    suspend fun excluirAbastecimento(a: Abastecimento) = db.withTransaction {
        db.despesaDao().excluirPorOrigem(OrigemDespesa.ABASTECIMENTO, a.id)
        db.abastecimentoDao().excluir(a)
    }

    // ---- Manutenção (gera a despesa de manutenção automaticamente) ----
    fun manutencoes(): Flow<List<Manutencao>> = db.manutencaoDao().observarTodas()

    suspend fun registrarManutencao(m: Manutencao) = db.withTransaction {
        val id = db.manutencaoDao().inserir(m)
        db.despesaDao().inserir(
            Despesa(
                data = m.data,
                categoria = CategoriaDespesa.MANUTENCAO,
                valorCentavos = m.valorCentavos,
                observacao = m.tipo.rotulo,
                origem = OrigemDespesa.MANUTENCAO,
                origemId = id,
            )
        )
    }

    suspend fun excluirManutencao(m: Manutencao) = db.withTransaction {
        db.despesaDao().excluirPorOrigem(OrigemDespesa.MANUTENCAO, m.id)
        db.manutencaoDao().excluir(m)
    }

    // ---- Reservas ----
    fun reservas(): Flow<List<ReservaDespesa>> = db.reservaDao().observarTodas()
    suspend fun salvarReserva(r: ReservaDespesa) = db.reservaDao().salvar(r)
    suspend fun excluirReserva(r: ReservaDespesa) = db.reservaDao().excluir(r)

    // ---- Metas ----
    fun meta(mes: String): Flow<Meta?> = db.metaDao().observar(mes)
    suspend fun salvarMeta(meta: Meta) = db.metaDao().salvar(meta)
}
