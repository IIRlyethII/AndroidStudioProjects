package com.ti3042.airmonitor.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ti3042.airmonitor.models.SensorData
import kotlinx.coroutines.*
import java.io.InputStreamReader

class MockDataService private constructor() {
    
    private var appContext: Context? = null
    
    companion object {
        private var instance: MockDataService? = null
        private const val TAG = "MockDataService"
        
        // 🔧 CONTROLA SI USAR DATOS SIMULADOS O REALES - Ahora lee desde SharedPreferences
        fun shouldUseMockData(context: Context): Boolean {
            val prefs = context.getSharedPreferences("AirMonitorSettings", Context.MODE_PRIVATE)
            val useMock = prefs.getBoolean("use_mock_data", true) // Default: true
            Log.d(TAG, "📊 Checking mock data preference: $useMock")
            return useMock
        }
        
        fun getInstance(): MockDataService {
            return instance ?: synchronized(this) {
                instance ?: MockDataService().also { instance = it }
            }
        }
    }
    
    private var mockDataList: List<MockSensorReading> = emptyList()
    private var currentIndex = 0
    private var isRunning = false
    private var dataUpdateCallback: ((SensorData) -> Unit)? = null
    private var connectionCallback: (() -> Unit)? = null
    private var job: Job? = null
    
    data class MockSensorReading(
        val timestamp: String,
        val airQuality: MockAirQuality,
        val gasComposition: MockGasComposition,
        val systemStatus: MockSystemStatus
    )
    
    data class MockAirQuality(
        val ppm: Int,
        val level: String,
        val temperature: Double,
        val humidity: Int
    )
    
    data class MockGasComposition(
        val oxygen: Int,
        val co2: Int,
        val smoke: Int,
        val vapor: Int,
        val others: Int
    )
    
    data class MockSystemStatus(
        val fanStatus: Boolean,
        val buzzerActive: Boolean,
        val autoMode: Boolean,
        val uptime: Long
    )
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val useMockData = shouldUseMockData(context)
        if (!useMockData) {
            Log.d(TAG, "🔄 Mock data desactivado - usando datos reales del ESP32")
            return
        }
        
        try {
            Log.d(TAG, "🎮 Inicializando MockDataService (modo simulación)")
            loadMockData(context)
            Log.d(TAG, "✅ Datos simulados cargados: ${mockDataList.size} registros")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando MockDataService: ${e.message}")
        }
    }
    
    private fun loadMockData(context: Context) {
        try {
            val inputStream = context.assets.open("mock_sensor_data.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val listType = object : TypeToken<List<MockSensorReading>>() {}.type
            
            mockDataList = gson.fromJson(reader, listType)
            reader.close()
            
            Log.d(TAG, "JSON cargado correctamente con ${mockDataList.size} registros")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando mock_sensor_data.json: ${e.message}")
            // Crear datos de respaldo si falla la carga
            createFallbackData()
        }
    }
    
    private fun createFallbackData() {
        Log.d(TAG, "Creando datos de respaldo")
        mockDataList = listOf(
            MockSensorReading(
                timestamp = "2024-11-04T12:00:00Z",
                airQuality = MockAirQuality(145, "buena", 23.5, 62),
                gasComposition = MockGasComposition(78, 15, 3, 2, 2),
                systemStatus = MockSystemStatus(true, false, true, 7500)
            ),
            MockSensorReading(
                timestamp = "2024-11-04T12:02:00Z",
                airQuality = MockAirQuality(234, "moderada", 24.8, 58),
                gasComposition = MockGasComposition(68, 25, 3, 2, 2),
                systemStatus = MockSystemStatus(true, true, true, 7620)
            ),
            MockSensorReading(
                timestamp = "2024-11-04T12:04:00Z",
                airQuality = MockAirQuality(421, "critica", 26.1, 48),
                gasComposition = MockGasComposition(62, 30, 5, 2, 1),
                systemStatus = MockSystemStatus(true, true, true, 7740)
            )
        )
    }
    
    fun startDataSimulation(
        onDataReceived: (SensorData) -> Unit,
        onConnected: () -> Unit
    ) {
        val useMockData = appContext?.let { shouldUseMockData(it) } ?: true
        if (!useMockData) {
            Log.d(TAG, "🔄 Simulación desactivada - esperando datos reales del ESP32")
            return
        }
        
        Log.d(TAG, "Iniciando simulación de datos")
        dataUpdateCallback = onDataReceived
        connectionCallback = onConnected
        isRunning = true
        
        // Simular conexión con delay
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // Simula tiempo de conexión
            connectionCallback?.invoke()
            Log.d(TAG, "Conexión simulada establecida")
            
            // Iniciar bucle de datos
            startDataLoop()
        }
    }
    
    private suspend fun startDataLoop() {
        while (isRunning && appContext?.let { shouldUseMockData(it) } == true) {
            try {
                val currentData = getCurrentMockData()
                val sensorData = convertToSensorData(currentData)
                
                dataUpdateCallback?.invoke(sensorData)
                Log.d(TAG, "Datos enviados: PPM=${currentData.airQuality.ppm}, Nivel=${currentData.airQuality.level}")
                
                // Avanzar al siguiente registro
                currentIndex = (currentIndex + 1) % mockDataList.size
                
                // Esperar 3 segundos antes del próximo update
                delay(3000)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error en bucle de datos: ${e.message}")
                delay(5000) // Esperar más tiempo si hay error
            }
        }
    }
    
    private fun getCurrentMockData(): MockSensorReading {
        return if (mockDataList.isNotEmpty()) {
            mockDataList[currentIndex]
        } else {
            // Datos por defecto si la lista está vacía
            MockSensorReading(
                timestamp = "2024-11-04T12:00:00Z",
                airQuality = MockAirQuality(150, "buena", 23.5, 60),
                gasComposition = MockGasComposition(78, 15, 3, 2, 2),
                systemStatus = MockSystemStatus(true, false, true, 7500)
            )
        }
    }
    
    private fun convertToSensorData(mockData: MockSensorReading): SensorData {
        // Necesito revisar los imports - voy a crear un método más simple
        
        // Crear datos usando el constructor completo
        val airQuality = com.ti3042.airmonitor.models.AirQuality(
            ppm = mockData.airQuality.ppm,
            level = mockData.airQuality.level,
            temperature = mockData.airQuality.temperature.toFloat(),
            humidity = mockData.airQuality.humidity
        )
        
        val systemStatus = com.ti3042.airmonitor.models.SystemStatus(
            fanStatus = mockData.systemStatus.fanStatus,
            buzzerActive = mockData.systemStatus.buzzerActive,
            autoMode = mockData.systemStatus.autoMode,
            uptime = mockData.systemStatus.uptime
        )
        
        val thresholds = com.ti3042.airmonitor.models.Thresholds(
            warning = 200,
            critical = 400
        )
        
        return SensorData(
            airQuality = airQuality,
            systemStatus = systemStatus,
            thresholds = thresholds,
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun stopDataSimulation() {
        Log.d(TAG, "Deteniendo simulación")
        isRunning = false
        job?.cancel()
        dataUpdateCallback = null
        connectionCallback = null
    }
    
    fun isSimulationActive(): Boolean {
        val useMock = appContext?.let { shouldUseMockData(it) } ?: false
        return useMock && isRunning
    }
    
    // Método para obtener datos específicos (para testing)
    fun getDataByIndex(index: Int): SensorData? {
        val useMock = appContext?.let { shouldUseMockData(it) } ?: false
        return if (useMock && index < mockDataList.size) {
            convertToSensorData(mockDataList[index])
        } else null
    }
    
    // Método para forzar un tipo específico de datos
    fun simulateScenario(scenario: String) {
        val useMock = appContext?.let { shouldUseMockData(it) } ?: false
        if (!useMock) return
        
        when (scenario) {
            "good_air" -> currentIndex = 0  // Datos de buena calidad
            "moderate_air" -> currentIndex = 1  // Datos moderados
            "bad_air" -> currentIndex = 4  // Datos críticos
            "improving" -> currentIndex = 5  // Datos mejorando
        }
        Log.d(TAG, "Simulando escenario: $scenario (índice: $currentIndex)")
    }
}