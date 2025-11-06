package com.ti3042.airmonitor.data.database.repository

import com.ti3042.airmonitor.data.database.dao.SensorReadingDao
import com.ti3042.airmonitor.data.database.entities.SensorReading
import com.ti3042.airmonitor.data.models.GasReading
import com.ti3042.airmonitor.data.models.GasReadingStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorDataRepository @Inject constructor(
    private val sensorReadingDao: SensorReadingDao
) {
    
    // Insert sensor reading
    suspend fun insertReading(reading: SensorReading) {
        sensorReadingDao.insert(reading)
    }
    
    // Get all readings as Flow
    fun getAllReadings(): Flow<List<SensorReading>> {
        return sensorReadingDao.getAllReadings()
    }
    
    // Get latest reading
    suspend fun getLatestReading(): SensorReading? {
        return sensorReadingDao.getLatestReading()
    }
    
    // Get readings for specific time range
    fun getReadingsByTimeRange(startTime: Date, endTime: Date): Flow<List<SensorReading>> {
        return sensorReadingDao.getReadingsByTimeRange(startTime, endTime)
    }
    
    // Get readings for specific gas type
    fun getGasReadings(gasType: String): Flow<List<GasReading>> {
        return when (gasType.uppercase()) {
            "CO2" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.co2Ppm,
                        gasType = "CO2",
                        unit = "ppm"
                    )
                }
            }
            "CO" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.coPpm,
                        gasType = "CO",
                        unit = "ppm"
                    )
                }
            }
            "NH3" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.nh3Ppm,
                        gasType = "NH3",
                        unit = "ppm"
                    )
                }
            }
            "NOX" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.noxPpm,
                        gasType = "NOX",
                        unit = "ppm"
                    )
                }
            }
            "ALCOHOL" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.alcoholPpm,
                        gasType = "Alcohol",
                        unit = "ppm"
                    )
                }
            }
            "BENZENE" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.benzenePpm,
                        gasType = "Benceno",
                        unit = "ppm"
                    )
                }
            }
            "TOLUENE" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.toluenePpm,
                        gasType = "Tolueno",
                        unit = "ppm"
                    )
                }
            }
            "ACETONE" -> sensorReadingDao.getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.acetonePpm,
                        gasType = "Acetona",
                        unit = "ppm"
                    )
                }
            }
            else -> getAllReadings().map { readings ->
                readings.map { reading ->
                    GasReading(
                        timestamp = reading.timestamp,
                        value = reading.co2Ppm,
                        gasType = gasType,
                        unit = "ppm"
                    )
                }
            }
        }
    }
    
    // Get statistics for gas
    suspend fun getGasStatistics(gasType: String, startTime: Date, endTime: Date): GasReadingStatistics {
        val readings = sensorReadingDao.getReadingsByTimeRange(startTime, endTime)
        
        // Convert to GasReadingStatistics (implementation depends on your needs)
        return GasReadingStatistics(
            gasType = gasType,
            averageValue = 0.0,
            minValue = 0.0,
            maxValue = 0.0,
            totalReadings = 0,
            timeRange = Pair(startTime, endTime)
        )
    }
    
    // Get readings that need sync
    suspend fun getUnsyncedReadings(): List<SensorReading> {
        return sensorReadingDao.getUnsyncedReadings()
    }
    
    // Mark readings as synced
    suspend fun markAsSynced(readingIds: List<String>) {
        readingIds.forEach { id ->
            sensorReadingDao.markAsSynced(id)
        }
    }
    
    // Clean old data
    suspend fun cleanOldData(cutoffDate: Date): Int {
        return sensorReadingDao.deleteOldReadings(cutoffDate)
    }
    
    // Get data for export
    suspend fun getDataForExport(startTime: Date, endTime: Date): List<SensorReading> {
        return sensorReadingDao.getReadingsForExport(startTime, endTime)
    }
}