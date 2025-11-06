package com.ti3042.airmonitor.services.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import com.ti3042.airmonitor.data.database.entities.SensorReading
import com.ti3042.airmonitor.data.database.entities.DeviceConfiguration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val SENSOR_READINGS_COLLECTION = "sensor_readings"
        private const val DEVICE_CONFIGURATIONS_COLLECTION = "device_configurations"
        private const val USER_PROFILES_COLLECTION = "user_profiles"
    }
    
    // Get current user ID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    // Sync sensor reading to Firestore
    suspend fun syncSensorReading(reading: SensorReading): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            
            val data = hashMapOf(
                "userId" to userId,
                "deviceId" to reading.deviceId,
                "timestamp" to reading.timestamp,
                "co2Ppm" to reading.co2Ppm,
                "coPpm" to reading.coPpm,
                "nh3Ppm" to reading.nh3Ppm,
                "noxPpm" to reading.noxPpm,
                "alcoholPpm" to reading.alcoholPpm,
                "benzenePpm" to reading.benzenePpm,
                "toluenePpm" to reading.toluenePpm,
                "acetonePpm" to reading.acetonePpm,
                "temperature" to reading.temperature,
                "humidity" to reading.humidity,
                "pressure" to reading.pressure,
                "airQualityIndex" to reading.airQualityIndex,
                "location" to reading.location,
                "sensorVoltage" to reading.sensorVoltage,
                "batteryLevel" to reading.batteryLevel,
                "signalStrength" to reading.signalStrength,
                "isSimulated" to reading.isSimulated,
                "notes" to reading.notes,
                "syncedAt" to Date()
            )
            
            firestore.collection(SENSOR_READINGS_COLLECTION)
                .document(reading.id)
                .set(data)
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Batch sync multiple readings
    suspend fun batchSyncReadings(readings: List<SensorReading>): Int {
        var successCount = 0
        
        readings.chunked(500).forEach { batch -> // Firestore batch limit is 500
            try {
                val batchWrite = firestore.batch()
                val userId = getCurrentUserId() ?: return@forEach
                
                batch.forEach { reading ->
                    val docRef = firestore.collection(SENSOR_READINGS_COLLECTION)
                        .document(reading.id)
                    
                    val data = hashMapOf(
                        "userId" to userId,
                        "deviceId" to reading.deviceId,
                        "timestamp" to reading.timestamp,
                        "co2Ppm" to reading.co2Ppm,
                        "coPpm" to reading.coPpm,
                        "nh3Ppm" to reading.nh3Ppm,
                        "noxPpm" to reading.noxPpm,
                        "alcoholPpm" to reading.alcoholPpm,
                        "benzenePpm" to reading.benzenePpm,
                        "toluenePpm" to reading.toluenePpm,
                        "acetonePpm" to reading.acetonePpm,
                        "temperature" to reading.temperature,
                        "humidity" to reading.humidity,
                        "pressure" to reading.pressure,
                        "airQualityIndex" to reading.airQualityIndex,
                        "isSimulated" to reading.isSimulated,
                        "syncedAt" to Date()
                    )
                    
                    batchWrite.set(docRef, data)
                }
                
                batchWrite.commit().await()
                successCount += batch.size
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return successCount
    }
    
    // Get readings from Firestore
    fun getCloudReadings(startTime: Date, endTime: Date): Flow<List<SensorReading>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection(SENSOR_READINGS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThanOrEqualTo("timestamp", endTime)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val readings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SensorReading(
                            id = doc.id,
                            deviceId = doc.getString("deviceId") ?: "",
                            timestamp = doc.getDate("timestamp") ?: Date(),
                            co2Ppm = doc.getDouble("co2Ppm") ?: 0.0,
                            coPpm = doc.getDouble("coPpm") ?: 0.0,
                            nh3Ppm = doc.getDouble("nh3Ppm") ?: 0.0,
                            noxPpm = doc.getDouble("noxPpm") ?: 0.0,
                            alcoholPpm = doc.getDouble("alcoholPpm") ?: 0.0,
                            benzenePpm = doc.getDouble("benzenePpm") ?: 0.0,
                            toluenePpm = doc.getDouble("toluenePpm") ?: 0.0,
                            acetonePpm = doc.getDouble("acetonePpm") ?: 0.0,
                            temperature = doc.getDouble("temperature") ?: 0.0,
                            humidity = doc.getDouble("humidity") ?: 0.0,
                            pressure = doc.getDouble("pressure") ?: 0.0,
                            airQualityIndex = doc.getLong("airQualityIndex")?.toInt() ?: 0,
                            location = doc.getString("location"),
                            sensorVoltage = doc.getDouble("sensorVoltage") ?: 0.0,
                            batteryLevel = doc.getDouble("batteryLevel") ?: 100.0,
                            signalStrength = doc.getDouble("signalStrength") ?: 0.0,
                            isSimulated = doc.getBoolean("isSimulated") ?: false,
                            notes = doc.getString("notes"),
                            isSynced = true,
                            syncedAt = doc.getDate("syncedAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(readings)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Sync device configuration
    suspend fun syncDeviceConfiguration(config: DeviceConfiguration): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            
            val data = hashMapOf(
                "userId" to userId,
                "deviceId" to config.deviceId,
                "deviceName" to config.deviceName,
                "co2Warning" to config.co2Warning,
                "co2Critical" to config.co2Critical,
                "coWarning" to config.coWarning,
                "coCritical" to config.coCritical,
                "nh3Warning" to config.nh3Warning,
                "nh3Critical" to config.nh3Critical,
                "samplingInterval" to config.samplingInterval,
                "alertsEnabled" to config.alertsEnabled,
                "autoCalibration" to config.autoCalibration,
                "isActive" to config.isActive,
                "lastUpdated" to Date()
            )
            
            firestore.collection(DEVICE_CONFIGURATIONS_COLLECTION)
                .document(config.deviceId)
                .set(data)
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Get device configurations from cloud
    suspend fun getCloudDeviceConfigurations(): List<DeviceConfiguration> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()
            
            val snapshot = firestore.collection(DEVICE_CONFIGURATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    DeviceConfiguration(
                        deviceId = doc.id,
                        deviceName = doc.getString("deviceName") ?: "",
                        co2Warning = doc.getDouble("co2Warning") ?: 1000.0,
                        co2Critical = doc.getDouble("co2Critical") ?: 5000.0,
                        coWarning = doc.getDouble("coWarning") ?: 30.0,
                        coCritical = doc.getDouble("coCritical") ?: 100.0,
                        nh3Warning = doc.getDouble("nh3Warning") ?: 25.0,
                        nh3Critical = doc.getDouble("nh3Critical") ?: 50.0,
                        samplingInterval = doc.getLong("samplingInterval")?.toInt() ?: 60,
                        alertsEnabled = doc.getBoolean("alertsEnabled") ?: true,
                        autoCalibration = doc.getBoolean("autoCalibration") ?: false,
                        isActive = doc.getBoolean("isActive") ?: true
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}