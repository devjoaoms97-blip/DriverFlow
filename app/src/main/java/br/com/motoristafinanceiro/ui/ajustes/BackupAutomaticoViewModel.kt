package br.com.motoristafinanceiro.ui.ajustes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.backup.AgendadorDeBackup
import br.com.motoristafinanceiro.data.backup.FrequenciaBackup
import br.com.motoristafinanceiro.data.backup.PastaDocumentos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Frequência, pasta e situação do backup automático em Ajustes. */
class BackupAutomaticoViewModel(
    private val app: Application,
    private val configuracoes: Configuracoes,
) : ViewModel() {

    val frequencia: Flow<FrequenciaBackup> = configuracoes.frequenciaBackup
    val pasta: Flow<String?> = configuracoes.pastaBackup
    val ultimoBackupMs: Flow<Long?> = configuracoes.ultimoBackupMs
    val erro: Flow<String?> = configuracoes.erroBackupAutomatico

    fun nomeDaPasta(uri: String): String? = PastaDocumentos.nome(app.contentResolver, uri)

    fun definirFrequencia(nova: FrequenciaBackup) {
        viewModelScope.launch {
            configuracoes.salvarFrequenciaBackup(nova)
            AgendadorDeBackup.rodarAgora(app)
        }
    }

    /** Pasta escolhida no seletor: guarda a permissão (vale depois de reiniciar) e solta a da pasta anterior. */
    fun definirPasta(uri: Uri, frequencia: FrequenciaBackup?) {
        viewModelScope.launch {
            val resolver = app.contentResolver
            resolver.takePersistableUriPermission(uri, PastaDocumentos.PERMISSOES)
            configuracoes.pastaBackup.first()?.let { anterior ->
                if (anterior != uri.toString()) {
                    runCatching { resolver.releasePersistableUriPermission(Uri.parse(anterior), PastaDocumentos.PERMISSOES) }
                }
            }
            configuracoes.salvarPastaBackup(uri.toString())
            frequencia?.let { configuracoes.salvarFrequenciaBackup(it) }
            AgendadorDeBackup.rodarAgora(app)
        }
    }

    /** "Fazer agora": grava na pasta já, sem esperar a frequência. */
    fun fazerAgora() = AgendadorDeBackup.rodarAgora(app, forcar = true)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                BackupAutomaticoViewModel(app, app.configuracoes)
            }
        }
    }
}
