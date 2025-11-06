package com.ti3042.airmonitor.feature.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ti3042.airmonitor.feature.auth.databinding.ActivityAuthBinding

/**
 * 🏠 Activity contenedora para el módulo de autenticación
 * Aloja los fragments de login/registro
 * 
 * **Módulo**: :feature:auth
 * **Propósito**: Activity principal para flujo de autenticación
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupEmergencyButton()
        
        // Solo agregar fragment si no existe uno
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, LoginFragment())
                .commit()
        }
    }
    
    /**
     * 🆘 SOLUCIÓN 2: Configurar botón de emergencia para casos de lockeo
     */
    private fun setupEmergencyButton() {
        val isChangeAccount = intent.getBooleanExtra("change_account", false)
        val forceFreshLogin = intent.getBooleanExtra("force_fresh_login", false)
        
        // Mostrar botón de emergencia solo si es cambio de cuenta
        if (isChangeAccount || forceFreshLogin) {
            binding.emergencyResetBtn.visibility = View.VISIBLE
            Log.d("AuthActivity", "🆘 Botón de emergencia activado para cambio de cuenta")
        }
        
        binding.emergencyResetBtn.setOnClickListener {
            performEmergencyReset()
        }
    }
    
    /**
     * 🚨 Reset de emergencia - Limpia TODA la autenticación y permite login fresco
     */
    private fun performEmergencyReset() {
        try {
            Log.d("AuthActivity", "🚨 Ejecutando reset de emergencia...")
            Toast.makeText(this, "🚨 Realizando reset de emergencia...", Toast.LENGTH_SHORT).show()
            
            // 1. Limpiar SharedPreferences del PersistentAuthManager
            val authPrefs = getSharedPreferences("air_monitor_auth", Context.MODE_PRIVATE)
            authPrefs.edit().clear().apply()
            
            // 2. Limpiar preferencias adicionales que puedan causar conflictos
            val legacyPrefs = getSharedPreferences("persistent_auth", Context.MODE_PRIVATE)
            legacyPrefs.edit().clear().apply()
            
            // 3. Limpiar cache de Firebase Auth (si existe)
            try {
                val firebasePrefs = getSharedPreferences("com.google.firebase.auth", Context.MODE_PRIVATE)
                firebasePrefs.edit().clear().apply()
            } catch (e: Exception) {
                Log.w("AuthActivity", "No se pudieron limpiar prefs de Firebase: ${e.message}")
            }
            
            // 4. Restaurar valores por defecto seguros
            authPrefs.edit().apply {
                putBoolean("first_time_user", true)
                putBoolean("auto_login_enabled", true)
                apply()
            }
            
            // 5. Ocultar el botón de emergencia
            binding.emergencyResetBtn.visibility = View.GONE
            
            // 6. Recargar el LoginFragment completamente
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, LoginFragment())
                .commitNow() // Commit inmediato
            
            Toast.makeText(this, "✅ Reset completo. Intenta hacer login nuevamente", Toast.LENGTH_LONG).show()
            Log.d("AuthActivity", "✅ Reset de emergencia completado - Estado limpio")
            
        } catch (e: Exception) {
            Log.e("AuthActivity", "❌ Error en reset de emergencia: ${e.message}")
            Toast.makeText(this, "❌ Error en reset. Cierra y abre la app.", Toast.LENGTH_LONG).show()
        }
    }
    
    companion object {
        /**
         * 🎯 Factory method para crear Intent hacia AuthActivity
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }
}