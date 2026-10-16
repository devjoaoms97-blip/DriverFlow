package br.com.motoristafinanceiro.ui.veiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.ResumoVeiculo
import br.com.motoristafinanceiro.domain.calcularAlertas
import br.com.motoristafinanceiro.domain.calcularEstatisticas
import br.com.motoristafinanceiro.domain.calcularResumoVeiculo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class VeiculoUiState(
    val resumo: ResumoVeiculo = ResumoVeiculo(null, null, null, null),
    /** Mais recentes primeiro. */
    val abastecimentos: List<Abastecimento> = emptyList(),
    /** Mais recentes primeiro. */
    val manutencoes: List<Manutencao> = emptyList(),
    /** Um por serviço, mais urgentes primeiro. */
    val alertas: List<AlertaManutencao> = emptyList(),
)

/** Regra pura da tela (sem Android), coberta por testes. */
internal fun montarEstadoVeiculo(
    abastecimentos: List<Abastecimento>,
    manutencoes: List<Manutencao>,
    ganhosMes: List<Ganho>,
    despesasMes: List<Despesa>,
): VeiculoUiState {
    val resumo = calcularResumoVeiculo(abastecimentos, manutencoes, calcularEstatisticas(ganhosMes, despesasMes))
    val odometro = resumo.odometroAtualKm
    return VeiculoUiState(
        resumo = resumo,
        abastecimentos = abastecimentos.sortedWith(
            compareByDescending<Abastecimento> { it.data.toEpochDay() }.thenByDescending { it.odometroKm }
        ),
        manutencoes = manutencoes.sortedWith(
            compareByDescending<Manutencao> { it.data.toEpochDay() }.thenByDescending { it.odometroKm }
        ),
        alertas = if (odometro != null) calcularAlertas(manutencoes, odometro) else emptyList(),
    )
}

class VeiculoViewModel(repo: Repositorio) : ViewModel() {

    private val mes = YearMonth.now()

    val uiState: StateFlow<VeiculoUiState> = combine(
        repo.abastecimentos(),
        repo.manutencoes(),
        repo.ganhos(mes.atDay(1), mes.atEndOfMonth()),
        repo.despesas(mes.atDay(1), mes.atEndOfMonth()),
    ) { abastecimentos, manutencoes, ganhos, despesas ->
        montarEstadoVeiculo(abastecimentos, manutencoes, ganhos, despesas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VeiculoUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { VeiculoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
