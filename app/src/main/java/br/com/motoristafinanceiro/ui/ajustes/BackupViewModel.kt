package br.com.motoristafinanceiro.ui.ajustes

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.backup.BackupInvalidoException
import br.com.motoristafinanceiro.data.backup.BackupManager
import br.com.motoristafinanceiro.data.backup.nomeArquivoBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

sealed interface EstadoBackup {
    data object Ocioso : EstadoBackup
    data class Trabalhando(val texto: String) : EstadoBackup
    data class Resultado(val texto: String, val erro: Boolean) : EstadoBackup
}

class BackupViewModel(
    private val backup: BackupManager,
    private val resolver: ContentResolver,
) : ViewModel() {

    var estado by mutableStateOf<EstadoBackup>(EstadoBackup.Ocioso)
        private set

    val nomeSugerido: String get() = nomeArquivoBackup(LocalDate.now())

    private val ocupado get() = estado is EstadoBackup.Trabalhando

    /** [destino] é o arquivo escolhido no seletor do sistema (null = a pessoa cancelou). */
    fun exportar(destino: Uri?) {
        if (destino == null || ocupado) return
        estado = EstadoBackup.Trabalhando("Salvando o backup...")
        viewModelScope.launch {
            estado = try {
                // NonCancellable: sair da tela no meio não pode deixar o arquivo pela metade.
                val resumo = withContext(Dispatchers.IO + NonCancellable) {
                    val saida = resolver.openOutputStream(destino) ?: error("Não foi possível abrir o arquivo escolhido.")
                    saida.use { backup.exportar(it) }
                }
                EstadoBackup.Resultado(textoBackupSalvo(resumo), erro = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoBackup.Resultado("Não foi possível salvar o backup: ${e.message ?: "erro desconhecido"}", erro = true)
            }
        }
    }

    /** [origem] é o arquivo escolhido no seletor do sistema. [aoRestaurar] roda só se a restauração der certo. */
    fun restaurar(origem: Uri?, aoRestaurar: () -> Unit) {
        if (origem == null || ocupado) return
        estado = EstadoBackup.Trabalhando("Restaurando o backup...")
        viewModelScope.launch {
            estado = try {
                // NonCancellable: sair da tela no meio não pode interromper a troca dos dados.
                val resumo = withContext(Dispatchers.IO + NonCancellable) {
                    val entrada = resolver.openInputStream(origem) ?: error("Não foi possível abrir o arquivo escolhido.")
                    entrada.use { backup.restaurar(it) }
                }
                aoRestaurar()
                EstadoBackup.Resultado(textoBackupRestaurado(resumo), erro = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupInvalidoException) {
                EstadoBackup.Resultado("${e.message} Nada foi alterado.", erro = true)
            } catch (e: Exception) {
                EstadoBackup.Resultado("Não foi possível restaurar o backup: ${e.message ?: "erro desconhecido"}", erro = true)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MotoristaApp
                BackupViewModel(app.backup, app.contentResolver)
            }
        }
    }
}
