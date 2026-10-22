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
import br.com.motoristafinanceiro.data.ComprovanteStorage
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.domain.fotoSubstituidaOuRemovida
import kotlinx.coroutines.launch

enum class TipoLancamento { GANHO, DESPESA }

/**
 * Serve para criar (tipo = null, id = 0) e para editar/excluir (tipo + id) um lançamento.
 */
class NovoLancamentoViewModel(
    private val repo: Repositorio,
    val comprovantes: ComprovanteStorage,
    private val tipo: TipoLancamento?,
    private val id: Long,
) : ViewModel() {

    var carregando by mutableStateOf(id != 0L)
        private set
    var ganhoOriginal by mutableStateOf<Ganho?>(null)
        private set
    var despesaOriginal by mutableStateOf<Despesa?>(null)
        private set

    /** Último ganho lançado, para o atalho "Repetir o último" (só ao criar). */
    var ultimoGanho by mutableStateOf<Ganho?>(null)
        private set

    private var ocupado = false

    init {
        if (id == 0L) viewModelScope.launch { ultimoGanho = repo.ultimoGanho() }
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

    /** Ao salvar, a foto antiga (trocada ou removida) é apagada do aparelho. */
    fun salvarDespesa(despesa: Despesa, aoConcluir: () -> Unit) {
        val antiga = despesaOriginal?.fotoUri
        executar(aoConcluir) {
            repo.salvarDespesa(despesa)
            comprovantes.excluir(fotoSubstituidaOuRemovida(antiga, despesa.fotoUri))
        }
    }

    /** Ao excluir a despesa, a foto dela também é apagada. */
    fun excluirDespesa(despesa: Despesa, aoConcluir: () -> Unit) =
        executar(aoConcluir) {
            repo.excluirDespesa(despesa)
            comprovantes.excluir(despesa.fotoUri)
        }

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
                val app = this[APPLICATION_KEY] as MotoristaApp
                NovoLancamentoViewModel(app.repositorio, app.comprovantes, tipo, id)
            }
        }
    }
}
