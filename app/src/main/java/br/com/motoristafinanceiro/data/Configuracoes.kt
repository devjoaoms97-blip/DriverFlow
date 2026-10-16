package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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

    companion object {
        private val CHAVE_DEPRECIACAO_POR_KM = longPreferencesKey("depreciacao_por_km_centavos")

        fun criar(context: Context): Configuracoes = Configuracoes(context.applicationContext.dataStoreConfiguracoes)
    }
}
