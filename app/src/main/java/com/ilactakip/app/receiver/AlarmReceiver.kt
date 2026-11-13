package com.ilactakip.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.ilactakip.app.IlacTakipApp
import com.ilactakip.app.alarm.AlarmScheduler
import com.ilactakip.app.ui.IncomingCallActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, 1)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "İlaç Vakti"
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        // Gelen Arama Ekranı için Intent
        val callIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(IncomingCallActivity.EXTRA_IS_SNOOZE, isSnooze)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            medicationId,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "İlacı Aldım" aksiyonu (doğrudan bildirimden de basılabilir)
        val takenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = IncomingCallActivity.ACTION_TAKEN_FROM_NOTIFICATION
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_NAME, medicationName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val takenPendingIntent = PendingIntent.getActivity(
            context,
            2000 + medicationId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "5 Dk Ertele" aksiyonu
        val snoozeIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = IncomingCallActivity.ACTION_SNOOZE_FROM_NOTIFICATION
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IncomingCallActivity.EXTRA_MEDICATION_NAME, medicationName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            3000 + medicationId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notificationBuilder = NotificationCompat.Builder(context, IlacTakipApp.CHANNEL_CALL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("📞 İLAÇ VAKTİ: $medicationName")
            .setContentText("Lütfen ilacınızı alınız ve onaylayınız.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000, 1000))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.checkbox_on_background, "İlacı Aldım", takenPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "5 Dk Ertele", snoozePendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(medicationId, notificationBuilder.build())

        // Ekran kilitliyken veya açıksa Activity'yi doğrudan başlatmayı dene
        try {
            context.startActivity(callIntent)
        } catch (_: Exception) {
            // Android 10+ arka plan başlatma kısıtlamasında FullScreenIntent devreye girer
        }

        // Eğer bu normal bir günlük alarmi idiyse, yarın için de planla
        if (!isSnooze && medicationId in 1..3) {
            val scheduler = AlarmScheduler(context)
            when (medicationId) {
                1 -> scheduler.scheduleDailyAlarm(1, "Kardoritm", 10, 0)
                2 -> scheduler.scheduleDailyAlarm(2, "Ecoprin Pro", 19, 0)
                3 -> scheduler.scheduleDailyAlarm(3, "Plavix", 20, 0)
            }
        }
    }

    companion object {
        const val ACTION_MEDICATION_ALARM = "com.ilactakip.app.ACTION_MEDICATION_ALARM"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }
}
