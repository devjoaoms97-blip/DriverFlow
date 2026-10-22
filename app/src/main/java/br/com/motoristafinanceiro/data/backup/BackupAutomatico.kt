package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.Configuracoes
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.time.LocalDate

/** De quanto em quanto tempo o backup automático roda. */
enum class FrequenciaBackup(val rotulo: String, val dias: Int?) {
    DESLIGADO("Desligado", null),
    DIARIO("Diário", 1),
    SEMANAL("Semanal", 7);

    companion object {
        /** Lê o nome guardado; desconhecido ou ausente = desligado. */
        fun deNome(nome: String?): FrequenciaBackup = entries.firstOrNull { it.name == nome } ?: DESLIGADO
    }
}

private const val MS_POR_HORA = 60L * 60 * 1000

/**
 * Se já é hora do backup automático. Uma hora de folga: a tarefa do Android não roda no minuto exato,
 * e um backup diário feito às 10h05 não pode esperar até as 10h05 do dia seguinte para valer como "um dia".
 */
fun backupAutomaticoDevido(frequencia: FrequenciaBackup, ultimoMs: Long?, agoraMs: Long): Boolean {
    val dias = frequencia.dias ?: return false
    if (ultimoMs == null) return true
    return agoraMs - ultimoMs >= dias * 24 * MS_POR_HORA - MS_POR_HORA
}

/** Prefixo dos arquivos do backup automático: só esses são apagados na limpeza. */
const val PREFIXO_BACKUP_AUTOMATICO = "driverflow-auto-"

/** Quantos backups automáticos ficam na pasta (os mais novos). */
const val BACKUPS_AUTOMATICOS_MANTIDOS = 7

/** "driverflow-auto-2026-10-22.zip": um por dia (o do mesmo dia é substituído). */
fun nomeBackupAutomatico(data: LocalDate): String = "$PREFIXO_BACKUP_AUTOMATICO$data.zip"

/**
 * Arquivos automáticos antigos a apagar, mantendo os [manter] mais novos. Arquivos que não começam com o
 * prefixo (como os backups manuais) nunca entram na lista.
 */
fun backupsAutomaticosParaApagar(nomes: List<String>, manter: Int = BACKUPS_AUTOMATICOS_MANTIDOS): List<String> =
    nomes.filter { it.startsWith(PREFIXO_BACKUP_AUTOMATICO) && it.endsWith(".zip") }
        .sortedDescending() // a data no nome (AAAA-MM-DD) ordena do mais novo para o mais velho
        .drop(manter)

/** A pasta de destino, vista só pelo que o backup automático precisa (no app é uma pasta do seletor do Android). */
interface PastaDeBackup {
    /** Cria (ou substitui) o arquivo e entrega a saída para gravar. */
    fun criar(nome: String): OutputStream

    fun listar(): List<String>

    fun apagar(nome: String)
}

/**
 * Regra do backup automático, sem Android: confere se é hora, grava o .zip na pasta, registra a hora e
 * limpa os automáticos antigos. [abrirPasta] devolve null quando não há pasta escolhida.
 */
class ExecutorBackupAutomatico(
    private val configuracoes: Configuracoes,
    private val backup: BackupManager,
    private val abrirPasta: (String) -> PastaDeBackup?,
) {
    enum class Resultado { DESLIGADO, AINDA_NAO, SEM_PASTA, FEITO, FALHOU }

    /** [forcar] = "Fazer agora" em Ajustes: ignora a frequência (mas precisa de pasta). */
    suspend fun executar(agoraMs: Long, hoje: LocalDate, forcar: Boolean = false): Resultado {
        val frequencia = configuracoes.frequenciaBackup.first()
        if (!forcar && frequencia == FrequenciaBackup.DESLIGADO) return Resultado.DESLIGADO
        if (!forcar && !backupAutomaticoDevido(frequencia, configuracoes.ultimoBackupMs.first(), agoraMs)) {
            return Resultado.AINDA_NAO
        }
        val uri = configuracoes.pastaBackup.first()
        val pasta = uri?.let(abrirPasta)
        if (pasta == null) {
            configuracoes.registrarErroBackupAutomatico("Escolha a pasta do backup automático de novo.")
            return Resultado.SEM_PASTA
        }
        return try {
            pasta.criar(nomeBackupAutomatico(hoje)).use { backup.exportar(it, agoraMs) }
            configuracoes.registrarBackupFeito(agoraMs)
            // Limpeza é melhor esforço: o backup novo já está salvo.
            runCatching { backupsAutomaticosParaApagar(pasta.listar()).forEach(pasta::apagar) }
            Resultado.FEITO
        } catch (e: Exception) {
            configuracoes.registrarErroBackupAutomatico("Não foi possível gravar na pasta: ${e.message ?: "erro desconhecido"}")
            Resultado.FALHOU
        }
    }
}
