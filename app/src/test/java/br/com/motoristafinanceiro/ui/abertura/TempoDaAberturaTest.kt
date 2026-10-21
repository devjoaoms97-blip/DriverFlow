package br.com.motoristafinanceiro.ui.abertura

import org.junit.Assert.assertEquals
import org.junit.Test

class TempoDaAberturaTest {

    @Test
    fun antesDoInicio_estaInvisivel() = assertEquals(0f, opacidadeNoQuadro(10f, 22f, 30f), 0f)

    @Test
    fun noMeioDaTransicao_estaMeioVisivel() = assertEquals(0.5f, opacidadeNoQuadro(26f, 22f, 30f), 0.0001f)

    @Test
    fun depoisDoFim_estaTotalmenteVisivel() = assertEquals(1f, opacidadeNoQuadro(60f, 22f, 30f), 0f)
}
