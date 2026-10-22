package br.com.motoristafinanceiro.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.notificacoes.AgendadorDeLembrete
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.ModoTema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** [agendarLembrete] marca (ligado, horário) ou desmarca o lembrete no Android; nos testes, não faz nada. */
class AjustesViewModel(
    private val configuracoes: Configuracoes,
    private val agendarLembrete: (Boolean, Int) -> Unit = { _, _ -> },
) : ViewModel() {

    private var ocupado = false

    suspend fun depreciacaoAtual(): Long = configuracoes.depreciacaoPorKmCentavos.first()

    val modoTema: Flow<ModoTema> = configuracoes.modoTema

    fun definirModoTema(modo: ModoTema) {
        viewModelScope.launch { configuracoes.salvarModoTema(modo) }
    }

    val notificacoesAtivas: Flow<Boolean> = configuracoes.notificacoesManutencaoAtivas

    fun definirNotificacoes(ativas: Boolean) {
        viewModelScope.launch { configuracoes.salvarNotificacoesManutencao(ativas) }
    }

    val lembreteAtivo: Flow<Boolean> = configuracoes.lembreteAtivo
    val lembreteHorarioMinutos: Flow<Int> = configuracoes.lembreteHorarioMinutos

    /** Liga, desliga ou muda o horário do lembrete; [horarioMinutos] null = mantém o atual. */
    fun definirLembrete(ativo: Boolean, horarioMinutos: Int? = null) {
        viewModelScope.launch {
            val horario = horarioMinutos ?: configuracoes.lembreteHorarioMinutos.first()
            configuracoes.salvarLembrete(ativo, horario)
            agendarLembrete(ativo, horario)
        }
    }

    val bloqueioAtivo: Flow<Boolean> = configuracoes.bloqueioAtivo

    fun definirBloqueio(ativo: Boolean) {
        viewModelScope.launch { configuracoes.salvarBloqueio(ativo) }
    }

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
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                AjustesViewModel(app.configuracoes) { ativo, horario ->
                    if (ativo) AgendadorDeLembrete.agendar(app, horario) else AgendadorDeLembrete.cancelar(app)
                }
            }
        }
    }
}
