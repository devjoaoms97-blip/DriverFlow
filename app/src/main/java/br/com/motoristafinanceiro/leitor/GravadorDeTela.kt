package br.com.motoristafinanceiro.leitor

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo gravação do leitor de corridas: guarda num arquivo de texto o que estava escrito na tela da Uber/99,
 * para aprender o formato do card de cada app. Só grava telas com "R$" ou "km" e pula telas repetidas.
 * Fica na pasta privada do app até o motorista compartilhar ou apagar; pode ter nome de passageiro.
 */
class GravadorDeTela(private val pasta: File, private val limiteBytes: Long = LIMITE_PADRAO_BYTES) {

    val arquivo: File get() = File(pasta, NOME_ARQUIVO)

    private var ultimaTela: List<String>? = null

    /** Devolve true quando gravou a tela. */
    @Synchronized
    fun registrar(agoraMs: Long, pacote: String, linhas: List<String>): Boolean {
        if (linhas.none { it.contains("R$") || it.contains("km", ignoreCase = true) }) return false
        if (linhas == ultimaTela) return false
        if (tamanhoBytes() >= limiteBytes) return false
        ultimaTela = linhas
        pasta.mkdirs()
        val hora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(agoraMs))
        arquivo.appendText(buildString {
            append("=== ").append(hora).append(' ').append(pacote).append('\n')
            linhas.forEach { append(it).append('\n') }
            append('\n')
        })
        return true
    }

    fun tamanhoBytes(): Long = if (arquivo.exists()) arquivo.length() else 0L

    @Synchronized
    fun apagar() {
        arquivo.delete()
        ultimaTela = null
    }

    companion object {
        /** Subpasta de filesDir, a mesma declarada em res/xml/file_paths.xml. */
        const val PASTA = "leitor"
        const val NOME_ARQUIVO = "gravacao.txt"
        const val LIMITE_PADRAO_BYTES = 5L * 1024 * 1024
    }
}
