package br.com.motoristafinanceiro.notificacoes

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.motoristafinanceiro.MotoristaApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/** Quando a verificação de manutenção roda. */
object AgendadorDeNotificacoes {
    const val NOME_DIARIA = "verificacao_manutencao_diaria"
    const val NOME_IMEDIATA = "verificacao_manutencao_imediata"

    /** Rede de segurança: uma verificação por dia (KEEP: não reinicia o relógio a cada abertura do app). */
    fun agendarVerificacaoDiaria(context: Context) {
        val pedido = PeriodicWorkRequestBuilder<VerificarManutencaoWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(NOME_DIARIA, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Verificação já. REPLACE junta várias mudanças seguidas (ex.: restaurar backup) em uma só. */
    fun verificarAgora(context: Context) {
        val pedido = OneTimeWorkRequestBuilder<VerificarManutencaoWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOME_IMEDIATA, ExistingWorkPolicy.REPLACE, pedido)
    }

    /**
     * Verifica a cada mudança em manutenções, no odômetro (leituras e km dos ganhos depois delas) ou na chave
     * "avisar sobre manutenção". Fica coletando enquanto a tela do app estiver aberta.
     */
    suspend fun verificarACadaMudanca(app: MotoristaApp) {
        combine(
            app.repositorio.manutencoes(),
            app.repositorio.odometro().map { it?.km }.distinctUntilChanged(),
            // distinctUntilChanged: o DataStore emite a cada gravação (até de outras chaves), e só esta interessa.
            app.configuracoes.notificacoesManutencaoAtivas.distinctUntilChanged(),
        ) { _, _, _ -> }.collect { verificarAgora(app) }
    }
}
