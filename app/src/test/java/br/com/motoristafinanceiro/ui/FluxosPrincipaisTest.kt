package br.com.motoristafinanceiro.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import br.com.motoristafinanceiro.ui.navigation.AppNav
import br.com.motoristafinanceiro.ui.navigation.TAG_BOTAO_NOVO
import br.com.motoristafinanceiro.ui.navigation.tagDaAba
import br.com.motoristafinanceiro.ui.theme.MotoristaTheme
import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fluxos principais pela interface, como o usuário faria: lançar, editar, excluir, filtrar e navegar.
 * Rodam no PC com Robolectric, com o app de verdade (banco Room em arquivo, limpo a cada teste):
 * sem emulador e sem tocar no celular.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi") // tela do tamanho de um celular comum
class FluxosPrincipaisTest {

    @get:Rule
    val tela = createComposeRule()

    @Before
    fun abrirApp() {
        tela.setContent { MotoristaTheme { AppNav() } }
    }

    /** O banco grava e avisa a tela em outra thread: espera o texto aparecer (ou sumir). */
    private fun esperarTexto(texto: String, presente: Boolean = true) =
        tela.waitUntil(timeoutMillis = 5_000) {
            tela.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() == presente
        }

    private fun abrirAba(rota: String) = tela.onNodeWithTag(tagDaAba(rota)).performClick()

    /** Clica no elemento clicável com esse texto (o mesmo texto pode aparecer também como rótulo). */
    private fun tocar(texto: String) =
        tela.onAllNodes(hasText(texto) and hasClickAction()).onFirst().performScrollTo().performClick()

    private fun lancarGanho(valor: String) {
        tela.onNodeWithTag(TAG_BOTAO_NOVO).performClick()
        tela.onNodeWithContentDescription("Valor bruto").performTextInput(valor)
        tela.onNodeWithText("Salvar ganho").performClick()
        esperarTexto("Novo lançamento", presente = false)
    }

    private fun lancarDespesa(valor: String) {
        tela.onNodeWithTag(TAG_BOTAO_NOVO).performClick()
        tela.onNodeWithText("Despesa").performClick()
        tela.onNodeWithContentDescription("Valor da despesa").performTextInput(valor)
        tela.onNodeWithText("Salvar despesa").performClick()
        esperarTexto("Novo lançamento", presente = false)
    }

    @Test
    fun lancarGanho_apareceNoLucroDeHoje() {
        lancarGanho("100")
        esperarTexto(10_000L.formatarReais())
        tela.onNodeWithText("Lucro líquido de hoje").assertExists()
    }

    @Test
    fun lancarDespesa_descontaDoLucro() {
        lancarGanho("100")
        lancarDespesa("30")
        esperarTexto(7_000L.formatarReais())
    }

    @Test
    fun salvar_soHabilitaComValor() {
        tela.onNodeWithTag(TAG_BOTAO_NOVO).performClick()
        tela.onNodeWithText("Salvar ganho").assertIsNotEnabled()
    }

    @Test
    fun historico_filtraPorTipo() {
        lancarGanho("100")
        lancarDespesa("30")
        abrirAba("historico")
        esperarTexto("Uber")
        esperarTexto("Combustível")

        tocar("Despesas")
        esperarTexto("Uber", presente = false)
        esperarTexto("− " + 3_000L.formatarReais()) // a linha da despesa (o nome também vira botão de filtro)

        tocar("Todos")
        esperarTexto("Uber")
    }

    @Test
    fun historico_editarGanho() {
        lancarGanho("100")
        abrirAba("historico")
        esperarTexto("Uber")

        tela.onNodeWithText("Uber").performClick()
        esperarTexto("Editar ganho")
        tela.onNodeWithContentDescription("Valor bruto").performTextClearance()
        tela.onNodeWithContentDescription("Valor bruto").performTextInput("150")
        tela.onNodeWithText("Salvar alterações").performClick()

        esperarTexto("+ " + 15_000L.formatarReais())
    }

    @Test
    fun historico_excluirGanhoComConfirmacao() {
        lancarGanho("100")
        abrirAba("historico")
        esperarTexto("Uber")

        tela.onNodeWithText("Uber").performClick()
        esperarTexto("Excluir lançamento")
        tocar("Excluir lançamento")
        tela.onNodeWithText("Excluir").performClick()

        esperarTexto("Nenhum lançamento neste período")
    }

    @Test
    fun novoGanho_repetirOUltimoCopiaAPlataforma() {
        tela.onNodeWithTag(TAG_BOTAO_NOVO).performClick()
        tela.onNodeWithText("Repetir o último").assertDoesNotExist() // ainda não há lançamento
        tela.onNodeWithText("99").performClick()
        tela.onNodeWithContentDescription("Valor bruto").performTextInput("80")
        tela.onNodeWithText("Salvar ganho").performClick()
        esperarTexto("Novo lançamento", presente = false)

        tela.onNodeWithTag(TAG_BOTAO_NOVO).performClick()
        esperarTexto("Repetir o último")
        tocar("Repetir o último")
        tela.onNode(hasText("99") and isSelected()).assertExists()
        tela.onNodeWithText("Salvar ganho").assertIsNotEnabled() // o valor não é copiado
    }

    @Test
    fun reservas_abrePeloAtalhoDoInicioEVolta() {
        tocar("Reservas")
        esperarTexto("Separar por dia")
        tela.onNodeWithTag(tagDaAba("inicio")).assertDoesNotExist() // sem barra inferior

        tela.onNodeWithContentDescription("Voltar").performClick()
        esperarTexto("Separar por dia", presente = false)
        tela.onNodeWithTag(tagDaAba("inicio")).assertExists()
    }

    @Test
    fun veiculo_botaoAbastecerAbreOFormulario() {
        abrirAba("veiculo")
        tela.onNodeWithText("Abastecer").performClick()
        esperarTexto("Novo abastecimento")
        tela.onNodeWithText("Salvar abastecimento").assertIsNotEnabled()
    }

    @Test
    fun analises_semGanhosMostraOAviso() {
        abrirAba("analises")
        esperarTexto("Nenhum ganho lançado neste período.")
    }
}
