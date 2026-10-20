package br.com.motoristafinanceiro

import android.app.Application
import br.com.motoristafinanceiro.data.ComprovanteStorage
import br.com.motoristafinanceiro.data.backup.BackupManager
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import br.com.motoristafinanceiro.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class MotoristaApp : Application() {
    val database by lazy { AppDatabase.criar(this) }
    val repositorio by lazy { Repositorio(database) }
    val configuracoes by lazy { Configuracoes.criar(this) }
    val comprovantes by lazy { ComprovanteStorage(File(filesDir, "comprovantes")) }
    val backup by lazy { BackupManager(repositorio, configuracoes, comprovantes, File(cacheDir, "restauracao")) }

    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Fotos que sobraram (ex.: foto tirada e formulário abandonado) são apagadas depois de 24 h.
        escopoApp.launch {
            runCatching { comprovantes.limparOrfaos(repositorio.nomesDeComprovantesEmUso().toSet()) }
        }
    }
}
