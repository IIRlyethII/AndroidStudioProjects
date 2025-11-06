package com.ti3042.airmonitor.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * 🔐 GESTOR DE SESIÓN PERSISTENTE
 * TI3042 - Manejo inteligente de autenticación
 * 
 * FUNCIONALIDADES:
 * ✅ Login persistente (una sola vez)
 * ✅ Auto-login al abrir app
 * ✅ Opción de "Cerrar Sesión"
 * ✅ Verificación de token válido
 * ✅ Configuración de seguridad
 */
class PersistentAuthManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PersistentAuthManager" 
        private const val PREFS_NAME = "air_monitor_auth"
        private const val KEY_AUTO_LOGIN = "auto_login_enabled"
        private const val KEY_LAST_LOGIN = "last_login_time"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_FIRST_TIME_USER = "first_time_user"
        
        // Configuración de seguridad
        private const val SESSION_VALIDITY_DAYS = 30 // 30 días de sesión válida
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * ✅ VERIFICAR SI DEBE HACER AUTO-LOGIN
     * 
     * Casos donde SÍ hace auto-login:
     * - Usuario ya logueado antes
     * - Sesión no expirada (30 días)
     * - Token Firebase válido
     * - Auto-login habilitado
     */
    fun shouldAutoLogin(): Boolean {
        try {
            // 1. Verificar si auto-login está habilitado
            if (!prefs.getBoolean(KEY_AUTO_LOGIN, true)) {
                Log.d(TAG, "🔒 Auto-login deshabilitado por usuario")
                return false
            }
            
            // 2. Verificar si hay usuario actual de Firebase
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "❌ No hay usuario Firebase actual")
                return false
            }
            
            // 3. Verificar validez de la sesión por tiempo
            val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0)
            val now = System.currentTimeMillis()
            val daysSinceLogin = (now - lastLogin) / MILLIS_PER_DAY
            
            if (daysSinceLogin > SESSION_VALIDITY_DAYS) {
                Log.d(TAG, "⏰ Sesión expirada ($daysSinceLogin días)")
                signOut() // Auto-limpiar sesión expirada
                return false
            }
            
            // 4. Verificar email coincide con guardado
            val savedEmail = prefs.getString(KEY_USER_EMAIL, "")
            if (currentUser.email != savedEmail) {
                Log.w(TAG, "⚠️ Email no coincide: ${currentUser.email} vs $savedEmail")
                return false
            }
            
            Log.d(TAG, "✅ Auto-login válido para: ${currentUser.email}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error verificando auto-login: ${e.message}")
            return false
        }
    }
    
    /**
     * 💾 GUARDAR SESIÓN EXITOSA
     * Se llama después de login manual exitoso
     */
    fun saveSuccessfulLogin(user: FirebaseUser) {
        try {
            prefs.edit().apply {
                putBoolean(KEY_AUTO_LOGIN, true)
                putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                putString(KEY_USER_EMAIL, user.email)
                putBoolean(KEY_FIRST_TIME_USER, false)
                apply()
            }
            
            Log.d(TAG, "💾 Sesión guardada para: ${user.email}")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error guardando sesión: ${e.message}")
        }
    }
    
    /**
     * 🚪 CERRAR SESIÓN COMPLETA
     * Limpia Firebase + SharedPreferences
     */
    fun signOut() {
        try {
            // Limpiar Firebase
            firebaseAuth.signOut()
            
            // Limpiar preferencias (mantener configuraciones)
            prefs.edit().apply {
                putBoolean(KEY_AUTO_LOGIN, false)
                remove(KEY_LAST_LOGIN)
                remove(KEY_USER_EMAIL)
                apply()
            }
            
            Log.d(TAG, "🚪 Sesión cerrada completamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error cerrando sesión: ${e.message}")
        }
    }
    
    /**
     * 🆘 RESET DE EMERGENCIA - Limpia ABSOLUTAMENTE TODO
     * Usado cuando hay problemas de autenticación persistente
     */
    fun emergencyReset() {
        try {
            Log.d(TAG, "🆘 Ejecutando reset de emergencia...")
            
            // 1. Limpiar Firebase completamente
            firebaseAuth.signOut()
            
            // 2. Limpiar TODAS las preferencias de autenticación
            prefs.edit().clear().apply()
            
            // 3. Reset a estado inicial
            prefs.edit().apply {
                putBoolean(KEY_FIRST_TIME_USER, true)
                putBoolean(KEY_AUTO_LOGIN, true) // Valor por defecto
                apply()
            }
            
            Log.d(TAG, "✅ Reset de emergencia completado - Estado limpio")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en reset de emergencia: ${e.message}")
        }
    }
    
    /**
     * ⚙️ CONFIGURAR AUTO-LOGIN
     * Permite al usuario habilitar/deshabilitar
     */
    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
        Log.d(TAG, "⚙️ Auto-login ${if (enabled) "habilitado" else "deshabilitado"}")
    }
    
    /**
     * 📊 OBTENER INFORMACIÓN DE SESIÓN
     */
    fun getSessionInfo(): SessionInfo {
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0)
        val userEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val autoLoginEnabled = prefs.getBoolean(KEY_AUTO_LOGIN, true)
        val daysSinceLogin = if (lastLogin > 0) {
            (System.currentTimeMillis() - lastLogin) / MILLIS_PER_DAY
        } else 0
        
        return SessionInfo(
            isLoggedIn = firebaseAuth.currentUser != null,
            userEmail = userEmail,
            autoLoginEnabled = autoLoginEnabled,
            lastLoginDays = daysSinceLogin.toInt(),
            sessionValid = daysSinceLogin <= SESSION_VALIDITY_DAYS
        )
    }
    
    /**
     * 🆕 VERIFICAR SI ES PRIMERA VEZ
     */
    fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME_USER, true)
    }
    
    /**
     * 🔄 REFRESCAR TIMESTAMP DE SESIÓN
     * Actualiza última actividad (llamar periódicamente)
     */
    fun refreshSession() {
        if (firebaseAuth.currentUser != null) {
            prefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
        }
    }
}

/**
 * 📊 DATA CLASS para información de sesión
 */
data class SessionInfo(
    val isLoggedIn: Boolean,
    val userEmail: String,
    val autoLoginEnabled: Boolean,
    val lastLoginDays: Int,
    val sessionValid: Boolean
)