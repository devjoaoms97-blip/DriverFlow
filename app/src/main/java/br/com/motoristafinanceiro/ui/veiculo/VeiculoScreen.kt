package br.com.motoristafinanceiro.ui.veiculo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.domain.ResumoVeiculo
import br.com.motoristafinanceiro.domain.StatusAlerta
import br.com.motoristafinanceiro.domain.descricaoManutencao
import br.com.motoristafinanceiro.domain.precoPorLitroCentavos
import br.com.motoristafinanceiro.ui.componentes.BarraProgresso
import br.com.motoristafinanceiro.ui.componentes.Cartao
import br.com.motoristafinanceiro.ui.componentes.Icones
import br.com.motoristafinanceiro.ui.componentes.MargemTela
import br.com.motoristafinanceiro.ui.componentes.RotuloSecao
import br.com.motoristafinanceiro.ui.componentes.Selo
import br.com.motoristafinanceiro.ui.componentes.TituloTela
import br.com.motoristafinanceiro.ui.componentes.ValorRotulado
import br.com.motoristafinanceiro.util.formatarKm
import br.com.motoristafinanceiro.util.formatarKmPorLitro
import br.com.motoristafinanceiro.util.formatarReais
import br.com.motoristafinanceiro.util.paraTextoNumero
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun VeiculoScreen(
    onEditarAbastecimento: (Long) -> Unit,
    onEditarManutencao: (Long) -> Unit,
    onNovoAbastecimento: () -> Unit,
    onNovaManutencao: () -> Unit,
    vm: VeiculoViewModel = viewModel(factory = VeiculoViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MargemTela, end = MargemTela, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "titulo") { TituloTela("Veículo") }
        item(key = "resumo") { CartaoResumo(state.resumo, state.odometro, state.depreciacaoPorKmCentavos) }

        item(key = "acoes") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoRegistro("Abastecer", Icones.Combustivel, onNovoAbastecimento, Modifier.weight(1f))
                BotaoRegistro("Manutenção", Icones.Chave, onNovaManutencao, Modifier.weight(1f))
            }
        }

        if (state.alertas.isNotEmpty()) {
            item(key = "rotulo-alertas") { RotuloSecao("Próximas manutenções", Modifier.padding(horizontal = 4.dp)) }
            state.alertas.forEach { a ->
                item(key = "alerta-${a.manutencao.id}") { CartaoAlerta(a) { onEditarManutencao(a.manutencao.id) } }
            }
        }

        item(key = "rotulo-manutencoes") { RotuloSecao("Manutenções feitas", Modifier.padding(horizontal = 4.dp)) }
        item(key = "manutencoes") {
            if (state.manutencoes.isEmpty()) {
                Vazio("Toque em Manutenção para registrar a primeira.")
            } else {
                ListaEmCartao(state.manutencoes) { m -> LinhaManutencao(m) { onEditarManutencao(m.id) } }
            }
        }

        item(key = "rotulo-abastecimentos") { RotuloSecao("Abastecimentos", Modifier.padding(horizontal = 4.dp)) }
        item(key = "abastecimentos") {
            if (state.abastecimentos.isEmpty()) {
                Vazio("Toque em Abastecer para registrar o primeiro.")
            } else {
                ListaEmCartao(state.abastecimentos) { a -> LinhaAbastecimento(a) { onEditarAbastecimento(a.id) } }
            }
        }
    }
}

@Composable
private fun CartaoResumo(r: ResumoVeiculo, odometro: Odometro?, depreciacaoPorKmCentavos: Long) {
    val esquema = MaterialTheme.colorScheme
    Cartao(espacamento = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotuloOdometro(odometro), style = MaterialTheme.typography.titleSmall, color = esquema.onSurfaceVariant)
            Text(odometro?.km?.formatarKm() ?: "—", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp))
            detalheOdometro(odometro)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = esquema.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValorRotulado("Consumo médio", r.consumoMedioKmPorLitro?.formatarKmPorLitro() ?: "—", Modifier.weight(1f))
            ValorRotulado("Último litro", r.precoUltimoLitroCentavos?.formatarReais() ?: "—", Modifier.weight(1f))
            ValorRotulado("Custo/km no mês", r.custoPorKmMesCentavos?.formatarReais() ?: "—", Modifier.weight(1f))
        }
        if (depreciacaoPorKmCentavos > 0) {
            Text(
                "O custo por km inclui depreciação de " + depreciacaoPorKmCentavos.formatarReais() +
                    "/km (ajuste na engrenagem do Início).",
                style = MaterialTheme.typography.bodySmall,
                color = esquema.onSurfaceVariant,
            )
        }
        if (r.consumoMedioKmPorLitro == null) {
            Text(
                "O consumo aparece depois de 2 abastecimentos com tanque cheio.",
                style = MaterialTheme.typography.bodySmall,
                color = esquema.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BotaoRegistro(texto: String, icone: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Cores do selo, da barra e da borda de cada estado de manutenção. */
private data class CoresAlerta(val cor: Color, val fundo: Color, val borda: Color)

@Composable
private fun StatusAlerta.cores(): CoresAlerta {
    val e = MaterialTheme.colorScheme
    return when (this) {
        StatusAlerta.ATRASADA -> CoresAlerta(e.error, e.errorContainer, e.error.copy(alpha = 0.45f))
        StatusAlerta.PROXIMA -> CoresAlerta(e.tertiary, e.tertiaryContainer, e.tertiary.copy(alpha = 0.45f))
        StatusAlerta.EM_DIA -> CoresAlerta(e.secondary, e.secondaryContainer, e.outlineVariant)
    }
}

@Composable
private fun CartaoAlerta(a: AlertaManutencao, onClick: () -> Unit) {
    val cores = a.status.cores()
    Cartao(
        onClick = onClick,
        forma = MaterialTheme.shapes.medium,
        borda = cores.borda,
        espacamentoInterno = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        espacamento = 10.dp,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                descricaoManutencao(a.manutencao),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Selo(a.seloTexto(), cores.cor, cores.fundo)
        }
        BarraProgresso(a.fracaoRodada(), cores.cor)
        Text(a.detalheTexto(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Vazio(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun <T> ListaEmCartao(itens: List<T>, linha: @Composable (T) -> Unit) {
    Cartao(forma = MaterialTheme.shapes.medium, espacamentoInterno = PaddingValues(0.dp), espacamento = 0.dp) {
        itens.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            linha(item)
        }
    }
}

@Composable
private fun LinhaRegistro(titulo: String, detalhes: String, valor: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(detalhes, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(valor, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
    }
}

@Composable
private fun LinhaManutencao(m: Manutencao, onClick: () -> Unit) {
    val detalhes = listOfNotNull(
        m.data.format(formatoData),
        m.odometroKm.formatarKm(),
        m.intervaloKm?.let { "a cada " + it.formatarKm() },
    ).joinToString(" · ")
    LinhaRegistro(descricaoManutencao(m), detalhes, m.valorCentavos.formatarReais(), onClick)
}

@Composable
private fun LinhaAbastecimento(a: Abastecimento, onClick: () -> Unit) {
    val preco = precoPorLitroCentavos(a)?.let { it.formatarReais() + "/L" }
    val detalhes = listOfNotNull(preco, a.odometroKm.formatarKm()).joinToString(" · ")
    LinhaRegistro(
        a.data.format(formatoData) + " · " + a.litros.paraTextoNumero() + " L" + if (a.tanqueCheio) "" else " (parcial)",
        detalhes,
        a.valorTotalCentavos.formatarReais(),
        onClick,
    )
}
