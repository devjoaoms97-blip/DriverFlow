package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import kotlinx.coroutines.launch

enum class TipoLancamento { GANHO, DESPESA }

/**
 * Serve para criar (tipo = null, id = 0) e para editar/excluir (tipo + id) um lançamento.
 */
class NovoLancamentoViewModel(
    private val repo: Repositorio,
    private val tipo: TipoLancamento?,
    private val id: Long,
) : ViewModel() {

    var carregando by mutableStateOf(id != 0L)
        private set
    var ganhoOriginal by mutableStateOf<Ganho?>(null)
        private set
    var despesaOriginal by mutableStateOf<Despesa?>(null)
        private set

    private var ocupado = false

    init {
        if (id != 0L) {
            viewModelScope.launch {
                when (tipo) {
                    TipoLancamento.GANHO -> ganhoOriginal = repo.ganhoPorId(id)
                    TipoLancamento.DESPESA -> despesaOriginal = repo.despesaPorId(id)
                    null -> Unit
                }
                carregando = false
            }
        }
    }

    fun salvarGanho(ganho: Ganho, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.salvarGanho(ganho) }

    fun excluirGanho(ganho: Ganho, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.excluirGanho(ganho) }

    fun salvarDespesa(despesa: Despesa, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.salvarDespesa(despesa) }

    fun excluirDespesa(despesa: Despesa, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.excluirDespesa(despesa) }

    /** Evita lançamento duplicado se o botão for tocado duas vezes. */
    private fun executar(aoConcluir: () -> Unit, bloco: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            bloco()
            aoConcluir()
        }
    }

    companion object {
        fun factory(tipo: TipoLancamento?, id: Long) = viewModelFactory {
            initializer {
                NovoLancamentoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio, tipo, id)
            }
        }
    }
}
