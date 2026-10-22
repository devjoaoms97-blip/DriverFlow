package br.com.motoristafinanceiro.ui.home

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.AlertaManutencao
import br.com.motoristafinanceiro.domain.Estatisticas
import br.com.motoristafinanceiro.domain.statusAlerta
import br.com.motoristafinanceiro.ui.reservas.ReservasUiState
import br.com.motoristafinanceiro.ui.reservas.montarEstadoReservas
import br.com.motoristafinanceiro.util.formatarReais
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TextoInicioTest {
    private val hoje = LocalDate.of(2026, 10, 21)

    private fun alerta(tipo: TipoManutencao, restantes: Int) = AlertaManutencao(
        Manutencao(tipo = tipo, data = hoje, odometroKm = 10_000, valorCentavos = 0, intervaloKm = 10_000),
        restantes,
        statusAlerta(restantes),
    )

    @Test
    fun dataDoInicio_diaDaSemanaCurtoEMesPorExtenso() =
        assertEquals("Quarta, 21 de outubro", rotuloDataInicio(hoje))

    @Test
    fun periodos_naOrdemDoSeletor() = assertEquals(listOf("Hoje", "Semana", "Mês"), periodosInicio)

    @Test
    fun tituloDoLucro_porPeriodo() {
        assertEquals("Lucro líquido de hoje", tituloLucroPeriodo(0, "19/10 a 25/10"))
        assertEquals("Lucro líquido da semana (19/10 a 25/10)", tituloLucroPeriodo(1, "19/10 a 25/10"))
        assertEquals("Lucro líquido da semana", tituloLucroPeriodo(1, ""))
        assertEquals("Lucro líquido do mês", tituloLucroPeriodo(2, "19/10 a 25/10"))
    }

    @Test
    fun rodado_kmEHoras_soUmOuTraco() {
        assertEquals("120 km · 8 h", textoRodado(Estatisticas(kmTotal = 120.0, minutosOnline = 480)))
        assertEquals("80,5 km", textoRodado(Estatisticas(kmTotal = 80.5)))
        assertEquals("6,5 h", textoRodado(Estatisticas(minutosOnline = 390)))
        assertEquals("—", textoRodado(Estatisticas()))
    }

    @Test
    fun atencaoManutencao_semAlertas_ehNulo() = assertNull(textoAtencaoManutencao(emptyList()))

    @Test
    fun atencaoManutencao_umAlerta_noSingular() {
        val t = textoAtencaoManutencao(listOf(alerta(TipoManutencao.OLEO, 320)))!!
        assertEquals("1 manutenção pede atenção", t.titulo)
        assertEquals("Troca de óleo: faltam 320 km", t.detalhe)
    }

    @Test
    fun atencaoManutencao_variosAlertas_mostraSoOsDoisPrimeirosNoDetalhe() {
        val t = textoAtencaoManutencao(
            listOf(
                alerta(TipoManutencao.ALINHAMENTO, -150),
                alerta(TipoManutencao.OLEO, 320),
                alerta(TipoManutencao.FREIOS, 400),
            )
        )!!
        assertEquals("3 manutenções pedem atenção", t.titulo)
        assertEquals("Alinhamento/Balanceamento: atrasada em 150 km · Troca de óleo: faltam 320 km", t.detalhe)
    }

    @Test
    fun avisoReservas_semReservas_convidaACadastrar() {
        val t = textoAvisoReservas(ReservasUiState())
        assertEquals("Reservas", t.titulo)
        assertEquals("Cadastre IPVA, seguro e outras despesas fixas", t.detalhe)
    }

    @Test
    fun avisoReservas_mostraTotalDoDiaEAProxima() {
        val seguro = ReservaDespesa(
            id = 1, nome = "Seguro", valorCentavos = 90_000,
            periodicidade = Periodicidade.MENSAL, proximoVencimento = hoje.plusDays(1),
        )
        val ipva = ReservaDespesa(
            id = 2, nome = "IPVA", valorCentavos = 365_000,
            periodicidade = Periodicidade.ANUAL, proximoVencimento = hoje.plusDays(80),
        )
        val estado = montarEstadoReservas(listOf(ipva, seguro), hoje)

        val t = textoAvisoReservas(estado)

        assertEquals("Reservas: separe ${4_000L.formatarReais()} hoje", t.titulo) // 3.000 + 1.000 por dia
        assertEquals("Próxima: Seguro · vence amanhã", t.detalhe)
    }

    @Test
    fun avisoBackup_semDados_naoAvisa() = assertNull(textoAvisoBackup(null, hoje, temDados = false))

    @Test
    fun avisoBackup_nuncaFeito() {
        val t = textoAvisoBackup(null, hoje, temDados = true)!!
        assertEquals("Você ainda não fez backup", t.titulo)
    }

    @Test
    fun avisoBackup_soDepoisDeSeteDias() {
        assertNull(textoAvisoBackup(hoje.minusDays(6), hoje, temDados = true))
        assertEquals("Último backup há 7 dias", textoAvisoBackup(hoje.minusDays(7), hoje, temDados = true)!!.titulo)
        assertEquals("Último backup há 30 dias", textoAvisoBackup(hoje.minusDays(30), hoje, temDados = true)!!.titulo)
    }
}
