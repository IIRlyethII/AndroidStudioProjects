package com.ti3042.airmonitor.data.datasource

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * 🔥 Firebase DataSource - Interacciones directas con Firebase
 * Encapsula toda la comunicación con Firestore y Storage
 * 
 * **Módulo**: :data
 * **Propósito**: Abstrae Firebase SDK del resto de la aplicación
 */
class FirebaseDataSource {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val tag = "FirebaseDataSource"
    
    /**
     * 📊 Guardar datos de sensores en Firestore
     */
    suspend fun saveSensorData(
        userId: String,
        sensorData: Map<String, Any>
    ): Result<String> {
        return try {
            Log.d(tag, "💾 Saving sensor data for user: $userId")
            
            val documentRef = firestore
                .collection("users")
                .document(userId)
                .collection("sensor_readings")
                .add(sensorData)
                .await()
            
            Log.d(tag, "✅ Sensor data saved with ID: ${documentRef.id}")
            Result.success(documentRef.id)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving sensor data: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 📖 Obtener últimas lecturas de sensores
     */
    suspend fun getLatestSensorReadings(
        userId: String,
        limit: Int = 10
    ): Result<List<Map<String, Any>>> {
        return try {
            Log.d(tag, "📖 Getting latest sensor readings for user: $userId")
            
            val querySnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("sensor_readings")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val readings = querySnapshot.documents.map { document ->
                document.data ?: emptyMap()
            }
            
            Log.d(tag, "✅ Retrieved ${readings.size} sensor readings")
            Result.success(readings)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error getting sensor readings: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 👤 Guardar perfil de usuario
     */
    suspend fun saveUserProfile(
        userId: String,
        profileData: Map<String, Any>
    ): Result<Unit> {
        return try {
            Log.d(tag, "👤 Saving user profile for: $userId")
            
            firestore
                .collection("users")
                .document(userId)
                .set(profileData)
                .await()
            
            Log.d(tag, "✅ User profile saved successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saving user profile: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 👤 Obtener perfil de usuario
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            Log.d(tag, "👤 Getting user profile for: $userId")
            
            val documentSnapshot = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()
            
            val profileData = documentSnapshot.data
            Log.d(tag, "✅ User profile retrieved")
            Result.success(profileData)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error getting user profile: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 📁 Subir archivo a Firebase Storage
     */
    suspend fun uploadFile(
        userId: String,
        fileName: String,
        data: ByteArray
    ): Result<String> {
        return try {
            Log.d(tag, "📁 Uploading file: $fileName for user: $userId")
            
            val storageRef = storage.reference
                .child("users/$userId/files/$fileName")
            
            val uploadTask = storageRef.putBytes(data).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Log.d(tag, "✅ File uploaded successfully: ${downloadUrl}")
            Result.success(downloadUrl.toString())
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error uploading file: ${e.message}")
            Result.failure(e)
        }
    }
}