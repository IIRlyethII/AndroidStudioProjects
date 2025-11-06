package com.ti3042.airmonitor.bluetooth

import android.content.Context
import android.util.Log
import com.ti3042.airmonitor.models.AppSettings
import com.ti3042.airmonitor.models.ControlCommand

/**
 * Manager principal para conexiones Bluetooth
 * Maneja tanto el servicio Mock como el Real
 */
class BluetoothManager private constructor() {
    
    private val tag = "BluetoothManager"
    private var bluetoothService: BluetoothService? = null
    private var connectionCallback: ConnectionCallback? = null
    
    companion object {
        @Volatile
        private var INSTANCE: BluetoothManager? = null
        
        fun getInstance(): BluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothManager().also { INSTANCE = it }
            }
        }
        
        /**
         * Limpia la instancia singleton (solo para testing)
         */
        @JvmStatic
        fun clearInstance() {
            INSTANCE?.destroy()
            INSTANCE = null
        }
    }
    
    /**
     * Inicializa el servicio Bluetooth apropiado
     */
    fun initialize(context: Context, useSimulation: Boolean = true) {
        // Destruir servicio anterior si existe
        (bluetoothService as? RealBluetoothService)?.destroy()
        
        bluetoothService = if (useSimulation) {
            Log.d(tag, "✅ Inicializando MockBluetoothService (Simulación)")
            MockBluetoothService()
        } else {
            Log.d(tag, "📱 Inicializando RealBluetoothService (Hardware ESP32)")
            RealBluetoothService(context)
        }
        
        bluetoothService?.setConnectionCallback(connectionCallback)
        
        Log.d(tag, "🔧 Servicio Bluetooth configurado: ${if (useSimulation) "SIMULACIÓN" else "REAL"}")
    }
    
    /**
     * Conecta al dispositivo ESP32
     */
    fun connect(deviceAddress: String? = null) {
        bluetoothService?.connect(deviceAddress)
    }
    
    /**
     * Desconecta del dispositivo
     */
    fun disconnect() {
        bluetoothService?.disconnect()
    }
    
    /**
     * Envía un comando al ESP32
     */
    fun sendCommand(command: ControlCommand) {
        bluetoothService?.sendCommand(command)
    }
    
    /**
     * Verifica si está conectado
     */
    fun isConnected(): Boolean {
        return bluetoothService?.isConnected() ?: false
    }
    
    /**
     * Establece el callback para eventos de conexión
     */
    fun setConnectionCallback(callback: ConnectionCallback?) {
        this.connectionCallback = callback
        bluetoothService?.setConnectionCallback(callback)
    }
    
    /**
     * Cambia entre modo simulación y modo real
     */
    fun switchToMode(context: Context, useSimulation: Boolean) {
        val wasConnected = isConnected()
        
        // Desconectar servicio actual
        disconnect()
        
        // Reinicializar con nuevo modo
        initialize(context, useSimulation)
        
        // Reconectar si estaba conectado
        if (wasConnected) {
            Log.d(tag, "🔄 Reconectando en nuevo modo...")
            connect()
        }
    }
    
    /**
     * Verifica si está en modo simulación
     */
    fun isSimulationMode(): Boolean {
        return bluetoothService is MockBluetoothService
    }
    
    /**
     * Obtiene información del dispositivo conectado (solo modo real)
     */
    fun getDeviceInfo(): String? {
        return (bluetoothService as? RealBluetoothService)?.getDeviceInfo()
    }
    
    /**
     * Obtiene el número de intentos de conexión (solo modo real)
     */
    fun getConnectionAttempts(): Int {
        return (bluetoothService as? RealBluetoothService)?.getConnectionAttempts() ?: 0
    }
    
    /**
     * Obtiene el servicio actual (útil para testing)
     */
    fun getCurrentService(): BluetoothService? = bluetoothService
    
    /**
     * Limpia recursos al destruir la aplicación
     */
    fun destroy() {
        Log.d(tag, "🛑 Destruyendo BluetoothManager...")
        (bluetoothService as? RealBluetoothService)?.destroy()
        bluetoothService = null
        connectionCallback = null
        INSTANCE = null
    }
}

