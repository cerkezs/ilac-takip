package com.ilactakip.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ilactakip.app.data.model.Medication
import com.ilactakip.app.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY hour ASC, minute ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Int): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Insert
    suspend fun insertLog(log: MedicationLog): Long

    @Query("SELECT * FROM medication_logs ORDER BY takenTimestamp DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE formattedDate = :todayDate AND action = 'TAKEN'")
    fun getTodayTakenLogs(todayDate: String): Flow<List<MedicationLog>>
}
