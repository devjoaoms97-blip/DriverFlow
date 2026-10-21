package br.com.motoristafinanceiro.domain

/**
 * Identifica "este alerta, neste estado": o plano atual do serviço (id do último registro dele)
 * mais o status. Mudou o status (próxima -> atrasada) ou foi registrada uma nova troca, a chave muda
 * e o alerta volta a poder avisar.
 */
fun chaveDoAlerta(alerta: AlertaManutencao): String = "${alerta.manutencao.id}:${alerta.status.name}"

/** Alertas que pedem atenção: próximos ou atrasados. "Em dia" nunca avisa. */
fun alertasUrgentes(alertas: List<AlertaManutencao>): List<AlertaManutencao> =
    alertas.filter { it.status != StatusAlerta.EM_DIA }

/** Urgentes que ainda não foram avisados neste estado. */
fun alertasParaNotificar(alertas: List<AlertaManutencao>, jaNotificados: Set<String>): List<AlertaManutencao> =
    alertasUrgentes(alertas).filter { chaveDoAlerta(it) !in jaNotificados }

/** O que guardar como "já avisado": só o que está urgente agora; o que foi resolvido é esquecido. */
fun chavesParaGuardar(alertas: List<AlertaManutencao>): Set<String> =
    alertasUrgentes(alertas).map { chaveDoAlerta(it) }.toSet()
