package com.ti3042.airmonitor.domain.model

/**
 * ⚙️ CONFIGURACIÓN DE VALIDACIÓN DE USUARIOS
 * Cambiar según el ambiente de desarrollo/producción
 */
object UserValidationConfig {
    /**
     * 🚧 DESARROLLO: Email verification deshabilitada
     * 🔒 PRODUCCIÓN: Cambiar a true para requerir email verificado
     */
    const val REQUIRE_EMAIL_VERIFICATION = false
    
    /**
     * 📝 Información sobre el estado de validación
     */
    fun getValidationStatus(): String {
        return if (REQUIRE_EMAIL_VERIFICATION) {
            "🔒 Modo ESTRICTO: Email verification requerida"
        } else {
            "🚧 Modo DESARROLLO: Email verification deshabilitada"
        }
    }
}

/**
 * 👤 Modelo de dominio para Usuario
 * Representación limpia e independiente del framework
 * 
 * **Módulo**: :domain
 * **Propósito**: Entidad de negocio para usuarios del sistema
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String = "",
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Validar si el usuario está completo para usar la app
     * 
     * � Usa UserValidationConfig para determinar el nivel de validación
     */
    fun isValidForAppUsage(): Boolean {
        val baseValidation = id.isNotEmpty() && email.isNotEmpty()
        
        return if (UserValidationConfig.REQUIRE_EMAIL_VERIFICATION) {
            baseValidation && isEmailVerified
        } else {
            baseValidation
        }
    }
    
    /**
     * 🔒 Validación ESTRICTA con email verificado (para producción)
     */
    fun isValidForAppUsageStrict(): Boolean {
        return id.isNotEmpty() && 
               email.isNotEmpty() && 
               isEmailVerified
    }
    
    /**
     * ⚠️ Verificar si necesita verificar email
     */
    fun needsEmailVerification(): Boolean {
        return UserValidationConfig.REQUIRE_EMAIL_VERIFICATION && !isEmailVerified
    }
    
    /**
     * 📊 Obtener estado de validación para debug
     */
    fun getValidationStatus(): String {
        return buildString {
            append("Usuario: $email\n")
            append("ID válido: ${id.isNotEmpty()}\n")
            append("Email válido: ${email.isNotEmpty()}\n")
            append("Email verificado: $isEmailVerified\n")
            append("Configuración: ${UserValidationConfig.getValidationStatus()}\n")
            append("Puede usar app: ${isValidForAppUsage()}")
        }
    }
    
    /**
     * Obtener nombre para mostrar (fallback a email)
     */
    fun getDisplayNameOrEmail(): String {
        return displayName.ifEmpty { email }
    }
}

/**
 * 🌬️ Modelo de dominio para Calidad de Aire
 * Lógica de negocio para análisis de gases
 */
data class AirQuality(
    val timestamp: Long,
    val gasReadings: List<GasReading>,
    val environmentalData: EnvironmentalData,
    val deviceInfo: DeviceInfo
) {
    /**
     * Calcular índice de calidad de aire (AQI)
     */
    fun calculateAQI(): Double {
        val coReading = gasReadings.find { it.gasType == GasType.CO }
        val co2Reading = gasReadings.find { it.gasType == GasType.CO2 }
        val pm25Reading = gasReadings.find { it.gasType == GasType.PM25 }
        val pm10Reading = gasReadings.find { it.gasType == GasType.PM10 }
        
        val coLevel = coReading?.concentration ?: 0.0
        val co2Level = co2Reading?.concentration ?: 0.0
        val pm25Level = pm25Reading?.concentration ?: 0.0
        val pm10Level = pm10Reading?.concentration ?: 0.0
        
        return when {
            coLevel > 50 || co2Level > 10000 || pm25Level > 100 || pm10Level > 200 -> 301.0 // Hazardous
            coLevel > 30 || co2Level > 5000 || pm25Level > 50 || pm10Level > 100 -> 201.0 // Very Unhealthy
            coLevel > 15 || co2Level > 2000 || pm25Level > 35 || pm10Level > 75 -> 151.0 // Unhealthy
            coLevel > 10 || co2Level > 1000 || pm25Level > 15 || pm10Level > 50 -> 101.0 // Unhealthy for Sensitive
            else -> 50.0 // Good
        }
    }
    
    /**
     * Determinar nivel de alerta
     */
    fun getAlertLevel(): AlertLevel {
        val aqi = calculateAQI()
        return when {
            aqi > 300 -> AlertLevel.CRITICAL
            aqi > 150 -> AlertLevel.WARNING
            else -> AlertLevel.NORMAL
        }
    }
    
    /**
     * Obtener gases con niveles peligrosos
     */
    fun getDangerousGases(): List<GasReading> {
        return gasReadings.filter { it.isDangerous() }
    }
}

/**
 * 🧪 Tipos de gases que puede detectar el sensor
 */
enum class GasType(val symbol: String, val fullName: String) {
    CO2("CO₂", "Dióxido de Carbono"),
    CO("CO", "Monóxido de Carbono"),
    PM25("PM2.5", "Partículas finas"),
    PM10("PM10", "Partículas gruesas"),
    OXYGEN("O₂", "Oxígeno"),
    CARBON_DIOXIDE("CO₂", "Dióxido de Carbono"),
    CARBON_MONOXIDE("CO", "Monóxido de Carbono"),
    AMMONIA("NH₃", "Amoníaco"),
    NITROGEN_OXIDES("NOₓ", "Óxidos de Nitrógeno"),
    WATER_VAPOR("H₂O", "Vapor de Agua"),
    ALCOHOL("C₂H₅OH", "Etanol"),
    TOLUENE("C₇H₈", "Tolueno"),
    BENZENE("C₆H₆", "Benceno"),
    ACETONE("C₃H₆O", "Acetona"),
    UNKNOWN("?", "Desconocido")
}

/**
 * 📊 Lectura individual de un gas
 */
data class GasReading(
    val gasType: GasType,
    val concentration: Double, // Concentración en ppm o μg/m³
    val unit: String = "ppm",
    val percentage: Double = concentration / 10000.0, // Conversión aproximada
    val isCalibrated: Boolean = true
) {
    /**
     * Determinar si el nivel es peligroso
     */
    fun isDangerous(): Boolean {
        return when (gasType) {
            GasType.CO -> concentration > 50
            GasType.CO2 -> concentration > 10000
            GasType.PM25 -> concentration > 100
            GasType.PM10 -> concentration > 200
            GasType.CARBON_MONOXIDE -> percentage > 0.2
            GasType.CARBON_DIOXIDE -> percentage > 0.5
            GasType.OXYGEN -> percentage < 16.0 || percentage > 25.0
            GasType.AMMONIA -> percentage > 1.5
            GasType.BENZENE -> percentage > 0.2
            else -> false
        }
    }
    
    /**
     * Obtener nivel de alerta para este gas
     */
    fun getAlertLevel(): AlertLevel {
        return when {
            isDangerous() -> AlertLevel.CRITICAL
            isAtWarningLevel() -> AlertLevel.WARNING
            else -> AlertLevel.NORMAL
        }
    }
    
    private fun isAtWarningLevel(): Boolean {
        return when (gasType) {
            GasType.CO -> concentration > 30
            GasType.CO2 -> concentration > 5000
            GasType.PM25 -> concentration > 50
            GasType.PM10 -> concentration > 100
            GasType.CARBON_MONOXIDE -> percentage > 0.05
            GasType.CARBON_DIOXIDE -> percentage > 0.1
            GasType.OXYGEN -> percentage < 18.0 || percentage > 23.0
            GasType.AMMONIA -> percentage > 0.5
            GasType.BENZENE -> percentage > 0.05
            else -> false
        }
    }
}

/**
 * 🌡️ Datos ambientales
 */
data class EnvironmentalData(
    val temperature: Double, // °C
    val humidity: Double,    // %
    val pressure: Double     // hPa
) {
    fun isComfortable(): Boolean {
        return temperature in 18.0..24.0 &&
               humidity in 40.0..60.0 &&
               pressure in 1013.25 - 50..1013.25 + 50
    }
}

/**
 * 📱 Información del dispositivo sensor
 */
data class DeviceInfo(
    val id: String,
    val name: String,
    val type: String = "MQ-135",
    val location: String = "",
    val lastCalibrationDate: Long = 0
) {
    fun needsCalibration(): Boolean {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return lastCalibrationDate < oneWeekAgo
    }
}

/**
 * 🚨 Niveles de alerta del sistema
 */
enum class AlertLevel {
    NORMAL,     // Verde: Todo bien
    WARNING,    // Amarillo: Precaución
    CRITICAL    // Rojo: Peligro inmediato
}

/**
 * 📊 Modelo de datos del sensor completo
 */
data class SensorData(
    val timestamp: Long = System.currentTimeMillis(),
    val airQuality: AirQualityInfo,
    val systemStatus: SystemStatus
)

/**
 * 🌬️ Información simplificada de calidad del aire para dashboard
 */
data class AirQualityInfo(
    val ppm: Int,
    val level: AirQualityLevel,
    val temperature: Float,
    val humidity: Int,
    val gasComposition: Map<String, Float> = mapOf(
        "oxygen" to 78f,
        "co2" to 15f,
        "smoke" to 3f,
        "vapor" to 2f,
        "others" to 2f
    )
)

/**
 * 🎛️ Estado del sistema de control
 */
data class SystemStatus(
    val fanActive: Boolean = false,
    val buzzerActive: Boolean = false,
    val autoMode: Boolean = true,
    val uptime: Long = System.currentTimeMillis(),
    val deviceConnected: Boolean = false
) {
    val formattedUptime: String
        get() {
            val currentTime = System.currentTimeMillis()
            val uptimeMs = currentTime - uptime
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val minutes = (uptimeMs / (1000 * 60)) % 60
            val seconds = (uptimeMs / 1000) % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}

/**
 * 🌡️ Niveles de calidad del aire
 */
enum class AirQualityLevel {
    GOOD,       // 0-150 PPM
    MODERATE,   // 151-250 PPM
    POOR,       // 251-400 PPM
    CRITICAL    // 401+ PPM
}