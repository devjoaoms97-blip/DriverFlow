package br.com.motoristafinanceiro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.motoristafinanceiro.ui.ajustes.AjustesScreen
import br.com.motoristafinanceiro.ui.analises.AnalisesScreen
import br.com.motoristafinanceiro.ui.historico.HistoricoScreen
import br.com.motoristafinanceiro.ui.home.HomeScreen
import br.com.motoristafinanceiro.ui.lancamento.NovoLancamentoScreen
import br.com.motoristafinanceiro.ui.lancamento.TipoLancamento
import br.com.motoristafinanceiro.ui.meta.MetaScreen
import br.com.motoristafinanceiro.ui.reservas.ReservaScreen
import br.com.motoristafinanceiro.ui.reservas.ReservasScreen
import br.com.motoristafinanceiro.ui.veiculo.AbastecimentoScreen
import br.com.motoristafinanceiro.ui.veiculo.ManutencaoScreen
import br.com.motoristafinanceiro.ui.veiculo.VeiculoScreen

private const val ROTA_VEICULO = "veiculo"
private const val ROTA_RESERVAS = "reservas"
private const val ROTA_RESERVA = "reserva/{id}"
private const val ROTA_NOVO = "novo"
private const val ROTA_GANHO = "ganho/{id}"
private const val ROTA_DESPESA = "despesa/{id}"
private const val ROTA_KM_PESSOAL = "kmpessoal/{id}"
private const val ROTA_ABASTECIMENTO = "abastecimento/{id}"
private const val ROTA_MANUTENCAO = "manutencao/{id}"
private const val ROTA_AJUSTES = "ajustes"
private const val ROTA_META = "meta"
/** Telas sem a barra inferior: formulários e telas abertas a partir do Início (Reservas, Meta, Ajustes). */
internal val rotasSemBarra = setOf(
    ROTA_NOVO, ROTA_GANHO, ROTA_DESPESA, ROTA_KM_PESSOAL, ROTA_ABASTECIMENTO, ROTA_MANUTENCAO,
    ROTA_AJUSTES, ROTA_RESERVA, ROTA_META, ROTA_RESERVAS,
)

/** Troca de aba sem empilhar telas: volta ao Início antes e restaura o estado da aba. */
private fun NavController.abrirAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * [destinoPedido]: rota para abrir de fora (ex.: toque na notificação). Depois de navegar, chama
 * [aoAbrirDestino] para o pedido não se repetir.
 */
@Composable
fun AppNav(destinoPedido: String? = null, aoAbrirDestino: () -> Unit = {}) {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rotaAtual = pilha?.destination?.route
    val grafoPronto = rotaAtual != null
    // Só navega depois que o NavHost montou o grafo (rotaAtual deixa de ser nula).
    LaunchedEffect(destinoPedido, grafoPronto) {
        if (destinoPedido != null && grafoPronto) {
            nav.abrirAba(destinoPedido)
            aoAbrirDestino()
        }
    }
    val semBarra = rotaAtual in rotasSemBarra
    val naTelaVeiculo = rotaAtual == ROTA_VEICULO
    var escolhendoRegistro by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!semBarra) {
                // Na aba Veículo o + pergunta se é abastecimento ou manutenção; nas demais, ganho/despesa.
                BarraInferior(
                    rotaAtual = rotaAtual,
                    onAbrir = { nav.abrirAba(it) },
                    onNovo = { if (naTelaVeiculo) escolhendoRegistro = true else nav.navigate(ROTA_NOVO) },
                    descricaoNovo = if (naTelaVeiculo) "Novo registro do veículo" else "Novo lançamento",
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
            composable("inicio") {
                HomeScreen(
                    onAbrirAjustes = { nav.navigate(ROTA_AJUSTES) },
                    onAbrirMeta = { nav.navigate(ROTA_META) },
                    onAbrirVeiculo = { nav.abrirAba(ROTA_VEICULO) },
                    onAbrirReservas = { nav.navigate(ROTA_RESERVAS) },
                )
            }
            composable(ROTA_AJUSTES) { AjustesScreen(onVoltar = { nav.popBackStack() }) }
            composable(ROTA_META) { MetaScreen(onVoltar = { nav.popBackStack() }) }
            composable("historico") {
                HistoricoScreen(
                    onEditarGanho = { nav.navigate("ganho/$it") },
                    onEditarDespesa = { nav.navigate("despesa/$it") },
                    onEditarAbastecimento = { nav.navigate("abastecimento/$it") },
                    onEditarManutencao = { nav.navigate("manutencao/$it") },
                )
            }
            composable(ROTA_VEICULO) {
                VeiculoScreen(
                    onEditarAbastecimento = { nav.navigate("abastecimento/$it") },
                    onEditarManutencao = { nav.navigate("manutencao/$it") },
                    onNovoAbastecimento = { nav.navigate("abastecimento/0") },
                    onNovaManutencao = { nav.navigate("manutencao/0") },
                    onEditarKmPessoal = { nav.navigate("kmpessoal/$it") },
                )
            }
            composable(ROTA_RESERVAS) {
                ReservasScreen(
                    onEditar = { nav.navigate("reserva/$it") },
                    onVoltar = { nav.popBackStack() },
                )
            }
            composable(
                ROTA_RESERVA,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                ReservaScreen(
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
            composable("analises") { AnalisesScreen() }

            composable(ROTA_NOVO) {
                NovoLancamentoScreen(tipo = null, id = 0L, onConcluir = { nav.popBackStack() })
            }
            composable(
                ROTA_GANHO,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                NovoLancamentoScreen(
                    tipo = TipoLancamento.GANHO,
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
            composable(
                ROTA_DESPESA,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                NovoLancamentoScreen(
                    tipo = TipoLancamento.DESPESA,
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
            composable(
                ROTA_KM_PESSOAL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                NovoLancamentoScreen(
                    tipo = TipoLancamento.KM_PESSOAL,
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
            composable(
                ROTA_ABASTECIMENTO,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                AbastecimentoScreen(
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
            composable(
                ROTA_MANUTENCAO,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                ManutencaoScreen(
                    id = e.arguments?.getLong("id") ?: 0L,
                    onConcluir = { nav.popBackStack() },
                )
            }
        }
    }

    if (escolhendoRegistro) {
        AlertDialog(
            onDismissRequest = { escolhendoRegistro = false },
            title = { Text("Registrar") },
            text = { Text("O que você quer registrar?") },
            confirmButton = {
                TextButton(onClick = {
                    escolhendoRegistro = false
                    nav.navigate("abastecimento/0")
                }) { Text("Abastecimento") }
            },
            dismissButton = {
                TextButton(onClick = {
                    escolhendoRegistro = false
                    nav.navigate("manutencao/0")
                }) { Text("Manutenção") }
            },
        )
    }
}
