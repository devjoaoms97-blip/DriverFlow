package br.com.motoristafinanceiro.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import br.com.motoristafinanceiro.domain.LADO_MAXIMO_COMPROVANTE_PX
import br.com.motoristafinanceiro.domain.calcularInSampleSize
import br.com.motoristafinanceiro.domain.grausDeRotacao
import java.io.File
import java.io.FileOutputStream

/** Ajusta fotos de comprovante: reduz, aplica a rotação do EXIF e regrava como JPEG. */
object ProcessadorDeImagem {

    /** Reescreve o arquivo no lugar. Devolve false se não for uma imagem legível. */
    fun otimizar(arquivo: File, ladoMax: Int = LADO_MAXIMO_COMPROVANTE_PX, qualidade: Int = 85): Boolean {
        return try {
            val caminho = arquivo.absolutePath

            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(caminho, limites)
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return false

            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = calcularInSampleSize(limites.outWidth, limites.outHeight, ladoMax)
            }
            val original = BitmapFactory.decodeFile(caminho, opcoes) ?: return false

            // A orientação precisa ser lida ANTES de regravar o arquivo.
            val graus = grausDeRotacao(
                ExifInterface(caminho).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            )
            val final = if (graus != 0) {
                val matriz = Matrix().apply { postRotate(graus.toFloat()) }
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
            } else {
                original
            }

            FileOutputStream(arquivo).use { final.compress(Bitmap.CompressFormat.JPEG, qualidade, it) }
            if (final !== original) original.recycle()
            final.recycle()
            true
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }

    /** Lê a imagem já reduzida para caber na tela; null se o arquivo não existir ou for inválido. */
    fun decodificar(arquivo: File, ladoMax: Int): Bitmap? = try {
        val caminho = arquivo.absolutePath
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(caminho, limites)
        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = calcularInSampleSize(limites.outWidth, limites.outHeight, ladoMax)
        }
        BitmapFactory.decodeFile(caminho, opcoes)
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
