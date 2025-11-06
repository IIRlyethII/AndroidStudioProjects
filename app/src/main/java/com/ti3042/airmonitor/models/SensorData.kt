package com.ti3042.airmonitor.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para la información recibida del ESP32
 * Basado en el protocolo JSON definido en la documentación
 */
data class SensorData(
    @SerializedName("device")
    val device: String = "AirMonitor_TI3042",
    
    @SerializedName("version")
    val version: String = "1.0_SIM",
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("air_quality")
    val airQuality: AirQuality,
    
    @SerializedName("system")
    val systemStatus: SystemStatus,
    
    @SerializedName("thresholds")
    val thresholds: Thresholds
)

data class AirQuality(
    @SerializedName("ppm")
    val ppm: Int,
    
    @SerializedName("level")
    val level: String, // "good", "moderate", "poor", "critical"
    
    @SerializedName("temperature")
    val temperature: Float,
    
    @SerializedName("humidity")
    val humidity: Int
) {
    /**
     * Retorna el color asociado al nivel de calidad del aire
     */
    fun getLevelColor(): String = when (level.lowercase()) {
        "good" -> "#4CAF50"      // Verde
        "moderate" -> "#FF9800"   // Naranja  
        "poor" -> "#F44336"       // Rojo
        "critical" -> "#9C27B0"   // Púrpura
        else -> "#9E9E9E"         // Gris por defecto
    }
    
    /**
     * Determina el nivel basado en PPM
     */
    companion object {
        fun getLevelFromPPM(ppm: Int): String = when {
            ppm <= 150 -> "good"
            ppm <= 300 -> "moderate" 
            ppm <= 500 -> "poor"
            else -> "critical"
        }
    }
}

data class SystemStatus(
    @SerializedName("fan_status")
    val fanStatus: Boolean,
    
    @SerializedName("buzzer_active")
    val buzzerActive: Boolean,
    
    @SerializedName("auto_mode")
    val autoMode: Boolean,
    
    @SerializedName("uptime")
    val uptime: Long
) {
    /**
     * Formatea el uptime en formato legible
     */
    fun getFormattedUptime(): String {
        val seconds = uptime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}

data class Thresholds(
    @SerializedName("warning")
    val warning: Int = 200,
    
    @SerializedName("critical")
    val critical: Int = 400
)