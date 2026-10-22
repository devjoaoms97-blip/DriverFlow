package br.com.motoristafinanceiro.ui.veiculo

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.StatusAlerta
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

    private val comIntervalo = manutencao.copy(intervaloKm = 10_000)

    private fun alertaComIntervalo(restantes: Int) = AlertaManutencao(comIntervalo, restantes, statusAlerta(restantes))

    @Test
    fun fracaoRodada_proporcionalAoIntervalo() {
        assertEquals(0.97f, alertaComIntervalo(300).fracaoRodada(), 0.001f)
        assertEquals(0.5f, alertaComIntervalo(5_000).fracaoRodada(), 0.001f)
        assertEquals(1f, alertaComIntervalo(-150).fracaoRodada(), 0f) // atrasada não passa de cheia
        assertEquals(0f, alertaComIntervalo(12_000).fracaoRodada(), 0f)
    }

    @Test
    fun fracaoRodada_semIntervalo_cheiaSoSeAtrasada() {
        assertEquals(1f, AlertaManutencao(manutencao, -10, StatusAlerta.ATRASADA).fracaoRodada(), 0f)
        assertEquals(0f, AlertaManutencao(manutencao, 10, StatusAlerta.PROXIMA).fracaoRodada(), 0f)
    }

    @Test
    fun selo_porEstado() {
        assertEquals("Atrasada", alertaComIntervalo(-150).seloTexto())
        assertEquals("Em 320 km", alertaComIntervalo(320).seloTexto())
        assertEquals("No limite", alertaComIntervalo(0).seloTexto())
        assertEquals("Em dia", alertaComIntervalo(8_400).seloTexto())
    }

    @Test
    fun detalhe_comIntervaloQuandoHa() {
        assertEquals("Faltam 8.400 km · a cada 10.000 km", alertaComIntervalo(8_400).detalheTexto())
        assertEquals("Faltam 300 km", alerta(300).detalheTexto())
    }
}
