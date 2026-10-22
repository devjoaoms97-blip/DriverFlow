package br.com.motoristafinanceiro.ui.ajustes

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.motoristafinanceiro.domain.LimitesCorrida
import br.com.motoristafinanceiro.leitor.GravadorDeTela
import br.com.motoristafinanceiro.leitor.LeitorDeCorridasService
import br.com.motoristafinanceiro.leitor.limitesDosCampos
import br.com.motoristafinanceiro.leitor.servicoLigado
import br.com.motoristafinanceiro.leitor.textoTamanhoGravacao
import br.com.motoristafinanceiro.ui.componentes.BotaoPrincipal
import br.com.motoristafinanceiro.ui.componentes.CampoCompacto
import br.com.motoristafinanceiro.ui.componentes.LinhaChave
import br.com.motoristafinanceiro.ui.componentes.TextoDeApoio
import br.com.motoristafinanceiro.util.paraTextoReais
import java.io.File

private fun leitorLigado(contexto: Context): Boolean {
    val componente = ComponentName(contexto, LeitorDeCorridasService::class.java)
    val lista = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return servicoLigado(lista, componente.flattenToString(), componente.flattenToShortString())
}

/** Leitor de corridas: estado do serviço de acessibilidade, faixas de cor do R$/km e o modo gravação. */
@Composable
fun SecaoLeitorCorridas(vm: AjustesViewModel) {
    val contexto = LocalContext.current
    var ligado by remember { mutableStateOf(leitorLigado(contexto)) }
    val gravador = remember(contexto) { GravadorDeTela(File(contexto.filesDir, GravadorDeTela.PASTA)) }
    var tamanho by remember { mutableLongStateOf(gravador.tamanhoBytes()) }
    val gravando by vm.leitorGravando.collectAsStateWithLifecycle(initialValue = false)
    val limites by vm.limitesCorrida.collectAsStateWithLifecycle(initialValue = null)

    // Ao voltar das configurações de acessibilidade (ou da Uber/99, depois de gravar), confere de novo.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ligado = leitorLigado(contexto)
        tamanho = gravador.tamanhoBytes()
    }

    SecaoAjustes("Leitor de corridas") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(10.dp).background(
                    if (ligado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
            )
            Text(
                if (ligado) "Ligado" else "Desligado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Quando a Uber Driver ou a 99 Motorista mostram uma oferta, aparece no alto da tela o R$/km contando o " +
                "deslocamento até o passageiro, o R$/h e o lucro estimado pelo seu custo por km dos últimos 30 dias, " +
                "numa cor pelas faixas abaixo. Só mostra a conta: nunca aceita nem recusa por você.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextoDeApoio(
            if (ligado) "Para desligar, use o mesmo caminho: Acessibilidade > DriverFlow - leitor de corridas."
            else "Para ligar: Acessibilidade > Apps instalados (ou Serviços baixados) > DriverFlow - leitor de corridas. " +
                "Se o Android disser que a opção é restrita, abra as informações do app DriverFlow, toque nos três " +
                "pontos e em \"Permitir configurações restritas\".",
        )
        TextButton(onClick = {
            contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text("Abrir acessibilidade do Android") }

        limites?.let { atuais -> key(atuais) { CamposLimites(atuais, vm::salvarLimitesCorrida) } }

        LinhaChave(
            "Modo gravação",
            "Guarda num arquivo o texto das telas da Uber/99 que têm R$ ou km, para ajustar a leitura ao card de " +
                "cada app. Pode guardar nome de passageiro: apague depois de enviar.",
            gravando,
            vm::definirLeitorGravando,
        )
        if (gravando || tamanho > 0) {
            TextoDeApoio(textoTamanhoGravacao(tamanho))
            if (tamanho > 0) {
                Row {
                    TextButton(onClick = { compartilharGravacao(contexto, gravador.arquivo) }) { Text("Compartilhar") }
                    TextButton(onClick = {
                        gravador.apagar()
                        tamanho = 0
                    }) { Text("Apagar gravação", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun CamposLimites(atuais: LimitesCorrida, salvar: (LimitesCorrida) -> Unit) {
    var bom by remember { mutableStateOf(atuais.bomPorKmCentavos.paraTextoReais()) }
    var minimo by remember { mutableStateOf(atuais.minimoPorKmCentavos.paraTextoReais()) }
    val novos = limitesDosCampos(bom, minimo)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoCompacto(
            "Verde a partir de (R$/km)", bom, { bom = it }, Modifier.weight(1f),
            dica = "2,00", teclado = KeyboardType.Decimal, erro = novos == null,
        )
        CampoCompacto(
            "Amarelo a partir de (R$/km)", minimo, { minimo = it }, Modifier.weight(1f),
            dica = "1,50", teclado = KeyboardType.Decimal, erro = novos == null,
        )
    }
    TextoDeApoio(
        if (novos == null) "Use valores como 2,00 e 1,50, com o amarelo menor ou igual ao verde."
        else "Abaixo do amarelo, a oferta fica vermelha.",
        erro = novos == null,
    )
    if (novos != null && novos != atuais) {
        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            BotaoPrincipal("Salvar faixas") { salvar(novos) }
        }
    }
}

private fun compartilharGravacao(contexto: Context, arquivo: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
        val enviar = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Gravação do leitor de corridas")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        contexto.startActivity(Intent.createChooser(enviar, "Compartilhar gravação"))
    }.onFailure { Toast.makeText(contexto, "Não foi possível compartilhar a gravação.", Toast.LENGTH_LONG).show() }
}
