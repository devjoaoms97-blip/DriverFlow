package br.com.motoristafinanceiro.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupAutomaticoTest {
    private val hora = 60L * 60 * 1000
    private val dia = 24 * hora
    private val agora = 1_000L * dia

    @Test
    fun frequencia_lidaPeloNome_desconhecidaViraDesligado() {
        assertEquals(FrequenciaBackup.SEMANAL, FrequenciaBackup.deNome("SEMANAL"))
        assertEquals(FrequenciaBackup.DESLIGADO, FrequenciaBackup.deNome(null))
        assertEquals(FrequenciaBackup.DESLIGADO, FrequenciaBackup.deNome("MENSAL"))
    }

    @Test
    fun desligado_nuncaEhHora() = assertFalse(backupAutomaticoDevido(FrequenciaBackup.DESLIGADO, null, agora))

    @Test
    fun nuncaFeito_ehHora() = assertTrue(backupAutomaticoDevido(FrequenciaBackup.SEMANAL, null, agora))

    @Test
    fun diario_comUmaHoraDeFolga() {
        assertFalse(backupAutomaticoDevido(FrequenciaBackup.DIARIO, agora - 22 * hora, agora))
        assertTrue(backupAutomaticoDevido(FrequenciaBackup.DIARIO, agora - 23 * hora, agora))
        assertTrue(backupAutomaticoDevido(FrequenciaBackup.DIARIO, agora - 3 * dia, agora))
    }

    @Test
    fun semanal_esperaSeteDias() {
        assertFalse(backupAutomaticoDevido(FrequenciaBackup.SEMANAL, agora - 6 * dia, agora))
        assertTrue(backupAutomaticoDevido(FrequenciaBackup.SEMANAL, agora - 7 * dia, agora))
    }

    @Test
    fun nomeDoArquivo_umPorDia() =
        assertEquals("driverflow-auto-2026-10-22.zip", nomeBackupAutomatico(LocalDate.of(2026, 10, 22)))

    @Test
    fun limpeza_mantemOsMaisNovosENuncaTocaNosManuais() {
        val automaticos = (1..10).map { nomeBackupAutomatico(LocalDate.of(2026, 10, it)) }
        val outros = listOf("motorista-financeiro-backup-2026-09-01.zip", "fotos.zip", "driverflow-auto-notas.txt")

        val apagar = backupsAutomaticosParaApagar((automaticos + outros).shuffled())

        assertEquals(automaticos.take(3).sortedDescending(), apagar) // dias 1 a 3: os mais velhos
        assertTrue(apagar.none { it in outros })
    }

    @Test
    fun limpeza_comPoucosArquivos_naoApagaNada() =
        assertTrue(backupsAutomaticosParaApagar(listOf(nomeBackupAutomatico(LocalDate.of(2026, 10, 1)))).isEmpty())
}
