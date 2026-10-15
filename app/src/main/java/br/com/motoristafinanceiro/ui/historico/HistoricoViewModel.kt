package br.com.motoristafinanceiro.ui.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

data class HistoricoUiState(
    val mes: YearMonth = YearMonth.now(),
    val filtro: FiltroHistorico = FiltroHistorico.TODOS,
    val dias: List<DiaHistorico> = emptyList(),
    /** Resumo do mês inteiro, independente do filtro. */
    val resumo: Estatisticas = Estatisticas(),
    val podeAvancar: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricoViewModel(private val repo: Repositorio) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now())
    private val filtro = MutableStateFlow(FiltroHistorico.TODOS)

    val uiState: StateFlow<HistoricoUiState> = mes.flatMapLatest { m ->
        combine(
            repo.ganhos(m.atDay(1), m.atEndOfMonth()),
            repo.despesas(m.atDay(1), m.atEndOfMonth()),
            filtro,
        ) { ganhos, despesas, f -> montarEstado(m, f, ganhos, despesas) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoricoUiState())

    fun mesAnterior() {
        mes.value = mes.value.minusMonths(1)
    }

    fun proximoMes() {
        if (mes.value < YearMonth.now()) mes.value = mes.value.plusMonths(1)
    }

    fun filtrar(novo: FiltroHistorico) {
        filtro.value = novo
    }

    private fun montarEstado(
        m: YearMonth,
        f: FiltroHistorico,
        ganhos: List<Ganho>,
        despesas: List<Despesa>,
    ): HistoricoUiState {
        val itens = buildList<ItemHistorico> {
            if (f != FiltroHistorico.DESPESAS) ganhos.forEach { add(ItemHistorico.DeGanho(it)) }
            if (f != FiltroHistorico.GANHOS) despesas.forEach { add(ItemHistorico.DeDespesa(it)) }
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
            mes = m,
            filtro = f,
            dias = dias,
            resumo = calcularEstatisticas(ganhos, despesas),
            podeAvancar = m < YearMonth.now(),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoricoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
