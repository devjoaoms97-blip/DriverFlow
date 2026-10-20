package br.com.motoristafinanceiro.data.backup

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** O backup em si: um .zip com `dados.json` e as fotos em `comprovantes/`. */
object ArquivoBackup {
    const val ENTRADA_DADOS = "dados.json"
    const val PASTA_FOTOS = "comprovantes/"

    /** Teto de sanidade para o texto dos dados (as fotos são copiadas em fluxo e não contam aqui). */
    private const val LIMITE_DADOS_BYTES = 64L * 1024 * 1024

    class Lido(val dados: DadosBackup, /** Nomes das fotos extraídas para a pasta temporária. */ val fotos: Set<String>)

    /**
     * Grava o backup em [saida] (e a fecha). Só entram as fotos que alguma despesa usa; as que
     * estão referenciadas mas não existem em [arquivoDaFoto] ficam de fora e são contadas em `fotosAusentes`.
     */
    fun escrever(
        saida: OutputStream,
        dados: DadosBackup,
        arquivoDaFoto: (String) -> File,
        criadoEmMs: Long,
    ): ResumoBackup {
        var incluidas = 0
        var ausentes = 0
        ZipOutputStream(saida).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRADA_DADOS))
            zip.write(BackupJson.gerar(dados, criadoEmMs).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (nome in dados.fotosReferenciadas.sorted()) {
                val arquivo = arquivoDaFoto(nome)
                if (arquivo.isFile) {
                    zip.putNextEntry(ZipEntry(PASTA_FOTOS + nome))
                    arquivo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    incluidas++
                } else {
                    ausentes++
                }
            }
        }
        return dados.paraResumo(fotos = incluidas, fotosAusentes = ausentes)
    }

    /**
     * Lê o backup de [entrada]. As fotos são extraídas para [pastaTemporaria]; nada além dela é tocado.
     * Entradas com nome suspeito (pastas, "..") e arquivos que não são `dados.json` nem fotos são ignorados.
     *
     * @throws BackupInvalidoException se o arquivo não for um backup válido.
     */
    fun ler(entrada: InputStream, pastaTemporaria: File): Lido {
        pastaTemporaria.mkdirs()
        var texto: String? = null
        val fotos = mutableSetOf<String>()
        try {
            ZipInputStream(entrada).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory) continue
                    if (e.name == ENTRADA_DADOS) {
                        texto = lerTexto(zip)
                    } else if (e.name.startsWith(PASTA_FOTOS)) {
                        val nome = e.name.removePrefix(PASTA_FOTOS)
                        if (nomeDeFotoValido(nome)) {
                            File(pastaTemporaria, nome).outputStream().use { zip.copyTo(it) }
                            fotos += nome
                        }
                    }
                }
            }
        } catch (e: ZipException) {
            throw BackupInvalidoException("O arquivo escolhido está corrompido ou não é um backup.")
        } catch (e: EOFException) {
            throw BackupInvalidoException("O arquivo escolhido está incompleto.")
        }
        val dadosTexto = texto
            ?: throw BackupInvalidoException("O arquivo escolhido não é um backup do Motorista Financeiro.")
        return Lido(BackupJson.ler(dadosTexto), fotos)
    }

    private fun lerTexto(entrada: InputStream): String {
        val saida = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val n = entrada.read(buffer)
            if (n < 0) break
            total += n
            if (total > LIMITE_DADOS_BYTES) throw BackupInvalidoException("Os dados do backup são grandes demais.")
            saida.write(buffer, 0, n)
        }
        return saida.toString(Charsets.UTF_8.name())
    }
}
