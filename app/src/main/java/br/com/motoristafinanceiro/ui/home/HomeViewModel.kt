package br.com.motoristafinanceiro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val hoje: Estatisticas = Estatisticas(),
    val mes: Estatisticas = Estatisticas(),
    val metaCentavos: Long? = null,
)

class HomeViewModel(repo: Repositorio) : ViewModel() {

    private val mesAtual = YearMonth.now()

    // TODO: ler a depreciação por km das configurações (DataStore) em vez de 0.
    val uiState: StateFlow<HomeUiState> = combine(
        repo.ganhos(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.despesas(mesAtual.atDay(1), mesAtual.atEndOfMonth()),
        repo.meta(mesAtual.toString()),
    ) { ganhos, despesas, meta ->
        val hoje = LocalDate.now()
        HomeUiState(
            hoje = calcularEstatisticas(ganhos.filter { it.data == hoje }, despesas.filter { it.data == hoje }),
            mes = calcularEstatisticas(ganhos, despesas),
            metaCentavos = meta?.valorAlvoLucroCentavos,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
