package br.com.motoristafinanceiro.data

import android.content.ContentResolver
import android.net.Uri
import br.com.motoristafinanceiro.domain.IDADE_MINIMA_ORFAO_MS
import br.com.motoristafinanceiro.domain.InfoArquivo
import br.com.motoristafinanceiro.domain.arquivosOrfaos
import br.com.motoristafinanceiro.domain.novoNomeComprovante
import java.io.File
import java.io.InputStream
import kotlin.random.Random

/**
 * Fotos de comprovante guardadas na pasta privada do app. No banco, o campo `fotoUri` da despesa
 * guarda apenas o NOME do arquivo (não um caminho), para continuar válido se o app for restaurado.
 */
class ComprovanteStorage(private val pasta: File) {

    /** Cria um arquivo vazio e único, pronto para receber uma foto. */
    fun novoArquivo(): File {
        pasta.mkdirs()
        var arquivo: File
        do {
            arquivo = File(pasta, novoNomeComprovante(System.currentTimeMillis(), Random.nextInt(1_000_000)))
        } while (arquivo.exists())
        arquivo.createNewFile()
        return arquivo
    }

    /** Resolve o nome para o arquivo, ignorando qualquer pasta que venha no nome. */
    fun arquivo(nome: String): File = File(pasta, File(nome).name)

    fun excluir(nome: String?) {
        if (nome.isNullOrBlank()) return
        val arquivo = arquivo(nome)
        if (arquivo.isFile) arquivo.delete()
    }

    /** Copia o conteúdo para um arquivo novo e devolve esse arquivo. */
    fun salvarEntrada(entrada: InputStream): File {
        val destino = novoArquivo()
        destino.outputStream().use { saida -> entrada.copyTo(saida) }
        return destino
    }

    /** Importa uma foto escolhida na galeria: copia, gira conforme o EXIF e reduz. Devolve o nome salvo. */
    fun importar(resolver: ContentResolver, uri: Uri): String? {
        var destino: File? = null
        return try {
            val entrada = resolver.openInputStream(uri) ?: return null
            destino = entrada.use { salvarEntrada(it) }
            if (ProcessadorDeImagem.otimizar(destino)) destino.name else { destino.delete(); null }
        } catch (e: Exception) {
            destino?.delete()
            null
        }
    }

    /** Apaga os arquivos que nenhuma despesa usa. Devolve quantos foram apagados. */
    fun limparOrfaos(
        emUso: Set<String>,
        agoraMs: Long = System.currentTimeMillis(),
        idadeMinimaMs: Long = IDADE_MINIMA_ORFAO_MS,
    ): Int {
        val arquivos = pasta.listFiles()?.filter { it.isFile }
            ?.map { InfoArquivo(it.name, it.lastModified()) }
            ?: return 0
        val orfaos = arquivosOrfaos(arquivos, emUso, agoraMs, idadeMinimaMs)
        orfaos.forEach { File(pasta, it).delete() }
        return orfaos.size
    }
}
