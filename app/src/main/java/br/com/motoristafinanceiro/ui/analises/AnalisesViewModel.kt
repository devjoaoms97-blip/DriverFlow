package br.com.motoristafinanceiro.ui.analises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.domain.Desempenho
import br.com.motoristafinanceiro.domain.DesempenhoDia
import br.com.motoristafinanceiro.domain.DesempenhoPlataforma
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import br.com.motoristafinanceiro.domain.analisarPorDiaDaSemana
import br.com.motoristafinanceiro.domain.analisarPorPlataforma
import br.com.motoristafinanceiro.domain.desempenhoDe
import br.com.motoristafinanceiro.domain.inicio
import br.com.motoristafinanceiro.domain.melhorDiaPorHora
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalisesUiState(
    val periodo: PeriodoAnalise = PeriodoAnalise.ULTIMOS_30,
    val total: Desempenho = Desempenho(),
    /** Sempre 7 itens, de segunda a domingo. */
    val dias: List<DesempenhoDia> = emptyList(),
    /** Só plataformas com ganho no período, da que mais rendeu para a que menos rendeu. */
    val plataformas: List<DesempenhoPlataforma> = emptyList(),
    val melhorDia: DesempenhoDia? = null,
) {
    val temDados: Boolean get() = total.lancamentos > 0
}

/** Regra pura da tela Análises (sem Android), coberta por testes. */
internal fun montarEstadoAnalises(periodo: PeriodoAnalise, ganhos: List<Ganho>): AnalisesUiState {
    val dias = analisarPorDiaDaSemana(ganhos)
    return AnalisesUiState(
        periodo = periodo,
        total = desempenhoDe(ganhos),
        dias = dias,
        plataformas = analisarPorPlataforma(ganhos),
        melhorDia = melhorDiaPorHora(dias),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalisesViewModel(private val repo: Repositorio) : ViewModel() {

    private val periodo = MutableStateFlow(PeriodoAnalise.ULTIMOS_30)

    val uiState: StateFlow<AnalisesUiState> = periodo.flatMapLatest { p ->
        val hoje = LocalDate.now()
        repo.ganhos(p.inicio(hoje), hoje).map { ganhos -> montarEstadoAnalises(p, ganhos) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalisesUiState())

    fun escolherPeriodo(novo: PeriodoAnalise) {
        periodo.value = novo
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AnalisesViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
