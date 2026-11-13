package com.ilactakip.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ilactakip.app.IlacTakipApp
import com.ilactakip.app.alarm.AlarmScheduler
import com.ilactakip.app.data.model.Medication
import com.ilactakip.app.data.model.MedicationLog
import com.ilactakip.app.ui.theme.IlacTakipTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Bildirim izni verildi.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        val app = application as IlacTakipApp
        val dao = app.database.medicationDao()
        val scheduler = AlarmScheduler(this)

        setContent {
            IlacTakipTheme {
                val medications by dao.getAllMedications().collectAsState(initial = emptyList())
                val logs by dao.getAllLogs().collectAsState(initial = emptyList())

                val todayDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                val todayTakenLogs by dao.getTodayTakenLogs(todayDate).collectAsState(initial = emptyList())

                MainScreen(
                    medications = medications,
                    logs = logs,
                    todayTakenLogs = todayTakenLogs,
                    onManualTake = { med ->
                        val now = System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val log = MedicationLog(
                            medicationId = med.id,
                            medicationName = med.name,
                            scheduledTime = String.format("%02d:%02d", med.hour, med.minute),
                            takenTimestamp = now,
                            formattedDate = dateFormat.format(Date(now)),
                            formattedTime = timeFormat.format(Date(now)),
                            action = "TAKEN"
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            dao.insertLog(log)
                        }
                        Toast.makeText(this, "${med.name} alındı olarak kaydedildi.", Toast.LENGTH_SHORT).show()
                    },
                    onTestAlarm = { med ->
                        scheduler.scheduleTestAlarm(med.id, med.name, delaySeconds = 5)
                        Toast.makeText(this, "5 saniye sonra ${med.name} araması çalacak!", Toast.LENGTH_LONG).show()
                    },
                    onRequestBatteryOptimization = { requestIgnoreBatteryOptimization() },
                    onRequestExactAlarmPermission = { requestExactAlarmSettings() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Pil kısıtlaması zaten kapalı (Sınırsız).", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheduler = AlarmScheduler(this)
            if (!scheduler.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tam saatinde alarm izni zaten açık.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainScreen(
    medications: List<Medication>,
    logs: List<MedicationLog>,
    todayTakenLogs: List<MedicationLog>,
    onManualTake: (Medication) -> Unit,
    onTestAlarm: (Medication) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val todayDateFormatted = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr")).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Üst Başlık
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(20.dp)
        ) {
            Text(
                text = "İlaç Takip & Hatırlatıcı",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = todayDateFormatted,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }

        // Sekmeler (Bugünkü İlaçlar / Geçmiş Loglar)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Bugünkü İlaçlar", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Medication, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Alım Geçmişi", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            TodayMedicationsTab(
                medications = medications,
                todayTakenLogs = todayTakenLogs,
                onManualTake = onManualTake,
                onTestAlarm = onTestAlarm,
                onRequestBatteryOptimization = onRequestBatteryOptimization,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission
            )
        } else {
            HistoryTab(logs = logs)
        }
    }
}

@Composable
fun TodayMedicationsTab(
    medications: List<Medication>,
    todayTakenLogs: List<MedicationLog>,
    onManualTake: (Medication) -> Unit,
    onTestAlarm: (Medication) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // İzin & Sağlık Kartı
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kesintisiz Çalması İçin İzinler",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Telefon kilitliyken veya uykudayken arama gibi çalabilmesi için pil kısıtlamasını kapatmalısınız.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestBatteryOptimization,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pil İzni", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onRequestExactAlarmPermission,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tam Alarm", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // İlaç Kartları
        items(medications) { med ->
            val takenLog = todayTakenLogs.find { it.medicationId == med.id }
            val isTaken = takenLog != null

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isTaken) Color(0xFF064E3B) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(med.colorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format("%02d", med.hour),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = med.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Saat " + String.format("%02d:%02d", med.hour, med.minute),
                                    color = Color(0xFF38BDF8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Durum Rozeti
                        if (isTaken) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Alındı (${takenLog?.formattedTime})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF475569), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Bekliyor",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Talimat Açıklaması (Aç / Tok)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📌 ${med.instruction}",
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isTaken) {
                            Button(
                                onClick = { onManualTake(med) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aldım", fontSize = 13.sp)
                            }
                        }

                        // Test Arama Butonu
                        Button(
                            onClick = { onTestAlarm(med) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aramayı Dene", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(logs: List<MedicationLog>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Henüz bir alım kaydı bulunmuyor.",
                color = Color(0xFF94A3B8),
                fontSize = 15.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                val isTaken = log.action == "TAKEN"
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = log.medicationName,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tarih: ${log.formattedDate} • ${log.formattedTime}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isTaken) Color(0xFF10B981) else Color(0xFFD97706),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isTaken) "KULLANILDI" else "ERTELENDİ (5 dk)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
