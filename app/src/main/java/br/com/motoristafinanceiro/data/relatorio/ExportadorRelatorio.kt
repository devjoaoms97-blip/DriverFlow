package br.com.motoristafinanceiro.data.relatorio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.motoristafinanceiro.domain.RelatorioMes
import java.io.File

/** Grava o PDF do relatório num arquivo escolhido pelo usuário ou prepara para compartilhar. Chame de uma thread de IO. */
class ExportadorRelatorio(private val context: Context) {

    /** [destino] vem do seletor "Salvar como" do Android. */
    fun salvar(relatorio: RelatorioMes, destino: Uri) {
        val saida = context.contentResolver.openOutputStream(destino) ?: error("não foi possível abrir o arquivo escolhido")
        saida.use { RelatorioPdf(context).escrever(relatorio, it) }
    }

    /** Cria o PDF numa pasta temporária do app e devolve o pedido de compartilhamento (WhatsApp, e-mail...). */
    fun intentDeCompartilhar(relatorio: RelatorioMes): Intent {
        val pasta = File(context.cacheDir, PASTA).apply {
            deleteRecursively() // só o relatório atual fica guardado
            mkdirs()
        }
        val arquivo = File(pasta, nomeArquivoRelatorio(relatorio.mes))
        arquivo.outputStream().use { RelatorioPdf(context).escrever(relatorio, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        val enviar = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Relatório de ${tituloDoMes(relatorio.mes)}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(enviar, "Compartilhar relatório")
    }

    companion object {
        /** Subpasta de cacheDir, a mesma declarada em res/xml/file_paths.xml. */
        const val PASTA = "relatorios"
    }
}
