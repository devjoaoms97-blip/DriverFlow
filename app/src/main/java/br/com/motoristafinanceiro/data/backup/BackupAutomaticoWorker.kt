package br.com.motoristafinanceiro.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.com.motoristafinanceiro.MotoristaApp
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Roda o [ExecutorBackupAutomatico] em segundo plano (WorkManager), mesmo com o app fechado. */
class BackupAutomaticoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MotoristaApp
        val executor = ExecutorBackupAutomatico(app.configuracoes, app.backup) { uri ->
            PastaDocumentos.abrir(app.contentResolver, uri)
        }
        // O erro, se houver, fica gravado nas configurações e aparece em Ajustes; a próxima rodada tenta de novo.
        runCatching {
            executor.executar(System.currentTimeMillis(), LocalDate.now(), forcar = inputData.getBoolean(FORCAR, false))
        }
        return Result.success()
    }

    companion object {
        const val FORCAR = "forcar"
    }
}

/** Quando o backup automático roda. */
object AgendadorDeBackup {
    const val NOME_PERIODICO = "backup_automatico_periodico"
    const val NOME_IMEDIATO = "backup_automatico_imediato"

    /**
     * Confere a cada 12 h se é hora (a frequência diária ou semanal é decidida pelo executor). KEEP: não reinicia
     * o relógio a cada abertura do app.
     */
    fun agendar(context: Context) {
        val pedido = PeriodicWorkRequestBuilder<BackupAutomaticoWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(NOME_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Roda já: ao abrir o app, ao trocar a frequência ou a pasta ([forcar] = false), ou no "Fazer agora" (true). */
    fun rodarAgora(context: Context, forcar: Boolean = false) {
        val pedido = OneTimeWorkRequestBuilder<BackupAutomaticoWorker>()
            .setInputData(workDataOf(BackupAutomaticoWorker.FORCAR to forcar))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOME_IMEDIATO, ExistingWorkPolicy.REPLACE, pedido)
    }
}
