package com.ti3042.airmonitor.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ti3042.airmonitor.data.database.converters.DateConverter
import java.util.*

@Entity(tableName = "sensor_readings")
@TypeConverters(DateConverter::class)
data class SensorReading(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Timestamp
    val timestamp: Date = Date(),
    val deviceId: String,
    val location: String? = null,
    
    // Gas readings (en las unidades apropiadas)
    val oxygen: Float,           // %
    val co2: Float,             // ppm  
    val co: Float,              // ppm
    val ammonia: Float,         // ppm
    val nox: Float,             // ppm
    val vapor: Float,           // %
    val smoke: Float,           // %
    val toluene: Float,         // %
    
    // Environmental data
    val temperature: Float,      // °C
    val humidity: Float,         // %
    val pressure: Float? = null, // hPa
    
    // Calculated values
    val totalPpm: Int,
    val airQualityLevel: String, // "good", "moderate", "unhealthy"
    val overallStatus: String,   // "normal", "warning", "critical"
    
    // Metadata
    val isSimulated: Boolean = false,
    val calibrationVersion: String = "1.0",
    val dataSource: String = "MQ135", // "MQ135", "Simulation", "Manual"
    
    // Firebase sync
    val isUploaded: Boolean = false,
    val lastSyncAttempt: Date? = null,
    val syncError: String? = null
)

// Data class for Firebase (sin Room annotations)
data class FirebaseSensorReading(
    val id: String = "",
    val timestamp: Long = 0L,
    val deviceId: String = "",
    val location: String? = null,
    
    val oxygen: Float = 0f,
    val co2: Float = 0f,
    val co: Float = 0f,
    val ammonia: Float = 0f,
    val nox: Float = 0f,
    val vapor: Float = 0f,
    val smoke: Float = 0f,
    val toluene: Float = 0f,
    
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val pressure: Float? = null,
    
    val totalPpm: Int = 0,
    val airQualityLevel: String = "",
    val overallStatus: String = "",
    
    val isSimulated: Boolean = false,
    val calibrationVersion: String = "1.0",
    val dataSource: String = "MQ135",
    
    val userId: String = "" // Para asociar con usuario de Firebase Auth
)

// Extension functions para convertir entre Room y Firebase
fun SensorReading.toFirebase(userId: String): FirebaseSensorReading {
    return FirebaseSensorReading(
        id = this.id,
        timestamp = this.timestamp.time,
        deviceId = this.deviceId,
        location = this.location,
        oxygen = this.oxygen,
        co2 = this.co2,
        co = this.co,
        ammonia = this.ammonia,
        nox = this.nox,
        vapor = this.vapor,
        smoke = this.smoke,
        toluene = this.toluene,
        temperature = this.temperature,
        humidity = this.humidity,
        pressure = this.pressure,
        totalPpm = this.totalPpm,
        airQualityLevel = this.airQualityLevel,
        overallStatus = this.overallStatus,
        isSimulated = this.isSimulated,
        calibrationVersion = this.calibrationVersion,
        dataSource = this.dataSource,
        userId = userId
    )
}

fun FirebaseSensorReading.toRoom(): SensorReading {
    return SensorReading(
        id = this.id,
        timestamp = Date(this.timestamp),
        deviceId = this.deviceId,
        location = this.location,
        oxygen = this.oxygen,
        co2 = this.co2,
        co = this.co,
        ammonia = this.ammonia,
        nox = this.nox,
        vapor = this.vapor,
        smoke = this.smoke,
        toluene = this.toluene,
        temperature = this.temperature,
        humidity = this.humidity,
        pressure = this.pressure,
        totalPpm = this.totalPpm,
        airQualityLevel = this.airQualityLevel,
        overallStatus = this.overallStatus,
        isSimulated = this.isSimulated,
        calibrationVersion = this.calibrationVersion,
        dataSource = this.dataSource,
        isUploaded = true // Viene de Firebase, ya está sincronizado
    )
}