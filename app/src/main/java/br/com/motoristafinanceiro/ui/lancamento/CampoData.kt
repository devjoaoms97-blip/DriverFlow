package br.com.motoristafinanceiro.ui.lancamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Botão com a data atual + (opcionalmente) atalhos "Hoje" e "Ontem". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    data: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    mostrarAtalhos: Boolean = true,
) {
    var aberto by remember { mutableStateOf(false) }

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = { aberto = true }) { Text(data.format(formatoData)) }
        if (mostrarAtalhos) {
            TextButton(onClick = { onChange(LocalDate.now()) }) { Text("Hoje") }
            TextButton(onClick = { onChange(LocalDate.now().minusDays(1)) }) { Text("Ontem") }
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
