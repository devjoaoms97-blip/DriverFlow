package br.com.motoristafinanceiro.ui.veiculo

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
import br.com.motoristafinanceiro.data.model.Abastecimento
import kotlinx.coroutines.launch

/** id = 0 cria um abastecimento novo; id != 0 edita/exclui um existente. */
class AbastecimentoViewModel(
    private val repo: Repositorio,
    private val id: Long,
) : ViewModel() {

    var carregando by mutableStateOf(id != 0L)
        private set
    var original by mutableStateOf<Abastecimento?>(null)
        private set

    private var ocupado = false

    init {
        if (id != 0L) {
            viewModelScope.launch {
                original = repo.abastecimentoPorId(id)
                carregando = false
            }
        }
    }

    fun salvar(abastecimento: Abastecimento, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.salvarAbastecimento(abastecimento) }

    fun excluir(abastecimento: Abastecimento, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.excluirAbastecimento(abastecimento) }

    /** Evita registro duplicado se o botão for tocado duas vezes. */
    private fun executar(aoConcluir: () -> Unit, bloco: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            bloco()
            aoConcluir()
        }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory {
            initializer { AbastecimentoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio, id) }
        }
    }
}
