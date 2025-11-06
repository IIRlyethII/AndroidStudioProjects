package com.ti3042.airmonitor.domain.usecase

import com.ti3042.airmonitor.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

/**
 * 📊 Use Case para monitoreo de calidad del aire en tiempo real
 */
interface MonitorAirQualityUseCase {
    suspend fun execute(): Flow<Result<SensorData>>
}

/**
 * 🎛️ Use Case para control de dispositivos (ventilador, buzzer)
 */
interface ControlDevicesUseCase {
    suspend fun setAutoMode(enabled: Boolean)
    suspend fun setFanState(enabled: Boolean)
    suspend fun setBuzzerState(enabled: Boolean)
}