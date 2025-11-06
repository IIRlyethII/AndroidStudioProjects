package com.ti3042.airmonitor.data.database

import androidx.room.*
import com.ti3042.airmonitor.data.database.dao.*
import com.ti3042.airmonitor.data.database.entities.*

@Database(
    entities = [
        SensorReading::class,
        DeviceConfiguration::class,
        CalibrationRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AirQualityDatabase : RoomDatabase() {
    
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun deviceConfigurationDao(): DeviceConfigurationDao
    abstract fun calibrationRecordDao(): CalibrationRecordDao
    
    companion object {
        const val DATABASE_NAME = "air_quality_database"
    }
}