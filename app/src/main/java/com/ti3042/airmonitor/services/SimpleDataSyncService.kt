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

class SimpleDataSyncService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var dataGenerationJob: Job? = null
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "SimpleDataSyncService"
        private const val DATA_GENERATION_INTERVAL_SECONDS = 15L
        private const val SENSOR_READINGS_COLLECTION = "sensor_readings"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔄 SimpleDataSyncService created")
        startDataGeneration()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 SimpleDataSyncService started")
        return START_STICKY
    }
    
    private fun startDataGeneration() {
        dataGenerationJob = serviceScope.launch {
            while (isActive) {
                try {
                    generateAndSyncSensorReading()
                    delay(DATA_GENERATION_INTERVAL_SECONDS * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating sensor data: ${e.message}")
                    delay(60000) // Wait 1 minute before retrying
                }
            }
        }
    }
    
    private suspend fun generateAndSyncSensorReading() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "No authenticated user, skipping data generation")
                return
            }
            
            // Generate realistic sensor data
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // CO2 levels vary by time (higher during day)
            val baseCO2 = when {
                currentHour in 6..8 -> 450 + Random.nextInt(150)  // Morning
                currentHour in 9..17 -> 600 + Random.nextInt(400) // Work hours  
                currentHour in 18..22 -> 500 + Random.nextInt(300) // Evening
                else -> 350 + Random.nextInt(100) // Night
            }
            
            val sensorData = hashMapOf(
                "userId" to userId,
                "deviceId" to "AirMonitor_TI3042_001",
                "timestamp" to Date(),
                "co2Ppm" to baseCO2.toDouble(),
                "coPpm" to (10.0 + Random.nextDouble(-5.0, 25.0)).coerceAtLeast(0.0),
                "nh3Ppm" to (5.0 + Random.nextDouble(-2.0, 20.0)).coerceAtLeast(0.0),
                "noxPpm" to (50.0 + Random.nextDouble(-20.0, 100.0)).coerceAtLeast(0.0),
                "alcoholPpm" to (100.0 + Random.nextDouble(-50.0, 300.0)).coerceAtLeast(0.0),
                "benzenePpm" to (2.0 + Random.nextDouble(-1.0, 8.0)).coerceAtLeast(0.0),
                "toluenePpm" to (150.0 + Random.nextDouble(-50.0, 200.0)).coerceAtLeast(0.0),
                "acetonePpm" to (500.0 + Random.nextDouble(-200.0, 500.0)).coerceAtLeast(0.0),
                "temperature" to 20.0 + Random.nextDouble(-5.0, 15.0),
                "humidity" to (45.0 + Random.nextDouble(-15.0, 30.0)).coerceIn(0.0, 100.0),
                "pressure" to 1013.25 + Random.nextDouble(-20.0, 20.0),
                "airQualityIndex" to ((baseCO2 / 1000.0) * 50 + Random.nextDouble(0.0, 50.0)).coerceIn(0.0, 500.0),
                "location" to "Laboratorio TI3042",
                "sensorVoltage" to 3.3 + Random.nextDouble(-0.2, 0.2),
                "batteryLevel" to (85.0 + Random.nextDouble(-10.0, 15.0)).coerceIn(0.0, 100.0),
                "signalStrength" to -45.0 + Random.nextDouble(-20.0, 10.0),
                "isSimulated" to true,
                "notes" to "Datos simulados generados automáticamente",
                "createdAt" to Date()
            )
            
            // Sync to Firestore
            firestore.collection(SENSOR_READINGS_COLLECTION)
                .add(sensorData)
                .addOnSuccessListener { documentRef ->
                    Log.d(TAG, "✅ Sensor data synced to Firebase: ${documentRef.id} - CO2: ${baseCO2}ppm")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error syncing to Firebase: ${e.message}")
                }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sensor data: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 SimpleDataSyncService destroyed")
        dataGenerationJob?.cancel()
        serviceScope.cancel()
    }
}