package com.ti3042.airmonitor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random

/**
 * 🔥 Servicio simplificado para enviar datos a Firebase
 * Este servicio funciona sin dependencias complejas y es fácil de visualizar en Firebase Console
 */
class FirebaseDataService : Service() {
    
    private val tag = "FirebaseDataService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var syncJob: Job? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "🔥 FirebaseDataService iniciado")
        
        startDataSync()
        
        return START_STICKY // Reiniciar automáticamente si se detiene
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "🛑 FirebaseDataService destruido")
        syncJob?.cancel()
        serviceScope.cancel()
    }
    
    /**
     * 🚀 Iniciar sincronización de datos cada 30 segundos
     */
    private fun startDataSync() {
        syncJob?.cancel() // Cancelar trabajo previo
        
        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    if (auth.currentUser != null) {
                        sendSensorDataToFirebase()
                    } else {
                        Log.w(tag, "⚠️ Usuario no autenticado - omitiendo envío")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "❌ Error en sincronización: ${e.message}")
                }
                
                delay(30_000) // Esperar 30 segundos
            }
        }
    }
    
    /**
     * 📡 Enviar datos de sensores simulados a Firebase
     */
    private fun sendSensorDataToFirebase() {
        val currentUser = auth.currentUser ?: return
        
        // Generar datos realistas de sensores
        val sensorData = generateRealisticSensorData()
        
        // Crear estructura de documento
        val document = hashMapOf(
            "userId" to currentUser.uid,
            "userEmail" to currentUser.email,
            "deviceId" to "ESP32_TI3042_001",
            "timestamp" to System.currentTimeMillis(),
            "date" to Date(),
            "sensorData" to sensorData,
            "metadata" to hashMapOf(
                "appVersion" to "1.0.0",
                "isSimulated" to true,
                "location" to "Laboratorio TI3042"
            )
        )
        
        // Enviar a colección "sensor_readings"
        firestore.collection("sensor_readings")
            .add(document)
            .addOnSuccessListener { documentReference ->
                Log.d(tag, "✅ Datos enviados exitosamente: ${documentReference.id}")
                Log.d(tag, "📊 CO2: ${sensorData["co2"]} ppm")
                Log.d(tag, "🌡️ Temperatura: ${sensorData["temperature"]}°C")
                Log.d(tag, "💧 Humedad: ${sensorData["humidity"]}%")
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Error enviando datos: ${exception.message}")
            }
    }
    
    /**
     * 🎲 Generar datos realistas de sensores
     */
    private fun generateRealisticSensorData(): HashMap<String, Any> {
        return hashMapOf(
            // Gases principales (ppm)
            "co2" to (400.0 + Random.nextDouble(-50.0, 200.0)).coerceIn(350.0, 5000.0),
            "co" to (0.1 + Random.nextDouble(-0.05, 0.4)).coerceIn(0.0, 50.0),
            "nh3" to (0.03 + Random.nextDouble(-0.01, 0.1)).coerceIn(0.0, 25.0),
            "no2" to (0.02 + Random.nextDouble(-0.01, 0.08)).coerceIn(0.0, 0.2),
            
            // Condiciones ambientales
            "temperature" to (20.0 + Random.nextDouble(-5.0, 15.0)),
            "humidity" to (45.0 + Random.nextDouble(-15.0, 30.0)).coerceIn(0.0, 100.0),
            "pressure" to (1013.25 + Random.nextDouble(-20.0, 20.0)),
            
            // Métricas calculadas
            "airQualityIndex" to Random.nextInt(15, 150),
            "riskLevel" to when (Random.nextInt(1, 11)) {
                in 1..6 -> "NORMAL"
                in 7..8 -> "PRECAUCION"
                9 -> "ALERTA"
                else -> "CRITICO"
            },
            
            // Estado del sensor
            "sensorVoltage" to (3.3 + Random.nextDouble(-0.2, 0.2)),
            "batteryLevel" to (85.0 + Random.nextDouble(-10.0, 15.0)).coerceIn(0.0, 100.0),
            "signalStrength" to (-45.0 + Random.nextDouble(-20.0, 10.0)),
            
            // Información adicional
            "calibrationStatus" to "OK",
            "lastCalibration" to Date(System.currentTimeMillis() - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000)),
            "sensorModel" to "MQ-135 + BME280",
            "firmwareVersion" to "v2.1.3"
        )
    }
}