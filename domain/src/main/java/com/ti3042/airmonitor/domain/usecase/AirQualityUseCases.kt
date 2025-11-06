package com.ti3042.airmonitor.domain.usecase

import com.ti3042.airmonitor.domain.model.*
import com.ti3042.airmonitor.domain.repository.AirQualityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 🌬️ Use Case: Obtener datos de calidad del aire en tiempo real
 * Encapsula la lógica de negocio para monitoreo de aire
 * 
 * **Módulo**: :domain
 * **Propósito**: Lógica de negocio para obtener y procesar datos de sensores
 */
class GetRealTimeAirQualityUseCase(
    private val airQualityRepository: AirQualityRepository
) {
    
    operator fun invoke(deviceId: String): Flow<Result<AirQuality>> {
        return airQualityRepository.getRealtimeData(deviceId)
            .map { result ->
                result.fold(
                    onSuccess = { airQuality ->
                        val processedData = processAirQualityData(airQuality)
                        Result.success(processedData)
                    },
                    onFailure = { error ->
                        Result.failure(mapDataError(error))
                    }
                )
            }
    }
    
    /**
     * 🔄 Procesar y validar datos de calidad del aire
     */
    private fun processAirQualityData(airQuality: AirQuality): AirQuality {
        // Validar que los datos no sean demasiado antiguos (más de 5 minutos)
        val currentTime = System.currentTimeMillis()
        val dataAge = currentTime - airQuality.timestamp
        
        if (dataAge > 5 * 60 * 1000) { // 5 minutos en milliseconds
            throw DataException("Datos demasiado antiguos: ${dataAge / 1000} segundos")
        }
        
        // Validar rangos de sensores
        validateSensorRanges(airQuality)
        
        return airQuality
    }
    
    private fun validateSensorRanges(airQuality: AirQuality) {
        airQuality.gasReadings.forEach { reading ->
            when (reading.gasType) {
                GasType.CO2 -> {
                    if (reading.concentration < 0 || reading.concentration > 50000) {
                        throw DataException("CO2 fuera de rango válido: ${reading.concentration} ppm")
                    }
                }
                GasType.CO -> {
                    if (reading.concentration < 0 || reading.concentration > 1000) {
                        throw DataException("CO fuera de rango válido: ${reading.concentration} ppm")
                    }
                }
                GasType.PM25 -> {
                    if (reading.concentration < 0 || reading.concentration > 500) {
                        throw DataException("PM2.5 fuera de rango válido: ${reading.concentration} µg/m³")
                    }
                }
                GasType.PM10 -> {
                    if (reading.concentration < 0 || reading.concentration > 600) {
                        throw DataException("PM10 fuera de rango válido: ${reading.concentration} µg/m³")
                    }
                }
                else -> {
                    // Otros tipos de gas no requieren validación específica
                }
            }
        }
        
        // Validar datos ambientales
        if (airQuality.environmentalData.temperature < -50 || airQuality.environmentalData.temperature > 80) {
            throw DataException("Temperatura fuera de rango: ${airQuality.environmentalData.temperature}°C")
        }
        
        if (airQuality.environmentalData.humidity < 0 || airQuality.environmentalData.humidity > 100) {
            throw DataException("Humedad fuera de rango: ${airQuality.environmentalData.humidity}%")
        }
    }
    
    private fun mapDataError(error: Throwable): Throwable {
        return when {
            error.message?.contains("network") == true -> 
                NetworkException("Error de conexión con los sensores")
            error.message?.contains("timeout") == true -> 
                TimeoutException("Timeout al obtener datos de sensores")
            else -> DataException("Error al obtener datos: ${error.message}")
        }
    }
}

/**
 * 📊 Use Case: Obtener historial de calidad del aire
 * Recupera datos históricos con análisis de tendencias
 */
class GetAirQualityHistoryUseCase(
    private val airQualityRepository: AirQualityRepository
) {
    
    suspend operator fun invoke(
        deviceId: String,
        startTime: Long,
        endTime: Long
    ): Result<List<AirQuality>> {
        
        // Validar parámetros de entrada
        if (startTime >= endTime) {
            return Result.failure(ValidationException("Fecha de inicio debe ser anterior a fecha final"))
        }
        
        val timeRange = endTime - startTime
        val maxRange = 30L * 24 * 60 * 60 * 1000 // 30 días en milliseconds
        
        if (timeRange > maxRange) {
            return Result.failure(ValidationException("Rango máximo de consulta es 30 días"))
        }
        
        return try {
            val result = airQualityRepository.getHistoricalData(deviceId, startTime, endTime)
            
            result.fold(
                onSuccess = { data ->
                    val processedData = processHistoricalData(data)
                    Result.success(processedData)
                },
                onFailure = { error ->
                    Result.failure(mapDataError(error))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 🔍 Procesar datos históricos y detectar anomalías
     */
    private fun processHistoricalData(data: List<AirQuality>): List<AirQuality> {
        if (data.isEmpty()) return data
        
        // Ordenar por timestamp
        val sortedData = data.sortedBy { it.timestamp }
        
        // Detectar y marcar datos anómalos
        return sortedData.map { reading ->
            val isAnomalous = detectAnomalies(reading, sortedData)
            reading.copy(
                // Aquí podríamos agregar metadata sobre anomalías si fuera necesario
            )
        }
    }
    
    private fun detectAnomalies(reading: AirQuality, allData: List<AirQuality>): Boolean {
        // Lógica simple de detección de anomalías
        // En producción esto sería más sofisticado
        
        val avgAQI = allData.map { it.calculateAQI() }.average()
        val currentAQI = reading.calculateAQI()
        
        // Si el AQI está 3 desviaciones estándar por encima del promedio
        val threshold = avgAQI * 1.5
        return currentAQI > threshold
    }
    
    private fun mapDataError(error: Throwable): Throwable {
        return when {
            error.message?.contains("network") == true -> 
                NetworkException("Error de conexión al obtener historial")
            error.message?.contains("not found") == true -> 
                DataNotFoundException("No se encontraron datos para el período especificado")
            else -> DataException("Error al obtener historial: ${error.message}")
        }
    }
}

/**
 * 🚨 Use Case: Configurar alertas personalizadas
 * Maneja la configuración de umbrales de alerta
 */
class ConfigureAlertsUseCase(
    private val airQualityRepository: AirQualityRepository
) {
    
    suspend operator fun invoke(
        userId: String,
        gasType: GasType,
        warningThreshold: Double,
        dangerThreshold: Double
    ): Result<Unit> {
        
        // Validaciones de negocio
        if (warningThreshold >= dangerThreshold) {
            return Result.failure(ValidationException("Umbral de advertencia debe ser menor que umbral de peligro"))
        }
        
        if (warningThreshold < 0 || dangerThreshold < 0) {
            return Result.failure(ValidationException("Los umbrales no pueden ser negativos"))
        }
        
        // Validar rangos específicos por tipo de gas
        val isValid = when (gasType) {
            GasType.CO2 -> warningThreshold <= 5000 && dangerThreshold <= 10000
            GasType.CO -> warningThreshold <= 30 && dangerThreshold <= 50
            GasType.PM25 -> warningThreshold <= 50 && dangerThreshold <= 100
            GasType.PM10 -> warningThreshold <= 100 && dangerThreshold <= 200
            else -> true // Otros gases no tienen validación específica
        }
        
        if (!isValid) {
            return Result.failure(ValidationException("Umbrales fuera del rango recomendado para $gasType"))
        }
        
        return try {
            airQualityRepository.configureAlerts(userId, gasType, warningThreshold, dangerThreshold)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 🔍 Use Case: Analizar tendencias de calidad del aire
 * Proporciona análisis de tendencias y pronósticos básicos
 */
class AnalyzeAirQualityTrendsUseCase(
    private val airQualityRepository: AirQualityRepository
) {
    
    suspend operator fun invoke(deviceId: String, days: Int = 7): Result<TrendAnalysis> {
        
        if (days < 1 || days > 30) {
            return Result.failure(ValidationException("El análisis debe ser entre 1 y 30 días"))
        }
        
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
        
        return try {
            val result = airQualityRepository.getHistoricalData(deviceId, startTime, endTime)
            
            result.fold(
                onSuccess = { data ->
                    if (data.size < 24) { // Mínimo 24 puntos de datos
                        Result.failure(InsufficientDataException("Datos insuficientes para análisis de tendencias"))
                    } else {
                        val analysis = analyzeTrends(data)
                        Result.success(analysis)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun analyzeTrends(data: List<AirQuality>): TrendAnalysis {
        val sortedData = data.sortedBy { it.timestamp }
        
        // Calcular promedios por día
        val dailyAverages = sortedData
            .groupBy { it.timestamp / (24 * 60 * 60 * 1000) } // Agrupar por día
            .values
            .map { dayData ->
                val avgAQI = dayData.map { it.calculateAQI() }.average()
                val avgTemp = dayData.map { it.environmentalData.temperature }.average()
                val avgHumidity = dayData.map { it.environmentalData.humidity }.average()
                
                DailyAverage(
                    day = dayData.first().timestamp,
                    aqi = avgAQI,
                    temperature = avgTemp,
                    humidity = avgHumidity
                )
            }
        
        // Calcular tendencia (simple: comparar primera mitad vs segunda mitad)
        val midpoint = dailyAverages.size / 2
        val firstHalfAvg = dailyAverages.take(midpoint).map { it.aqi }.average()
        val secondHalfAvg = dailyAverages.drop(midpoint).map { it.aqi }.average()
        
        val trend = when {
            secondHalfAvg > firstHalfAvg * 1.1 -> TrendDirection.WORSENING
            secondHalfAvg < firstHalfAvg * 0.9 -> TrendDirection.IMPROVING
            else -> TrendDirection.STABLE
        }
        
        return TrendAnalysis(
            period = dailyAverages.size,
            trend = trend,
            averageAQI = dailyAverages.map { it.aqi }.average(),
            dailyAverages = dailyAverages
        )
    }
}

/**
 * 📈 Modelo para análisis de tendencias
 */
data class TrendAnalysis(
    val period: Int,
    val trend: TrendDirection,
    val averageAQI: Double,
    val dailyAverages: List<DailyAverage>
)

data class DailyAverage(
    val day: Long,
    val aqi: Double,
    val temperature: Double,
    val humidity: Double
)

enum class TrendDirection {
    IMPROVING,    // Calidad del aire mejorando
    WORSENING,    // Calidad del aire empeorando
    STABLE        // Calidad del aire estable
}

/**
 * ❌ Excepciones específicas del dominio
 */
class DataException(message: String) : Exception(message)
class DataNotFoundException(message: String) : Exception(message)
class TimeoutException(message: String) : Exception(message)
class InsufficientDataException(message: String) : Exception(message)