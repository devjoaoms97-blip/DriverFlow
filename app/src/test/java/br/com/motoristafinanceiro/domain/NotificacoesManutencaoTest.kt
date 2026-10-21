package br.com.motoristafinanceiro.domain

import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.TipoManutencao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NotificacoesManutencaoTest {

    private val data = LocalDate.of(2026, 10, 1)

    private fun troca(id: Long, tipo: TipoManutencao = TipoManutencao.OLEO, odometro: Int = 10_000, intervalo: Int? = 10_000) =
        Manutencao(id = id, tipo = tipo, data = data, odometroKm = odometro, valorCentavos = 0, intervaloKm = intervalo)

    private fun alertas(odometroAtual: Int, vararg trocas: Manutencao) = calcularAlertas(trocas.toList(), odometroAtual)

    @Test
    fun emDia_naoNotifica() {
        val a = alertas(12_000, troca(1))
        assertTrue(alertasUrgentes(a).isEmpty())
        assertTrue(alertasParaNotificar(a, emptySet()).isEmpty())
    }

    @Test
    fun proxima_notificaNaPrimeiraVezENaoRepete() {
        val a = alertas(19_700, troca(1)) // faltam 300 km
        assertEquals(1, alertasParaNotificar(a, emptySet()).size)

        val guardadas = chavesParaGuardar(a)
        assertTrue(alertasParaNotificar(a, guardadas).isEmpty())
    }

    @Test
    fun proximaQueViraAtrasada_notificaDeNovo() {
        val guardadas = chavesParaGuardar(alertas(19_700, troca(1)))

        val atrasada = alertas(20_200, troca(1)) // atrasada em 200 km
        assertEquals(StatusAlerta.ATRASADA, atrasada.single().status)
        assertEquals(1, alertasParaNotificar(atrasada, guardadas).size)
    }

    @Test
    fun novaTrocaDoServico_esqueceOAvisoAnteriorEPermiteAvisarNoCicloSeguinte() {
        val guardadas = chavesParaGuardar(alertas(20_200, troca(1)))
        assertEquals(1, guardadas.size)

        // Trocou o óleo aos 20.300: novo plano (id 2), em dia. O aviso antigo é esquecido.
        val emDia = alertas(20_300, troca(1), troca(2, odometro = 20_300))
        assertTrue(chavesParaGuardar(emDia).isEmpty())

        // Dez mil km depois, o plano novo fica próximo e avisa de novo.
        val proximoCiclo = alertas(29_900, troca(1), troca(2, odometro = 20_300))
        assertEquals(1, alertasParaNotificar(proximoCiclo, emptySet()).size)
    }

    @Test
    fun variosServicos_avisaSoOsQueAindaNaoForamAvisados() {
        val a = alertas(
            19_700,
            troca(1), // óleo: faltam 300 km
            troca(2, TipoManutencao.PNEUS, odometro = 0, intervalo = 19_500), // pneus: atrasados em 200 km
        )
        assertEquals(2, alertasParaNotificar(a, emptySet()).size)

        val soOleoJaAvisado = setOf(chaveDoAlerta(a.first { it.manutencao.id == 1L }))
        val novos = alertasParaNotificar(a, soOleoJaAvisado)
        assertEquals(listOf(2L), novos.map { it.manutencao.id })
    }

    @Test
    fun chavesParaGuardar_ficamSoComOsUrgentes() {
        val a = alertas(
            19_700,
            troca(1), // próxima
            troca(2, TipoManutencao.CORREIA, odometro = 10_000, intervalo = 60_000), // em dia
        )
        assertEquals(setOf("1:PROXIMA"), chavesParaGuardar(a))
    }

    @Test
    fun servicoSemIntervalo_naoGeraAviso() {
        val a = alertas(50_000, troca(1, intervalo = null))
        assertTrue(a.isEmpty())
        assertTrue(alertasParaNotificar(a, emptySet()).isEmpty())
    }
}
