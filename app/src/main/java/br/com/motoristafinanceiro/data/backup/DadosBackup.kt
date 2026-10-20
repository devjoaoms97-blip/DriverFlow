package br.com.motoristafinanceiro.data.backup

import br.com.motoristafinanceiro.data.model.Abastecimento
import br.com.motoristafinanceiro.data.model.Despesa
import br.com.motoristafinanceiro.data.model.Ganho
import br.com.motoristafinanceiro.data.model.Manutencao
import br.com.motoristafinanceiro.data.model.Meta
import br.com.motoristafinanceiro.data.model.ReservaDespesa
import java.time.LocalDate

/** Tudo o que o backup guarda, além das fotos: as seis tabelas e a depreciação por km (DataStore). */
data class DadosBackup(
    val ganhos: List<Ganho> = emptyList(),
    val despesas: List<Despesa> = emptyList(),
    val abastecimentos: List<Abastecimento> = emptyList(),
    val manutencoes: List<Manutencao> = emptyList(),
    val reservas: List<ReservaDespesa> = emptyList(),
    val metas: List<Meta> = emptyList(),
    val depreciacaoPorKmCentavos: Long = 0,
) {
    /** Nomes dos arquivos de comprovante usados pelas despesas. */
    val fotosReferenciadas: Set<String> get() = despesas.mapNotNull { it.fotoUri }.toSet()
}

/** Contagens para mostrar ao usuário depois de exportar ou restaurar. */
data class ResumoBackup(
    val ganhos: Int,
    val despesas: Int,
    val abastecimentos: Int,
    val manutencoes: Int,
    val reservas: Int,
    val metas: Int,
    val fotos: Int,
    /** Comprovantes que não estavam disponíveis (no aparelho, ao exportar; no arquivo, ao restaurar). */
    val fotosAusentes: Int,
)

fun DadosBackup.paraResumo(fotos: Int, fotosAusentes: Int) = ResumoBackup(
    ganhos = ganhos.size,
    despesas = despesas.size,
    abastecimentos = abastecimentos.size,
    manutencoes = manutencoes.size,
    reservas = reservas.size,
    metas = metas.size,
    fotos = fotos,
    fotosAusentes = fotosAusentes,
)

/** O arquivo não é um backup válido (ou está corrompido). A mensagem é própria para mostrar ao usuário. */
class BackupInvalidoException(mensagem: String) : Exception(mensagem)

/** Nome simples de arquivo: sem pasta, sem barras e sem "..". Protege contra caminhos maliciosos no .zip. */
fun nomeDeFotoValido(nome: String): Boolean =
    nome.isNotBlank() && nome.length <= 255 && nome != "." && nome != ".." && '/' !in nome && '\\' !in nome

/**
 * Despesas cuja foto não veio no arquivo ficam sem foto (em vez de apontar para um arquivo que não existe).
 * Devolve os dados ajustados e quantas despesas perderam a foto.
 */
fun reconciliarFotos(dados: DadosBackup, nomesNoArquivo: Set<String>): Pair<DadosBackup, Int> {
    var ausentes = 0
    val despesas = dados.despesas.map { d ->
        val foto = d.fotoUri
        if (foto != null && foto !in nomesNoArquivo) {
            ausentes++
            d.copy(fotoUri = null)
        } else {
            d
        }
    }
    return dados.copy(despesas = despesas) to ausentes
}

/** "motorista-financeiro-backup-2026-10-20.zip" */
fun nomeArquivoBackup(data: LocalDate): String = "motorista-financeiro-backup-$data.zip"
