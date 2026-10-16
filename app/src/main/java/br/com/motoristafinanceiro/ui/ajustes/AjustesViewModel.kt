package br.com.motoristafinanceiro.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Configuracoes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AjustesViewModel(private val configuracoes: Configuracoes) : ViewModel() {

    private var ocupado = false

    suspend fun depreciacaoAtual(): Long = configuracoes.depreciacaoPorKmCentavos.first()

    fun salvarDepreciacao(centavos: Long, aoConcluir: () -> Unit) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            configuracoes.salvarDepreciacaoPorKm(centavos)
            aoConcluir()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AjustesViewModel((this[APPLICATION_KEY] as MotoristaApp).configuracoes) }
        }
    }
}
