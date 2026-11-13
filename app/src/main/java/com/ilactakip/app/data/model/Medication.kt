package com.ilactakip.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey val id: Int,
    val name: String,
    val hour: Int,
    val minute: Int,
    val instruction: String, // "Tok karna", "Aç karna", "Aç veya tok karna"
    val description: String,
    val colorHex: Long = 0xFF2563EB
)
