package com.ti3042.airmonitor.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security Manager cumpliendo ISO 27400 para aplicaciones IoT
 */
class SecurityManager private constructor() {

    private val tag = "SecurityManager"
    private lateinit var encryptedPrefs: SharedPreferences
    private lateinit var masterKey: MasterKey
    
    companion object {
        @Volatile
        private var INSTANCE: SecurityManager? = null
        
        fun getInstance(): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager().also { INSTANCE = it }
            }
        }
        
        // Security constants
        private const val ENCRYPTED_PREFS_NAME = "air_monitor_secure_prefs"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_ALIAS = "air_monitor_master_key"
    }
    
    /**
     * Initialize security manager with encrypted storage
     */
    fun initialize(context: Context) {
        try {
            // Create master key for encryption
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // Create encrypted shared preferences
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            Log.d(tag, "✅ Security Manager initialized with encryption")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error initializing security manager: ${e.message}")
            throw SecurityException("Failed to initialize security manager", e)
        }
    }
    
    /**
     * Encrypt sensitive data using AES-GCM
     */
    fun encryptData(plaintext: String): String? {
        try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV + encrypted data + key for storage
            val combined = ByteArray(iv.size + encryptedData.size + secretKey.encoded.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            System.arraycopy(secretKey.encoded, 0, combined, iv.size + encryptedData.size, secretKey.encoded.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error encrypting data: ${e.message}")
            return null
        }
    }
    
    /**
     * Decrypt sensitive data using AES-GCM
     */
    fun decryptData(encryptedData: String): String? {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV, encrypted data, and key
            val iv = ByteArray(GCM_IV_LENGTH)
            val keyBytes = ByteArray(32) // AES-256 key size
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH - 32)
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)
            System.arraycopy(combined, GCM_IV_LENGTH + encrypted.size, keyBytes, 0, keyBytes.size)
            
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(encrypted)
            return String(decryptedData, Charsets.UTF_8)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error decrypting data: ${e.message}")
            return null
        }
    }
    
    /**
     * Store sensitive configuration securely
     */
    fun storeSecureConfig(key: String, value: String): Boolean {
        return try {
            encryptedPrefs.edit()
                .putString(key, value)
                .apply()
            Log.d(tag, "✅ Secure config stored: $key")
            true
        } catch (e: Exception) {
            Log.e(tag, "❌ Error storing secure config: ${e.message}")
            false
        }
    }
    
    /**
     * Retrieve secure configuration
     */
    fun getSecureConfig(key: String, defaultValue: String = ""): String {
        return try {
            encryptedPrefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Log.e(tag, "❌ Error retrieving secure config: ${e.message}")
            defaultValue
        }
    }
    
    /**
     * Validate user input to prevent injection attacks
     */
    fun validateInput(input: String, type: InputType): ValidationResult {
        return when (type) {
            InputType.EMAIL -> validateEmail(input)
            InputType.NUMERIC -> validateNumeric(input)
            InputType.ALPHANUMERIC -> validateAlphanumeric(input)
            InputType.DEVICE_NAME -> validateDeviceName(input)
        }
    }
    
    private fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email no puede estar vacío")
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Formato de email inválido")
        }
        
        if (email.length > 254) {
            return ValidationResult(false, "Email demasiado largo")
        }
        
        return ValidationResult(true, "Email válido")
    }
    
    private fun validateNumeric(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult(false, "Valor numérico requerido")
        }
        
        try {
            val value = input.toDouble()
            if (value < 0 || value > 10000) {
                return ValidationResult(false, "Valor fuera de rango permitido (0-10000)")
            }
            return ValidationResult(true, "Valor numérico válido")
        } catch (e: NumberFormatException) {
            return ValidationResult(false, "Formato numérico inválido")
        }
    }
    
    private fun validateAlphanumeric(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult(false, "Campo no puede estar vacío")
        }
        
        if (!input.matches(Regex("^[a-zA-Z0-9\\s._-]+$"))) {
            return ValidationResult(false, "Solo se permiten letras, números, espacios, puntos, guiones y guiones bajos")
        }
        
        if (input.length > 100) {
            return ValidationResult(false, "Texto demasiado largo (máximo 100 caracteres)")
        }
        
        return ValidationResult(true, "Texto válido")
    }
    
    private fun validateDeviceName(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult(false, "Nombre de dispositivo requerido")
        }
        
        if (!input.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return ValidationResult(false, "Solo se permiten letras, números, guiones y guiones bajos")
        }
        
        if (input.length < 3 || input.length > 32) {
            return ValidationResult(false, "Nombre debe tener entre 3 y 32 caracteres")
        }
        
        return ValidationResult(true, "Nombre de dispositivo válido")
    }
    
    /**
     * Generate secure API key or device token
     */
    fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }
    
    /**
     * Validate Firebase Auth token
     */
    fun validateAuthToken(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user != null && !user.isAnonymous
    }
    
    /**
     * Log security event
     */
    fun logSecurityEvent(event: String, details: String = "") {
        Log.i(tag, "🔒 SECURITY EVENT: $event - $details")
        
        // Store in encrypted preferences for audit
        val timestamp = System.currentTimeMillis()
        val eventLog = "$timestamp|$event|$details"
        
        try {
            val existingLogs = getSecureConfig("security_events", "")
            val updatedLogs = if (existingLogs.isNotEmpty()) {
                "$existingLogs\n$eventLog"
            } else {
                eventLog
            }
            
            // Keep only last 100 events
            val lines = updatedLogs.split("\n")
            val recentLogs = if (lines.size > 100) {
                lines.takeLast(100).joinToString("\n")
            } else {
                updatedLogs
            }
            
            storeSecureConfig("security_events", recentLogs)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error logging security event: ${e.message}")
        }
    }
    
    /**
     * Clear all secure data (for logout)
     */
    fun clearSecureData() {
        try {
            encryptedPrefs.edit().clear().apply()
            Log.d(tag, "✅ Secure data cleared")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error clearing secure data: ${e.message}")
        }
    }
}

enum class InputType {
    EMAIL, NUMERIC, ALPHANUMERIC, DEVICE_NAME
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)