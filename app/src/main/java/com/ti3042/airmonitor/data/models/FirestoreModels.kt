package com.ti3042.airmonitor.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de datos para almacenar lecturas de sensores en Firestore
 */
data class SensorReading(
    @DocumentId
    var id: String = "",
    
    // Datos del sensor
    val ppm: Int = 0,
    val airQualityLevel: String = "",
    val temperature: Float = 0.0f,
    val humidity: Int = 0,
    
    // Composición de gases
    val gasComposition: Map<String, Float> = mapOf(),
    
    // Información del dispositivo
    val deviceId: String = "",
    val userId: String = "",
    
    // Ubicación (opcional)
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Timestamp automático de Firestore
    @ServerTimestamp
    val timestamp: Date? = null,
    
    // Estados del sistema
    val systemStatus: SystemStatusData? = null
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        id = "",
        ppm = 0,
        airQualityLevel = "",
        temperature = 0.0f,
        humidity = 0,
        gasComposition = mapOf(),
        deviceId = "",
        userId = "",
        location = null,
        latitude = null,
        longitude = null,
        timestamp = null,
        systemStatus = null
    )
}

/**
 * Estado del sistema para almacenar en Firestore
 */
data class SystemStatusData(
    val fanStatus: Boolean = false,
    val buzzerActive: Boolean = false,
    val autoMode: Boolean = true,
    val uptime: Long = 0L,
    val batteryLevel: Int = 100,
    val wifiSignal: Int = 0,
    val bluetoothConnected: Boolean = false
) {
    constructor() : this(
        fanStatus = false,
        buzzerActive = false,
        autoMode = true,
        uptime = 0L,
        batteryLevel = 100,
        wifiSignal = 0,
        bluetoothConnected = false
    )
}

/**
 * Configuración de usuario
 */
data class UserSettings(
    @DocumentId
    var id: String = "",
    
    val userId: String = "",
    val email: String = "",
    
    // Configuraciones de alertas
    val alertThreshold: Int = 300,
    val notificationsEnabled: Boolean = true,
    val alertSound: Boolean = true,
    val vibration: Boolean = true,
    
    // Configuraciones de monitoreo
    val autoSaveInterval: Int = 60, // segundos
    val dataRetentionDays: Int = 30,
    
    // Configuraciones de dispositivo
    val deviceName: String = "AirMonitor",
    val location: String = "",
    
    @ServerTimestamp
    val lastModified: Date? = null
) {
    constructor() : this(
        id = "",
        userId = "",
        email = "",
        alertThreshold = 300,
        notificationsEnabled = true,
        alertSound = true,
        vibration = true,
        autoSaveInterval = 60,
        dataRetentionDays = 30,
        deviceName = "AirMonitor",
        location = "",
        lastModified = null
    )
}