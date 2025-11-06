package com.ti3042.airmonitor.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ti3042.airmonitor.data.database.converters.DateConverter
import java.util.*

@Entity(tableName = "device_configurations")
@TypeConverters(DateConverter::class)
data class DeviceConfiguration(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // Device info
    val deviceId: String,
    val deviceName: String,
    val deviceType: String = "MQ135", // MQ135, Simulation, etc.
    
    // Calibration settings
    val calibrationDate: Date,
    val calibrationVersion: String = "1.0",
    val calibrationStatus: String = "active", // active, expired, pending
    
    // Thresholds customization (JSON strings)
    val oxygenThresholds: String, // {"normal":[19,22],"warning":[16,19],"critical":[0,16]}
    val co2Thresholds: String,
    val coThresholds: String,
    val ammoniaThresholds: String,
    val noxThresholds: String,
    val vaporThresholds: String,
    val smokeThresholds: String,
    val tolueneThresholds: String,
    
    // Location settings
    val location: String? = null,
    val roomType: String? = null, // bedroom, kitchen, office, living_room
    val isIndoor: Boolean = true,
    
    // Alert settings
    val alertsEnabled: Boolean = true,
    val criticalAlertsEnabled: Boolean = true,
    val alertCooldownMinutes: Int = 15,
    val emailNotifications: Boolean = false,
    val pushNotifications: Boolean = true,
    
    // Sync settings
    val autoSync: Boolean = true,
    val syncIntervalMinutes: Int = 30,
    val onlyWifi: Boolean = false,
    
    // Metadata
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isActive: Boolean = true,
    val userId: String? = null
)

@Entity(tableName = "calibration_records")
@TypeConverters(DateConverter::class)
data class CalibrationRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val deviceId: String,
    val calibrationType: String, // "manual", "automatic", "factory_reset"
    val calibrationDate: Date = Date(),
    
    // Calibration values
    val referenceValues: String, // JSON con valores de referencia
    val measuredValues: String,  // JSON con valores medidos antes
    val adjustmentFactors: String, // JSON con factores de corrección aplicados
    
    // Environmental conditions during calibration
    val temperature: Float,
    val humidity: Float,
    val pressure: Float? = null,
    
    // Results
    val calibrationSuccess: Boolean,
    val accuracyImprovement: Float? = null, // % de mejora en precisión
    val notes: String? = null,
    
    // Metadata  
    val performedBy: String? = null, // user, system, technician
    val version: String = "1.0",
    val isActive: Boolean = true
)