package br.com.motoristafinanceiro.domain

/** Regras puras dos comprovantes (sem Android), cobertas por testes. */

const val LADO_MAXIMO_COMPROVANTE_PX = 1_600

/** 24 horas: arquivos mais novos que isso nunca são apagados como órfãos (pode haver foto em andamento). */
const val IDADE_MINIMA_ORFAO_MS = 24L * 60 * 60 * 1000

/** Nome único de arquivo: comprovante_<instante>_<aleatório>.jpg */
fun novoNomeComprovante(agoraMs: Long, aleatorio: Int): String =
    "comprovante_${agoraMs}_${aleatorio.coerceAtLeast(0)}.jpg"

/**
 * Maior potência de 2 que ainda deixa o maior lado da imagem >= [ladoMax].
 * Serve para ler a foto já reduzida (economiza memória) sem ficar menor que o necessário.
 */
fun calcularInSampleSize(largura: Int, altura: Int, ladoMax: Int): Int {
    if (largura <= 0 || altura <= 0 || ladoMax <= 0) return 1
    var maior = maxOf(largura, altura)
    var amostra = 1
    while (maior / 2 >= ladoMax) {
        maior /= 2
        amostra *= 2
    }
    return amostra
}

/** Converte a orientação EXIF (6, 3, 8...) em graus para girar a imagem. */
fun grausDeRotacao(orientacaoExif: Int): Int = when (orientacaoExif) {
    6 -> 90
    3 -> 180
    8 -> 270
    else -> 0
}

data class InfoArquivo(val nome: String, val modificadoEmMs: Long)

/** Arquivos que nenhuma despesa usa e que já têm idade suficiente para serem apagados. */
fun arquivosOrfaos(
    arquivos: List<InfoArquivo>,
    emUso: Set<String>,
    agoraMs: Long,
    idadeMinimaMs: Long = IDADE_MINIMA_ORFAO_MS,
): List<String> = arquivos
    .filter { it.nome !in emUso && agoraMs - it.modificadoEmMs >= idadeMinimaMs }
    .map { it.nome }

/** Foto que deixou de ser usada ao salvar (trocada ou removida); null se nada a apagar. */
fun fotoSubstituidaOuRemovida(antiga: String?, nova: String?): String? =
    antiga?.takeIf { it != nova }
