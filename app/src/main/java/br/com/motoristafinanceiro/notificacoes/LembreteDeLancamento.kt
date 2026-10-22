package br.com.motoristafinanceiro.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.motoristafinanceiro.MotoristaApp
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Quanto falta até o próximo lembrete: hoje no [horarioMinutos], ou amanhã se esse horário já passou. */
fun atrasoAteLembrete(agora: LocalDateTime, horarioMinutos: Int): Duration {
    val hojeNoHorario = agora.toLocalDate().atStartOfDay().plusMinutes(horarioMinutos.toLong())
    val proximo = if (hojeNoHorario.isAfter(agora)) hojeNoHorario else hojeNoHorario.plusDays(1)
    return Duration.between(agora, proximo)
}

/**
 * Regra do lembrete, sem Android: avisa só com o lembrete ligado e sem nenhum ganho lançado hoje
 * (despesa sozinha, como um abastecimento, não conta: o lembrete é sobre o turno).
 */
class LembreteDeLancamento(
    private val repositorio: Repositorio,
    private val configuracoes: Configuracoes,
    private val notificar: () -> Boolean,
) {
    enum class Resultado { DESLIGADO, JA_LANCOU, AVISOU, NAO_ENTREGUE }

    suspend fun executar(hoje: LocalDate): Resultado {
        if (!configuracoes.lembreteAtivo.first()) return Resultado.DESLIGADO
        if (repositorio.ganhos(hoje, hoje).first().isNotEmpty()) return Resultado.JA_LANCOU
        return if (notificar()) Resultado.AVISOU else Resultado.NAO_ENTREGUE
    }
}

/** Roda o lembrete no horário e já marca o do dia seguinte. */
class LembreteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MotoristaApp
        val notificador = NotificadorAndroid(applicationContext)
        runCatching { LembreteDeLancamento(app.repositorio, app.configuracoes, notificador::mostrarLembrete).executar(LocalDate.now()) }
        if (app.configuracoes.lembreteAtivo.first()) {
            // APPEND_OR_REPLACE: o próximo entra na fila depois deste, sem cancelar o que está rodando.
            AgendadorDeLembrete.agendar(applicationContext, app.configuracoes.lembreteHorarioMinutos.first(), ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        return Result.success()
    }
}

/** Marca (ou desmarca) o lembrete do fim do dia. */
object AgendadorDeLembrete {
    const val NOME = "lembrete_lancamento"

    /** REPLACE ao mudar o horário; KEEP ao abrir o app (não desmarca o de hoje que ainda não rodou). */
    fun agendar(context: Context, horarioMinutos: Int, politica: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val atraso = atrasoAteLembrete(LocalDateTime.now(), horarioMinutos)
        val pedido = OneTimeWorkRequestBuilder<LembreteWorker>()
            .setInitialDelay(atraso.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOME, politica, pedido)
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOME)
    }

    /** Ao abrir o app: garante que o lembrete ligado está marcado. */
    suspend fun garantir(context: Context, configuracoes: Configuracoes) {
        if (configuracoes.lembreteAtivo.first()) {
            agendar(context, configuracoes.lembreteHorarioMinutos.first(), ExistingWorkPolicy.KEEP)
        }
    }
}
