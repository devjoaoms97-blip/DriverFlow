package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.domain.ConferenciaKm
import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.util.formatarKm
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")

/** "Odômetro estimado" quando há km lançados (ganhos e pessoais) somados à leitura; senão "Odômetro atual". */
fun rotuloOdometro(o: Odometro?): String = if (o?.estimado == true) "Odômetro estimado" else "Odômetro atual"

/** "Última leitura: 48.500 km em 19/10 + 220 km lançados"; null quando não há estimativa. */
fun detalheOdometro(o: Odometro?): String? = o?.takeIf { it.estimado }?.let {
    "Última leitura: ${it.leitura.km.formatarKm()} em ${it.leitura.data.format(formatoDiaMes)} + " +
        "${it.kmEstimados.formatarKm()} lançados"
}

/** Textos do cartão de conferência do odômetro. [atencao] quando muitos km ficaram sem lançamento (ou sobraram). */
data class TextoConferencia(val titulo: String, val selo: String, val detalhe: String, val atencao: Boolean)

/** Fora dessa faixa de km lançados, o cartão chama atenção: há km esquecidos ou leitura errada. */
val FAIXA_LANCADO_OK = 90..110

fun textoConferencia(c: ConferenciaKm): TextoConferencia {
    val periodo = c.inicio.data.format(formatoDiaMes) + " a " + c.fim.data.format(formatoDiaMes)
    val lancados = c.kmLancados.roundToInt().formatarKm()
    val base = "De $periodo o odômetro andou ${c.kmReais.formatarKm()} e você lançou $lancados" +
        (if (c.kmPessoal > 0) " (${c.kmPessoal.roundToInt().formatarKm()} pessoais)" else "") + "."
    val atencao = c.percentualLancado !in FAIXA_LANCADO_OK
    val detalhe = when {
        c.kmSemLancamento > 0 && atencao ->
            "$base ${c.kmSemLancamento.formatarKm()} ficaram sem lançamento: o custo por km aparece mais alto do " +
                "que é. Lance os km pessoais e confira os km dos turnos."
        c.kmSemLancamento < 0 && atencao ->
            "$base Foram lançados ${(-c.kmSemLancamento).formatarKm()} a mais do que o odômetro mostra: confira os " +
                "km dos turnos e o odômetro dos abastecimentos."
        else -> base
    }
    return TextoConferencia("Km lançados × odômetro", "${c.percentualLancado}% lançados", detalhe, atencao)
}
