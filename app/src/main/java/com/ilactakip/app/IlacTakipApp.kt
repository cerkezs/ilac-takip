package com.ilactakip.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.ilactakip.app.alarm.AlarmScheduler
import com.ilactakip.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IlacTakipApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // İlk açılışta alarmları planla
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = AlarmScheduler(this@IlacTakipApp)
            scheduler.scheduleAllDailyAlarms()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Arama/Alarm tarzı yüksek öncelikli bildirim kanalı
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_CALL_ID,
                "İlaç Arama Hatırlatıcı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "İlaç saati geldiğinde arama gibi tam ekran çalan bildirimler"
                setSound(ringtoneUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000, 1000, 1000, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            notificationManager.createNotificationChannel(callChannel)
        }
    }

    companion object {
        const val CHANNEL_CALL_ID = "channel_medication_incoming_call"
    }
}
