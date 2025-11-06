package com.ti3042.airmonitor.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.ti3042.airmonitor.models.SensorData

/**
 * Manager para Firebase Analytics
 * Registra eventos importantes de la aplicación
 */
class FirebaseManager private constructor() {
    
    private val tag = "FirebaseManager"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    
    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null
        
        fun getInstance(): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Inicializa Firebase Analytics
     */
    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.d(tag, "Firebase Analytics inicializado correctamente")
            
            // Registrar evento de app iniciada
            logAppStarted()
        } catch (e: Exception) {
            Log.e(tag, "Error inicializando Firebase: ${e.message}")
        }
    }
    
    /**
     * Registra cuando la app se inicia
     */
    fun logAppStarted() {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putString("version", "1.0_SIM")
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("air_monitor_started", bundle)
            Log.d(tag, "Evento app_started registrado")
        }
    }
    
    /**
     * Registra conexión Bluetooth
     */
    fun logBluetoothConnection(success: Boolean, deviceName: String? = null) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putBoolean("success", success)
                putString("device_name", deviceName ?: "Unknown")
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("bluetooth_connection", bundle)
            Log.d(tag, "Evento bluetooth_connection registrado - Success: $success")
        }
    }
    
    /**
     * Registra datos de sensores recibidos
     */
    fun logSensorData(sensorData: SensorData) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putInt("ppm", sensorData.airQuality.ppm)
                putString("air_level", sensorData.airQuality.level)
                putDouble("temperature", sensorData.airQuality.temperature.toDouble())
                putInt("humidity", sensorData.airQuality.humidity)
                putBoolean("fan_status", sensorData.systemStatus.fanStatus)
                putBoolean("buzzer_active", sensorData.systemStatus.buzzerActive)
                putBoolean("auto_mode", sensorData.systemStatus.autoMode)
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("sensor_data_received", bundle)
            
            // Solo logear cada 30 segundos para no saturar
            if (System.currentTimeMillis() % 30000 < 2000) {
                Log.d(tag, "Evento sensor_data_received registrado - PPM: ${sensorData.airQuality.ppm}")
            }
        }
    }
    
    /**
     * Registra comandos de control enviados
     */
    fun logControlCommand(commandType: String, value: Any? = null) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putString("command_type", commandType)
                when (value) {
                    is Boolean -> putBoolean("value", value)
                    is Int -> putInt("value", value)
                    is String -> putString("value", value)
                }
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("control_command_sent", bundle)
            Log.d(tag, "Evento control_command_sent registrado - Type: $commandType")
        }
    }
    
    /**
     * Registra alertas de calidad del aire
     */
    fun logAirQualityAlert(ppmLevel: Int, alertLevel: String) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putInt("ppm_level", ppmLevel)
                putString("alert_level", alertLevel)
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("air_quality_alert", bundle)
            Log.d(tag, "Evento air_quality_alert registrado - PPM: $ppmLevel, Level: $alertLevel")
        }
    }
    
    /**
     * Registra navegación entre pantallas
     */
    fun logScreenView(screenName: String) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(tag, "Screen view registrado: $screenName")
        }
    }
    
    /**
     * Registra errores importantes
     */
    fun logError(errorType: String, errorMessage: String) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putString("error_type", errorType)
                putString("error_message", errorMessage)
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent("app_error", bundle)
            Log.d(tag, "Error registrado: $errorType - $errorMessage")
        }
    }
    
    /**
     * Establece propiedades de usuario
     */
    fun setUserProperties(simulationMode: Boolean) {
        firebaseAnalytics?.let { analytics ->
            analytics.setUserProperty("simulation_mode", simulationMode.toString())
            analytics.setUserProperty("app_version", "1.0")
            Log.d(tag, "Propiedades de usuario establecidas")
        }
    }
}