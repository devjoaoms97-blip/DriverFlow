package br.com.motoristafinanceiro.ui.veiculo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.util.formatarKm

/** "Atrasada em 200 km", "Faltam 300 km" ou "No limite". */
fun AlertaManutencao.resumoTexto(): String = when {
    kmRestantes < 0 -> "Atrasada em " + (-kmRestantes).formatarKm()
    kmRestantes == 0 -> "No limite"
    else -> "Faltam " + kmRestantes.formatarKm()
}

@Composable
private fun StatusAlerta.cor(): Color = when (this) {
    StatusAlerta.ATRASADA -> MaterialTheme.colorScheme.error
    StatusAlerta.PROXIMA -> MaterialTheme.colorScheme.tertiary
    StatusAlerta.EM_DIA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Lista de alertas (usada na tela Veículo e no card de atenção do Início). */
@Composable
fun ListaAlertas(alertas: List<AlertaManutencao>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        alertas.forEach { a ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(descricaoManutencao(a.manutencao), Modifier.weight(1f))
                Text(a.resumoTexto(), color = a.status.cor())
            }
        }
    }
}
