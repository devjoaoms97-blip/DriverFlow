package br.com.motoristafinanceiro.notificacoes

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.motoristafinanceiro.MainActivity
import br.com.motoristafinanceiro.R

/** Mostra e remove a notificação de manutenção. É a única parte que mexe no sistema de notificações. */
class NotificadorAndroid(private val context: Context) {

    /** Devolve false (sem mostrar nada) quando o sistema não permite notificar. */
    @SuppressLint("MissingPermission") // a permissão é conferida em permitido()
    fun mostrar(texto: TextoNotificacao): Boolean {
        if (!permitido(context)) return false
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CANAL_MANUTENCAO, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Alertas de manutenção")
                .setDescription("Avisos de manutenção do carro próxima ou atrasada")
                .build()
        )
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificacao = NotificationCompat.Builder(context, CANAL_MANUTENCAO)
            .setSmallIcon(R.drawable.ic_stat_manutencao)
            .setContentTitle(texto.titulo)
            .setContentText(texto.resumo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto.detalhes))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        // Mesmo id sempre: a notificação nova substitui a anterior em vez de empilhar.
        manager.notify(ID_MANUTENCAO, notificacao)
        return true
    }

    fun cancelar() {
        NotificationManagerCompat.from(context).cancel(ID_MANUTENCAO)
    }

    companion object {
        const val CANAL_MANUTENCAO = "manutencao"
        private const val ID_MANUTENCAO = 1001

        /** Permissão (Android 13+) concedida e notificações do app não bloqueadas nas configurações. */
        fun permitido(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return false
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
