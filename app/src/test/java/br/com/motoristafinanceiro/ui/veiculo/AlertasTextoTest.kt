package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.statusAlerta
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AlertasTextoTest {
    private val manutencao = Manutencao(
        tipo = TipoManutencao.OLEO, data = LocalDate.of(2026, 10, 12), odometroKm = 10_000, valorCentavos = 0,
    )

    private fun alerta(restantes: Int) = AlertaManutencao(manutencao, restantes, statusAlerta(restantes))

    @Test
    fun atrasada() = assertEquals("Atrasada em 500 km", alerta(-500).resumoTexto())

    @Test
    fun proxima() = assertEquals("Faltam 300 km", alerta(300).resumoTexto())

    @Test
    fun noLimite() = assertEquals("No limite", alerta(0).resumoTexto())

    @Test
    fun emDia_comSeparadorDeMilhar() = assertEquals("Faltam 7.000 km", alerta(7_000).resumoTexto())
}
