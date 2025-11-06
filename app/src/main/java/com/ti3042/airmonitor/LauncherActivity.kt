package com.ti3042.airmonitor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ti3042.airmonitor.feature.auth.AuthActivity
import com.ti3042.airmonitor.auth.PersistentAuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🚀 Launcher Activity - Punto de entrada con AUTH PERSISTENTE
 * Coordina el flujo inicial y decide si hacer auto-login o mostrar login
 * 
 * **NUEVO**: Sistema de login persistente inteligente
 * - Login solo UNA VEZ (30 días de validez)
 * - Auto-login al abrir app
 * - Verificación de token válido
 */
class LauncherActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LauncherActivity"
        private const val SPLASH_DURATION = 1500L // 1.5 segundos
    }
    
    private lateinit var authManager: PersistentAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 Launcher started with PERSISTENT AUTH")
        
        authManager = PersistentAuthManager(this)
        
        // Mostrar splash screen y determinar flujo inicial
        lifecycleScope.launch {
            delay(SPLASH_DURATION)
            determineInitialFlow()
        }
    }
    
    /**
     * 🎯 Determina el flujo inicial con LÓGICA INTELIGENTE
     */
    private fun determineInitialFlow() {
        try {
            Log.d(TAG, "🔍 Verificando estado de autenticación...")
            
            if (authManager.shouldAutoLogin()) {
                // ✅ AUTO-LOGIN: Usuario ya autenticado, ir directo al dashboard
                val sessionInfo = authManager.getSessionInfo()
                Log.d(TAG, "✅ AUTO-LOGIN para: ${sessionInfo.userEmail} (${sessionInfo.lastLoginDays} días)")
                
                authManager.refreshSession() // Actualizar timestamp
                navigateToMain()
                
            } else {
                // 🔐 REQUIERE LOGIN: Primera vez o sesión expirada
                val sessionInfo = authManager.getSessionInfo()
                val reason = when {
                    !sessionInfo.isLoggedIn -> "no hay usuario"
                    !sessionInfo.sessionValid -> "sesión expirada (${sessionInfo.lastLoginDays} días)" 
                    !sessionInfo.autoLoginEnabled -> "auto-login deshabilitado"
                    else -> "verificación fallida"
                }
                
                Log.d(TAG, "🔐 LOGIN REQUERIDO: $reason")
                navigateToAuth()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error determining initial flow: ${e.message}", e)
            navigateToAuth() // Fallback seguro
        }
    }
    
    /**
     * 🔐 Navegar al módulo de autenticación
     */
    private fun navigateToAuth() {
        Log.d(TAG, "🔐 Navigating to Auth module")
        
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
        
        // Animación de transición
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
    
    /**
     * 📱 Navegar al dashboard principal (AUTO-LOGIN exitoso)
     */
    private fun navigateToMain() {
        Log.d(TAG, "📱 AUTO-LOGIN exitoso -> Main Dashboard")
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("auth_source", "auto_login")
            putExtra("user_email", authManager.getSessionInfo().userEmail)
        }
        startActivity(intent)
        finish()
        
        overridePendingTransition(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )
    }
}