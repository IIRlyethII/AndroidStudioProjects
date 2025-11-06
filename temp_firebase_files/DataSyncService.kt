package com.ti3042.airmonitor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ti3042.airmonitor.data.database.entities.SensorReading
import com.ti3042.airmonitor.data.database.repository.SensorDataRepository
import com.ti3042.airmonitor.data.MockDataService
import com.ti3042.airmonitor.services.firebase.FirebaseService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

//@AndroidEntryPoint
class DataSyncService : Service() {
    
    @Inject
    lateinit var sensorDataRepository: SensorDataRepository
    
    @Inject
    lateinit var firebaseService: FirebaseService
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private var dataGenerationJob: Job? = null
    
    companion object {
        private const val TAG = "DataSyncService"
        private const val SYNC_INTERVAL_MINUTES = 5L
        private const val DATA_GENERATION_INTERVAL_SECONDS = 30L
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔄 DataSyncService created")
        startDataGeneration()
        startPeriodicSync()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 DataSyncService started")
        return START_STICKY // Restart service if killed
    }
    
    private fun startDataGeneration() {
        dataGenerationJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check if mock data is enabled
                    if (MockDataService.shouldUseMockData(this@DataSyncService)) {
                        generateAndStoreSensorReading()
                    }
                    
                    delay(DATA_GENERATION_INTERVAL_SECONDS * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating sensor data: ${e.message}")
                    delay(60000) // Wait 1 minute before retrying
                }
            }
        }
    }
    
    private fun startPeriodicSync() {
        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    performSync()
                    delay(SYNC_INTERVAL_MINUTES * 60 * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic sync: ${e.message}")
                    delay(60000) // Wait 1 minute before retrying
                }
            }
        }
    }
    
    private suspend fun generateAndStoreSensorReading() {
        try {
            val reading = createSimulatedReading()
            sensorDataRepository.insertReading(reading)
            Log.d(TAG, "📊 Generated sensor reading: CO2=${reading.co2Ppm}ppm, CO=${reading.coPpm}ppm")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing sensor reading: ${e.message}")
        }
    }
    
    private fun createSimulatedReading(): SensorReading {
        // Generate realistic sensor data based on time of day and random variations
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // CO2 levels vary by time (higher during day when people are active)
        val baseCO2 = when {
            currentHour in 6..8 -> 450 + Random.nextInt(150)  // Morning
            currentHour in 9..17 -> 600 + Random.nextInt(400) // Work hours
            currentHour in 18..22 -> 500 + Random.nextInt(300) // Evening
            else -> 350 + Random.nextInt(100) // Night
        }
        
        // Other gases with more variation
        val co = 10.0 + Random.nextDouble(-5.0, 25.0)
        val nh3 = 5.0 + Random.nextDouble(-2.0, 20.0)
        val nox = 50.0 + Random.nextDouble(-20.0, 100.0)
        val alcohol = 100.0 + Random.nextDouble(-50.0, 300.0)
        val benzene = 2.0 + Random.nextDouble(-1.0, 8.0)
        val toluene = 150.0 + Random.nextDouble(-50.0, 200.0)
        val acetone = 500.0 + Random.nextDouble(-200.0, 500.0)
        
        // Environmental data
        val temperature = 20.0 + Random.nextDouble(-5.0, 15.0)
        val humidity = 45.0 + Random.nextDouble(-15.0, 30.0)
        val pressure = 1013.25 + Random.nextDouble(-20.0, 20.0)
        
        // Calculate simple AQI
        val aqi = ((baseCO2 / 1000.0) * 50 + (co / 100.0) * 100).toInt().coerceIn(0, 500)
        
        return SensorReading(
            id = UUID.randomUUID().toString(),
            deviceId = "AirMonitor_TI3042_001",
            timestamp = Date(),
            co2Ppm = baseCO2.toDouble(),
            coPpm = co.coerceAtLeast(0.0),
            nh3Ppm = nh3.coerceAtLeast(0.0),
            noxPpm = nox.coerceAtLeast(0.0),
            alcoholPpm = alcohol.coerceAtLeast(0.0),
            benzenePpm = benzene.coerceAtLeast(0.0),
            toluenePpm = toluene.coerceAtLeast(0.0),
            acetonePpm = acetone.coerceAtLeast(0.0),
            temperature = temperature,
            humidity = humidity.coerceIn(0.0, 100.0),
            pressure = pressure,
            airQualityIndex = aqi,
            location = "Laboratorio TI3042",
            sensorVoltage = 3.3 + Random.nextDouble(-0.2, 0.2),
            batteryLevel = 85.0 + Random.nextDouble(-10.0, 15.0),
            signalStrength = -45.0 + Random.nextDouble(-20.0, 10.0),
            isSimulated = true,
            notes = "Datos simulados generados automáticamente",
            isSynced = false,
            syncedAt = null
        )
    }
    
    private suspend fun performSync() {
        try {
            Log.d(TAG, "🔄 Starting Firebase sync...")
            
            // Get unsynced readings
            val unsyncedReadings = sensorDataRepository.getUnsyncedReadings()
            
            if (unsyncedReadings.isEmpty()) {
                Log.d(TAG, "✅ No data to sync")
                return
            }
            
            Log.d(TAG, "📤 Syncing ${unsyncedReadings.size} readings to Firebase...")
            
            // Sync to Firebase
            val syncedCount = firebaseService.batchSyncReadings(unsyncedReadings)
            
            if (syncedCount > 0) {
                // Mark as synced
                val syncedIds = unsyncedReadings.take(syncedCount).map { it.id }
                sensorDataRepository.markAsSynced(syncedIds)
                
                Log.d(TAG, "✅ Successfully synced $syncedCount readings to Firebase")
            } else {
                Log.w(TAG, "⚠️ No readings were synced to Firebase")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase sync failed: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 DataSyncService destroyed")
        
        // Cancel all jobs
        syncJob?.cancel()
        dataGenerationJob?.cancel()
        serviceScope.cancel()
    }
}