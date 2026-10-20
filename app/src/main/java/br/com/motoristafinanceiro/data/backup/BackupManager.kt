package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.ComprovanteStorage
import br.com.motoristafinanceiro.data.Configuracoes
import br.com.motoristafinanceiro.data.Repositorio
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Exportar e restaurar o backup completo: banco, depreciação por km e fotos dos comprovantes.
 * Faz trabalho de arquivo; chame de uma thread de IO.
 */
class BackupManager(
    private val repo: Repositorio,
    private val configuracoes: Configuracoes,
    private val comprovantes: ComprovanteStorage,
    private val pastaTemporaria: File,
) {

    suspend fun exportar(saida: OutputStream, agoraMs: Long = System.currentTimeMillis()): ResumoBackup {
        val dados = repo.lerTudo(configuracoes.depreciacaoPorKmCentavos.first())
        return ArquivoBackup.escrever(saida, dados, comprovantes::arquivo, agoraMs)
    }

    /**
     * Substitui TODOS os dados atuais pelos do backup.
     *
     * Ordem pensada para não perder nada se algo falhar: o arquivo inteiro é lido e validado primeiro
     * (erro aqui não altera nada); depois as fotos são só adicionadas, o banco é trocado em uma transação,
     * a depreciação é gravada e, por último, as fotos que não pertencem mais a nenhuma despesa são apagadas.
     *
     * @throws BackupInvalidoException se o arquivo não for um backup válido; os dados atuais ficam intactos.
     */
    suspend fun restaurar(entrada: InputStream): ResumoBackup {
        pastaTemporaria.deleteRecursively()
        try {
            val lido = ArquivoBackup.ler(entrada, pastaTemporaria)
            val (dados, ausentes) = reconciliarFotos(lido.dados, lido.fotos)
            val fotos = dados.fotosReferenciadas

            fotos.forEach { comprovantes.restaurar(File(pastaTemporaria, it), it) }
            repo.substituirTudo(dados)
            configuracoes.salvarDepreciacaoPorKm(dados.depreciacaoPorKmCentavos)
            comprovantes.manterSomente(fotos)

            return dados.paraResumo(fotos = fotos.size, fotosAusentes = ausentes)
        } finally {
            pastaTemporaria.deleteRecursively()
        }
    }
}
