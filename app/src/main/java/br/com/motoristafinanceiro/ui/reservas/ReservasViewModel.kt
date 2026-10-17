package br.com.motoristafinanceiro.ui.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.domain.diasParaVencer
import br.com.motoristafinanceiro.domain.proximaOcorrencia
import br.com.motoristafinanceiro.domain.reservaDiariaCentavos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ItemReserva(
    val reserva: ReservaDespesa,
    val diariaCentavos: Long,
    val proximaOcorrencia: LocalDate,
    val diasParaVencer: Long,
)

data class ReservasUiState(
    /** Vencimentos mais próximos primeiro. */
    val itens: List<ItemReserva> = emptyList(),
    val totalDiarioCentavos: Long = 0,
    val totalSemanalCentavos: Long = 0,
)

/** Regra pura da tela (sem Android), coberta por testes. */
internal fun montarEstadoReservas(reservas: List<ReservaDespesa>, hoje: LocalDate): ReservasUiState {
    val itens = reservas
        .map { ItemReserva(it, reservaDiariaCentavos(it), proximaOcorrencia(it, hoje), diasParaVencer(it, hoje)) }
        .sortedBy { it.diasParaVencer }
    val totalDiario = itens.sumOf { it.diariaCentavos }
    return ReservasUiState(itens, totalDiario, totalDiario * 7)
}

class ReservasViewModel(repo: Repositorio) : ViewModel() {

    val uiState: StateFlow<ReservasUiState> = repo.reservas()
        .map { montarEstadoReservas(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservasUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { ReservasViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
