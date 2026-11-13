package com.ilactakip.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Int,
    val medicationName: String,
    val scheduledTime: String,      // "10:00", "19:00", "20:00"
    val takenTimestamp: Long,       // System.currentTimeMillis()
    val formattedDate: String,      // "13.11.2026"
    val formattedTime: String,      // "20:04"
    val action: String              // "TAKEN" (Kullanıldı), "SNOOZED" (Ertelendi)
)
