package com.ilactakip.app.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilactakip.app.IlacTakipApp
import com.ilactakip.app.alarm.AlarmScheduler
import com.ilactakip.app.data.model.MedicationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncomingCallActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoSnoozeTimer: CountDownTimer? = null

    private var medicationId: Int = 1
    private var medicationName: String = "İlaç Vakti"
    private var isSnooze: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeUpAndUnlock()

        medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, 1)
        medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: getFallbackName(medicationId)
        isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        // Bildirim üzerinden gelen doğrudan aksiyon kontrolü
        when (intent.action) {
            ACTION_TAKEN_FROM_NOTIFICATION -> {
                handleMedicationTaken()
                return
            }
            ACTION_SNOOZE_FROM_NOTIFICATION -> {
                handleMedicationSnooze()
                return
            }
        }

        startRingtoneAndVibration()
        startAutoSnoozeCountdown()

        val instruction = when (medicationId) {
            1 -> "Sabah dozu • Aç veya tok karna"
            2 -> "Akşam dozu • Aç karna"
            3 -> "Akşam dozu • Tok karna"
            else -> "Lütfen zamanında alınız"
        }

        setContent {
            IncomingCallScreen(
                medicationName = medicationName,
                instruction = instruction,
                isSnooze = isSnooze,
                onTakenClicked = { handleMedicationTaken() },
                onSnoozeClicked = { handleMedicationSnooze() }
            )
        }
    }

    private fun getFallbackName(id: Int): String = when (id) {
        1 -> "Kardoritm"
        2 -> "Ecoprin Pro"
        3 -> "Plavix"
        else -> "İlaç Vakti"
    }

    private fun wakeUpAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun startRingtoneAndVibration() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@IncomingCallActivity, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Ses çalınamazsa sadece titreşime devam et
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 1000, 1000, 1000, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopRingtoneAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}

        autoSnoozeTimer?.cancel()
        autoSnoozeTimer = null

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicationId)
    }

    /**
     * 60 saniye boyunca butona basılmazsa otomatik olarak 5 dakika ertele
     */
    private fun startAutoSnoozeCountdown() {
        autoSnoozeTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                handleMedicationSnooze(isAuto = true)
            }
        }.start()
    }

    private fun handleMedicationTaken() {
        stopRingtoneAndVibration()

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val scheduledTime = when (medicationId) {
            1 -> "10:00"
            2 -> "19:00"
            3 -> "20:00"
            else -> timeFormat.format(Date(now))
        }

        val log = MedicationLog(
            medicationId = medicationId,
            medicationName = medicationName,
            scheduledTime = scheduledTime,
            takenTimestamp = now,
            formattedDate = dateFormat.format(Date(now)),
            formattedTime = timeFormat.format(Date(now)),
            action = "TAKEN"
        )

        val app = application as IlacTakipApp
        CoroutineScope(Dispatchers.IO).launch {
            app.database.medicationDao().insertLog(log)
        }

        Toast.makeText(this, "✅ $medicationName alındı olarak kaydedildi!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun handleMedicationSnooze(isAuto: Boolean = false) {
        stopRingtoneAndVibration()

        val scheduler = AlarmScheduler(this)
        scheduler.scheduleSnoozeAlarm(medicationId, medicationName, snoozeMinutes = 5)

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val log = MedicationLog(
            medicationId = medicationId,
            medicationName = medicationName,
            scheduledTime = timeFormat.format(Date(now)),
            takenTimestamp = now,
            formattedDate = dateFormat.format(Date(now)),
            formattedTime = timeFormat.format(Date(now)),
            action = if (isAuto) "AUTO_SNOOZED" else "SNOOZED"
        )

        val app = application as IlacTakipApp
        CoroutineScope(Dispatchers.IO).launch {
            app.database.medicationDao().insertLog(log)
        }

        val msg = if (isAuto) {
            "⏰ $medicationName yanıtlanmadı, 5 dakika sonra tekrar çalacak."
        } else {
            "⏰ $medicationName 5 dakika ertelendi!"
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        const val ACTION_TAKEN_FROM_NOTIFICATION = "com.ilactakip.app.ACTION_TAKEN_NOTIFICATION"
        const val ACTION_SNOOZE_FROM_NOTIFICATION = "com.ilactakip.app.ACTION_SNOOZE_NOTIFICATION"
    }
}

@Composable
fun IncomingCallScreen(
    medicationName: String,
    instruction: String,
    isSnooze: Boolean,
    onTakenClicked: () -> Unit,
    onSnoozeClicked: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF020617)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Başlık & Arama Bilgisi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isSnooze) "⏰ ERTELENEN İLAÇ HATIRLATMASI" else "📞 GELEN İLAÇ ARAMASI",
                    color = if (isSnooze) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = medicationName,
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = instruction,
                        color = Color(0xFFE2E8F0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Orta Kısım - Titreşen İlaç/Arama İkonu
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .border(3.dp, Color(0xFF10B981).copy(alpha = 0.4f), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Zil",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Alt Kısım - Butonlar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // İlacı Kullandım Butonu (Büyük Yeşil)
                Button(
                    onClick = onTakenClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Kullandım",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "İlACı KULLANDIM",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Dk Ertele Butonu (Sarı/Turuncu)
                Button(
                    onClick = onSnoozeClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Ertele",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "5 Dakika Ertele",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
