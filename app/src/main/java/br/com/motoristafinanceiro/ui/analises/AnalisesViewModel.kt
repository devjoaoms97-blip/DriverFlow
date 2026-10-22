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
import br.com.motoristafinanceiro.domain.DesempenhoFaixa
import br.com.motoristafinanceiro.domain.DesempenhoPlataforma
import br.com.motoristafinanceiro.domain.MESES_NA_EVOLUCAO
import br.com.motoristafinanceiro.domain.PeriodoAnalise
import br.com.motoristafinanceiro.domain.ResumoMes
import br.com.motoristafinanceiro.domain.analisarPorDiaDaSemana
import br.com.motoristafinanceiro.domain.analisarPorFaixa
import br.com.motoristafinanceiro.domain.analisarPorPlataforma
import br.com.motoristafinanceiro.domain.desempenhoDe
import br.com.motoristafinanceiro.domain.evolucaoMensal
import br.com.motoristafinanceiro.domain.ganhosSemHorario
import br.com.motoristafinanceiro.domain.inicio
import br.com.motoristafinanceiro.domain.melhorDiaPorHora
import br.com.motoristafinanceiro.domain.melhorFaixa
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class AnalisesUiState(
    val periodo: PeriodoAnalise = PeriodoAnalise.ULTIMOS_30,
    val total: Desempenho = Desempenho(),
    /** Sempre 7 itens, de segunda a domingo. */
    val dias: List<DesempenhoDia> = emptyList(),
    /** Só plataformas com ganho no período, da que mais rendeu para a que menos rendeu. */
    val plataformas: List<DesempenhoPlataforma> = emptyList(),
    val melhorDia: DesempenhoDia? = null,
    /** Sempre 4 itens (madrugada, manhã, tarde, noite); zerados quando nenhum ganho tem horário. */
    val faixas: List<DesempenhoFaixa> = emptyList(),
    val melhorFaixa: DesempenhoFaixa? = null,
    /** Lançamentos do período que ficaram fora da análise por faixa (sem horário de início ou sem horas). */
    val ganhosSemHorario: Int = 0,
    /** Últimos meses (o atual por último), independente do período escolhido. */
    val evolucao: List<ResumoMes> = emptyList(),
) {
    val temDados: Boolean get() = total.lancamentos > 0
}

/** Regra pura da tela Análises (sem Android), coberta por testes. */
internal fun montarEstadoAnalises(periodo: PeriodoAnalise, ganhos: List<Ganho>): AnalisesUiState {
    val dias = analisarPorDiaDaSemana(ganhos)
    val faixas = analisarPorFaixa(ganhos)
    return AnalisesUiState(
        periodo = periodo,
        total = desempenhoDe(ganhos),
        dias = dias,
        plataformas = analisarPorPlataforma(ganhos),
        melhorDia = melhorDiaPorHora(dias),
        faixas = faixas,
        melhorFaixa = melhorFaixa(faixas),
        ganhosSemHorario = ganhosSemHorario(ganhos),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalisesViewModel(private val repo: Repositorio) : ViewModel() {

    private val periodo = MutableStateFlow(PeriodoAnalise.ULTIMOS_30)

    private val porPeriodo = periodo.flatMapLatest { p ->
        val hoje = LocalDate.now()
        repo.ganhos(p.inicio(hoje), hoje).map { ganhos -> montarEstadoAnalises(p, ganhos) }
    }

    /** Evolução dos últimos meses: não depende do período escolhido. */
    private val evolucao = YearMonth.now().let { mes ->
        val inicio = mes.minusMonths(MESES_NA_EVOLUCAO - 1L).atDay(1)
        combine(repo.ganhos(inicio, mes.atEndOfMonth()), repo.despesas(inicio, mes.atEndOfMonth())) { g, d ->
            evolucaoMensal(g, d, mes)
        }
    }

    val uiState: StateFlow<AnalisesUiState> = combine(porPeriodo, evolucao) { estado, meses ->
        estado.copy(evolucao = meses)
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
