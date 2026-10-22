package br.com.motoristafinanceiro.ui.componentes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guarda que os formulários seguem o "Painel de bordo": esqueleto comum (cabeçalho, campos que rolam e
 * Salvar fixo no rodapé) e nada dos campos antigos do Material (o Gradle roda com app/ como pasta de trabalho).
 */
class FormulariosNoVisualTest {

    private val raiz = "src/main/java/br/com/motoristafinanceiro/ui"

    private val formularios = listOf(
        "veiculo/AbastecimentoScreen.kt",
        "veiculo/ManutencaoScreen.kt",
        "reservas/ReservaScreen.kt",
        "meta/MetaScreen.kt",
    )

    private fun texto(caminho: String) = File("$raiz/$caminho").readText()

    @Test
    fun formulariosSecundarios_usamOEsqueletoComum() {
        formularios.forEach { assertTrue("$it não usa TelaFormulario", texto(it).contains("TelaFormulario(")) }
    }

    @Test
    fun nenhumaTela_usaOsCamposAntigos() {
        (formularios + listOf("ajustes/AjustesScreen.kt", "lancamento/NovoLancamentoScreen.kt")).forEach {
            assertFalse("$it ainda usa OutlinedTextField", texto(it).contains("OutlinedTextField"))
            assertFalse("$it ainda usa FilterChip", texto(it).contains("FilterChip"))
        }
    }

    @Test
    fun ajustes_temCabecalhoESecoesEmCartao() {
        val ajustes = texto("ajustes/AjustesScreen.kt")
        assertTrue(ajustes.contains("CabecalhoVoltar(\"Ajustes\""))
        listOf("SecaoAparencia.kt" to "Aparência", "SecaoNotificacoes.kt" to "Notificações", "SecaoBackup.kt" to "Backup")
            .forEach { (arquivo, titulo) -> assertTrue(texto("ajustes/$arquivo").contains("SecaoAjustes(\"$titulo\")")) }
    }
}
