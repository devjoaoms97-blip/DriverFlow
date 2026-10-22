package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.motoristafinanceiro.ui.componentes.Chip
import br.com.motoristafinanceiro.ui.componentes.Icones
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Hoje · 21/10/2026", "Ontem · 20/10/2026" ou só a data. */
internal fun rotuloData(data: LocalDate, hoje: LocalDate = LocalDate.now()): String = when (data) {
    hoje -> "Hoje · " + data.format(formatoData)
    hoje.minusDays(1) -> "Ontem · " + data.format(formatoData)
    else -> data.format(formatoData)
}

/** Linha com a data (toque abre o calendário) + (opcionalmente) atalhos "Hoje" e "Ontem". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    data: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    mostrarAtalhos: Boolean = true,
) {
    var aberto by remember { mutableStateOf(false) }
    val esquema = MaterialTheme.colorScheme
    val hoje = LocalDate.now()

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = { aberto = true },
            shape = MaterialTheme.shapes.small,
            color = esquema.surfaceContainer,
            border = BorderStroke(1.dp, esquema.outlineVariant),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icones.Calendario, contentDescription = null, tint = esquema.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(rotuloData(data, hoje), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            }
        }
        if (mostrarAtalhos) {
            Chip("Hoje", data == hoje, { onChange(hoje) })
            Chip("Ontem", data == hoje.minusDays(1), { onChange(hoje.minusDays(1)) })
        }
    }

    if (aberto) {
        // O DatePicker trabalha em milissegundos UTC à meia-noite.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}
