package br.com.motoristafinanceiro.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.motoristafinanceiro.MotoristaApp

/** Roda o [VerificadorDeManutencao] em segundo plano (WorkManager), mesmo com o app fechado. */
class VerificarManutencaoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MotoristaApp
        val notificador = NotificadorAndroid(applicationContext)
        val verificador = VerificadorDeManutencao(
            repositorio = app.repositorio,
            configuracoes = app.configuracoes,
            notificar = notificador::mostrar,
            limpar = notificador::cancelar,
        )
        // Melhor esforço: se algo falhar, a próxima verificação tenta de novo.
        runCatching { verificador.verificar() }
        return Result.success()
    }
}
