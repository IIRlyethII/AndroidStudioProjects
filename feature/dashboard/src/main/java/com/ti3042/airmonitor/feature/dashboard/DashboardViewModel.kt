package com.ti3042.airmonitor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ti3042.airmonitor.domain.model.SensorData
import com.ti3042.airmonitor.domain.usecase.MonitorAirQualityUseCase
import com.ti3042.airmonitor.domain.usecase.ControlDevicesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * 📊 ViewModel para el Dashboard principal
 * 
 * Responsabilidades:
 * - Gestionar datos de sensores en tiempo real
 * - Controlar dispositivos (ventilador, buzzer)
 * - Manejar estado de conexión
 * - Coordinar con use cases del dominio
 */
class DashboardViewModel(
    private val monitorAirQualityUseCase: MonitorAirQualityUseCase,
    private val controlDevicesUseCase: ControlDevicesUseCase
) : ViewModel() {
    
    private val tag = "DashboardViewModel"
    
    // Estado de datos del sensor
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()
    
    // Estado de conexión
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    // Errores
    private val _errors = MutableStateFlow<String?>(null)
    val errors: StateFlow<String?> = _errors.asStateFlow()
    
    // Control de modo automático
    private val _isAutoMode = MutableStateFlow(true)
    val isAutoMode: StateFlow<Boolean> = _isAutoMode.asStateFlow()
    
    init {
        Log.d(tag, "📊 DashboardViewModel inicializado")
    }
    
    /**
     * 🚀 Iniciar monitoreo de calidad del aire
     */
    fun startMonitoring() {
        Log.d(tag, "🚀 Iniciando monitoreo de sensores")
        
        viewModelScope.launch {
            try {
                monitorAirQualityUseCase.execute().collect { result ->
                    result.fold(
                        onSuccess = { data ->
                            _sensorData.value = data
                            _connectionState.value = true
                            Log.d(tag, "✅ Datos recibidos - PPM: ${data.airQuality.ppm}")
                        },
                        onFailure = { exception ->
                            _errors.value = exception.message
                            _connectionState.value = false
                            Log.e(tag, "❌ Error monitoreando: ${exception.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                _errors.value = "Error iniciando monitoreo: ${e.message}"
                Log.e(tag, "❌ Exception en startMonitoring: ${e.message}")
            }
        }
    }
    
    /**
     * 🛑 Detener monitoreo
     */
    fun stopMonitoring() {
        Log.d(tag, "🛑 Deteniendo monitoreo de sensores")
        // El use case se encarga de limpiar recursos
    }
    
    /**
     * 🤖 Configurar modo automático
     */
    fun setAutoMode(enabled: Boolean) {
        _isAutoMode.value = enabled
        Log.d(tag, "🤖 Modo automático: ${if (enabled) "ACTIVADO" else "DESACTIVADO"}")
        
        viewModelScope.launch {
            try {
                controlDevicesUseCase.setAutoMode(enabled)
            } catch (e: Exception) {
                _errors.value = "Error configurando modo automático: ${e.message}"
                Log.e(tag, "❌ Error en setAutoMode: ${e.message}")
            }
        }
    }
    
    /**
     * 🌪️ Controlar ventilador manualmente
     */
    fun setFanState(enabled: Boolean) {
        if (_isAutoMode.value) {
            Log.w(tag, "⚠️ Intento de control manual en modo automático")
            return
        }
        
        Log.d(tag, "🌪️ Control manual ventilador: ${if (enabled) "ON" else "OFF"}")
        
        viewModelScope.launch {
            try {
                controlDevicesUseCase.setFanState(enabled)
            } catch (e: Exception) {
                _errors.value = "Error controlando ventilador: ${e.message}"
                Log.e(tag, "❌ Error en setFanState: ${e.message}")
            }
        }
    }
    
    /**
     * 🚨 Controlar buzzer/alerta manualmente
     */
    fun setAlertState(enabled: Boolean) {
        if (_isAutoMode.value) {
            Log.w(tag, "⚠️ Intento de control manual de alerta en modo automático")
            return
        }
        
        Log.d(tag, "🚨 Control manual alerta: ${if (enabled) "ON" else "OFF"}")
        
        viewModelScope.launch {
            try {
                controlDevicesUseCase.setBuzzerState(enabled)
            } catch (e: Exception) {
                _errors.value = "Error controlando alerta: ${e.message}"
                Log.e(tag, "❌ Error en setAlertState: ${e.message}")
            }
        }
    }
    
    /**
     * 🔄 Limpiar errores
     */
    fun clearErrors() {
        _errors.value = null
    }
}

/**
 * 🏭 Factory para crear DashboardViewModel con dependencias
 */
class DashboardViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            // TODO: Inyectar use cases reales cuando tengamos DI
            // Por ahora creamos implementaciones mock
            val monitorUseCase = createMockMonitorUseCase()
            val controlUseCase = createMockControlUseCase()
            
            return DashboardViewModel(monitorUseCase, controlUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    
    private fun createMockMonitorUseCase(): MonitorAirQualityUseCase {
        // TODO: Reemplazar con inyección de dependencias real
        return object : MonitorAirQualityUseCase {
            override suspend fun execute(): kotlinx.coroutines.flow.Flow<Result<SensorData>> {
                // Implementación mock temporal
                return flow {
                    // Emitir datos simulados
                }
            }
        }
    }
    
    private fun createMockControlUseCase(): ControlDevicesUseCase {
        // TODO: Reemplazar con inyección de dependencias real
        return object : ControlDevicesUseCase {
            override suspend fun setAutoMode(enabled: Boolean) {
                // Implementación mock
            }
            
            override suspend fun setFanState(enabled: Boolean) {
                // Implementación mock
            }
            
            override suspend fun setBuzzerState(enabled: Boolean) {
                // Implementación mock
            }
        }
    }
}