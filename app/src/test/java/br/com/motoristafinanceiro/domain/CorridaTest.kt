package br.com.motoristafinanceiro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Os cards abaixo imitam o formato da Uber e da 99 que se vê em vídeos e prints; serão trocados pelos textos
 * das gravações reais (modo gravação) assim que houver.
 */
class CorridaTest {

    private val cardUber = listOf(
        "UberX",
        "R$ 14,50",
        "★ 4,92",
        "6 min (2,1 km) de distância",
        "Rua Augusta, Consolação",
        "Viagem de 18 min (8,4 km)",
        "Av. Paulista, Bela Vista",
        "Aceitar",
    )

    @Test
    fun cardDaUber_lerTudo() {
        assertEquals(
            OfertaCorrida(valorCentavos = 1_450, kmAteBusca = 2.1, minAteBusca = 6, kmViagem = 8.4, minViagem = 18, nota = 4.92),
            lerOferta(cardUber),
        )
    }

    @Test
    fun cardDa99_comMetrosEHoras() {
        val card = listOf("99Pop", "R$ 32,90", "4,85", "A 800 m · 3 min", "1 h 5 min · 27,3 km", "Aceitar corrida")
        val o = lerOferta(card)!!
        assertEquals(3_290L, o.valorCentavos)
        assertEquals(0.8, o.kmAteBusca!!, 1e-9)
        assertEquals(3, o.minAteBusca)
        assertEquals(27.3, o.kmViagem!!, 1e-9)
        assertEquals(65, o.minViagem)
        assertEquals(4.85, o.nota!!, 1e-9)
    }

    @Test
    fun valor_ignoraAcrescimoETaxaPorKm_eAceitaMilhar() {
        val o = lerOferta(listOf("+R$ 3,00 dinâmico", "R$ 1,80/km", "R$ 1.020,00", "10 min (5 km)"))!!
        assertEquals(102_000L, o.valorCentavos)
        // A linha com R$/km não vira trecho.
        assertNull(o.kmAteBusca)
        assertEquals(5.0, o.kmViagem!!, 1e-9)
    }

    @Test
    fun valorSemCentavos_eEspacoInquebravel() {
        assertEquals(1_200L, lerOferta(listOf("R$ 12", "3 km"))!!.valorCentavos)
    }

    @Test
    fun umTrechoSo_contaComoViagem() {
        val o = lerOferta(listOf("R$ 9,00", "Total: 12 min (4,5 km)"))!!
        assertNull(o.kmAteBusca)
        assertEquals(4.5, o.kmViagem!!, 1e-9)
        assertEquals(12, o.minTotal)
    }

    @Test
    fun semValorOuSemDistancia_naoEhOferta() {
        assertNull(lerOferta(listOf("Você está online", "Procurando viagens")))
        assertNull(lerOferta(listOf("Ganhos de hoje", "R$ 182,40")))
        assertNull(lerOferta(listOf("Siga por 2,3 km", "Vire à direita em 200 m")))
        assertNull(lerOferta(emptyList()))
    }

    @Test
    fun nota_soComEstrelaOuSozinha() {
        assertNull(lerOferta(listOf("R$ 10,00", "Viagem 4,5 km", "3,5 vezes"))!!.nota)
        assertEquals(5.0, lerOferta(listOf("R$ 10,00", "5,00 ★ (310)", "4,5 km"))!!.nota!!, 1e-9)
    }

    @Test
    fun totais_somamBuscaEViagem() {
        val o = OfertaCorrida(1_000, kmAteBusca = 2.0, minAteBusca = 5, kmViagem = 8.0, minViagem = null)
        assertEquals(10.0, o.kmTotal, 1e-9)
        assertEquals(5, o.minTotal)
        assertNull(OfertaCorrida(1_000, kmViagem = 3.0).minTotal)
    }

    private val limites = LimitesCorrida(bomPorKmCentavos = 200, minimoPorKmCentavos = 150)

    @Test
    fun avaliar_contaODeslocamentoAteOPassageiro() {
        // R$ 14,50 ÷ (2,1 + 8,4 km) = R$ 1,38/km; ÷ 24 min = R$ 36,25/h.
        val a = avaliarOferta(lerOferta(cardUber)!!, custoPorKmCentavos = 80, limites = limites)
        assertEquals(138L, a.porKmCentavos)
        assertEquals(3_625L, a.porHoraCentavos)
        assertEquals(1_450L - 840L, a.lucroCentavos) // 10,5 km × R$ 0,80
        assertEquals(CorOferta.VERMELHO, a.cor)
    }

    @Test
    fun avaliar_coresPelasFaixas() {
        fun cor(valor: Long) = avaliarOferta(OfertaCorrida(valor, kmViagem = 10.0), null, limites).cor
        assertEquals(CorOferta.VERDE, cor(2_000))
        assertEquals(CorOferta.AMARELO, cor(1_994))
        assertEquals(CorOferta.AMARELO, cor(1_500))
        assertEquals(CorOferta.VERMELHO, cor(1_494))
    }

    @Test
    fun avaliar_semCustoOuSemTempo_deixaEmBranco() {
        val a = avaliarOferta(OfertaCorrida(1_000, kmViagem = 5.0), null, limites)
        assertNull(a.lucroCentavos)
        assertNull(a.porHoraCentavos)
        assertEquals(200L, a.porKmCentavos)
    }

    @Test
    fun limites_amareloNaoPassaDoVerde() {
        assertEquals(true, LimitesCorrida(200, 150).validos)
        assertEquals(true, LimitesCorrida(200, 200).validos)
        assertEquals(false, LimitesCorrida(150, 200).validos)
        assertEquals(false, LimitesCorrida(200, 0).validos)
    }
}
