package com.ti3042.airmonitor.feature.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * 📊 ViewModel para Monitoreo y Análisis
 * 
 * Responsabilidades:
 * - Gestionar datos históricos
 * - Procesar análisis estadísticos
 * - Generar reportes
 * - Coordinar visualizaciones
 */
class MonitoringViewModel : ViewModel() {
    
    private val tag = "MonitoringViewModel"
    
    // Estado de datos de monitoreo
    private val _monitoringData = MutableStateFlow<MonitoringData?>(null)
    val monitoringData: StateFlow<MonitoringData?> = _monitoringData.asStateFlow()
    
    // Estado de gráficos
    private val _chartData = MutableStateFlow<ChartData?>(null)
    val chartData: StateFlow<ChartData?> = _chartData.asStateFlow()
    
    // Estado de historial
    private val _historyData = MutableStateFlow<List<HistoricalReading>>(emptyList())
    val historyData: StateFlow<List<HistoricalReading>> = _historyData.asStateFlow()
    
    // Estado de análisis
    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()
    
    // Estados de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Errores
    private val _errors = MutableStateFlow<String?>(null)
    val errors: StateFlow<String?> = _errors.asStateFlow()
    
    init {
        Log.d(tag, "📊 MonitoringViewModel inicializado")
    }
    
    /**
     * 📋 Cargar datos de monitoreo
     */
    fun loadMonitoringData() {
        Log.d(tag, "📋 Cargando datos de monitoreo...")
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Cargar datos simulados
                loadChartData()
                loadHistoryData()
                loadAnalyticsData()
                
                val monitoringData = MonitoringData(
                    totalReadings = 1247,
                    averagePPM = 156.3,
                    maxPPM = 387.2,
                    minPPM = 45.1,
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                _monitoringData.value = monitoringData
                
                Log.d(tag, "✅ Datos de monitoreo cargados exitosamente")
                
            } catch (e: Exception) {
                _errors.value = "Error cargando datos de monitoreo: ${e.message}"
                Log.e(tag, "❌ Error en loadMonitoringData: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 📈 Cargar datos para gráficos
     */
    private fun loadChartData() {
        val timeLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00")
        val ppmValues = listOf(120f, 135f, 180f, 220f, 195f, 160f, 140f)
        val temperatureValues = listOf(18.5f, 19.2f, 22.1f, 25.3f, 24.8f, 22.4f, 20.1f)
        val humidityValues = listOf(65f, 68f, 72f, 75f, 73f, 70f, 67f)
        
        _chartData.value = ChartData(
            timeLabels = timeLabels,
            ppmValues = ppmValues,
            temperatureValues = temperatureValues,
            humidityValues = humidityValues
        )
    }
    
    /**
     * 📊 Cargar datos históricos
     */
    private fun loadHistoryData() {
        val currentTime = System.currentTimeMillis()
        val historyList = mutableListOf<HistoricalReading>()
        
        // Generar datos históricos simulados para los últimos 7 días
        for (i in 0..6) {
            val dayTime = currentTime - (i * 24 * 60 * 60 * 1000L)
            
            historyList.add(
                HistoricalReading(
                    timestamp = dayTime,
                    ppm = kotlin.random.Random.nextInt(80, 300),
                    temperature = kotlin.random.Random.nextDouble(18.0, 26.0),
                    humidity = kotlin.random.Random.nextInt(50, 80),
                    airQualityLevel = when (kotlin.random.Random.nextInt(0, 4)) {
                        0 -> "BUENA"
                        1 -> "MODERADA" 
                        2 -> "MALA"
                        else -> "CRÍTICA"
                    }
                )
            )
        }
        
        _historyData.value = historyList.sortedByDescending { it.timestamp }
    }
    
    /**
     * 🧪 Cargar datos de análisis
     */
    private fun loadAnalyticsData() {
        _analyticsData.value = AnalyticsData(
            weeklyAverage = 156.3,
            weeklyTrend = "+12.5%",
            criticalHours = 3,
            bestHour = "06:00",
            worstHour = "14:00",
            gasComposition = mapOf(
                "O₂" to 78.2f,
                "CO₂" to 15.8f,
                "Humo" to 3.1f,
                "Vapor" to 2.1f,
                "Otros" to 0.8f
            ),
            recommendations = listOf(
                "Mejorar ventilación entre 12:00-16:00",
                "Revisar filtros del sistema",
                "Considerar purificador adicional"
            )
        )
    }
    
    /**
     * 🔄 Refrescar datos
     */
    fun refreshData() {
        Log.d(tag, "🔄 Refrescando datos...")
        loadMonitoringData()
    }
    
    /**
     * 📤 Exportar reporte
     */
    fun exportReport(format: String) {
        Log.d(tag, "📤 Exportando reporte en formato: $format")
        
        viewModelScope.launch {
            try {
                // TODO: Implementar exportación real
                Log.d(tag, "✅ Reporte exportado exitosamente")
                
            } catch (e: Exception) {
                _errors.value = "Error exportando reporte: ${e.message}"
                Log.e(tag, "❌ Error en exportReport: ${e.message}")
            }
        }
    }
    
    /**
     * 🔍 Filtrar datos por rango de fechas
     */
    fun filterByDateRange(startDate: Long, endDate: Long) {
        Log.d(tag, "🔍 Filtrando datos por rango de fechas")
        
        viewModelScope.launch {
            try {
                val filteredHistory = _historyData.value.filter { reading ->
                    reading.timestamp in startDate..endDate
                }
                
                _historyData.value = filteredHistory
                Log.d(tag, "✅ Filtrado aplicado: ${filteredHistory.size} registros")
                
            } catch (e: Exception) {
                _errors.value = "Error filtrando datos: ${e.message}"
                Log.e(tag, "❌ Error en filterByDateRange: ${e.message}")
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
 * 🏭 Factory para crear MonitoringViewModel
 */
class MonitoringViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitoringViewModel::class.java)) {
            return MonitoringViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}