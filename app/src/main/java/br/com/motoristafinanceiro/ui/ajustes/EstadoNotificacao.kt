package br.com.motoristafinanceiro.ui.ajustes

enum class EstadoNotificacao { DESLIGADAS, ATIVAS, SEM_PERMISSAO }

/** [ativas] = a chave de Ajustes; [sistemaPermite] = permissão concedida e notificações não bloqueadas. */
fun estadoNotificacao(ativas: Boolean, sistemaPermite: Boolean): EstadoNotificacao = when {
    !ativas -> EstadoNotificacao.DESLIGADAS
    sistemaPermite -> EstadoNotificacao.ATIVAS
    else -> EstadoNotificacao.SEM_PERMISSAO
}

/** Só o Android 13 (API 33) ou mais novo tem a pergunta de permissão; antes dele as notificações já vêm liberadas. */
fun precisaPedirPermissao(sdkInt: Int, sistemaPermite: Boolean): Boolean = sdkInt >= 33 && !sistemaPermite
