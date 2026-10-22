package br.com.motoristafinanceiro.ui.ajustes

import br.com.motoristafinanceiro.data.backup.ResumoBackup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TextoBackupTest {
    private fun resumo(
        ganhos: Int = 0, despesas: Int = 0, abastecimentos: Int = 0, manutencoes: Int = 0,
        reservas: Int = 0, metas: Int = 0, fotos: Int = 0, fotosAusentes: Int = 0,
    ) = ResumoBackup(ganhos, despesas, abastecimentos, manutencoes, reservas, metas, fotos, fotosAusentes)

    @Test
    fun contagem_singularEPlural() {
        assertEquals("1 ganho", contagem(1, "ganho", "ganhos"))
        assertEquals("3 ganhos", contagem(3, "ganho", "ganhos"))
        assertEquals("0 ganhos", contagem(0, "ganho", "ganhos"))
    }

    @Test
    fun listaEmPortugues() {
        assertEquals("", listaEmPortugues(emptyList()))
        assertEquals("a", listaEmPortugues(listOf("a")))
        assertEquals("a e b", listaEmPortugues(listOf("a", "b")))
        assertEquals("a, b e c", listaEmPortugues(listOf("a", "b", "c")))
    }

    @Test
    fun salvo_listaSoOQueTemItens() {
        val r = resumo(ganhos = 120, despesas = 1, manutencoes = 2, fotos = 3)

        assertEquals("Backup salvo: 120 ganhos, 1 despesa, 2 manutenções e 3 fotos.", textoBackupSalvo(r))
    }

    @Test
    fun salvo_semNadaDizSemLancamentos() {
        assertEquals("Backup salvo: sem lançamentos.", textoBackupSalvo(resumo()))
    }

    @Test
    fun salvo_avisaComprovantesQueFicaramDeFora() {
        assertEquals(
            "Backup salvo: 1 ganho. 1 comprovante sem arquivo no aparelho ficou de fora.",
            textoBackupSalvo(resumo(ganhos = 1, fotosAusentes = 1)),
        )
        assertEquals(
            "Backup salvo: 1 ganho. 2 comprovantes sem arquivo no aparelho ficaram de fora.",
            textoBackupSalvo(resumo(ganhos = 1, fotosAusentes = 2)),
        )
    }

    @Test
    fun restaurado_listaOConteudo() {
        assertEquals(
            "Backup restaurado: 2 ganhos, 4 despesas, 2 abastecimentos, 2 manutenções, 1 reserva, 2 metas e 1 foto.",
            textoBackupRestaurado(resumo(2, 4, 2, 2, 1, 2, 1)),
        )
    }

    @Test
    fun restaurado_avisaComprovantesQueNaoVieramNoArquivo() {
        assertEquals(
            "Backup restaurado: 1 despesa. 1 comprovante não estava no arquivo e foi removido da despesa.",
            textoBackupRestaurado(resumo(despesas = 1, fotosAusentes = 1)),
        )
        assertEquals(
            "Backup restaurado: 1 despesa. 3 comprovantes não estavam no arquivo e foram removidos das despesas.",
            textoBackupRestaurado(resumo(despesas = 1, fotosAusentes = 3)),
        )
    }

    @Test
    fun ultimoBackup_dataEHoraOuNunca() {
        val ms = LocalDateTime.of(2026, 10, 22, 3, 10).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("Último backup: 22/10 às 03:10", textoUltimoBackup(ms, ZoneOffset.UTC))
        assertEquals("Nenhum backup feito ainda", textoUltimoBackup(null))
    }

    @Test
    fun pasta_nomeGenericoOuNenhuma() {
        assertEquals("Download", textoPasta("content://x", "Download"))
        assertEquals("Pasta escolhida", textoPasta("content://x", null))
        assertEquals("Nenhuma", textoPasta(null, null))
    }
}
