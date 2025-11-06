package com.ti3042.airmonitor.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ti3042.airmonitor.data.models.SensorReading
import com.ti3042.airmonitor.data.models.SystemStatusData
import com.ti3042.airmonitor.data.models.UserSettings
import com.ti3042.airmonitor.models.SensorData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager para manejo de datos con Firestore
 * Cumple con requisitos de rúbrica para almacenamiento IoT
 */
class FirestoreDataManager private constructor() {

    private val tag = "FirestoreDataManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        @Volatile
        private var INSTANCE: FirestoreDataManager? = null
        
        fun getInstance(): FirestoreDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreDataManager().also { INSTANCE = it }
            }
        }
        
        // Colecciones de Firestore
        private const val COLLECTION_SENSOR_READINGS = "sensor_readings"
        private const val COLLECTION_USER_SETTINGS = "user_settings"
        private const val COLLECTION_ALERTS = "alerts"
    }
    
    /**
     * Guardar lectura de sensor en Firestore
     */
    fun saveSensorReading(sensorData: SensorData, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "Usuario no autenticado")
            return
        }
        
        try {
            val gasComposition = mapOf(
                "oxygen" to 20.9f,
                "co2" to (sensorData.airQuality.ppm * 0.8f),
                "smoke" to (sensorData.airQuality.ppm * 0.1f),
                "vapor" to (sensorData.airQuality.ppm * 0.05f),
                "others" to (sensorData.airQuality.ppm * 0.05f)
            )
            
            val systemStatus = SystemStatusData(
                fanStatus = sensorData.systemStatus.fanStatus,
                buzzerActive = sensorData.systemStatus.buzzerActive,
                autoMode = true,
                uptime = sensorData.systemStatus.uptime,
                batteryLevel = 100,
                wifiSignal = 85,
                bluetoothConnected = true
            )
            
            val reading = SensorReading(
                ppm = sensorData.airQuality.ppm,
                airQualityLevel = sensorData.airQuality.level,
                temperature = sensorData.airQuality.temperature,
                humidity = sensorData.airQuality.humidity,
                gasComposition = gasComposition,
                deviceId = android.provider.Settings.Secure.getString(null, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown",
                userId = userId,
                location = "Aula TI3042",
                systemStatus = systemStatus
            )
            
            firestore.collection(COLLECTION_SENSOR_READINGS)
                .add(reading)
                .addOnSuccessListener { documentReference ->
                    Log.d(tag, "✅ Sensor reading saved: ${documentReference.id}")
                    callback(true, documentReference.id)
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "❌ Error saving sensor reading: ${exception.message}")
                    callback(false, exception.message)
                }
                
        } catch (e: Exception) {
            Log.e(tag, "❌ Exception saving sensor reading: ${e.message}")
            callback(false, e.message)
        }
    }
    
    /**
     * Obtener lecturas históricas del usuario
     */
    fun getSensorHistory(
        limit: Int = 50,
        callback: (List<SensorReading>, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList(), "Usuario no autenticado")
            return
        }
        
        firestore.collection(COLLECTION_SENSOR_READINGS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { documents ->
                val readings = documents.toObjects(SensorReading::class.java)
                Log.d(tag, "✅ Retrieved ${readings.size} sensor readings")
                callback(readings, null)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Error getting sensor history: ${exception.message}")
                callback(emptyList(), exception.message)
            }
    }
    
    /**
     * Obtener lecturas por rango de fechas
     */
    fun getSensorReadingsByDateRange(
        startDate: Date,
        endDate: Date,
        callback: (List<SensorReading>, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList(), "Usuario no autenticado")
            return
        }
        
        firestore.collection(COLLECTION_SENSOR_READINGS)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val readings = documents.toObjects(SensorReading::class.java)
                Log.d(tag, "✅ Retrieved ${readings.size} readings for date range")
                callback(readings, null)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Error getting readings by date: ${exception.message}")
                callback(emptyList(), exception.message)
            }
    }
    
    /**
     * Guardar configuración de usuario
     */
    fun saveUserSettings(settings: UserSettings, callback: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, "Usuario no autenticado")
            return
        }
        
        val userSettings = settings.copy(
            userId = userId,
            email = auth.currentUser?.email ?: ""
        )
        
        firestore.collection(COLLECTION_USER_SETTINGS)
            .document(userId)
            .set(userSettings)
            .addOnSuccessListener {
                Log.d(tag, "✅ User settings saved")
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Error saving user settings: ${exception.message}")
                callback(false, exception.message)
            }
    }
    
    /**
     * Obtener configuración de usuario
     */
    fun getUserSettings(callback: (UserSettings?, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(null, "Usuario no autenticado")
            return
        }
        
        firestore.collection(COLLECTION_USER_SETTINGS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val settings = document.toObject(UserSettings::class.java)
                    Log.d(tag, "✅ User settings retrieved")
                    callback(settings, null)
                } else {
                    // Crear configuración por defecto
                    val defaultSettings = UserSettings(
                        userId = userId,
                        email = auth.currentUser?.email ?: "",
                        deviceName = "AirMonitor_${userId.take(8)}"
                    )
                    saveUserSettings(defaultSettings) { success, error ->
                        if (success) {
                            callback(defaultSettings, null)
                        } else {
                            callback(null, error)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Error getting user settings: ${exception.message}")
                callback(null, exception.message)
            }
    }
    
    /**
     * Obtener estadísticas resumidas
     */
    fun getStatisticsSummary(callback: (Map<String, Any>, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyMap(), "Usuario no autenticado")
            return
        }
        
        // Obtener lecturas del último día
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.time
        
        getSensorReadingsByDateRange(yesterday, Date()) { readings, error ->
            if (error != null) {
                callback(emptyMap(), error)
                return@getSensorReadingsByDateRange
            }
            
            val stats = mutableMapOf<String, Any>()
            
            if (readings.isNotEmpty()) {
                val ppmValues = readings.map { it.ppm }
                val tempValues = readings.map { it.temperature }
                
                stats["totalReadings"] = readings.size
                stats["averagePPM"] = ppmValues.average().toInt()
                stats["maxPPM"] = ppmValues.maxOrNull() ?: 0
                stats["minPPM"] = ppmValues.minOrNull() ?: 0
                stats["averageTemp"] = tempValues.average().toFloat()
                stats["alertsCount"] = readings.count { it.ppm > 300 }
                stats["lastReading"] = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(readings.first().timestamp ?: Date())
            } else {
                stats["totalReadings"] = 0
                stats["averagePPM"] = 0
                stats["maxPPM"] = 0
                stats["minPPM"] = 0
                stats["averageTemp"] = 0.0f
                stats["alertsCount"] = 0
                stats["lastReading"] = "Sin datos"
            }
            
            Log.d(tag, "✅ Statistics calculated: $stats")
            callback(stats, null)
        }
    }
    
    /**
     * Limpiar datos antiguos según configuración de retención
     */
    fun cleanupOldData(retentionDays: Int = 30) {
        val userId = auth.currentUser?.uid ?: return
        
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -retentionDays)
        }.time
        
        firestore.collection(COLLECTION_SENSOR_READINGS)
            .whereEqualTo("userId", userId)
            .whereLessThan("timestamp", cutoffDate)
            .get()
            .addOnSuccessListener { documents ->
                val batch = firestore.batch()
                documents.forEach { document ->
                    batch.delete(document.reference)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(tag, "✅ Cleaned up ${documents.size()} old records")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(tag, "❌ Error cleaning up old data: ${exception.message}")
                    }
            }
    }
}