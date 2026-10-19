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
import br.com.motoristafinanceiro.domain.descricaoManutencao
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

    suspend fun nomesDeComprovantesEmUso(): List<String> = db.despesaDao().fotosEmUso()

    suspend fun salvarDespesa(despesa: Despesa) {
        if (despesa.id == 0L) db.despesaDao().inserir(despesa) else db.despesaDao().atualizar(despesa)
    }

    suspend fun excluirDespesa(despesa: Despesa) = db.despesaDao().excluir(despesa)

    // ---- Abastecimento (gera a despesa de combustível automaticamente) ----
    fun abastecimentos(): Flow<List<Abastecimento>> = db.abastecimentoDao().observarTodos()

    suspend fun abastecimentoPorId(id: Long): Abastecimento? = db.abastecimentoDao().buscarPorId(id)

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

    /** Atualiza o abastecimento e a despesa de combustível vinculada, na mesma transação. */
    suspend fun atualizarAbastecimento(a: Abastecimento) = db.withTransaction {
        db.abastecimentoDao().atualizar(a)
        db.despesaDao().atualizarPorOrigem(OrigemDespesa.ABASTECIMENTO, a.id, a.data, a.valorTotalCentavos, null)
    }

    /** Novo (id = 0) ou existente. */
    suspend fun salvarAbastecimento(a: Abastecimento) {
        if (a.id == 0L) registrarAbastecimento(a) else atualizarAbastecimento(a)
    }

    suspend fun excluirAbastecimento(a: Abastecimento) = db.withTransaction {
        db.despesaDao().excluirPorOrigem(OrigemDespesa.ABASTECIMENTO, a.id)
        db.abastecimentoDao().excluir(a)
    }

    // ---- Manutenção (gera a despesa de manutenção automaticamente) ----
    fun manutencoes(): Flow<List<Manutencao>> = db.manutencaoDao().observarTodas()

    suspend fun manutencaoPorId(id: Long): Manutencao? = db.manutencaoDao().buscarPorId(id)

    suspend fun registrarManutencao(m: Manutencao) = db.withTransaction {
        val id = db.manutencaoDao().inserir(m)
        db.despesaDao().inserir(
            Despesa(
                data = m.data,
                categoria = CategoriaDespesa.MANUTENCAO,
                valorCentavos = m.valorCentavos,
                observacao = descricaoManutencao(m),
                origem = OrigemDespesa.MANUTENCAO,
                origemId = id,
            )
        )
    }

    /** Atualiza a manutenção e a despesa vinculada, na mesma transação. */
    suspend fun atualizarManutencao(m: Manutencao) = db.withTransaction {
        db.manutencaoDao().atualizar(m)
        db.despesaDao().atualizarPorOrigem(
            OrigemDespesa.MANUTENCAO, m.id, m.data, m.valorCentavos, descricaoManutencao(m)
        )
    }

    /** Nova (id = 0) ou existente. */
    suspend fun salvarManutencao(m: Manutencao) {
        if (m.id == 0L) registrarManutencao(m) else atualizarManutencao(m)
    }

    suspend fun excluirManutencao(m: Manutencao) = db.withTransaction {
        db.despesaDao().excluirPorOrigem(OrigemDespesa.MANUTENCAO, m.id)
        db.manutencaoDao().excluir(m)
    }

    // ---- Reservas ----
    fun reservas(): Flow<List<ReservaDespesa>> = db.reservaDao().observarTodas()
    suspend fun reservaPorId(id: Long): ReservaDespesa? = db.reservaDao().buscarPorId(id)
    suspend fun salvarReserva(r: ReservaDespesa) = db.reservaDao().salvar(r)
    suspend fun excluirReserva(r: ReservaDespesa) = db.reservaDao().excluir(r)

    // ---- Metas ----
    fun meta(mes: String): Flow<Meta?> = db.metaDao().observar(mes)
    suspend fun salvarMeta(meta: Meta) = db.metaDao().salvar(meta)
}
