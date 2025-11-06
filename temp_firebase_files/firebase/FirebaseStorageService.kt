package com.ti3042.airmonitor.services.firebase

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageService @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val REPORTS_FOLDER = "reports"
        private const val BACKUPS_FOLDER = "backups"
        private const val CALIBRATION_FOLDER = "calibration_data"
    }
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    // Upload PDF report
    suspend fun uploadReport(
        reportFile: File,
        reportType: String,
        startDate: Date,
        endDate: Date
    ): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            
            val fileName = "${reportType}_${startDate.time}_${endDate.time}.pdf"
            val reportRef = storage.reference
                .child("$REPORTS_FOLDER/$userId/$fileName")
            
            val uploadTask = reportRef.putFile(android.net.Uri.fromFile(reportFile))
            uploadTask.await()
            
            // Get download URL
            val downloadUrl = reportRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Upload database backup
    suspend fun uploadDatabaseBackup(backupFile: File): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            
            val fileName = "backup_${Date().time}.db"
            val backupRef = storage.reference
                .child("$BACKUPS_FOLDER/$userId/$fileName")
            
            val uploadTask = backupRef.putFile(android.net.Uri.fromFile(backupFile))
            uploadTask.await()
            
            val downloadUrl = backupRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Download database backup
    suspend fun downloadDatabaseBackup(backupUrl: String, destinationFile: File): Boolean {
        return try {
            val backupRef = storage.getReferenceFromUrl(backupUrl)
            backupRef.getFile(destinationFile).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Upload calibration data
    suspend fun uploadCalibrationData(
        calibrationData: String,
        deviceId: String,
        calibrationDate: Date
    ): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            
            val fileName = "calibration_${deviceId}_${calibrationDate.time}.json"
            val calibrationRef = storage.reference
                .child("$CALIBRATION_FOLDER/$userId/$fileName")
            
            val uploadTask = calibrationRef.putBytes(calibrationData.toByteArray())
            uploadTask.await()
            
            val downloadUrl = calibrationRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // List user reports
    suspend fun getUserReports(): List<StorageFileInfo> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()
            
            val reportsRef = storage.reference.child("$REPORTS_FOLDER/$userId")
            val listResult = reportsRef.listAll().await()
            
            listResult.items.map { item ->
                val metadata = item.metadata.await()
                StorageFileInfo(
                    name = item.name,
                    size = metadata.sizeBytes,
                    createdTime = Date(metadata.creationTimeMillis),
                    downloadUrl = item.downloadUrl.await().toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Delete report
    suspend fun deleteReport(reportPath: String): Boolean {
        return try {
            val reportRef = storage.getReferenceFromUrl(reportPath)
            reportRef.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Get storage usage
    suspend fun getStorageUsage(): StorageUsage {
        return try {
            val userId = getCurrentUserId() ?: return StorageUsage(0, 0, 0)
            
            val userRef = storage.reference.child(userId)
            val listResult = userRef.listAll().await()
            
            var totalSize = 0L
            var reportCount = 0
            var backupCount = 0
            
            listResult.items.forEach { item ->
                val metadata = item.metadata.await()
                totalSize += metadata.sizeBytes
                
                when {
                    item.path.contains(REPORTS_FOLDER) -> reportCount++
                    item.path.contains(BACKUPS_FOLDER) -> backupCount++
                }
            }
            
            StorageUsage(totalSize, reportCount, backupCount)
        } catch (e: Exception) {
            e.printStackTrace()
            StorageUsage(0, 0, 0)
        }
    }
}

data class StorageFileInfo(
    val name: String,
    val size: Long,
    val createdTime: Date,
    val downloadUrl: String
)

data class StorageUsage(
    val totalSizeBytes: Long,
    val reportCount: Int,
    val backupCount: Int
) {
    val totalSizeMB: Double
        get() = totalSizeBytes / (1024.0 * 1024.0)
}