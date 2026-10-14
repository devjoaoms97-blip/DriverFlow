package br.com.motoristafinanceiro.ui.lancamento

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
import kotlinx.coroutines.launch
import java.time.LocalDate

class NovoLancamentoViewModel(private val repo: Repositorio) : ViewModel() {

    fun salvarGanho(
        data: LocalDate,
        plataforma: Plataforma,
        valorCentavos: Long,
        gorjetaBonusCentavos: Long,
        km: Double,
        minutosOnline: Int,
        aoConcluir: () -> Unit,
    ) {
        viewModelScope.launch {
            repo.salvarGanho(
                Ganho(
                    data = data,
                    plataforma = plataforma,
                    valorCentavos = valorCentavos,
                    gorjetaBonusCentavos = gorjetaBonusCentavos,
                    km = km,
                    minutosOnline = minutosOnline,
                )
            )
            aoConcluir()
        }
    }

    fun salvarDespesa(
        data: LocalDate,
        categoria: CategoriaDespesa,
        valorCentavos: Long,
        observacao: String?,
        aoConcluir: () -> Unit,
    ) {
        viewModelScope.launch {
            repo.salvarDespesa(
                Despesa(data = data, categoria = categoria, valorCentavos = valorCentavos, observacao = observacao)
            )
            aoConcluir()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NovoLancamentoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
