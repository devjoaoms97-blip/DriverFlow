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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import br.com.motoristafinanceiro.ui.historico.HistoricoScreen
import br.com.motoristafinanceiro.ui.home.HomeScreen
import br.com.motoristafinanceiro.ui.lancamento.NovoLancamentoScreen
import br.com.motoristafinanceiro.ui.lancamento.TipoLancamento

private data class Destino(val rota: String, val rotulo: String, val icone: ImageVector)

private val destinos = listOf(
    Destino("inicio", "Início", Icons.Filled.Home),
    Destino("historico", "Histórico", Icons.AutoMirrored.Filled.List),
    Destino("veiculo", "Veículo", Icons.Filled.Build),
    Destino("reservas", "Reservas", Icons.Filled.DateRange),
    Destino("analises", "Análises", Icons.Filled.Star),
)

private const val ROTA_NOVO = "novo"
private const val ROTA_GANHO = "ganho/{id}"
private const val ROTA_DESPESA = "despesa/{id}"
private val rotasFormulario = setOf(ROTA_NOVO, ROTA_GANHO, ROTA_DESPESA)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rotaAtual = pilha?.destination?.route
    val emFormulario = rotaAtual in rotasFormulario

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
                FloatingActionButton(onClick = { nav.navigate(ROTA_NOVO) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Novo lançamento")
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
            composable("inicio") { HomeScreen() }
            composable("historico") {
                HistoricoScreen(
                    onEditarGanho = { nav.navigate("ganho/$it") },
                    onEditarDespesa = { nav.navigate("despesa/$it") },
                )
            }
            composable("veiculo") { EmConstrucao("Veículo") }
            composable("reservas") { EmConstrucao("Reservas") }
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
        }
    }
}

@Composable
private fun EmConstrucao(titulo: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // TODO: implementar conforme docs/ESPECIFICACAO.md
        Text("$titulo — em construção")
    }
}
