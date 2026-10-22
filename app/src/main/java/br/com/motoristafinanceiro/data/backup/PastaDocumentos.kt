package br.com.motoristafinanceiro.data.backup

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream

/**
 * Pasta escolhida no seletor de pastas do Android (Storage Access Framework). Funciona com qualquer
 * provedor que aceite pastas: Downloads, Documentos, cartão SD e apps de nuvem que ofereçam isso.
 */
class PastaDocumentos private constructor(
    private val resolver: ContentResolver,
    private val arvore: Uri,
) : PastaDeBackup {

    private val pastaRaiz: Uri =
        DocumentsContract.buildDocumentUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))

    /** Nome e endereço de cada arquivo direto da pasta. */
    private fun filhos(): List<Pair<String, Uri>> {
        val lista = DocumentsContract.buildChildDocumentsUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))
        val colunas = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return resolver.query(lista, colunas, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(c.getString(1) to DocumentsContract.buildDocumentUriUsingTree(arvore, c.getString(0)))
                }
            }
        } ?: emptyList()
    }

    override fun criar(nome: String): OutputStream {
        // Mesmo nome no mesmo dia: substitui, em vez de criar "arquivo (1).zip".
        filhos().firstOrNull { it.first == nome }?.let { (_, uri) ->
            return resolver.openOutputStream(uri, "wt") ?: error("não foi possível abrir $nome")
        }
        val novo = DocumentsContract.createDocument(resolver, pastaRaiz, "application/zip", nome)
            ?: error("a pasta não aceitou o arquivo novo")
        return resolver.openOutputStream(novo, "w") ?: error("não foi possível abrir $nome")
    }

    override fun listar(): List<String> = filhos().map { it.first }

    override fun apagar(nome: String) {
        filhos().filter { it.first == nome }.forEach { DocumentsContract.deleteDocument(resolver, it.second) }
    }

    companion object {
        /** Permissões que o app guarda da pasta, para gravar mesmo depois de reiniciar o celular. */
        const val PERMISSOES = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /** Null quando a permissão da pasta foi perdida (pasta apagada, app reinstalado...). */
        fun abrir(resolver: ContentResolver, uri: String): PastaDocumentos? {
            val arvore = Uri.parse(uri)
            val temPermissao = resolver.persistedUriPermissions.any { it.uri == arvore && it.isWritePermission }
            return if (temPermissao) PastaDocumentos(resolver, arvore) else null
        }

        /** Nome da pasta para mostrar em Ajustes ("Download", "DriverFlow"...); null se não der para ler. */
        fun nome(resolver: ContentResolver, uri: String): String? = runCatching {
            val arvore = Uri.parse(uri)
            val doc = DocumentsContract.buildDocumentUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))
            resolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
    }
}
