package br.com.motoristafinanceiro.ui.meta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.model.Meta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Define, edita e remove a meta de lucro líquido do mês atual. */
class MetaViewModel(
    private val repo: Repositorio,
    val mes: YearMonth = YearMonth.now(),
) : ViewModel() {

    private var ocupado = false

    /** Meta do mês em centavos, ou null se ainda não foi definida. */
    suspend fun metaAtual(): Long? = repo.meta(mes.toString()).first()?.valorAlvoLucroCentavos

    fun salvar(centavos: Long, aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.salvarMeta(Meta(mes.toString(), centavos)) }

    fun remover(aoConcluir: () -> Unit) =
        executar(aoConcluir) { repo.excluirMeta(mes.toString()) }

    /** Evita gravação dupla se o botão for tocado duas vezes. */
    private fun executar(aoConcluir: () -> Unit, bloco: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            bloco()
            aoConcluir()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MetaViewModel((this[APPLICATION_KEY] as MotoristaApp).repositorio) }
        }
    }
}
