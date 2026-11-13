package com.ilactakip.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilactakip.app.data.model.Medication
import com.ilactakip.app.data.model.MedicationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Medication::class, MedicationLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medication_tracker_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate()
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialMedications(database.medicationDao())
                    }
                }
            }
        }

        suspend fun populateInitialMedications(dao: MedicationDao) {
            val initialList = listOf(
                Medication(
                    id = 1,
                    name = "Kardoritm",
                    hour = 10,
                    minute = 0,
                    instruction = "Aç veya tok karna",
                    description = "Sabah kardiyak ritim dozu",
                    colorHex = 0xFF0284C7 // Canlı Mavi
                ),
                Medication(
                    id = 2,
                    name = "Ecoprin Pro",
                    hour = 19,
                    minute = 0,
                    instruction = "Aç karna",
                    description = "Akşam aç karnına kan sulandırıcı dozu",
                    colorHex = 0xFFEA580C // Canlı Turuncu
                ),
                Medication(
                    id = 3,
                    name = "Plavix",
                    hour = 20,
                    minute = 0,
                    instruction = "Tok karna",
                    description = "Akşam yemek sonrası tok karnına dozu",
                    colorHex = 0xFF7C3AED // Mor
                )
            )
            dao.insertMedications(initialList)
        }
    }
}
