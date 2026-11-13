package com.ilactakip.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.ilactakip.app.receiver.AlarmReceiver
import com.ilactakip.app.ui.MainActivity
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Tam saatinde günlük alarmı kurar (10:00, 19:00, 20:00)
     */
    fun scheduleDailyAlarm(medicationId: Int, medicationName: String, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Eğer bugün bu saat geçtiyse yarınki saate kur
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        setAlarmInternal(
            requestCode = medicationId,
            triggerAtMillis = calendar.timeInMillis,
            medicationId = medicationId,
            medicationName = medicationName,
            isSnooze = false
        )
    }

    /**
     * 5 dakikalık erteleme alarmı kurar
     */
    fun scheduleSnoozeAlarm(medicationId: Int, medicationName: String, snoozeMinutes: Int = 5) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        // Snooze için farklı requestCode aralığı (örneğin 1000 + id)
        setAlarmInternal(
            requestCode = 1000 + medicationId,
            triggerAtMillis = triggerAtMillis,
            medicationId = medicationId,
            medicationName = medicationName,
            isSnooze = true
        )
    }

    /**
     * Test amaçlı: Belirtilen saniye sonra alarmı çaldırır
     */
    fun scheduleTestAlarm(medicationId: Int, medicationName: String, delaySeconds: Int = 5) {
        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        setAlarmInternal(
            requestCode = 9999,
            triggerAtMillis = triggerAtMillis,
            medicationId = medicationId,
            medicationName = medicationName,
            isSnooze = false
        )
    }

    private fun setAlarmInternal(
        requestCode: Int,
        triggerAtMillis: Long,
        medicationId: Int,
        medicationName: String,
        isSnooze: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MEDICATION_ALARM
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(AlarmReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, isSnooze)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock Doze mode ve pil kısıtlamalarını aşar, en güvenilir tam alarmdır
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    /**
     * 3 temel ilacın günlük alarmlarını kurar
     */
    fun scheduleAllDailyAlarms() {
        scheduleDailyAlarm(1, "Kardoritm", 10, 0)
        scheduleDailyAlarm(2, "Ecoprin Pro", 19, 0)
        scheduleDailyAlarm(3, "Plavix", 20, 0)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
