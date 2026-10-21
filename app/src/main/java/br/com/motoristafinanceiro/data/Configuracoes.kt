package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStoreConfiguracoes: DataStore<Preferences> by preferencesDataStore(name = "configuracoes")

/** Ajustes do usuário, guardados fora do Room (DataStore). */
class Configuracoes(private val dataStore: DataStore<Preferences>) {

    /** Depreciação estimada do carro por km rodado, em centavos. Padrão: 0 (ignorada). */
    val depreciacaoPorKmCentavos: Flow<Long> =
        dataStore.data.map { it[CHAVE_DEPRECIACAO_POR_KM] ?: 0L }

    suspend fun salvarDepreciacaoPorKm(centavos: Long) {
        dataStore.edit { it[CHAVE_DEPRECIACAO_POR_KM] = centavos.coerceAtLeast(0) }
    }

    /** Avisar (notificação) quando uma manutenção ficar próxima ou atrasada. Padrão: ligado. */
    val notificacoesManutencaoAtivas: Flow<Boolean> =
        dataStore.data.map { it[CHAVE_NOTIFICACOES_MANUTENCAO] ?: true }

    suspend fun salvarNotificacoesManutencao(ativas: Boolean) {
        dataStore.edit { it[CHAVE_NOTIFICACOES_MANUTENCAO] = ativas }
    }

    /** Se o app já mostrou a pergunta de permissão de notificação ao abrir (para perguntar só uma vez). */
    val permissaoNotificacaoPerguntada: Flow<Boolean> =
        dataStore.data.map { it[CHAVE_PERMISSAO_PERGUNTADA] ?: false }

    suspend fun marcarPermissaoNotificacaoPerguntada() {
        dataStore.edit { it[CHAVE_PERMISSAO_PERGUNTADA] = true }
    }

    /** Chaves dos alertas que já geraram notificação (evita avisar o mesmo alerta de novo). */
    val alertasNotificados: Flow<Set<String>> =
        dataStore.data.map { it[CHAVE_ALERTAS_NOTIFICADOS] ?: emptySet() }

    suspend fun salvarAlertasNotificados(chaves: Set<String>) {
        dataStore.edit { it[CHAVE_ALERTAS_NOTIFICADOS] = chaves }
    }

    companion object {
        private val CHAVE_DEPRECIACAO_POR_KM = longPreferencesKey("depreciacao_por_km_centavos")
        private val CHAVE_NOTIFICACOES_MANUTENCAO = booleanPreferencesKey("notificacoes_manutencao_ativas")
        private val CHAVE_ALERTAS_NOTIFICADOS = stringSetPreferencesKey("alertas_manutencao_notificados")
        private val CHAVE_PERMISSAO_PERGUNTADA = booleanPreferencesKey("permissao_notificacao_perguntada")

        fun criar(context: Context): Configuracoes = Configuracoes(context.applicationContext.dataStoreConfiguracoes)
    }
}
