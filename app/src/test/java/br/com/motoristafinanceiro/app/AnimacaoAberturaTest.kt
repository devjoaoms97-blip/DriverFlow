package br.com.motoristafinanceiro.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guarda os defeitos que fariam o Lottie do Android recusar ou não mostrar a animação:
 * escala animada com um valor só (o Lottie lê dois) e camadas de texto (dentro do Compose o Lottie não
 * carrega fontes, então o nome e o lema são desenhados pelo Compose).
 * Robolectric porque o org.json de verdade só existe no Android.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnimacaoAberturaTest {

    private val json = JSONObject(File("src/main/res/raw/driverflow_splash.json").readText())

    @Test
    fun escalaAnimada_temTresValoresEmCadaQuadro() {
        val camadas = json.getJSONArray("layers")
        for (i in 0 until camadas.length()) {
            val camada = camadas.getJSONObject(i)
            val escala = camada.getJSONObject("ks").getJSONObject("s")
            if (escala.getInt("a") != 1) continue
            val quadros = escala.getJSONArray("k")
            for (q in 0 until quadros.length()) {
                val quadro = quadros.getJSONObject(q)
                listOf("s", "e").filter { quadro.has(it) }.forEach { campo ->
                    assertEquals("${camada.getString("nm")} quadro $q ($campo)", 3, quadro.getJSONArray(campo).length())
                }
            }
        }
    }

    @Test
    fun semCamadasDeTexto_onomeEOLemaSaoDoCompose() {
        val camadas = json.getJSONArray("layers")
        val tipos = (0 until camadas.length()).map { camadas.getJSONObject(it).getInt("ty") }
        assertTrue("Camada de texto (ty 5) não aparece no Lottie dentro do Compose", 5 !in tipos)
    }

    @Test
    fun barraDeProgresso_temOPreenchimentoPorCimaDoFundo() {
        val camadas = json.getJSONArray("layers")
        val barra = (0 until camadas.length()).map { camadas.getJSONObject(it) }.first { it.getString("nm") == "Loading accent" }
        // No Lottie o primeiro item da lista fica por cima: o preenchimento vem antes do fundo da barra.
        assertEquals("Progress", barra.getJSONArray("shapes").getJSONObject(0).getString("nm"))
        assertEquals("Track", barra.getJSONArray("shapes").getJSONObject(1).getString("nm"))
    }

    @Test
    fun logo_eAImagemRealDoIconeEmbutidaNoJson() {
        val imagens = json.getJSONArray("assets")
        assertEquals(1, imagens.length())
        val imagem = imagens.getJSONObject(0)
        assertTrue(imagem.getString("p").startsWith("data:image/png;base64,"))

        val camadas = json.getJSONArray("layers")
        val usaAImagem = (0 until camadas.length()).any { camadas.getJSONObject(it).optString("refId") == imagem.getString("id") }
        assertTrue("Nenhuma camada usa a imagem do logo", usaAImagem)
    }

    @Test
    fun duracao_eDeDoisSegundosEMeio() {
        assertEquals(30.0, json.getDouble("fr"), 0.0)
        assertEquals(75.0, json.getDouble("op"), 0.0)
        assertEquals(json.getDouble("op").toFloat(), br.com.motoristafinanceiro.ui.abertura.QUADROS_DA_ABERTURA, 0f)
    }
}
