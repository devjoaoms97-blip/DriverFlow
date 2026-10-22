package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.domain.Odometro
import br.com.motoristafinanceiro.util.formatarKm
import java.time.format.DateTimeFormatter

private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")

/** "Odômetro estimado" quando há km dos ganhos somados à leitura; senão "Odômetro atual". */
fun rotuloOdometro(o: Odometro?): String = if (o?.estimado == true) "Odômetro estimado" else "Odômetro atual"

/** "Última leitura: 48.500 km em 19/10 + 220 km dos ganhos"; null quando não há estimativa. */
fun detalheOdometro(o: Odometro?): String? = o?.takeIf { it.estimado }?.let {
    "Última leitura: ${it.leitura.km.formatarKm()} em ${it.leitura.data.format(formatoDiaMes)} + " +
        "${it.kmEstimados.formatarKm()} dos ganhos"
}
