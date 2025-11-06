package com.ti3042.airmonitor.data.database.dao

import androidx.room.*
import com.ti3042.airmonitor.data.database.entities.DeviceConfiguration
import com.ti3042.airmonitor.data.database.entities.CalibrationRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface DeviceConfigurationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: DeviceConfiguration)
    
    @Update
    suspend fun update(config: DeviceConfiguration)
    
    @Query("SELECT * FROM device_configurations WHERE isActive = 1")
    fun getActiveConfigurations(): Flow<List<DeviceConfiguration>>
    
    @Query("SELECT * FROM device_configurations WHERE deviceId = :deviceId AND isActive = 1 LIMIT 1")
    suspend fun getConfigurationByDevice(deviceId: String): DeviceConfiguration?
    
    @Query("SELECT * FROM device_configurations WHERE deviceId = :deviceId AND isActive = 1 LIMIT 1")
    fun getConfigurationByDeviceFlow(deviceId: String): Flow<DeviceConfiguration?>
    
    @Query("UPDATE device_configurations SET isActive = 0 WHERE deviceId = :deviceId")
    suspend fun deactivateDevice(deviceId: String)
    
    @Query("DELETE FROM device_configurations WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)
}

@Dao
interface CalibrationRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CalibrationRecord)
    
    @Query("SELECT * FROM calibration_records WHERE deviceId = :deviceId ORDER BY calibrationDate DESC")
    fun getCalibrationHistory(deviceId: String): Flow<List<CalibrationRecord>>
    
    @Query("SELECT * FROM calibration_records WHERE deviceId = :deviceId AND isActive = 1 ORDER BY calibrationDate DESC LIMIT 1")
    suspend fun getLatestCalibration(deviceId: String): CalibrationRecord?
    
    @Query("SELECT COUNT(*) FROM calibration_records WHERE deviceId = :deviceId AND calibrationSuccess = 1")
    suspend fun getSuccessfulCalibrationsCount(deviceId: String): Int
    
    @Query("DELETE FROM calibration_records WHERE calibrationDate < :cutoffDate")
    suspend fun deleteOldRecords(cutoffDate: Date): Int
}