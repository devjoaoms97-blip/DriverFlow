package br.com.motoristafinanceiro.data.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric porque o org.json de verdade só existe no Android (no JUnit puro ele é um "stub"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupJsonTest {

    /** Gera o JSON de exemplo, deixa o teste alterá-lo e devolve o texto. */
    private fun alterado(bloco: (JSONObject) -> Unit): String {
        val json = JSONObject(BackupJson.gerar(dadosDeExemplo(), 1L))
        bloco(json)
        return json.toString()
    }

    private fun assertInvalido(texto: String, trecho: String) {
        try {
            BackupJson.ler(texto)
            fail("Deveria ter recusado o backup (esperava mensagem com \"$trecho\")")
        } catch (e: BackupInvalidoException) {
            assertTrue("Mensagem: ${e.message}", e.message!!.contains(trecho))
        }
    }

    // ---- Ida e volta ----

    @Test
    fun gerarELer_devolveOsMesmosDados() {
        val original = dadosDeExemplo()

        assertEquals(original, BackupJson.ler(BackupJson.gerar(original, 123L)))
    }

    @Test
    fun gerarELer_guardaAChaveDoAvisoDeManutencao() {
        listOf(true, false).forEach { chave ->
            val original = DadosBackup(notificacoesManutencaoAtivas = chave)
            assertEquals(chave, BackupJson.ler(BackupJson.gerar(original, 0L)).notificacoesManutencaoAtivas)
        }
    }

    @Test
    fun ler_backupAntigoSemAChave_devolveNulo() {
        val json = JSONObject(BackupJson.gerar(DadosBackup(notificacoesManutencaoAtivas = true), 0L))
        json.remove("notificacoesManutencaoAtivas")

        assertEquals(null, BackupJson.ler(json.toString()).notificacoesManutencaoAtivas)
    }

    @Test
    fun recusa_chaveDoAvisoQueNaoEhVerdadeiroOuFalso() =
        assertInvalido(alterado { it.put("notificacoesManutencaoAtivas", "sim") }, "notificacoesManutencaoAtivas")

    @Test
    fun gerarELer_dadosVazios() {
        assertEquals(DadosBackup(), BackupJson.ler(BackupJson.gerar(DadosBackup(), 0L)))
    }

    @Test
    fun gerar_trazFormatoVersaoEData() {
        val json = JSONObject(BackupJson.gerar(dadosDeExemplo(), 1_700_000_000_000L))

        assertEquals("motorista-financeiro-backup", json.getString("formato"))
        assertEquals(BackupJson.VERSAO_ATUAL, json.getInt("versaoSchema"))
        assertEquals(1_700_000_000_000L, json.getLong("criadoEmMs"))
        assertEquals(25L, json.getLong("depreciacaoPorKmCentavos"))
    }

    @Test
    fun gerar_datasEmIsoEEnumsPeloNome() {
        val ganho = JSONObject(BackupJson.gerar(dadosDeExemplo(), 1L)).getJSONArray("ganhos").getJSONObject(0)

        assertEquals("2026-10-12", ganho.getString("data"))
        assertEquals("UBER", ganho.getString("plataforma"))
    }

    @Test
    fun camposOpcionaisAusentesViramNulo() {
        val lido = BackupJson.ler(BackupJson.gerar(dadosDeExemplo(), 1L))

        assertEquals(null, lido.ganhos[1].observacao)
        assertEquals(null, lido.despesas[3].fotoUri)
        assertEquals(null, lido.despesas[3].origemId)
        assertEquals(null, lido.manutencoes[1].intervaloKm)
    }

    // ---- Arquivo que não é backup ----

    @Test
    fun recusa_textoQueNaoEJson() = assertInvalido("isso não é json", "corrompido")

    @Test
    fun recusa_jsonQueNaoEUmObjeto() = assertInvalido("[]", "corrompido")

    @Test
    fun recusa_formatoDeOutroApp() = assertInvalido(alterado { it.put("formato", "outro-app") }, "não é um backup")

    @Test
    fun recusa_semFormato() = assertInvalido(alterado { it.remove("formato") }, "não é um backup")

    @Test
    fun recusa_semVersao() = assertInvalido(alterado { it.remove("versaoSchema") }, "versão")

    @Test
    fun recusa_versaoMaisNovaQueOApp() = assertInvalido(alterado { it.put("versaoSchema", 99) }, "versão mais nova")

    // ---- Dados incompletos ou inválidos ----

    @Test
    fun recusa_tabelaAusente() = assertInvalido(alterado { it.remove("ganhos") }, "ganhos")

    @Test
    fun recusa_campoObrigatorioAusente() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(0).remove("valorCentavos") }, "ganhos #1")

    @Test
    fun recusa_enumDesconhecido() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(0).put("plataforma", "TAXI") }, "valor desconhecido")

    @Test
    fun recusa_dataInvalida() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(1).put("data", "12/10/2026") }, "ganhos #2")

    @Test
    fun recusa_valorNegativo() =
        assertInvalido(alterado { it.getJSONArray("despesas").getJSONObject(0).put("valorCentavos", -1) }, "negativo")

    @Test
    fun recusa_tipoErrado() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(0).put("km", "muito") }, "km")

    @Test
    fun recusa_idRepetido() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(1).put("id", 1) }, "repetidos")

    @Test
    fun recusa_idZero() =
        assertInvalido(alterado { it.getJSONArray("abastecimentos").getJSONObject(0).put("id", 0) }, "id")

    @Test
    fun recusa_despesaApontandoParaAbastecimentoInexistente() =
        assertInvalido(alterado { it.getJSONArray("despesas").getJSONObject(1).put("origemId", 99) }, "não existe")

    @Test
    fun recusa_despesaDeManutencaoSemOrigemId() =
        assertInvalido(alterado { it.getJSONArray("despesas").getJSONObject(2).remove("origemId") }, "não existe")

    @Test
    fun recusa_fotoComCaminho() =
        assertInvalido(alterado { it.getJSONArray("despesas").getJSONObject(0).put("fotoUri", "../fuga.jpg") }, "comprovante")

    @Test
    fun recusa_depreciacaoAcimaDoTeto() =
        assertInvalido(alterado { it.put("depreciacaoPorKmCentavos", 1_001) }, "Depreciação")

    @Test
    fun recusa_mesDeMetaInvalido() =
        assertInvalido(alterado { it.getJSONArray("metas").getJSONObject(0).put("mes", "outubro") }, "mes")

    @Test
    fun recusa_metaDoMesRepetida() =
        assertInvalido(alterado { it.getJSONArray("metas").getJSONObject(1).put("mes", "2026-09") }, "repetidos")

    @Test
    fun mensagemDeErroDizQualRegistroTemProblema() =
        assertInvalido(alterado { it.getJSONArray("manutencoes").getJSONObject(1).put("tipo", "XYZ") }, "manutencoes #2")

    // ---- Início do turno (campo opcional, desde o banco v2) ----

    @Test
    fun inicioDoTurno_vaiEVoltaNoBackup() {
        val ganhos = BackupJson.ler(BackupJson.gerar(dadosDeExemplo(), 1L)).ganhos
        assertEquals(18 * 60 + 30, ganhos.first { it.id == 1L }.inicioTurnoMinutos)
        assertEquals(null, ganhos.first { it.id == 2L }.inicioTurnoMinutos)
    }

    @Test
    fun backupDaVersao1SemInicioDoTurno_restauraSemHorario() {
        val texto = alterado { json ->
            json.put("versaoSchema", 1) // backups feitos antes do horário do turno
            val ganhos = json.getJSONArray("ganhos")
            for (i in 0 until ganhos.length()) ganhos.getJSONObject(i).remove("inicioTurnoMinutos")
        }
        assertTrue(BackupJson.ler(texto).ganhos.all { it.inicioTurnoMinutos == null })
    }

    @Test
    fun recusa_inicioDoTurnoForaDoDia() = assertInvalido(
        alterado { it.getJSONArray("ganhos").getJSONObject(0).put("inicioTurnoMinutos", 1440) },
        "inicioTurnoMinutos",
    )

    // ---- Versão 3: corridas e km pessoais ----

    @Test
    fun gerarELer_guardaCorridasEKmPessoais() {
        val lido = BackupJson.ler(BackupJson.gerar(dadosDeExemplo(), 0L))
        assertEquals(12, lido.ganhos.first { it.id == 1L }.corridas)
        assertEquals(null, lido.ganhos.first { it.id == 2L }.corridas)
        assertEquals(dadosDeExemplo().kmPessoais, lido.kmPessoais)
    }

    @Test
    fun ler_backupDaVersao2_semCorridasNemKmPessoais() {
        val texto = alterado { json ->
            json.put("versaoSchema", 2)
            json.remove("kmPessoais")
            val ganhos = json.getJSONArray("ganhos")
            for (i in 0 until ganhos.length()) ganhos.getJSONObject(i).remove("corridas")
        }
        val lido = BackupJson.ler(texto)
        assertTrue(lido.ganhos.all { it.corridas == null })
        assertTrue(lido.kmPessoais.isEmpty())
    }

    @Test
    fun recusa_corridasNegativas() =
        assertInvalido(alterado { it.getJSONArray("ganhos").getJSONObject(0).put("corridas", -1) }, "corridas")

    @Test
    fun recusa_kmPessoalNegativo() =
        assertInvalido(alterado { it.getJSONArray("kmPessoais").getJSONObject(0).put("km", -5) }, "kmPessoais #1")

    @Test
    fun recusa_kmPessoalRepetido() =
        assertInvalido(alterado { it.getJSONArray("kmPessoais").getJSONObject(1).put("id", 1) }, "kmPessoais")
}
