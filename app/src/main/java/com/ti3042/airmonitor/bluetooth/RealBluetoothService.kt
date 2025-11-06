package com.ti3042.airmonitor.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ti3042.airmonitor.models.*
import com.ti3042.airmonitor.utils.JsonParser
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 📱 SERVICIO BLUETOOTH REAL - ESP32 AIR MONITOR
 * TI3042 - Comunicación real con hardware ESP32
 * 
 * FUNCIONALIDADES:
 * ✅ Conexión Bluetooth SPP real con ESP32
 * ✅ Protocolo JSON bidireccional
 * ✅ Reconexión automática
 * ✅ Buffer de comandos
 * ✅ Parsing robusto de datos
 * ✅ Manejo de errores
 */
class RealBluetoothService(private val context: Context) : BluetoothService {
    
    companion object {
        private const val TAG = "RealBluetoothService"
        private const val ESP32_NAME = "ESP32_AirMonitor_TI3042"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECTION_TIMEOUT = 10000L // 10 segundos
        private const val RECONNECT_DELAY = 5000L     // 5 segundos
        private const val READ_BUFFER_SIZE = 1024
    }
    
    // 🔧 COMPONENTES BLUETOOTH
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    // 📊 ESTADO DE CONEXIÓN
    private var callback: ConnectionCallback? = null
    private var isConnected = false
    private var isConnecting = false
    private var deviceAddress: String? = null
    private var targetDevice: BluetoothDevice? = null
    
    // 🔄 CORRUTINAS Y THREADS
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readingJob: Job? = null
    private var connectionJob: Job? = null
    
    // 📋 BUFFER DE COMANDOS
    private val commandQueue = ConcurrentLinkedQueue<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 📊 DATOS ACTUALES
    private var currentSensorData: SensorData? = null
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 5
    
    init {
        initializeBluetooth()
    }
    
    // 🚀 INICIALIZACIÓN
    private fun initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "❌ Bluetooth no disponible en este dispositivo")
            mainHandler.post {
                callback?.onError("Bluetooth no disponible")
            }
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "⚠️ Bluetooth deshabilitado")
            mainHandler.post {
                callback?.onError("Bluetooth deshabilitado - Por favor habilítalo")
            }
            return
        }
        
        Log.d(TAG, "✅ Bluetooth inicializado correctamente")
    }
    
    // 🔗 CONEXIÓN
    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String?) {
        if (isConnected || isConnecting) {
            Log.w(TAG, "⚠️ Ya conectado o conectando...")
            return
        }
        
        this.deviceAddress = deviceAddress
        isConnecting = true
        connectionAttempts = 0
        
        Log.d(TAG, "🔄 Iniciando conexión con ESP32...")
        
        // Buscar dispositivo ESP32
        connectionJob = serviceScope.launch {
            try {
                findAndConnectDevice()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en conexión: ${e.message}", e)
                handleConnectionError(e.message ?: "Error desconocido")
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun findAndConnectDevice() {
        // Buscar dispositivo por dirección o nombre
        targetDevice = if (deviceAddress != null) {
            bluetoothAdapter?.getRemoteDevice(deviceAddress)
        } else {
            // Buscar por nombre
            bluetoothAdapter?.bondedDevices?.find { device ->
                device.name?.contains(ESP32_NAME, ignoreCase = true) == true
            }
        }
        
        if (targetDevice == null) {
            Log.e(TAG, "❌ Dispositivo ESP32 no encontrado")
            handleConnectionError("ESP32 no encontrado - Asegúrate de que esté emparejado")
            return
        }
        
        Log.d(TAG, "📱 ESP32 encontrado: ${targetDevice?.name} (${targetDevice?.address})")
        
        // Intentar conexión con reintentos
        var connected = false
        while (!connected && connectionAttempts < maxConnectionAttempts && isConnecting) {
            connectionAttempts++
            Log.d(TAG, "🔄 Intento de conexión #$connectionAttempts...")
            
            try {
                connected = attemptConnection()
            } catch (e: Exception) {
                Log.w(TAG, "❌ Intento #$connectionAttempts falló: ${e.message}")
                if (connectionAttempts < maxConnectionAttempts) {
                    delay(RECONNECT_DELAY)
                }
            }
        }
        
        if (!connected) {
            handleConnectionError("No se pudo conectar después de $maxConnectionAttempts intentos")
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun attemptConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Crear socket RFCOMM
                bluetoothSocket = targetDevice?.createRfcommSocketToServiceRecord(SPP_UUID)
                
                // Conectar con timeout
                withTimeout(CONNECTION_TIMEOUT) {
                    bluetoothSocket?.connect()
                }
                
                // Configurar streams
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                
                // Marcar como conectado
                isConnected = true
                isConnecting = false
                
                // Notificar conexión exitosa
                mainHandler.post {
                    callback?.onConnected()
                    callback?.onConnectionStateChanged(true)
                }
                
                // Iniciar lectura de datos
                startDataReading()
                
                Log.d(TAG, "✅ Conexión establecida con ESP32")
                true
                
            } catch (e: Exception) {
                cleanup()
                throw e
            }
        }
    }
    
    // 📊 LECTURA DE DATOS
    private fun startDataReading() {
        readingJob = serviceScope.launch {
            val buffer = ByteArray(READ_BUFFER_SIZE)
            var partialMessage = ""
            
            Log.d(TAG, "📊 Iniciando lectura de datos...")
            
            try {
                while (isConnected && inputStream != null) {
                    try {
                        val bytesRead = inputStream!!.read(buffer)
                        if (bytesRead > 0) {
                            val data = String(buffer, 0, bytesRead)
                            partialMessage += data
                            
                            // Procesar mensajes completos (separados por \n)
                            val messages = partialMessage.split("\n")
                            partialMessage = messages.last() // Conservar mensaje incompleto
                            
                            // Procesar cada mensaje completo
                            for (i in 0 until messages.size - 1) {
                                val message = messages[i].trim()
                                if (message.isNotEmpty()) {
                                    processReceivedMessage(message)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "❌ Error leyendo datos: ${e.message}")
                        handleConnectionLost()
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en lectura de datos: ${e.message}", e)
                handleConnectionLost()
            }
        }
    }
    
    // 🔍 PROCESAMIENTO DE MENSAJES
    private fun processReceivedMessage(message: String) {
        try {
            Log.d(TAG, "📨 Mensaje recibido: $message")
            
            // Intentar parsear como JSON
            val jsonObject = JSONObject(message)
            
            // Verificar si es un mensaje de datos de sensores
            if (jsonObject.has("air_quality") || jsonObject.has("ppm")) {
                val sensorData = parseJsonToSensorData(jsonObject)
                currentSensorData = sensorData
                
                // Enviar datos al callback en el hilo principal
                mainHandler.post {
                    callback?.onDataReceived(sensorData)
                }
                
                Log.d(TAG, "📊 Datos de sensores procesados: PPM=${sensorData.airQuality.ppm}")
                
            } else if (jsonObject.has("type") && jsonObject.getString("type") == "response") {
                // Respuesta a comando
                val success = jsonObject.optBoolean("success", false)
                val responseMessage = jsonObject.optString("message", "")
                
                Log.d(TAG, "📋 Respuesta de comando: success=$success, message=$responseMessage")
                
                if (!success) {
                    mainHandler.post {
                        callback?.onError("Error en comando: $responseMessage")
                    }
                }
            }
            
        } catch (e: JSONException) {
            Log.w(TAG, "⚠️ Mensaje no es JSON válido: $message")
            // Podría ser un mensaje de debug del ESP32
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando mensaje: ${e.message}", e)
        }
    }
    
    // 🔄 PARSEO DE JSON A MODELO
    private fun parseJsonToSensorData(jsonObject: JSONObject): SensorData {
        val timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
        val device = jsonObject.optString("device", "ESP32_TI3042")
        val version = jsonObject.optString("version", "1.0.0")
        
        // Parsear air_quality
        val airQualityJson = jsonObject.optJSONObject("air_quality") ?: jsonObject
        val ppm = airQualityJson.optInt("ppm", 0)
        val level = airQualityJson.optString("level", "unknown")
        val temperature = airQualityJson.optDouble("temperature", 0.0).toFloat()
        val humidity = airQualityJson.optInt("humidity", 0)
        
        val airQuality = AirQuality(
            ppm = ppm,
            level = level.ifEmpty { AirQuality.getLevelFromPPM(ppm) },
            temperature = temperature,
            humidity = humidity
        )
        
        // Parsear system status
        val systemJson = jsonObject.optJSONObject("system")
        val systemStatus = SystemStatus(
            fanStatus = systemJson?.optBoolean("fan_status", false) ?: false,
            buzzerActive = systemJson?.optBoolean("buzzer_active", false) ?: false,
            autoMode = systemJson?.optBoolean("auto_mode", true) ?: true,
            uptime = systemJson?.optLong("uptime", 0L) ?: 0L
        )
        
        // Parsear thresholds
        val thresholdsJson = jsonObject.optJSONObject("thresholds")
        val thresholds = Thresholds(
            warning = thresholdsJson?.optInt("warning", 1000) ?: 1000,
            critical = thresholdsJson?.optInt("critical", 2000) ?: 2000
        )
        
        return SensorData(
            device = device,
            version = version,
            timestamp = timestamp,
            airQuality = airQuality,
            systemStatus = systemStatus,
            thresholds = thresholds
        )
    }
    
    // 📤 ENVÍO DE COMANDOS
    override fun sendCommand(command: ControlCommand) {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "⚠️ No conectado - comando encolado")
            val jsonCommand = JsonParser.commandToJson(command)
            commandQueue.offer(jsonCommand)
            return
        }
        
        serviceScope.launch {
            try {
                val jsonCommand = JsonParser.commandToJson(command)
                val commandWithNewline = "$jsonCommand\n"
                
                Log.d(TAG, "📤 Enviando comando: $jsonCommand")
                
                outputStream?.write(commandWithNewline.toByteArray())
                outputStream?.flush()
                
                Log.d(TAG, "✅ Comando enviado exitosamente")
                
            } catch (e: IOException) {
                Log.e(TAG, "❌ Error enviando comando: ${e.message}")
                handleConnectionLost()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando comando: ${e.message}", e)
                mainHandler.post {
                    callback?.onError("Error enviando comando: ${e.message}")
                }
            }
        }
    }
    
    // 📋 ENVÍO DE COMANDOS PENDIENTES
    private fun sendQueuedCommands() {
        serviceScope.launch {
            while (commandQueue.isNotEmpty() && isConnected) {
                val command = commandQueue.poll()
                if (command != null) {
                    try {
                        val commandWithNewline = "$command\n"
                        outputStream?.write(commandWithNewline.toByteArray())
                        outputStream?.flush()
                        Log.d(TAG, "📋 Comando pendiente enviado: $command")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error enviando comando pendiente: ${e.message}")
                        break
                    }
                }
            }
        }
    }
    
    // 🔌 DESCONEXIÓN
    override fun disconnect() {
        Log.d(TAG, "🔌 Desconectando...")
        
        isConnecting = false
        connectionJob?.cancel()
        
        cleanup()
        
        mainHandler.post {
            callback?.onDisconnected()
            callback?.onConnectionStateChanged(false)
        }
    }
    
    // 🧹 LIMPIEZA
    private fun cleanup() {
        isConnected = false
        readingJob?.cancel()
        
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error cerrando recursos: ${e.message}")
        }
        
        inputStream = null
        outputStream = null
        bluetoothSocket = null
        
        Log.d(TAG, "🧹 Recursos limpiados")
    }
    
    // ❌ MANEJO DE ERRORES
    private fun handleConnectionError(error: String) {
        Log.e(TAG, "❌ Error de conexión: $error")
        
        isConnecting = false
        cleanup()
        
        mainHandler.post {
            callback?.onError(error)
            callback?.onConnectionStateChanged(false)
        }
    }
    
    private fun handleConnectionLost() {
        Log.w(TAG, "📶 Conexión perdida - intentando reconectar...")
        
        cleanup()
        
        mainHandler.post {
            callback?.onConnectionStateChanged(false)
        }
        
        // Intentar reconexión automática
        if (deviceAddress != null || targetDevice != null) {
            mainHandler.postDelayed({
                if (!isConnected) {
                    Log.d(TAG, "🔄 Intentando reconexión automática...")
                    connect(deviceAddress ?: targetDevice?.address)
                }
            }, RECONNECT_DELAY)
        }
    }
    
    // 📊 GETTERS
    override fun isConnected(): Boolean = isConnected
    
    override fun setConnectionCallback(callback: ConnectionCallback?) {
        this.callback = callback
    }
    
    fun getCurrentData(): SensorData? = currentSensorData
    
    fun getConnectionAttempts(): Int = connectionAttempts
    
    fun getDeviceInfo(): String? {
        return targetDevice?.let { "${it.name} (${it.address})" }
    }
    
    // 🧪 MÉTODOS DE TESTING
    fun getQueuedCommandsCount(): Int = commandQueue.size
    
    fun clearCommandQueue() {
        commandQueue.clear()
        Log.d(TAG, "📋 Cola de comandos limpiada")
    }
    
    // 🛑 DESTRUCTOR
    fun destroy() {
        Log.d(TAG, "🛑 Destruyendo servicio...")
        disconnect()
        serviceScope.cancel()
        commandQueue.clear()
    }
}