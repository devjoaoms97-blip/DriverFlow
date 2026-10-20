package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.model.Despesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupDadosTest {

    @Test
    fun fotosReferenciadas_soAsQueAlgumaDespesaUsa() {
        assertEquals(setOf("comp_1.jpg"), dadosDeExemplo().fotosReferenciadas)
        assertTrue(DadosBackup().fotosReferenciadas.isEmpty())
    }

    @Test
    fun nomeDeFotoValido_aceitaNomeSimples() {
        assertTrue(nomeDeFotoValido("comp_1730000000_123456.jpg"))
    }

    @Test
    fun nomeDeFotoValido_recusaCaminhosEVazios() {
        assertFalse(nomeDeFotoValido(""))
        assertFalse(nomeDeFotoValido("   "))
        assertFalse(nomeDeFotoValido("."))
        assertFalse(nomeDeFotoValido(".."))
        assertFalse(nomeDeFotoValido("../fuga.jpg"))
        assertFalse(nomeDeFotoValido("pasta/foto.jpg"))
        assertFalse(nomeDeFotoValido("pasta\\foto.jpg"))
        assertFalse(nomeDeFotoValido("a".repeat(256)))
    }

    @Test
    fun reconciliarFotos_mantemFotosPresentesELimpaAsAusentes() {
        val base = dadosDeExemplo()
        val comAusente = base.copy(
            despesas = base.despesas + Despesa(
                id = 5, data = LocalDate.of(2026, 10, 14),
                categoria = base.despesas[0].categoria, valorCentavos = 500, fotoUri = "sumiu.jpg",
            ),
        )

        val (ajustado, ausentes) = reconciliarFotos(comAusente, setOf("comp_1.jpg"))

        assertEquals(1, ausentes)
        assertEquals("comp_1.jpg", ajustado.despesas.first { it.id == 1L }.fotoUri)
        assertNull(ajustado.despesas.first { it.id == 5L }.fotoUri)
    }

    @Test
    fun reconciliarFotos_semFotosAusentesNaoMudaNada() {
        val base = dadosDeExemplo()

        val (ajustado, ausentes) = reconciliarFotos(base, setOf("comp_1.jpg", "sobrando.jpg"))

        assertEquals(0, ausentes)
        assertEquals(base, ajustado)
    }

    @Test
    fun reconciliarFotos_arquivoSemNenhumaFotoLimpaTodas() {
        val (ajustado, ausentes) = reconciliarFotos(dadosDeExemplo(), emptySet())

        assertEquals(1, ausentes)
        assertTrue(ajustado.fotosReferenciadas.isEmpty())
    }

    @Test
    fun paraResumo_contaCadaTabela() {
        val r = dadosDeExemplo().paraResumo(fotos = 1, fotosAusentes = 2)

        assertEquals(2, r.ganhos)
        assertEquals(4, r.despesas)
        assertEquals(2, r.abastecimentos)
        assertEquals(2, r.manutencoes)
        assertEquals(1, r.reservas)
        assertEquals(2, r.metas)
        assertEquals(1, r.fotos)
        assertEquals(2, r.fotosAusentes)
    }

    @Test
    fun nomeArquivoBackup_levaADataNoNome() {
        assertEquals("motorista-financeiro-backup-2026-10-20.zip", nomeArquivoBackup(LocalDate.of(2026, 10, 20)))
    }
}
