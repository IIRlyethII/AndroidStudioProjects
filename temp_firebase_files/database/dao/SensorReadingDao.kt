package com.ti3042.airmonitor.data.database.dao

import androidx.room.*
import com.ti3042.airmonitor.data.database.entities.SensorReading
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface SensorReadingDao {
    
    // Insertar nuevas lecturas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: SensorReading)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<SensorReading>)
    
    // Consultas básicas
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<SensorReading>>
    
    @Query("SELECT * FROM sensor_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getReadingsByDevice(deviceId: String): Flow<List<SensorReading>>
    
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int = 100): Flow<List<SensorReading>>
    
    // Consultas por rango de tiempo
    @Query("SELECT * FROM sensor_readings WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getReadingsBetween(startDate: Date, endDate: Date): Flow<List<SensorReading>>
    
    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getReadingsSince(since: Date): Flow<List<SensorReading>>
    
    // Consultas para dashboard
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReading(): SensorReading?
    
    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :today ORDER BY timestamp DESC")
    fun getTodaysReadings(today: Date): Flow<List<SensorReading>>
    
    // Consultas para análisis
    @Query("SELECT AVG(co2) as avgCO2, AVG(co) as avgCO, AVG(oxygen) as avgO2, AVG(temperature) as avgTemp FROM sensor_readings WHERE timestamp >= :since")
    suspend fun getAveragesSince(since: Date): AverageReadings?
    
    @Query("SELECT MAX(co2) as maxCO2, MAX(co) as maxCO, MIN(oxygen) as minO2 FROM sensor_readings WHERE timestamp >= :since")
    suspend fun getExtremesSince(since: Date): ExtremeReadings?
    
    // Consultas por estado
    @Query("SELECT * FROM sensor_readings WHERE overallStatus = :status ORDER BY timestamp DESC")
    fun getReadingsByStatus(status: String): Flow<List<SensorReading>>
    
    @Query("SELECT COUNT(*) FROM sensor_readings WHERE overallStatus = 'critical' AND timestamp >= :since")
    suspend fun getCriticalAlertsCount(since: Date): Int
    
    // Consultas para sync con Firebase
    @Query("SELECT * FROM sensor_readings WHERE isUploaded = 0 ORDER BY timestamp ASC")
    suspend fun getUnuploadedReadings(): List<SensorReading>
    
    @Query("UPDATE sensor_readings SET isUploaded = 1 WHERE id = :readingId")
    suspend fun markAsUploaded(readingId: String)
    
    @Query("UPDATE sensor_readings SET isUploaded = 1, lastSyncAttempt = :syncTime WHERE id IN (:readingIds)")
    suspend fun markMultipleAsUploaded(readingIds: List<String>, syncTime: Date)
    
    @Query("UPDATE sensor_readings SET syncError = :error, lastSyncAttempt = :syncTime WHERE id = :readingId")
    suspend fun markSyncError(readingId: String, error: String, syncTime: Date)
    
    // Limpieza de datos
    @Query("DELETE FROM sensor_readings WHERE timestamp < :cutoffDate")
    suspend fun deleteOldReadings(cutoffDate: Date): Int
    
    @Query("DELETE FROM sensor_readings WHERE isSimulated = 1 AND timestamp < :cutoffDate")
    suspend fun deleteOldSimulatedReadings(cutoffDate: Date): Int
    
    @Query("SELECT COUNT(*) FROM sensor_readings")
    suspend fun getReadingsCount(): Int
    
    // Borrar todo (para testing)
    @Query("DELETE FROM sensor_readings")
    suspend fun deleteAll()
}

// Data classes para consultas agregadas
data class AverageReadings(
    val avgCO2: Float,
    val avgCO: Float,
    val avgO2: Float,
    val avgTemp: Float
)

data class ExtremeReadings(
    val maxCO2: Float,
    val maxCO: Float,
    val minO2: Float
)