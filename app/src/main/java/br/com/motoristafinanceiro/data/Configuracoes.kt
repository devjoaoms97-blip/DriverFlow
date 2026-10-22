package br.com.motoristafinanceiro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.motoristafinanceiro.data.backup.FrequenciaBackup
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

    /** Aparência do app (Automático, Claro ou Escuro). Padrão: Escuro. Fica só no aparelho, fora do backup. */
    val modoTema: Flow<ModoTema> = dataStore.data.map { ModoTema.deNome(it[CHAVE_MODO_TEMA]) }

    suspend fun salvarModoTema(modo: ModoTema) {
        dataStore.edit { it[CHAVE_MODO_TEMA] = modo.name }
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

    /** Frequência do backup automático. Padrão: desligado (precisa de uma pasta escolhida). */
    val frequenciaBackup: Flow<FrequenciaBackup> =
        dataStore.data.map { FrequenciaBackup.deNome(it[CHAVE_BACKUP_FREQUENCIA]) }

    suspend fun salvarFrequenciaBackup(frequencia: FrequenciaBackup) {
        dataStore.edit { it[CHAVE_BACKUP_FREQUENCIA] = frequencia.name }
    }

    /** Pasta (árvore do seletor do Android) onde o backup automático grava; null = nenhuma escolhida. */
    val pastaBackup: Flow<String?> = dataStore.data.map { it[CHAVE_BACKUP_PASTA] }

    suspend fun salvarPastaBackup(uri: String?) {
        dataStore.edit { if (uri == null) it.remove(CHAVE_BACKUP_PASTA) else it[CHAVE_BACKUP_PASTA] = uri }
    }

    /** Quando foi o último backup que deu certo (manual ou automático), em ms; null = nunca. */
    val ultimoBackupMs: Flow<Long?> = dataStore.data.map { it[CHAVE_BACKUP_ULTIMO_MS] }

    /** Erro da última tentativa automática; null quando deu certo (ou nunca tentou). */
    val erroBackupAutomatico: Flow<String?> = dataStore.data.map { it[CHAVE_BACKUP_ERRO] }

    /** Backup feito agora: guarda a hora e apaga o erro anterior, se havia. */
    suspend fun registrarBackupFeito(agoraMs: Long) {
        dataStore.edit {
            it[CHAVE_BACKUP_ULTIMO_MS] = agoraMs
            it.remove(CHAVE_BACKUP_ERRO)
        }
    }

    suspend fun registrarErroBackupAutomatico(mensagem: String) {
        dataStore.edit { it[CHAVE_BACKUP_ERRO] = mensagem }
    }

    /** Pedir a digital ou o PIN do celular ao abrir o app. Padrão: desligado. Fica só no aparelho, fora do backup. */
    val bloqueioAtivo: Flow<Boolean> = dataStore.data.map { it[CHAVE_BLOQUEIO] ?: false }

    suspend fun salvarBloqueio(ativo: Boolean) {
        dataStore.edit { it[CHAVE_BLOQUEIO] = ativo }
    }

    /** Lembrete no fim do dia quando nenhum ganho foi lançado. Padrão: desligado. */
    val lembreteAtivo: Flow<Boolean> = dataStore.data.map { it[CHAVE_LEMBRETE_ATIVO] ?: false }

    /** Horário do lembrete em minutos desde a meia-noite. Padrão: 22:00. */
    val lembreteHorarioMinutos: Flow<Int> = dataStore.data.map { it[CHAVE_LEMBRETE_HORARIO] ?: HORARIO_PADRAO_LEMBRETE }

    suspend fun salvarLembrete(ativo: Boolean, horarioMinutos: Int) {
        dataStore.edit {
            it[CHAVE_LEMBRETE_ATIVO] = ativo
            it[CHAVE_LEMBRETE_HORARIO] = horarioMinutos.coerceIn(0, 24 * 60 - 1)
        }
    }

    companion object {
        const val HORARIO_PADRAO_LEMBRETE = 22 * 60
        private val CHAVE_LEMBRETE_ATIVO = booleanPreferencesKey("lembrete_ativo")
        private val CHAVE_LEMBRETE_HORARIO = intPreferencesKey("lembrete_horario_minutos")
        private val CHAVE_BLOQUEIO = booleanPreferencesKey("bloqueio_ativo")
        private val CHAVE_BACKUP_FREQUENCIA = stringPreferencesKey("backup_frequencia")
        private val CHAVE_BACKUP_PASTA = stringPreferencesKey("backup_pasta_uri")
        private val CHAVE_BACKUP_ULTIMO_MS = longPreferencesKey("backup_ultimo_ms")
        private val CHAVE_BACKUP_ERRO = stringPreferencesKey("backup_automatico_erro")
        private val CHAVE_DEPRECIACAO_POR_KM = longPreferencesKey("depreciacao_por_km_centavos")
        private val CHAVE_NOTIFICACOES_MANUTENCAO = booleanPreferencesKey("notificacoes_manutencao_ativas")
        private val CHAVE_ALERTAS_NOTIFICADOS = stringSetPreferencesKey("alertas_manutencao_notificados")
        private val CHAVE_PERMISSAO_PERGUNTADA = booleanPreferencesKey("permissao_notificacao_perguntada")
        private val CHAVE_MODO_TEMA = stringPreferencesKey("modo_tema")

        fun criar(context: Context): Configuracoes = Configuracoes(context.applicationContext.dataStoreConfiguracoes)
    }
}
