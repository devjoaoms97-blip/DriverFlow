package br.com.motoristafinanceiro.ui.ajustes

import br.com.motoristafinanceiro.data.backup.ResumoBackup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 1 -> "1 ganho"; 3 -> "3 ganhos". */
fun contagem(n: Int, singular: String, plural: String): String = if (n == 1) "1 $singular" else "$n $plural"

/** ["a", "b", "c"] -> "a, b e c". */
fun listaEmPortugues(itens: List<String>): String = when (itens.size) {
    0 -> ""
    1 -> itens[0]
    else -> itens.dropLast(1).joinToString(", ") + " e " + itens.last()
}

private fun descreverConteudo(r: ResumoBackup): String {
    val itens = buildList {
        if (r.ganhos > 0) add(contagem(r.ganhos, "ganho", "ganhos"))
        if (r.despesas > 0) add(contagem(r.despesas, "despesa", "despesas"))
        if (r.abastecimentos > 0) add(contagem(r.abastecimentos, "abastecimento", "abastecimentos"))
        if (r.manutencoes > 0) add(contagem(r.manutencoes, "manutenção", "manutenções"))
        if (r.reservas > 0) add(contagem(r.reservas, "reserva", "reservas"))
        if (r.metas > 0) add(contagem(r.metas, "meta", "metas"))
        if (r.kmPessoais > 0) add(contagem(r.kmPessoais, "km pessoal", "km pessoais"))
        if (r.fotos > 0) add(contagem(r.fotos, "foto", "fotos"))
    }
    return if (itens.isEmpty()) "sem lançamentos" else listaEmPortugues(itens)
}

fun textoBackupSalvo(r: ResumoBackup): String {
    val base = "Backup salvo: ${descreverConteudo(r)}."
    val n = r.fotosAusentes
    if (n == 0) return base
    val aviso = if (n == 1) {
        "1 comprovante sem arquivo no aparelho ficou de fora."
    } else {
        "$n comprovantes sem arquivo no aparelho ficaram de fora."
    }
    return "$base $aviso"
}

fun textoBackupRestaurado(r: ResumoBackup): String {
    val base = "Backup restaurado: ${descreverConteudo(r)}."
    val n = r.fotosAusentes
    if (n == 0) return base
    val aviso = if (n == 1) {
        "1 comprovante não estava no arquivo e foi removido da despesa."
    } else {
        "$n comprovantes não estavam no arquivo e foram removidos das despesas."
    }
    return "$base $aviso"
}

private val formatoUltimoBackup = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")

/** "Último backup: 22/10 às 03:10" ou "Nenhum backup feito ainda". */
fun textoUltimoBackup(ms: Long?, zona: ZoneId = ZoneId.systemDefault()): String =
    if (ms == null) "Nenhum backup feito ainda"
    else "Último backup: " + Instant.ofEpochMilli(ms).atZone(zona).format(formatoUltimoBackup)

/** Pasta do backup automático: o nome, um texto genérico se não deu para ler, ou "Nenhuma". */
fun textoPasta(uri: String?, nome: String?): String = when {
    uri == null -> "Nenhuma"
    nome.isNullOrBlank() -> "Pasta escolhida"
    else -> nome
}
