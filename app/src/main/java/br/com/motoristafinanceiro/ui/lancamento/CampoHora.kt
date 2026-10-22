package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.motoristafinanceiro.util.minutosParaHorario

/** Hora sugerida ao abrir o relógio sem horário escolhido: agora, arredondado para baixo na meia hora. */
internal fun horaSugerida(agoraMinutos: Int): Int = agoraMinutos - agoraMinutos % 30

/**
 * Campo compacto de hora (início do turno). Toque abre o relógio de 24 h; "Limpar" deixa sem horário.
 * [minutos] em minutos desde a meia-noite, ou null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    rotulo: String,
    minutos: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    sugestaoMinutos: () -> Int = { java.time.LocalTime.now().let { horaSugerida(it.hour * 60 + it.minute) } },
) {
    var aberto by remember { mutableStateOf(false) }
    val esquema = MaterialTheme.colorScheme

    Surface(
        onClick = { aberto = true },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = esquema.surfaceContainer,
        border = BorderStroke(1.dp, esquema.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = esquema.onSurfaceVariant, maxLines = 1)
            Text(
                minutos?.minutosParaHorario() ?: "opcional",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (minutos != null) 17.sp else 15.sp,
                    color = if (minutos != null) esquema.onSurface else esquema.onSurfaceVariant.copy(alpha = 0.7f),
                ),
            )
        }
    }

    if (aberto) {
        val inicial = minutos ?: sugestaoMinutos()
        val estado = rememberTimePickerState(initialHour = inicial / 60, initialMinute = inicial % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { aberto = false },
            title = { Text(rotulo) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(estado.hour * 60 + estado.minute)
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (minutos != null) {
                        TextButton(onClick = {
                            onChange(null)
                            aberto = false
                        }) { Text("Limpar") }
                    }
                    TextButton(onClick = { aberto = false }) { Text("Cancelar") }
                }
            },
        )
    }
}
