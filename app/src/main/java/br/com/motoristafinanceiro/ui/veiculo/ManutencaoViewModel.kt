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
import br.com.motoristafinanceiro.data.model.Manutencao
import kotlinx.coroutines.launch

/** id = 0 cria uma manutenção nova; id != 0 edita/exclui uma existente. */
class ManutencaoViewModel(
    private val repo: Repositorio,
    private val id: Long,
) : ViewModel() {

    var carregando by mutableStateOf(id != 0L)
        private set
    var original by mutableStateOf<Manutencao?>(null)
        private set

    private var ocupado = false

    init {
        if (id != 0L) {
            viewModelScope.launch {
                original = repo.manutencaoPorId(id)
                carregando = false
            }
        }
    }

    fun salvar(manutencao: Manutencao, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.salvarManutencao(manutencao) }

    fun excluir(manutencao: Manutencao, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.excluirManutencao(manutencao) }

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
            initializer { ManutencaoViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio, id) }
        }
    }
}
