package com.ti3042.airmonitor.bluetooth

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ti3042.airmonitor.models.*
import com.ti3042.airmonitor.utils.JsonParser
import kotlin.random.Random

/**
 * Servicio de simulación Bluetooth que genera datos realistas del ESP32
 */
class MockBluetoothService : BluetoothService {
    
    private val tag = "MockBluetoothService"
    private var callback: ConnectionCallback? = null
    private var isRunning = false
    private var currentSensorData: SensorData? = null
    
    // Simulación de estado del sistema
    private var fanEnabled = false
    private var buzzerEnabled = false
    private var autoMode = true
    private var warningThreshold = 200
    private var criticalThreshold = 400
    private var startTime = System.currentTimeMillis()
    
    // Handler para simular datos cada 2 segundos
    private val dataHandler = Handler(Looper.getMainLooper())
    private val dataRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                generateAndSendData()
                dataHandler.postDelayed(this, 2000) // Cada 2 segundos
            }
        }
    }
    
    override fun connect(deviceAddress: String?) {
        Log.d(tag, "Iniciando conexión simulada...")
        
        // Simular delay de conexión
        Handler(Looper.getMainLooper()).postDelayed({
            isRunning = true
            callback?.onConnected()
            callback?.onConnectionStateChanged(true)
            
            // Comenzar a generar datos
            dataHandler.post(dataRunnable)
            Log.d(tag, "Conexión simulada establecida")
        }, 1000)
    }
    
    override fun disconnect() {
        Log.d(tag, "Desconectando simulación...")
        isRunning = false
        dataHandler.removeCallbacks(dataRunnable)
        callback?.onDisconnected()
        callback?.onConnectionStateChanged(false)
    }
    
    override fun sendCommand(command: ControlCommand) {
        Log.d(tag, "Comando recibido: ${JsonParser.commandToJson(command)}")
        
        // Procesar comandos y actualizar estado simulado
        command.fan?.let { 
            fanEnabled = it.enable
            Log.d(tag, "Ventilador ${if (fanEnabled) "encendido" else "apagado"}")
        }
        
        command.buzzer?.let { 
            buzzerEnabled = it.enable
            Log.d(tag, "Buzzer ${if (buzzerEnabled) "activado" else "desactivado"}")
        }
        
        command.autoMode?.let { 
            autoMode = it
            Log.d(tag, "Modo automático ${if (autoMode) "habilitado" else "deshabilitado"}")
        }
        
        command.thresholds?.let {
            warningThreshold = it.warning
            criticalThreshold = it.critical
            Log.d(tag, "Umbrales actualizados: warning=$warningThreshold, critical=$criticalThreshold")
        }
        
        // Simular respuesta inmediata del ESP32
        generateAndSendData()
    }
    
    override fun isConnected(): Boolean = isRunning
    
    override fun setConnectionCallback(callback: ConnectionCallback?) {
        this.callback = callback
    }
    
    /**
     * Genera datos de sensores realistas
     */
    private fun generateAndSendData() {
        // Simular PPM con variaciones naturales
        val basePPM = when {
            autoMode && fanEnabled -> Random.nextInt(80, 150)  // Aire limpio con ventilador
            fanEnabled -> Random.nextInt(120, 200)            // Ventilador manual
            else -> Random.nextInt(180, 400)                  // Sin ventilador
        }
        
        // Agregar ruido y variaciones naturales
        val ppmNoise = Random.nextInt(-20, 20)
        val finalPPM = (basePPM + ppmNoise).coerceIn(50, 800)
        
        // Determinar si el buzzer debería activarse automáticamente
        val shouldBuzzerBeActive = if (autoMode) {
            finalPPM >= warningThreshold
        } else {
            buzzerEnabled
        }
        
        // Determinar si el ventilador debería activarse automáticamente
        val shouldFanBeActive = if (autoMode) {
            finalPPM >= warningThreshold
        } else {
            fanEnabled
        }
        
        // Actualizar estados automáticos
        if (autoMode) {
            fanEnabled = shouldFanBeActive
            buzzerEnabled = shouldBuzzerBeActive
        }
        
        // Generar temperatura y humedad realistas
        val temperature = Random.nextDouble(18.0, 28.0).toFloat()
        val humidity = Random.nextInt(40, 80)
        
        // Crear objeto SensorData
        val sensorData = SensorData(
            device = "AirMonitor_TI3042",
            version = "1.0_SIM",
            timestamp = System.currentTimeMillis(),
            airQuality = AirQuality(
                ppm = finalPPM,
                level = AirQuality.getLevelFromPPM(finalPPM),
                temperature = temperature,
                humidity = humidity
            ),
            systemStatus = SystemStatus(
                fanStatus = fanEnabled,
                buzzerActive = buzzerEnabled,
                autoMode = autoMode,
                uptime = System.currentTimeMillis() - startTime
            ),
            thresholds = Thresholds(
                warning = warningThreshold,
                critical = criticalThreshold
            )
        )
        
        currentSensorData = sensorData
        
        // Enviar datos al callback
        callback?.onDataReceived(sensorData)
        
        Log.d(tag, "Datos generados - PPM: $finalPPM, Nivel: ${sensorData.airQuality.level}, Fan: $fanEnabled, Auto: $autoMode")
    }
    
    /**
     * Simula diferentes escenarios de calidad del aire
     */
    fun simulateScenario(scenario: String) {
        when (scenario) {
            "good_air" -> {
                fanEnabled = true
                autoMode = true
            }
            "poor_air" -> {
                fanEnabled = false
                autoMode = false  
            }
            "critical_air" -> {
                fanEnabled = false
                autoMode = false
            }
        }
        generateAndSendData()
    }
    
    fun getCurrentData(): SensorData? = currentSensorData
}

/**
 * Interface base para servicios Bluetooth (Mock y Real)
 */
interface BluetoothService {
    fun connect(deviceAddress: String?)
    fun disconnect()
    fun sendCommand(command: ControlCommand)
    fun isConnected(): Boolean
    fun setConnectionCallback(callback: ConnectionCallback?)
}

