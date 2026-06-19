package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

data class InspirationMessage(
    val id: Int,
    val text: String,
    val authorOrSource: String,
    val isBibleVerse: Boolean
)

object InspirationNotificationHelper {

    private const val CHANNEL_ID = "vencer_inspiration_channel"
    private const val CHANNEL_NAME = "Motivação & Fé"
    private const val CHANNEL_DESC = "Mensagens diárias com versículos bíblicos e frases motivacionais para vencer o vício."
    const val NOTIFICATION_ID = 4001

    val messages = listOf(
        InspirationMessage(1, "Não fui eu que ordenei? Seja forte e corajoso! Não se apavore nem desanime, pois o Senhor, o seu Deus, estará com você por onde você andar.", "Josué 1:9", true),
        InspirationMessage(2, "Tudo posso naquele que me fortalece.", "Filipenses 4:13", true),
        InspirationMessage(3, "Sujeitai-vos, pois, a Deus, resisti ao diabo, e ele fugirá de vós.", "Tiago 4:7", true),
        InspirationMessage(4, "Não sobreveio a vocês nenhuma tentação que não fosse comum aos homens. E Deus é fiel; ele não permitirá que vocês sejam tentados além do que podem suportar.", "1 Coríntios 10:13", true),
        InspirationMessage(5, "Mas os que esperam no Senhor renovarão as suas forças; subirão com asas como águias; correrão, e não se cansarão; caminharão, e não se fatigarão.", "Isaías 40:31", true),
        InspirationMessage(6, "Vigiai e orai, para que não entreis em tentação; na verdade, o espírito está pronto, mas a carne é fraca.", "Mateus 26:41", true),
        InspirationMessage(7, "Guardo a tua palavra no meu coração para não pecar contra ti.", "Salmo 119:11", true),
        InspirationMessage(8, "Seja forte! Cada impulso que você resiste hoje reconstrói a sua mente de forma permanente. Viva livre!", "Vencer", false),
        InspirationMessage(9, "Não troque uma vida inteira de paz, dignidade e inteligência por 15 segundos de um prazer falso e vazio.", "Vencer", false),
        InspirationMessage(10, "A sua mente é o seu maior castelo. Quando você a vence, nenhuma fraqueza exterior consegue derrotá-lo.", "Vencer", false),
        InspirationMessage(11, "A dor da autodisciplina é temporária. O orgulho e a paz de ser livre das amarras duram toda a eternidade.", "Vencer", false),
        InspirationMessage(12, "Cada dia sem pornografia restabelece os seus níveis de dopamina, o seu brilho e a sua masculinidade divina.", "Vencer", false),
        InspirationMessage(13, "Fugi da impureza. O vosso corpo é santuário do Espírito Santo, glorificai pois a Deus no vosso corpo.", "1 Coríntios 6:18", true),
        InspirationMessage(14, "O pecado não terá domínio sobre vós, pois não estais debaixo da lei, mas debaixo da graça.", "Romanos 6:14", true),
        InspirationMessage(15, "Escreva a sua fraqueza na areia para que o vento a apague, e grave a sua força na pedra para que dure para sempre.", "Vencer", false),
        InspirationMessage(16, "Antes de ir dormir, ponha o telemóvel longe da cama. Defenda o seu santuário de paz e garanta o seu amanhã vitorioso.", "Vencer", false)
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFF10B981.toInt() // Emerald Green
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getRandomMessage(): InspirationMessage {
        return messages.random()
    }

    fun showInspirationNotification(context: Context, customMessage: InspirationMessage? = null) {
        createNotificationChannel(context)
        val message = customMessage ?: getRandomMessage()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (message.isBibleVerse) "🕊️ Bíblia Sagrada (${message.authorOrSource})" else "🛡️ Força & Motivação"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System standard info icon
            .setContentTitle(title)
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Note: Since POST_NOTIFICATIONS is requested at runtime on API 33+, 
            // of course the app checks it before sending.
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun scheduleDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, InspirationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set alarm calendar for daily morning encouragement (e.g., 08:00 AM)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, InspirationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun showParentAlertNotification(context: Context, details: String) {
        val channelId = "parent_alerts_channel"
        val channelName = "Alertas de Controlo Parental 🚨"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Alertas enviados aos pais quando conteúdo impróprio (+18) é detectado."
                enableLights(true)
                lightColor = 0xFFEF4444.toInt() // Red
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Alerta Parental: Conteúdo Impróprio!")
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(5002, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
