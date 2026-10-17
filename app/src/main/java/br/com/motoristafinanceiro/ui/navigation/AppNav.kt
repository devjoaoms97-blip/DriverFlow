package br.com.motoristafinanceiro.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.motoristafinanceiro.ui.ajustes.AjustesScreen
import br.com.motoristafinanceiro.ui.historico.HistoricoScreen
import br.com.motoristafinanceiro.ui.home.HomeScreen
import br.com.motoristafinanceiro.ui.lancamento.NovoLancamentoScreen
import br.com.motoristafinanceiro.ui.lancamento.TipoLancamento
import br.com.motoristafinanceiro.ui.reservas.ReservaScreen
import br.com.motoristafinanceiro.ui.reservas.ReservasScreen
import br.com.motoristafinanceiro.ui.veiculo.AbastecimentoScreen
import br.com.motoristafinanceiro.ui.veiculo.ManutencaoScreen
import br.com.motoristafinanceiro.ui.veiculo.VeiculoScreen

private data class Destino(val rota: String, val rotulo: String, val icone: ImageVector)

private val destinos = listOf(
    Destino("inicio", "Início", Icons.Filled.Home),
    Destino("historico", "Histórico", Icons.AutoMirrored.Filled.List),
    Destino("veiculo", "Veículo", Icons.Filled.Build),
    Destino("reservas", "Reservas", Icons.Filled.DateRange),
    Destino("analises", "Análises", Icons.Filled.Star),
)

private const val ROTA_VEICULO = "veiculo"
private const val ROTA_RESERVAS = "reservas"
private const val ROTA_RESERVA = "reserva/{id}"
private const val ROTA_NOVO = "novo"
private const val ROTA_GANHO = "ganho/{id}"
private const val ROTA_DESPESA = "despesa/{id}"
private const val ROTA_ABASTECIMENTO = "abastecimento/{id}"
private const val ROTA_MANUTENCAO = "manutencao/{id}"
private const val ROTA_AJUSTES = "ajustes"
private val rotasFormulario = setOf(ROTA_NOVO, ROTA_GANHO, ROTA_DESPESA, ROTA_ABASTECIMENTO, ROTA_MANUTENCAO, ROTA_AJUSTES, ROTA_RESERVA)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rotaAtual = pilha?.destination?.route
    val emFormulario = rotaAtual in rotasFormulario
    val naTelaVeiculo = rotaAtual == ROTA_VEICULO
    val naTelaReservas = rotaAtual == ROTA_RESERVAS
    var escolhendoRegistro by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!emFormulario) {
                NavigationBar {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = rotaAtual == d.rota,
                            onClick = {
                                nav.navigate(d.rota) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icone, contentDescription = d.rotulo) },
                            label = { Text(d.rotulo) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!emFormulario) {
                // Na aba Veículo o + pergunta se é abastecimento ou manutenção; nas demais, ganho/despesa.
                FloatingActionButton(
                    onClick = {
                        when {
                            naTelaVeiculo -> escolhendoRegistro = true
                            naTelaReservas -> nav.navigate("reserva/0")
                            else -> nav.navigate(ROTA_NOVO)
                        }
                    },
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = when {
                            naTelaVeiculo -> "Novo registro do veículo"
                            naTelaReservas -> "Nova reserva"
                            else -> "Novo lançamento"
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
            composable("inicio") { HomeScreen(onAbrirAjustes = { nav.navigate(ROTA_AJUSTES) }) }
            composable(ROTA_AJUSTES) { AjustesScreen(onVoltar = { nav.popBackStack() }) }
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
                )
            }
            composable(ROTA_RESERVAS) {
                ReservasScreen(onEditar = { nav.navigate("reserva/$it") })
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
            composable("analises") { EmConstrucao("Análises") }

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

@Composable
private fun EmConstrucao(titulo: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // TODO: implementar conforme docs/ESPECIFICACAO.md
        Text("$titulo — em construção")
    }
}
