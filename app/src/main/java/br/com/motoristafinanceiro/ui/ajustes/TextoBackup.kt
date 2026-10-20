package br.com.motoristafinanceiro.ui.ajustes

import br.com.motoristafinanceiro.data.backup.ResumoBackup

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
