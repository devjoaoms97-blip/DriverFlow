package br.com.motoristafinanceiro.ui.comprovante

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.motoristafinanceiro.data.ProcessadorDeImagem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface EstadoImagem {
    data object Carregando : EstadoImagem
    data class Pronta(val imagem: ImageBitmap) : EstadoImagem
    data object Indisponivel : EstadoImagem
}

/** Mostra a foto do arquivo, lida já reduzida e fora da thread principal. */
@Composable
fun MiniaturaComprovante(
    arquivo: File,
    ladoMax: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val estado by produceState<EstadoImagem>(EstadoImagem.Carregando, arquivo.path, arquivo.lastModified()) {
        val bitmap = withContext(Dispatchers.IO) { ProcessadorDeImagem.decodificar(arquivo, ladoMax) }
        value = if (bitmap != null) EstadoImagem.Pronta(bitmap.asImageBitmap()) else EstadoImagem.Indisponivel
    }

    when (val atual = estado) {
        is EstadoImagem.Pronta -> Image(
            bitmap = atual.imagem,
            contentDescription = "Comprovante",
            modifier = modifier,
            contentScale = contentScale,
        )
        EstadoImagem.Carregando -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        EstadoImagem.Indisponivel -> Box(modifier, contentAlignment = Alignment.Center) {
            Text("Imagem indisponível")
        }
    }
}

/** Foto ampliada em uma janela. */
@Composable
fun ComprovanteDialog(arquivo: File, onFechar: () -> Unit) {
    Dialog(onDismissRequest = onFechar) {
        Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniaturaComprovante(
                    arquivo = arquivo,
                    ladoMax = 1_600,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 480.dp),
                    contentScale = ContentScale.Fit,
                )
                Button(onClick = onFechar, modifier = Modifier.fillMaxWidth()) { Text("Fechar") }
            }
        }
    }
}
