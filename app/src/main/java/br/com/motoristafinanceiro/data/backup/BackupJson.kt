package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.CategoriaDespesa
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.OrigemDespesa
import br.com.motoristafinanceiro.data.model.Periodicidade
import br.com.motoristafinanceiro.data.model.Plataforma
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import br.com.motoristafinanceiro.data.model.TipoManutencao
import br.com.motoristafinanceiro.domain.DEPRECIACAO_MAXIMA_POR_KM_CENTAVOS
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Conversão entre [DadosBackup] e o texto do `dados.json`.
 *
 * Datas em ISO ("2026-10-20"), meses como "2026-10", enums pelo nome, dinheiro em centavos.
 * Na leitura TUDO é validado antes de devolver: se algo estiver errado, nada é restaurado.
 */
object BackupJson {
    const val FORMATO = "motorista-financeiro-backup"

    /** Versão do formato. Suba quando o formato mudar e continue lendo as versões antigas. */
    const val VERSAO_ATUAL = 1

    fun gerar(dados: DadosBackup, criadoEmMs: Long): String =
        JSONObject()
            .put("formato", FORMATO)
            .put("versaoSchema", VERSAO_ATUAL)
            .put("criadoEmMs", criadoEmMs)
            .put("depreciacaoPorKmCentavos", dados.depreciacaoPorKmCentavos)
            .put("notificacoesManutencaoAtivas", dados.notificacoesManutencaoAtivas ?: JSONObject.NULL)
            .put("ganhos", array(dados.ganhos) {
                JSONObject()
                    .put("id", it.id)
                    .put("data", it.data.toString())
                    .put("plataforma", it.plataforma.name)
                    .put("valorCentavos", it.valorCentavos)
                    .put("gorjetaBonusCentavos", it.gorjetaBonusCentavos)
                    .put("km", it.km)
                    .put("minutosOnline", it.minutosOnline)
                    .put("observacao", it.observacao)
            })
            .put("despesas", array(dados.despesas) {
                JSONObject()
                    .put("id", it.id)
                    .put("data", it.data.toString())
                    .put("categoria", it.categoria.name)
                    .put("valorCentavos", it.valorCentavos)
                    .put("observacao", it.observacao)
                    .put("fotoUri", it.fotoUri)
                    .put("origem", it.origem.name)
                    .put("origemId", it.origemId)
            })
            .put("abastecimentos", array(dados.abastecimentos) {
                JSONObject()
                    .put("id", it.id)
                    .put("data", it.data.toString())
                    .put("litros", it.litros)
                    .put("valorTotalCentavos", it.valorTotalCentavos)
                    .put("odometroKm", it.odometroKm)
                    .put("tanqueCheio", it.tanqueCheio)
            })
            .put("manutencoes", array(dados.manutencoes) {
                JSONObject()
                    .put("id", it.id)
                    .put("tipo", it.tipo.name)
                    .put("data", it.data.toString())
                    .put("odometroKm", it.odometroKm)
                    .put("valorCentavos", it.valorCentavos)
                    .put("intervaloKm", it.intervaloKm)
                    .put("observacao", it.observacao)
            })
            .put("reservas", array(dados.reservas) {
                JSONObject()
                    .put("id", it.id)
                    .put("nome", it.nome)
                    .put("valorCentavos", it.valorCentavos)
                    .put("periodicidade", it.periodicidade.name)
                    .put("proximoVencimento", it.proximoVencimento.toString())
            })
            .put("metas", array(dados.metas) {
                JSONObject()
                    .put("mes", it.mes)
                    .put("valorAlvoLucroCentavos", it.valorAlvoLucroCentavos)
            })
            .toString()

    /** @throws BackupInvalidoException se o texto não for um backup válido e completo. */
    fun ler(texto: String): DadosBackup {
        val raiz = try {
            JSONObject(texto)
        } catch (e: JSONException) {
            throw BackupInvalidoException("O arquivo de dados está corrompido ou não é um backup.")
        }
        if (raiz.optString("formato") != FORMATO) {
            throw BackupInvalidoException("Este arquivo não é um backup do Motorista Financeiro.")
        }
        val versao = raiz.optInt("versaoSchema", -1)
        if (versao < 1) throw BackupInvalidoException("O backup não informa a versão do formato.")
        if (versao > VERSAO_ATUAL) {
            throw BackupInvalidoException("Este backup foi feito por uma versão mais nova do app. Atualize o app antes de restaurar.")
        }

        val depreciacao = raiz.longo("depreciacaoPorKmCentavos")
        if (depreciacao !in 0..DEPRECIACAO_MAXIMA_POR_KM_CENTAVOS) {
            throw BackupInvalidoException("Depreciação por km fora do limite permitido.")
        }

        val notificacoes = raiz.booleanoOpc("notificacoesManutencaoAtivas")

        val ganhos = tabela(raiz, "ganhos") {
            Ganho(
                id = it.idPositivo(),
                data = it.data("data"),
                plataforma = it.enumerado<Plataforma>("plataforma"),
                valorCentavos = it.centavos("valorCentavos"),
                gorjetaBonusCentavos = it.centavos("gorjetaBonusCentavos"),
                km = it.duploNaoNegativo("km"),
                minutosOnline = it.inteiroNaoNegativo("minutosOnline"),
                observacao = it.textoOpc("observacao"),
            )
        }
        val abastecimentos = tabela(raiz, "abastecimentos") {
            Abastecimento(
                id = it.idPositivo(),
                data = it.data("data"),
                litros = it.duploNaoNegativo("litros"),
                valorTotalCentavos = it.centavos("valorTotalCentavos"),
                odometroKm = it.inteiroNaoNegativo("odometroKm"),
                tanqueCheio = it.booleano("tanqueCheio"),
            )
        }
        val manutencoes = tabela(raiz, "manutencoes") {
            Manutencao(
                id = it.idPositivo(),
                tipo = it.enumerado<TipoManutencao>("tipo"),
                data = it.data("data"),
                odometroKm = it.inteiroNaoNegativo("odometroKm"),
                valorCentavos = it.centavos("valorCentavos"),
                intervaloKm = it.inteiroOpc("intervaloKm"),
                observacao = it.textoOpc("observacao"),
            )
        }
        val despesas = tabela(raiz, "despesas") {
            val foto = it.textoOpc("fotoUri")
            if (foto != null && !nomeDeFotoValido(foto)) {
                throw BackupInvalidoException("nome de comprovante inválido.")
            }
            Despesa(
                id = it.idPositivo(),
                data = it.data("data"),
                categoria = it.enumerado<CategoriaDespesa>("categoria"),
                valorCentavos = it.centavos("valorCentavos"),
                observacao = it.textoOpc("observacao"),
                fotoUri = foto,
                origem = it.enumerado<OrigemDespesa>("origem"),
                origemId = it.longoOpc("origemId"),
            )
        }
        val reservas = tabela(raiz, "reservas") {
            ReservaDespesa(
                id = it.idPositivo(),
                nome = it.texto("nome"),
                valorCentavos = it.centavos("valorCentavos"),
                periodicidade = it.enumerado<Periodicidade>("periodicidade"),
                proximoVencimento = it.data("proximoVencimento"),
            )
        }
        val metas = tabela(raiz, "metas") {
            val mes = it.texto("mes")
            try {
                YearMonth.parse(mes)
            } catch (e: DateTimeParseException) {
                throw BackupInvalidoException("campo \"mes\" inválido.")
            }
            Meta(mes = mes, valorAlvoLucroCentavos = it.centavos("valorAlvoLucroCentavos"))
        }

        exigirUnicos("ganhos", ganhos.map { it.id })
        exigirUnicos("despesas", despesas.map { it.id })
        exigirUnicos("abastecimentos", abastecimentos.map { it.id })
        exigirUnicos("manutencoes", manutencoes.map { it.id })
        exigirUnicos("reservas", reservas.map { it.id })
        exigirUnicos("metas", metas.map { it.mes })
        conferirVinculos(despesas, abastecimentos.map { it.id }.toSet(), manutencoes.map { it.id }.toSet())

        return DadosBackup(ganhos, despesas, abastecimentos, manutencoes, reservas, metas, depreciacao, notificacoes)
    }

    /** Despesa gerada por abastecimento/manutenção precisa apontar para um registro que existe no backup. */
    private fun conferirVinculos(despesas: List<Despesa>, abastecimentos: Set<Long>, manutencoes: Set<Long>) {
        despesas.forEachIndexed { i, d ->
            val existe = when (d.origem) {
                OrigemDespesa.MANUAL -> true
                OrigemDespesa.ABASTECIMENTO -> d.origemId in abastecimentos
                OrigemDespesa.MANUTENCAO -> d.origemId in manutencoes
            }
            if (!existe) throw BackupInvalidoException("despesas #${i + 1}: aponta para um registro que não existe no backup.")
        }
    }

    private fun exigirUnicos(tabela: String, chaves: List<Any>) {
        if (chaves.toSet().size != chaves.size) throw BackupInvalidoException("Tabela \"$tabela\" tem registros repetidos.")
    }

    private fun <T> array(lista: List<T>, paraJson: (T) -> JSONObject): JSONArray =
        JSONArray().also { a -> lista.forEach { a.put(paraJson(it)) } }

    private fun <T> tabela(raiz: JSONObject, nome: String, linha: (JSONObject) -> T): List<T> {
        val arr = raiz.optJSONArray(nome) ?: throw BackupInvalidoException("Tabela \"$nome\" ausente no backup.")
        return List(arr.length()) { i ->
            val obj = arr.optJSONObject(i) ?: throw BackupInvalidoException("$nome #${i + 1}: registro inválido.")
            try {
                linha(obj)
            } catch (e: BackupInvalidoException) {
                throw BackupInvalidoException("$nome #${i + 1}: ${e.message}")
            }
        }
    }

    // ---- Leitura de campos, com mensagens em português ----

    private fun JSONObject.ausente(chave: String) = !has(chave) || isNull(chave)

    private fun JSONObject.obrig(chave: String): Any {
        if (ausente(chave)) throw BackupInvalidoException("campo \"$chave\" ausente.")
        return get(chave)
    }

    private fun JSONObject.longo(chave: String): Long = when (val v = obrig(chave)) {
        is Int -> v.toLong()
        is Long -> v
        else -> throw BackupInvalidoException("campo \"$chave\" não é um número inteiro.")
    }

    private fun JSONObject.longoOpc(chave: String): Long? = if (ausente(chave)) null else longo(chave)

    private fun JSONObject.inteiro(chave: String): Int {
        val v = longo(chave)
        if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) throw BackupInvalidoException("campo \"$chave\" fora do limite.")
        return v.toInt()
    }

    private fun JSONObject.inteiroOpc(chave: String): Int? = if (ausente(chave)) null else inteiro(chave)

    private fun JSONObject.inteiroNaoNegativo(chave: String): Int =
        inteiro(chave).also { if (it < 0) throw BackupInvalidoException("campo \"$chave\" não pode ser negativo.") }

    private fun JSONObject.idPositivo(): Long =
        longo("id").also { if (it <= 0) throw BackupInvalidoException("campo \"id\" inválido.") }

    private fun JSONObject.centavos(chave: String): Long =
        longo(chave).also { if (it < 0) throw BackupInvalidoException("campo \"$chave\" não pode ser negativo.") }

    private fun JSONObject.duploNaoNegativo(chave: String): Double {
        val v = when (val bruto = obrig(chave)) {
            is Number -> bruto.toDouble()
            else -> throw BackupInvalidoException("campo \"$chave\" não é um número.")
        }
        if (v.isNaN() || v.isInfinite() || v < 0) throw BackupInvalidoException("campo \"$chave\" inválido.")
        return v
    }

    private fun JSONObject.booleano(chave: String): Boolean = when (val v = obrig(chave)) {
        is Boolean -> v
        else -> throw BackupInvalidoException("campo \"$chave\" não é verdadeiro/falso.")
    }

    private fun JSONObject.booleanoOpc(chave: String): Boolean? = if (ausente(chave)) null else booleano(chave)

    private fun JSONObject.texto(chave: String): String = when (val v = obrig(chave)) {
        is String -> v
        else -> throw BackupInvalidoException("campo \"$chave\" não é texto.")
    }

    private fun JSONObject.textoOpc(chave: String): String? = if (ausente(chave)) null else texto(chave)

    private fun JSONObject.data(chave: String): LocalDate = try {
        LocalDate.parse(texto(chave))
    } catch (e: DateTimeParseException) {
        throw BackupInvalidoException("campo \"$chave\" não é uma data válida.")
    }

    private inline fun <reified E : Enum<E>> JSONObject.enumerado(chave: String): E = try {
        enumValueOf<E>(texto(chave))
    } catch (e: IllegalArgumentException) {
        throw BackupInvalidoException("campo \"$chave\" tem um valor desconhecido.")
    }
}
