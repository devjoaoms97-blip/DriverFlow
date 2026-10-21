package br.com.motoristafinanceiro.ui.theme

import br.com.motoristafinanceiro.data.ModoTema

/** Se o app deve usar a paleta escura, dado o modo escolhido e o modo atual do celular. */
fun temaEscuro(modo: ModoTema, sistemaEscuro: Boolean): Boolean = when (modo) {
    ModoTema.ESCURO -> true
    ModoTema.CLARO -> false
    ModoTema.AUTOMATICO -> sistemaEscuro
}
