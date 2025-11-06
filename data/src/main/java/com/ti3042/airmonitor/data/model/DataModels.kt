package com.ti3042.airmonitor.data.model

/**
 * 📊 Modelo de datos para lecturas de sensores de calidad de aire
 * Representa una lectura completa del sensor MQ-135
 * 
 * **Módulo**: :data
 * **Propósito**: Estructura de datos para sensores de calidad de aire
 */
data class SensorReading(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    
    // Gases principales
    val oxygenPercentage: Double = 0.0,       // O2 %
    val carbonDioxidePercentage: Double = 0.0, // CO2 %
    val carbonMonoxidePercentage: Double = 0.0, // CO %
    
    // Gases adicionales  
    val ammoniaPercentage: Double = 0.0,      // NH3 %
    val nitrogenOxidesPercentage: Double = 0.0, // NOx %
    val waterVaporPercentage: Double = 0.0,   // H2O %
    
    // Compuestos orgánicos volátiles
    val alcoholPercentage: Double = 0.0,      // Etanol %
    val toluenePercentage: Double = 0.0,      // Tolueno %
    val benzenePercentage: Double = 0.0,      // Benceno %
    val acetonePercentage: Double = 0.0,      // Acetona %
    
    // Datos ambientales
    val temperature: Double = 0.0,            // Temperatura °C
    val humidity: Double = 0.0,               // Humedad %
    val pressure: Double = 0.0,               // Presión atmosférica hPa
    
    // Metadatos
    val deviceId: String = "",
    val location: String = "",
    val qualityIndex: Int = 0,                // 0-500 (AQI)
    val alertLevel: AlertLevel = AlertLevel.NORMAL
)

/**
 * 🚨 Niveles de alerta para calidad de aire
 */
enum class AlertLevel {
    NORMAL,     // Verde: Calidad buena
    WARNING,    // Amarillo: Precaución
    CRITICAL    // Rojo: Peligroso
}

/**
 * 👤 Modelo de datos para perfil de usuario
 */
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val deviceIds: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val alertThresholds: AlertThresholds = AlertThresholds(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

/**
 * ⚙️ Configuración de umbrales de alerta personalizados
 */
data class AlertThresholds(
    val co2WarningPercentage: Double = 0.1,    // 0.1% CO2
    val co2CriticalPercentage: Double = 0.5,   // 0.5% CO2
    val coWarningPercentage: Double = 0.2,     // 0.2% CO
    val coCriticalPercentage: Double = 0.5,    // 0.5% CO
    val o2LowPercentage: Double = 16.0,        // 16% O2
    val o2CriticalPercentage: Double = 12.0    // 12% O2
)

/**
 * 📱 Modelo para configuración de dispositivos
 */
data class DeviceConfig(
    val id: String = "",
    val name: String = "",
    val type: String = "MQ-135",
    val location: String = "",
    val calibrationDate: Long = 0,
    val isActive: Boolean = true,
    val samplingInterval: Int = 30, // segundos
    val userId: String = ""
)