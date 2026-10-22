package br.com.motoristafinanceiro.leitor

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import br.com.motoristafinanceiro.R
import br.com.motoristafinanceiro.domain.AvaliacaoCorrida
import br.com.motoristafinanceiro.domain.CorOferta

/** Cor de fundo da janela; o texto é branco (contraste acima de 4,5:1 nas três). */
internal fun corDeFundo(cor: CorOferta): Int = when (cor) {
    CorOferta.VERDE -> 0xFF0E7A4F.toInt()
    CorOferta.AMARELO -> 0xFF8A5A00.toInt()
    CorOferta.VERMELHO -> 0xFFB3261E.toInt()
}

/**
 * Janela que aparece por cima da Uber/99 no alto da tela, com a conta da oferta.
 * Usa a janela de acessibilidade (não precisa da permissão "sobrepor a outros apps").
 * Tocar fecha; ela nunca toca em nada do outro app.
 */
class JanelaDaOferta(private val contexto: Context) {
    private val gerenciador = contexto.getSystemService(WindowManager::class.java)
    private var vista: LinearLayout? = null
    private lateinit var titulo: TextView
    private lateinit var valores: TextView
    private lateinit var percurso: TextView

    val visivel: Boolean get() = vista != null

    fun mostrar(a: AvaliacaoCorrida) {
        val v = vista ?: criar().also { vista = it }
        (v.background as GradientDrawable).setColor(corDeFundo(a.cor))
        titulo.text = tituloAvaliacao(a)
        valores.text = linhaValores(a)
        percurso.text = linhaPercurso(a)
    }

    fun esconder() {
        vista?.let { runCatching { gerenciador.removeView(it) } }
        vista = null
    }

    private fun dp(valor: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor.toFloat(), contexto.resources.displayMetrics).toInt()

    private fun texto(tamanhoSp: Float, negrito: Boolean, fonte: Typeface?): TextView = TextView(contexto).apply {
        setTextColor(0xFFFFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanhoSp)
        typeface = Typeface.create(fonte, if (negrito) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun criar(): LinearLayout {
        val fonte = runCatching { ResourcesCompat.getFont(contexto, R.font.manrope) }.getOrNull()
        titulo = texto(30f, true, fonte)
        valores = texto(16f, true, fonte)
        percurso = texto(14f, false, fonte)
        val rodape = texto(11f, false, fonte).apply {
            text = "DriverFlow · só a conta, quem aceita é você · toque para fechar"
            alpha = 0.8f
        }
        val v = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat() }
            elevation = dp(8).toFloat()
            addView(titulo)
            addView(valores)
            addView(percurso)
            addView(rodape)
            setOnClickListener { esconder() }
        }
        val largura = contexto.resources.displayMetrics.widthPixels - dp(24)
        val parametros = WindowManager.LayoutParams(
            largura,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(40)
        }
        gerenciador.addView(v, parametros)
        return v
    }
}
