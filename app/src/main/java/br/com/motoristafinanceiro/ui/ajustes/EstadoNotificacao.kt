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

/**
 * Com o aviso ligado por padrão, o app pergunta a permissão uma única vez ao abrir
 * (só no Android 13+ e só se ainda não estiver liberada).
 */
fun devePedirPermissaoAoAbrir(ativas: Boolean, jaPerguntou: Boolean, sdkInt: Int, sistemaPermite: Boolean): Boolean =
    ativas && !jaPerguntou && precisaPedirPermissao(sdkInt, sistemaPermite)
