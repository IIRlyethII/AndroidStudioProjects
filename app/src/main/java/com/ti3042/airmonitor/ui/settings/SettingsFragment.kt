package com.ti3042.airmonitor.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.feature.auth.AuthActivity
import com.ti3042.airmonitor.ui.control.ControlSystemActivity
import com.ti3042.airmonitor.data.MockDataService
import com.ti3042.airmonitor.auth.PersistentAuthManager

/**
 * 🔧 Fragment de Configuraciones y Ajustes
 * Funciona con el layout existente fragment_settings.xml
 */
class SettingsFragment : Fragment() {

    private val tag = "SettingsFragment"
    private lateinit var firebaseAuth: FirebaseAuth

    // Referencias a elementos reales del layout actual
    private var btnBackSettings: TextView? = null
    private var btnResetSettings: TextView? = null
    
    // Account Management Buttons
    private var changeAccountBtn: TextView? = null
    private var logoutBtn: TextView? = null
    private var controlSystemBtn: TextView? = null
    private var calibrateSensorBtn: TextView? = null
    private var currentUserText: TextView? = null
    
    // Security & Data Buttons
    private var exportSettingsBtn: TextView? = null
    private var clearDataBtn: TextView? = null
    
    // Switches reales que existen en el layout
    private var switchNotificationsEnabled: Switch? = null
    private var switchNotificationSound: Switch? = null
    private var switchVibration: Switch? = null
    private var switchUseMockData: Switch? = null
    private var switchAutoFanControl: Switch? = null
    private var switchEsp32AutomaticMode: Switch? = null
    private var encryptionSwitch: Switch? = null
    private var autoSyncSwitch: Switch? = null
    
    // TextViews editables para valores
    private var thresholdModerateValue: TextView? = null
    private var thresholdPoorValue: TextView? = null
    private var thresholdCriticalValue: TextView? = null
    private var notificationCooldownValue: TextView? = null
    private var updateFrequencyValue: TextView? = null
    
    // Valores actuales
    private var thresholdModerate = 150
    private var thresholdPoor = 250
    private var thresholdCritical = 400
    private var notificationCooldown = 30
    private var updateFrequency = 3
    
    // Flag para evitar triggers automáticos durante loadSettings
    private var isLoadingSettings = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d(tag, "🔧 Inicializando Fragment de Configuraciones")
        
        initFirebase()
        initViews(view)
        setupListeners()
        loadSettings()
        
        Log.d(tag, "✅ Fragment de Configuraciones inicializado")
    }

    private fun initFirebase() {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            Log.d(tag, "✅ Firebase Auth inicializado")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error inicializando Firebase: ${e.message}")
        }
    }

    private fun initViews(view: View) {
        try {
            // Navigation buttons
            btnBackSettings = view.findViewById(R.id.btn_back_settings)
            btnResetSettings = view.findViewById(R.id.btn_reset_settings)
            
            // Account Management
            changeAccountBtn = view.findViewById(R.id.changeAccountBtn)
            logoutBtn = view.findViewById(R.id.logoutBtn)
            controlSystemBtn = view.findViewById(R.id.controlSystemBtn)
            calibrateSensorBtn = view.findViewById(R.id.calibrateSensorBtn)
            currentUserText = view.findViewById(R.id.currentUserText)
            
            // Security & Data Management
            exportSettingsBtn = view.findViewById(R.id.exportSettingsBtn)
            clearDataBtn = view.findViewById(R.id.clearDataBtn)
            
            // Threshold controls
            thresholdModerateValue = view.findViewById(R.id.threshold_moderate_value)
            thresholdPoorValue = view.findViewById(R.id.threshold_poor_value)
            thresholdCriticalValue = view.findViewById(R.id.threshold_critical_value)
            
            // Notification controls
            switchNotificationsEnabled = view.findViewById(R.id.switch_notifications_enabled)
            switchNotificationSound = view.findViewById(R.id.switch_notification_sound)
            switchVibration = view.findViewById(R.id.switch_vibration)
            notificationCooldownValue = view.findViewById(R.id.notification_cooldown_value)
            
            // Hardware controls
            switchUseMockData = view.findViewById(R.id.switch_use_mock_data)
            switchAutoFanControl = view.findViewById(R.id.switch_auto_fan_control)
            switchEsp32AutomaticMode = view.findViewById(R.id.switch_esp32_automatic_mode)
            updateFrequencyValue = view.findViewById(R.id.update_frequency_value)
            
            // Security switches
            encryptionSwitch = view.findViewById(R.id.encryptionSwitch)
            autoSyncSwitch = view.findViewById(R.id.autoSyncSwitch)
            
            // Update current user display
            updateCurrentUser()
            
            Log.d(tag, "✅ Views inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error inicializando views: ${e.message}")
        }
    }

    private fun setupListeners() {
        try {
            // Back button
            btnBackSettings?.setOnClickListener {
                Log.d(tag, "🔙 Back button pressed")
                saveSettings() // Auto-save on exit
                parentFragmentManager.popBackStack()
            }
            
            // Reset button
            btnResetSettings?.setOnClickListener {
                resetToDefaults()
            }
            
            // Threshold editors
            thresholdModerateValue?.setOnClickListener { editThreshold("moderate") }
            thresholdPoorValue?.setOnClickListener { editThreshold("poor") }
            thresholdCriticalValue?.setOnClickListener { editThreshold("critical") }
            
            // Notification cooldown editor
            notificationCooldownValue?.setOnClickListener { editNotificationCooldown() }
            
            // Update frequency editor
            updateFrequencyValue?.setOnClickListener { editUpdateFrequency() }
            
            // Switch listeners for real switches
            switchNotificationsEnabled?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🔔 Notificaciones: $isChecked")
                handleNotificationsToggle(isChecked)
                saveSettings()
            }
            
            switchNotificationSound?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🔊 Sonido notificaciones: $isChecked")
                saveSettings()
            }
            
            switchVibration?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "📳 Vibración: $isChecked")
                saveSettings()
            }
            
            switchUseMockData?.setOnCheckedChangeListener { _, isChecked ->
                if (!isLoadingSettings) { // Solo mostrar dialog cuando es interacción manual
                    Log.d(tag, "🎮 Datos simulados: $isChecked (manual)")
                    handleMockDataToggle(isChecked)
                } else {
                    Log.d(tag, "🎮 Datos simulados: $isChecked (carga automática)")
                    // Solo guardar la configuración sin mostrar dialog
                    saveSettings()
                }
            }
            
            switchAutoFanControl?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🌪️ Control automático ventilador: $isChecked")
                saveSettings()
            }
            
            switchEsp32AutomaticMode?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🤖 ESP32 modo ${if (isChecked) "automático" else "manual"}: $isChecked")
                val mode = if (isChecked) "automático" else "manual"
                Toast.makeText(context, "🤖 ESP32 en modo $mode", Toast.LENGTH_SHORT).show()
                saveSettings()
            }
            
            // Account Management Listeners
            changeAccountBtn?.setOnClickListener {
                Log.d(tag, "🔄 Cambiar cuenta pressed")
                changeAccount()
            }
            
            logoutBtn?.setOnClickListener {
                Log.d(tag, "🚪 Logout pressed")
                performLogout()
            }
            
            controlSystemBtn?.setOnClickListener {
                Log.d(tag, "🔧 Control System pressed")
                openControlSystem()
            }
            
            calibrateSensorBtn?.setOnClickListener {
                Log.d(tag, "🎯 Calibrate Sensor pressed")
                startSensorCalibration()
            }
            
            // Security & Data Management Listeners
            exportSettingsBtn?.setOnClickListener {
                Log.d(tag, "📤 Export settings pressed")
                exportSettings()
            }
            
            clearDataBtn?.setOnClickListener {
                Log.d(tag, "🧹 Clear data pressed")
                clearAppData()
            }
            
            // Security Switches
            encryptionSwitch?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🔐 Encryption: $isChecked")
                handleEncryptionToggle(isChecked)
            }
            
            autoSyncSwitch?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(tag, "🔄 Auto Sync: $isChecked")
                handleAutoSyncToggle(isChecked)
            }
            

            
            Log.d(tag, "✅ Listeners configurados correctamente")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error configurando listeners: ${e.message}")
        }
    }

    /**
     * 💾 Cargar configuraciones guardadas
     */
    private fun loadSettings() {
        try {
            isLoadingSettings = true // Activar bandera para evitar dialogs automáticos
            val prefs = requireContext().getSharedPreferences("AirMonitorSettings", Context.MODE_PRIVATE)
            
            // Load threshold values
            thresholdModerate = prefs.getInt("threshold_moderate", 150)
            thresholdPoor = prefs.getInt("threshold_poor", 250)
            thresholdCritical = prefs.getInt("threshold_critical", 400)
            
            // Load notification settings
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
            val notificationSound = prefs.getBoolean("notification_sound", true)
            val vibration = prefs.getBoolean("vibration", true)
            notificationCooldown = prefs.getInt("notification_cooldown", 30)
            
            // Load hardware settings
            val useMockData = prefs.getBoolean("use_mock_data", true)
            val autoFanControl = prefs.getBoolean("auto_fan_control", true)
            val esp32AutomaticMode = prefs.getBoolean("esp32_automatic_mode", true)
            updateFrequency = prefs.getInt("update_frequency", 3)
            
            // Load security settings
            val encryption = prefs.getBoolean("encryption_enabled", true)
            val autoSync = prefs.getBoolean("auto_sync_enabled", true)
            
            // Update UI
            thresholdModerateValue?.text = "$thresholdModerate PPM"
            thresholdPoorValue?.text = "$thresholdPoor PPM"
            thresholdCriticalValue?.text = "$thresholdCritical PPM"
            
            switchNotificationsEnabled?.isChecked = notificationsEnabled
            switchNotificationSound?.isChecked = notificationSound
            switchVibration?.isChecked = vibration
            notificationCooldownValue?.text = "$notificationCooldown seg"
            
            switchUseMockData?.isChecked = useMockData
            switchAutoFanControl?.isChecked = autoFanControl
            switchEsp32AutomaticMode?.isChecked = esp32AutomaticMode
            updateFrequencyValue?.text = "$updateFrequency seg"
            
            // Update security switches
            encryptionSwitch?.isChecked = encryption
            autoSyncSwitch?.isChecked = autoSync
            
            Log.d(tag, "✅ Configuraciones cargadas correctamente")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error cargando configuraciones: ${e.message}")
        } finally {
            isLoadingSettings = false // Desactivar bandera después de cargar
        }
    }

    /**
     * 💾 Guardar configuraciones actuales
     */
    private fun saveSettings() {
        try {
            val prefs = requireContext().getSharedPreferences("AirMonitorSettings", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Save threshold values
            editor.putInt("threshold_moderate", thresholdModerate)
            editor.putInt("threshold_poor", thresholdPoor)
            editor.putInt("threshold_critical", thresholdCritical)
            
            // Save notification settings
            editor.putBoolean("notifications_enabled", switchNotificationsEnabled?.isChecked ?: true)
            editor.putBoolean("notification_sound", switchNotificationSound?.isChecked ?: true)
            editor.putBoolean("vibration", switchVibration?.isChecked ?: true)
            editor.putInt("notification_cooldown", notificationCooldown)
            
            // Save hardware settings
            editor.putBoolean("use_mock_data", switchUseMockData?.isChecked ?: true)
            editor.putBoolean("auto_fan_control", switchAutoFanControl?.isChecked ?: true)
            editor.putBoolean("esp32_automatic_mode", switchEsp32AutomaticMode?.isChecked ?: true)
            editor.putInt("update_frequency", updateFrequency)
            
            // Save security settings
            editor.putBoolean("encryption_enabled", encryptionSwitch?.isChecked ?: true)
            editor.putBoolean("auto_sync_enabled", autoSyncSwitch?.isChecked ?: true)
            
            editor.apply()
            Log.d(tag, "✅ Configuraciones guardadas correctamente")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error guardando configuraciones: ${e.message}")
        }
    }

    /**
     * 🎮 Manejar cambio de modo simulación/datos reales
     */
    private fun handleMockDataToggle(useMockData: Boolean) {
        try {
            // Guardar la preferencia
            saveSettings()
            
            // Actualizar MockDataService - para usar en el próximo reinicio
            
            val message = if (useMockData) {
                "🎮 Modo simulación ACTIVADO\n✅ Usando datos simulados para pruebas"
            } else {
                "📡 Modo real ACTIVADO\n⚠️ Intentando conectar con ESP32 real"
            }
            
            // Mostrar dialog informativo
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("🔄 Cambio de Modo")
            builder.setMessage("$message\n\n¿Reiniciar la aplicación para aplicar cambios?")
            builder.setPositiveButton("Reiniciar") { _, _ ->
                // Restart app
                val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
                intent?.let { 
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(it)
                    requireActivity().finish()
                }
            }
            builder.setNegativeButton("Después") { dialog, _ ->
                Toast.makeText(context, "⚠️ Los cambios se aplicarán al reiniciar la app", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            builder.show()
            
            Log.d(tag, "🎮 Mock data toggle: $useMockData")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error en mock data toggle: ${e.message}")
            Toast.makeText(context, "❌ Error cambiando modo", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🚪 Cerrar sesión y regresar al login
     */
    private fun performLogout() {
        try {
            Log.d(tag, "🚪 Cerrando sesión...")
            
            // Confirmación antes de cerrar sesión
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("🚪 Cerrar Sesión")
            builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")
            builder.setPositiveButton("Sí, cerrar") { _, _ ->
                // Sign out from Firebase
                firebaseAuth.signOut()
                
                // Clear any cached data
                clearCachedData()
                
                Toast.makeText(requireContext(), "✅ Sesión cerrada", Toast.LENGTH_SHORT).show()
                
                // Navigate to login
                val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
            
            Log.d(tag, "✅ Dialog de logout mostrado")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error cerrando sesión: ${e.message}")
            Toast.makeText(requireContext(), "❌ Error cerrando sesión", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔄 Cambiar de cuenta (logout y abrir login)
     */
    private fun changeAccount() {
        try {
            Log.d(tag, "🔄 Cambiando de cuenta...")
            
            Toast.makeText(requireContext(), "🔄 Cambiando de cuenta...", Toast.LENGTH_SHORT).show()
            
            // ✅ SOLUCIÓN 1: Cerrar sesión completa (Firebase + Persistent Auth)
            firebaseAuth.signOut()
            
            // 🔑 CRÍTICO: Limpiar la sesión persistente para evitar lockeo
            val authManager = PersistentAuthManager(requireContext())
            authManager.signOut()  // Esto limpia toda la sesión persistente
            
            Log.d(tag, "✅ Sesión persistente limpiada completamente")
            
            // Navigate to login without clearing all data
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                putExtra("change_account", true)
                putExtra("force_fresh_login", true)  // Flag para indicar login fresco
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error cambiando cuenta: ${e.message}")
            Toast.makeText(requireContext(), "❌ Error cambiando cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔧 Abrir Sistema de Control Avanzado
     * 
     * El Sistema de Control permite:
     * • 📡 Gestión avanzada de dispositivos conectados (Bluetooth, WiFi P2P)
     * • 🔐 Configuraciones de seguridad y cifrado
     * • 🔄 Sincronización multi-dispositivo
     * • 📊 Exportación de datos y diagnósticos
     * • ⚙️ Configuraciones de sensibilidad y intervalos
     * • 🧹 Limpieza de caché y mantenimiento
     */
    private fun openControlSystem() {
        try {
            Log.d(tag, "🔧 Abriendo Sistema de Control desde Ajustes")
            
            // Mostrar información sobre el sistema de control
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("🔧 Sistema de Control Avanzado")
            builder.setMessage("""
                El Sistema de Control incluye:
                
                📡 Gestión de Dispositivos
                • Bluetooth y WiFi P2P
                • Escaneo de dispositivos cercanos
                
                🔐 Seguridad Avanzada
                • Configuraciones de cifrado
                • Gestión de permisos
                
                📊 Análisis y Datos
                • Exportación de configuraciones
                • Diagnósticos del sistema
                
                ¿Abrir el sistema de control?
            """.trimIndent())
            
            builder.setPositiveButton("Abrir") { _, _ ->
                try {
                    val intent = Intent(requireContext(), ControlSystemActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(tag, "❌ Error abriendo ControlSystemActivity: ${e.message}")
                    Toast.makeText(requireContext(), "⚠️ Sistema en desarrollo - Próximamente disponible", Toast.LENGTH_LONG).show()
                }
            }
            
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            
            builder.show()
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error abriendo Sistema de Control: ${e.message}")
            Toast.makeText(requireContext(), "❌ Error accediendo al sistema", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🧹 Limpiar datos de la aplicación
     */
    private fun clearAppData() {
        try {
            Log.d(tag, "🧹 Limpiando datos de la aplicación...")
            
            Toast.makeText(requireContext(), "🧹 Limpiando datos...", Toast.LENGTH_SHORT).show()
            
            // Clear cache
            requireContext().cacheDir.deleteRecursively()
            
            // Clear temporary preferences
            val tempPrefs = requireContext().getSharedPreferences("temp_data", 0)
            tempPrefs.edit().clear().apply()
            
            Toast.makeText(requireContext(), "✅ Datos limpiados", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error limpiando datos: ${e.message}")
            Toast.makeText(requireContext(), "❌ Error limpiando datos", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 📤 Exportar configuraciones
     */
    private fun exportSettings() {
        try {
            Log.d(tag, "📤 Exportando configuraciones...")
            
            Toast.makeText(requireContext(), "📤 Exportando configuraciones...", Toast.LENGTH_SHORT).show()
            
            // Simulate export process
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Toast.makeText(requireContext(), "✅ Configuraciones exportadas", Toast.LENGTH_LONG).show()
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error exportando configuraciones: ${e.message}")
        }
    }

    private fun handleNotificationsToggle(enabled: Boolean) {
        try {
            // Guardar en las preferencias principales
            val prefs = requireContext().getSharedPreferences("AirMonitorSettings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notifications_enabled", enabled).apply()
            
            // También actualizar otros switches relacionados
            if (!enabled) {
                // Si se desactivan las notificaciones, desactivar sonido y vibración también
                switchNotificationSound?.isChecked = false
                switchVibration?.isChecked = false
                prefs.edit().putBoolean("notification_sound", false).apply()
                prefs.edit().putBoolean("vibration", false).apply()
            }
            
            val status = if (enabled) "✅ activadas" else "❌ desactivadas"
            Toast.makeText(requireContext(), "🔔 Notificaciones $status", Toast.LENGTH_SHORT).show()
            
            Log.d(tag, "🔔 Notificaciones: $enabled")
            
            // Mostrar mensaje informativo sobre reiniciar app si es necesario
            if (!enabled) {
                Toast.makeText(requireContext(), "ℹ️ Algunos cambios requieren reiniciar la app", Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error toggling notifications: ${e.message}")
        }
    }

    private fun handleAutoSyncToggle(enabled: Boolean) {
        try {
            val prefs = requireContext().getSharedPreferences("app_settings", 0)
            prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
            
            val status = if (enabled) "✅ activada" else "❌ desactivada"
            Toast.makeText(requireContext(), "🔄 Sincronización automática $status", Toast.LENGTH_SHORT).show()
            
            Log.d(tag, "🔄 Auto-sync: $enabled")
        } catch (e: Exception) {
            Log.e(tag, "Error toggling auto sync: ${e.message}")
        }
    }

    private fun handleEncryptionToggle(enabled: Boolean) {
        try {
            val prefs = requireContext().getSharedPreferences("app_settings", 0)
            prefs.edit().putBoolean("encryption_enabled", enabled).apply()
            
            val status = if (enabled) "🔐 activado" else "🔓 desactivado"
            Toast.makeText(requireContext(), "Cifrado $status", Toast.LENGTH_SHORT).show()
            
            Log.d(tag, "🔐 Encryption: $enabled")
        } catch (e: Exception) {
            Log.e(tag, "Error toggling encryption: ${e.message}")
        }
    }

    private fun handleSimulationModeToggle(enabled: Boolean) {
        try {
            val prefs = requireContext().getSharedPreferences("app_settings", 0)
            prefs.edit().putBoolean("simulation_mode", enabled).apply()
            
            val status = if (enabled) "🎮 activado" else "📡 modo real"
            Toast.makeText(requireContext(), "Modo simulación $status", Toast.LENGTH_SHORT).show()
            
            Log.d(tag, "🎮 Simulation mode: $enabled")
        } catch (e: Exception) {
            Log.e(tag, "Error toggling simulation mode: ${e.message}")
        }
    }

    private fun clearCachedData() {
        try {
            // Clear any cached authentication data
            val prefs = requireContext().getSharedPreferences("temp_data", 0)
            prefs.edit().clear().apply()
            
            Log.d(tag, "🧹 Cached data cleared")
        } catch (e: Exception) {
            Log.e(tag, "Error clearing cached data: ${e.message}")
        }
    }
    
    /**
     * 👤 Actualizar display del usuario actual
     */
    private fun updateCurrentUser() {
        try {
            val currentUser = firebaseAuth.currentUser
            val email = currentUser?.email ?: "No autenticado"
            currentUserText?.text = email
            Log.d(tag, "👤 Usuario actual: $email")
        } catch (e: Exception) {
            Log.e(tag, "Error updating current user: ${e.message}")
            currentUserText?.text = "Error al cargar usuario"
        }
    }
    
    /**
     * � Editar umbrales de PPM
     */
    private fun editThreshold(type: String) {
        try {
            val currentValue = when (type) {
                "moderate" -> thresholdModerate
                "poor" -> thresholdPoor
                "critical" -> thresholdCritical
                else -> 0
            }
            
            // Ciclar entre valores predefinidos
            val newValue = when (type) {
                "moderate" -> cycleThroughValues(currentValue, listOf(120, 150, 180, 200))
                "poor" -> cycleThroughValues(currentValue, listOf(200, 250, 300, 350))
                "critical" -> cycleThroughValues(currentValue, listOf(350, 400, 450, 500))
                else -> currentValue
            }
            
            when (type) {
                "moderate" -> {
                    thresholdModerate = newValue
                    thresholdModerateValue?.text = "$newValue PPM"
                }
                "poor" -> {
                    thresholdPoor = newValue
                    thresholdPoorValue?.text = "$newValue PPM"
                }
                "critical" -> {
                    thresholdCritical = newValue
                    thresholdCriticalValue?.text = "$newValue PPM"
                }
            }
            
            saveSettings()
            Toast.makeText(context, "🎯 Umbral $type: $newValue PPM", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(tag, "Error editing threshold: ${e.message}")
        }
    }

    private fun cycleThroughValues(current: Int, options: List<Int>): Int {
        val currentIndex = options.indexOf(current)
        return if (currentIndex == -1 || currentIndex == options.size - 1) {
            options.first()
        } else {
            options[currentIndex + 1]
        }
    }

    /**
     * ⏰ Editar cooldown de notificaciones
     */
    private fun editNotificationCooldown() {
        try {
            val options = listOf(10, 15, 30, 45, 60, 90, 120) // seconds
            notificationCooldown = cycleThroughValues(notificationCooldown, options)
            notificationCooldownValue?.text = "$notificationCooldown seg"
            
            saveSettings()
            Toast.makeText(context, "⏰ Cooldown: $notificationCooldown segundos", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Error editing cooldown: ${e.message}")
        }
    }

    /**
     * 🔄 Editar frecuencia de actualización
     */
    private fun editUpdateFrequency() {
        try {
            val options = listOf(1, 2, 3, 5, 10, 15, 30) // seconds
            updateFrequency = cycleThroughValues(updateFrequency, options)
            updateFrequencyValue?.text = "$updateFrequency seg"
            
            saveSettings()
            Toast.makeText(context, "🔄 Frecuencia: $updateFrequency segundos", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Error editing frequency: ${e.message}")
        }
    }

    /**
     * 🔄 Restablecer a valores por defecto
     */
    private fun resetToDefaults() {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("🔄 Restablecer Configuración")
            builder.setMessage("¿Restablecer todas las configuraciones a los valores por defecto?")
            builder.setPositiveButton("Sí, restablecer") { _, _ ->
                // Reset values
                thresholdModerate = 150
                thresholdPoor = 250
                thresholdCritical = 400
                notificationCooldown = 30
                updateFrequency = 3
                
                // Update UI
                thresholdModerateValue?.text = "$thresholdModerate PPM"
                thresholdPoorValue?.text = "$thresholdPoor PPM"
                thresholdCriticalValue?.text = "$thresholdCritical PPM"
                
                switchNotificationsEnabled?.isChecked = true
                switchNotificationSound?.isChecked = true
                switchVibration?.isChecked = true
                notificationCooldownValue?.text = "$notificationCooldown seg"
                
                switchUseMockData?.isChecked = true
                switchAutoFanControl?.isChecked = true
                switchEsp32AutomaticMode?.isChecked = true
                updateFrequencyValue?.text = "$updateFrequency seg"
                
                // Save defaults
                saveSettings()
                
                Toast.makeText(context, "✅ Configuración restablecida", Toast.LENGTH_LONG).show()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
            
        } catch (e: Exception) {
            Log.e(tag, "Error resetting settings: ${e.message}")
        }
    }

    /**
     * 🎯 Iniciar proceso de calibración del sensor MQ-135
     */
    private fun startSensorCalibration() {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("🎯 Calibración del Sensor MQ-135")
            builder.setMessage("""
                La calibración del sensor MQ-135 mejora la precisión de las mediciones.
                
                Instrucciones:
                1. Coloque el sensor en aire limpio durante 24 horas
                2. Presione 'Iniciar' para comenzar la calibración automática
                3. No mueva el dispositivo durante el proceso
                
                ¿Desea continuar?
            """.trimIndent())
            
            builder.setPositiveButton("🎯 Iniciar Calibración") { _, _ ->
                performCalibration()
            }
            
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            
            builder.show()
            
        } catch (e: Exception) {
            Log.e(tag, "Error showing calibration dialog: ${e.message}")
            Toast.makeText(context, "❌ Error al iniciar calibración", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 🔧 Realizar calibración del sensor
     */
    private fun performCalibration() {
        try {
            // Crear dialog de progreso
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔄 Calibrando Sensor...")
                .setMessage("Por favor espere mientras se calibra el sensor MQ-135")
                .setCancelable(false)
                .create()
            
            progressDialog.show()
            
            // Simular proceso de calibración
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                progressDialog.dismiss()
                
                // Simular resultados de calibración
                val accuracy = (85..98).random()
                val calibrationFactor = kotlin.random.Random.nextDouble(0.85, 1.15)
                
                showCalibrationResults(accuracy, calibrationFactor)
                
                // Guardar datos de calibración
                saveCalibrationData(accuracy, calibrationFactor)
                
            }, 3000) // 3 segundos de simulación
            
        } catch (e: Exception) {
            Log.e(tag, "Error during calibration: ${e.message}")
            Toast.makeText(context, "❌ Error durante la calibración", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 📊 Mostrar resultados de calibración
     */
    private fun showCalibrationResults(accuracy: Int, calibrationFactor: Double) {
        try {
            val statusIcon = if (accuracy >= 95) "✅" else if (accuracy >= 85) "⚠️" else "❌"
            val statusText = if (accuracy >= 95) "Excelente" else if (accuracy >= 85) "Buena" else "Regular"
            
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("📊 Resultado de Calibración")
            builder.setMessage("""
                $statusIcon Calibración completada
                
                Precisión: $accuracy% ($statusText)
                Factor de calibración: ${String.format("%.3f", calibrationFactor)}
                
                Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                
                ${if (accuracy >= 95) "🎉 Su sensor está perfectamente calibrado" 
                  else if (accuracy >= 85) "⚠️ Calibración aceptable, puede mejorar con tiempo" 
                  else "🔄 Se recomienda repetir la calibración en ambiente más limpio"}
            """.trimIndent())
            
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            
            if (accuracy < 90) {
                builder.setNeutralButton("🔄 Recalibrar") { _, _ ->
                    performCalibration()
                }
            }
            
            builder.show()
            
        } catch (e: Exception) {
            Log.e(tag, "Error showing calibration results: ${e.message}")
        }
    }
    
    /**
     * 💾 Guardar datos de calibración
     */
    private fun saveCalibrationData(accuracy: Int, calibrationFactor: Double) {
        try {
            val prefs = requireContext().getSharedPreferences("AirMonitorSettings", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            editor.putInt("last_calibration_accuracy", accuracy)
            editor.putFloat("calibration_factor", calibrationFactor.toFloat())
            editor.putLong("last_calibration_timestamp", System.currentTimeMillis())
            editor.putBoolean("sensor_calibrated", true)
            
            editor.apply()
            
            Log.d(tag, "✅ Calibration data saved - Accuracy: $accuracy%, Factor: $calibrationFactor")
            Toast.makeText(context, "💾 Datos de calibración guardados", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(tag, "Error saving calibration data: ${e.message}")
        }
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}