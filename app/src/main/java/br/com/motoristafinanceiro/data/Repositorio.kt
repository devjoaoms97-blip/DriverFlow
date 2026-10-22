package br.com.motoristafinanceiro.data

import androidx.room.withTransaction
import br.com.motoristafinanceiro.data.backup.DadosBackup
import br.com.motoristafinanceiro.data.db.AppDatabase
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.domain.odometroEstimado
import br.com.motoristafinanceiro.domain.ultimaLeitura
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Ponto único de acesso aos dados. A UI nunca fala direto com os DAOs. */
class Repositorio(private val db: AppDatabase) {

    private companion object {
        /** Limite final para "daqui em diante" nas buscas por período. */
        val FIM_DOS_TEMPOS: LocalDate = LocalDate.of(9999, 12, 31)
    }

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

    // ---- Odômetro ----
    /**
     * Odômetro dos alertas: última leitura (abastecimento ou manutenção) + km dos ganhos lançados depois dela.
     * Só busca os ganhos a partir do dia seguinte à leitura; muda quando qualquer um dos três muda.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun odometro(): Flow<Odometro?> =
        combine(abastecimentos(), manutencoes()) { a, m -> a to m }
            .flatMapLatest { (a, m) ->
                val leitura = ultimaLeitura(a, m)
                if (leitura == null) {
                    flowOf(null)
                } else {
                    ganhos(leitura.data.plusDays(1), FIM_DOS_TEMPOS).map { odometroEstimado(a, m, it) }
                }
            }

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
    suspend fun excluirMeta(mes: String) = db.metaDao().excluir(mes)

    // ---- Backup ----

    /** Foto consistente de todas as tabelas (uma única transação). A depreciação vem do DataStore. */
    suspend fun lerTudo(depreciacaoPorKmCentavos: Long): DadosBackup = db.withTransaction {
        DadosBackup(
            ganhos = db.ganhoDao().listarTodos(),
            despesas = db.despesaDao().listarTodos(),
            abastecimentos = db.abastecimentoDao().listarTodos(),
            manutencoes = db.manutencaoDao().listarTodos(),
            reservas = db.reservaDao().listarTodos(),
            metas = db.metaDao().listarTodos(),
            depreciacaoPorKmCentavos = depreciacaoPorKmCentavos,
        )
    }

    /** Apaga tudo e insere os dados do backup mantendo os ids (os vínculos despesa ↔ abastecimento/manutenção dependem deles). */
    suspend fun substituirTudo(d: DadosBackup) = db.withTransaction {
        db.ganhoDao().apagarTodos()
        db.despesaDao().apagarTodos()
        db.abastecimentoDao().apagarTodos()
        db.manutencaoDao().apagarTodos()
        db.reservaDao().apagarTodos()
        db.metaDao().apagarTodos()

        db.ganhoDao().inserirTodos(d.ganhos)
        db.despesaDao().inserirTodos(d.despesas)
        db.abastecimentoDao().inserirTodos(d.abastecimentos)
        db.manutencaoDao().inserirTodos(d.manutencoes)
        db.reservaDao().inserirTodos(d.reservas)
        db.metaDao().inserirTodos(d.metas)
    }
}
