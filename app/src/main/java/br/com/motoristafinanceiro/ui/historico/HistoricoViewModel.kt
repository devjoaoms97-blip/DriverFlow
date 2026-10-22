package br.com.motoristafinanceiro.ui.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.RelatorioMes
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.montarRelatorioMes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

enum class FiltroHistorico(val rotulo: String) {
    TODOS("Todos"),
    GANHOS("Ganhos"),
    DESPESAS("Despesas"),
}

sealed interface ItemHistorico {
    val data: LocalDate
    val chave: String

    data class DeGanho(val ganho: Ganho) : ItemHistorico {
        override val data: LocalDate get() = ganho.data
        override val chave: String get() = "g${ganho.id}"
    }

    data class DeDespesa(val despesa: Despesa) : ItemHistorico {
        override val data: LocalDate get() = despesa.data
        override val chave: String get() = "d${despesa.id}"
    }
}

data class DiaHistorico(
    val data: LocalDate,
    val itens: List<ItemHistorico>,
    /** Ganhos − despesas do dia (considerando só os itens exibidos). */
    val saldoCentavos: Long,
)

/** [rotulo] é o nome da plataforma ou da categoria escolhida. */
data class ResumoFiltro(val rotulo: String, val totalCentavos: Long, val quantidade: Int)

data class HistoricoUiState(
    val mes: YearMonth = YearMonth.now(),
    val filtro: FiltroHistorico = FiltroHistorico.TODOS,
    /** Só vale com o filtro Ganhos; null = todas as plataformas. */
    val plataforma: Plataforma? = null,
    /** Só vale com o filtro Despesas; null = todas as categorias. */
    val categoria: CategoriaDespesa? = null,
    /** Total e quantidade do que está sendo exibido quando há plataforma ou categoria escolhida. */
    val resumoFiltro: ResumoFiltro? = null,
    val dias: List<DiaHistorico> = emptyList(),
    /** Resumo do mês inteiro, independente do filtro. */
    val resumo: Estatisticas = Estatisticas(),
    val podeAvancar: Boolean = false,
)

/** Regra pura do histórico (sem Android), coberta por testes. */
internal fun montarEstadoHistorico(
    mes: YearMonth,
    filtro: FiltroHistorico,
    ganhos: List<Ganho>,
    despesas: List<Despesa>,
    mesAtual: YearMonth = YearMonth.now(),
    plataforma: Plataforma? = null,
    categoria: CategoriaDespesa? = null,
): HistoricoUiState {
    // Plataforma só faz sentido para ganhos e categoria só para despesas; fora disso são ignoradas.
    val plataformaAplicada = plataforma.takeIf { filtro == FiltroHistorico.GANHOS }
    val categoriaAplicada = categoria.takeIf { filtro == FiltroHistorico.DESPESAS }
    val ganhosExibidos = ganhos.filter { plataformaAplicada == null || it.plataforma == plataformaAplicada }
    val despesasExibidas = despesas.filter { categoriaAplicada == null || it.categoria == categoriaAplicada }
    val itens = buildList<ItemHistorico> {
        if (filtro != FiltroHistorico.DESPESAS) ganhosExibidos.forEach { add(ItemHistorico.DeGanho(it)) }
        if (filtro != FiltroHistorico.GANHOS) despesasExibidas.forEach { add(ItemHistorico.DeDespesa(it)) }
    }
    val resumoFiltro = when {
        plataformaAplicada != null ->
            ResumoFiltro(plataformaAplicada.rotulo, ganhosExibidos.sumOf { it.totalCentavos }, ganhosExibidos.size)
        categoriaAplicada != null ->
            ResumoFiltro(categoriaAplicada.rotulo, despesasExibidas.sumOf { it.valorCentavos }, despesasExibidas.size)
        else -> null
    }
    val dias = itens.groupBy { it.data }.entries
        .sortedByDescending { it.key.toEpochDay() }
        .map { (data, lista) ->
            val saldo = lista.sumOf {
                when (it) {
                    is ItemHistorico.DeGanho -> it.ganho.totalCentavos
                    is ItemHistorico.DeDespesa -> -it.despesa.valorCentavos
                }
            }
            DiaHistorico(data, lista, saldo)
        }
    return HistoricoUiState(
        mes = mes,
        filtro = filtro,
        plataforma = plataformaAplicada,
        categoria = categoriaAplicada,
        resumoFiltro = resumoFiltro,
        dias = dias,
        resumo = calcularEstatisticas(ganhos, despesas),
        podeAvancar = mes < mesAtual,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricoViewModel(private val repo: Repositorio) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now())
    private val filtro = MutableStateFlow(FiltroHistorico.TODOS)
    private val plataforma = MutableStateFlow<Plataforma?>(null)
    private val categoria = MutableStateFlow<CategoriaDespesa?>(null)

    val uiState: StateFlow<HistoricoUiState> = mes.flatMapLatest { m ->
        combine(
            repo.ganhos(m.atDay(1), m.atEndOfMonth()),
            repo.despesas(m.atDay(1), m.atEndOfMonth()),
            filtro,
            plataforma,
            categoria,
        ) { ganhos, despesas, f, plat, cat ->
            montarEstadoHistorico(m, f, ganhos, despesas, plataforma = plat, categoria = cat)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoricoUiState())

    /** Relatório em PDF do mês que está na tela (todos os lançamentos, sem os filtros). */
    suspend fun relatorioDoMes(): RelatorioMes {
        val m = mes.value
        val inicio = m.atDay(1)
        val fim = m.atEndOfMonth()
        return montarRelatorioMes(m, repo.ganhos(inicio, fim).first(), repo.despesas(inicio, fim).first())
    }

    fun mesAnterior() {
        mes.value = mes.value.minusMonths(1)
    }

    fun proximoMes() {
        if (mes.value < YearMonth.now()) mes.value = mes.value.plusMonths(1)
    }

    /** Trocar o tipo (Todos/Ganhos/Despesas) limpa a plataforma e a categoria escolhidas. */
    fun filtrar(novo: FiltroHistorico) {
        filtro.value = novo
        plataforma.value = null
        categoria.value = null
    }

    fun filtrarPlataforma(nova: Plataforma?) {
        plataforma.value = nova
    }

    fun filtrarCategoria(nova: CategoriaDespesa?) {
        categoria.value = nova
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoricoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
