package com.ti3042.airmonitor.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ti3042.airmonitor.R

/**
 * ⚙️ SettingsFragment - Configuración TI3042
 * 
 * Pantalla para configurar:
 * - Umbrales de alerta PPM personalizables
 * - Configuraciones de notificaciones
 * - Settings de hardware ESP32
 * - Información de la aplicación
 */
class SettingsManagerFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsFragment"
        
        // SharedPreferences keys
        private const val PREFS_NAME = "AirMonitorSettings"
        private const val KEY_THRESHOLD_MODERATE = "threshold_moderate"
        private const val KEY_THRESHOLD_POOR = "threshold_poor" 
        private const val KEY_THRESHOLD_CRITICAL = "threshold_critical"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_NOTIFICATION_COOLDOWN = "notification_cooldown"
        private const val KEY_USE_MOCK_DATA = "use_mock_data"
        private const val KEY_AUTO_FAN_CONTROL = "auto_fan_control"
        private const val KEY_UPDATE_FREQUENCY = "update_frequency"
        
        fun newInstance(): SettingsManagerFragment {
            return SettingsManagerFragment()
        }
    }

    // UI References
    private var btnBack: TextView? = null
    
    // Threshold values
    private var thresholdModerateValue: TextView? = null
    private var thresholdPoorValue: TextView? = null
    private var thresholdCriticalValue: TextView? = null
    
    // Notification switches
    private var switchNotificationsEnabled: Switch? = null
    private var switchNotificationSound: Switch? = null
    private var switchVibration: Switch? = null
    private var notificationCooldownValue: TextView? = null
    
    // Hardware switches
    private var switchUseMockData: Switch? = null
    private var switchAutoFanControl: Switch? = null
    private var updateFrequencyValue: TextView? = null
    
    // Reset button
    private var btnResetSettings: TextView? = null
    
    // Current settings values
    private var thresholdModerate = 150
    private var thresholdPoor = 250
    private var thresholdCritical = 400
    private var notificationCooldown = 30 // seconds
    private var updateFrequency = 3 // seconds

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "Creating SettingsFragment view")
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        loadSettings()
        setupListeners()
        
        Log.d(TAG, "✅ SettingsFragment initialized successfully")
    }

    private fun initViews(view: View) {
        // Navigation
        btnBack = view.findViewById(R.id.btn_back_settings)
        
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
        updateFrequencyValue = view.findViewById(R.id.update_frequency_value)
        
        // Reset button
        btnResetSettings = view.findViewById(R.id.btn_reset_settings)
        
        Log.d(TAG, "Views initialized")
    }

    private fun setupListeners() {
        // Back button
        btnBack?.setOnClickListener {
            Log.d(TAG, "Back button pressed")
            saveSettings() // Auto-save on exit
            parentFragmentManager.popBackStack()
        }
        
        // Threshold editors
        thresholdModerateValue?.setOnClickListener { editThreshold("moderate") }
        thresholdPoorValue?.setOnClickListener { editThreshold("poor") }
        thresholdCriticalValue?.setOnClickListener { editThreshold("critical") }
        
        // Notification cooldown editor
        notificationCooldownValue?.setOnClickListener { editNotificationCooldown() }
        
        // Update frequency editor
        updateFrequencyValue?.setOnClickListener { editUpdateFrequency() }
        
        // Switch listeners for auto-save
        setupSwitchListeners()
        
        // Reset settings
        btnResetSettings?.setOnClickListener { resetToDefaults() }
        
        Log.d(TAG, "Listeners configured")
    }

    private fun setupSwitchListeners() {
        switchNotificationsEnabled?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Notifications enabled: $isChecked")
            saveSettings()
        }
        
        switchNotificationSound?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Notification sound: $isChecked")
            saveSettings()
        }
        
        switchVibration?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Vibration: $isChecked")
            saveSettings()
        }
        
        switchUseMockData?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Use mock data: $isChecked")
            saveSettings()
            
            // Show restart suggestion
            if (isChecked != getCurrentMockDataSetting()) {
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Cambio requiere reinicio de la app para aplicarse",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
        
        switchAutoFanControl?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Auto fan control: $isChecked")
            saveSettings()
        }
    }

    /**
     * 📊 Editar umbrales de PPM
     */
    private fun editThreshold(type: String) {
        val context = this.context ?: return
        
        val currentValue = when (type) {
            "moderate" -> thresholdModerate
            "poor" -> thresholdPoor
            "critical" -> thresholdCritical
            else -> 0
        }
        
        Log.d(TAG, "Editing $type threshold, current: $currentValue")
        
        // TODO: Implementar diálogo de edición numérica
        // Por ahora, ciclar entre valores predefinidos
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
        
        android.widget.Toast.makeText(
            context,
            "🎯 Umbral $type actualizado: $newValue PPM",
            android.widget.Toast.LENGTH_SHORT
        ).show()
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
        val options = listOf(10, 15, 30, 45, 60, 90, 120) // seconds
        notificationCooldown = cycleThroughValues(notificationCooldown, options)
        notificationCooldownValue?.text = "$notificationCooldown seg"
        
        saveSettings()
        
        android.widget.Toast.makeText(
            context,
            "⏰ Cooldown actualizado: $notificationCooldown segundos",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 🔄 Editar frecuencia de actualización
     */
    private fun editUpdateFrequency() {
        val options = listOf(1, 2, 3, 5, 10, 15, 30) // seconds
        updateFrequency = cycleThroughValues(updateFrequency, options)
        updateFrequencyValue?.text = "$updateFrequency seg"
        
        saveSettings()
        
        android.widget.Toast.makeText(
            context,
            "🔄 Frecuencia actualizada: $updateFrequency segundos",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 💾 Cargar configuraciones guardadas
     */
    private fun loadSettings() {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Load threshold values
            thresholdModerate = prefs.getInt(KEY_THRESHOLD_MODERATE, 150)
            thresholdPoor = prefs.getInt(KEY_THRESHOLD_POOR, 250)
            thresholdCritical = prefs.getInt(KEY_THRESHOLD_CRITICAL, 400)
            
            // Load notification settings
            val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            val notificationSound = prefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
            val vibration = prefs.getBoolean(KEY_VIBRATION, true)
            notificationCooldown = prefs.getInt(KEY_NOTIFICATION_COOLDOWN, 30)
            
            // Load hardware settings
            val useMockData = prefs.getBoolean(KEY_USE_MOCK_DATA, true)
            val autoFanControl = prefs.getBoolean(KEY_AUTO_FAN_CONTROL, true)
            updateFrequency = prefs.getInt(KEY_UPDATE_FREQUENCY, 3)
            
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
            updateFrequencyValue?.text = "$updateFrequency seg"
            
            Log.d(TAG, "✅ Settings loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading settings: ${e.message}")
        }
    }

    /**
     * 💾 Guardar configuraciones actuales
     */
    private fun saveSettings() {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Save threshold values
            editor.putInt(KEY_THRESHOLD_MODERATE, thresholdModerate)
            editor.putInt(KEY_THRESHOLD_POOR, thresholdPoor)
            editor.putInt(KEY_THRESHOLD_CRITICAL, thresholdCritical)
            
            // Save notification settings
            editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, switchNotificationsEnabled?.isChecked ?: true)
            editor.putBoolean(KEY_NOTIFICATION_SOUND, switchNotificationSound?.isChecked ?: true)
            editor.putBoolean(KEY_VIBRATION, switchVibration?.isChecked ?: true)
            editor.putInt(KEY_NOTIFICATION_COOLDOWN, notificationCooldown)
            
            // Save hardware settings
            editor.putBoolean(KEY_USE_MOCK_DATA, switchUseMockData?.isChecked ?: true)
            editor.putBoolean(KEY_AUTO_FAN_CONTROL, switchAutoFanControl?.isChecked ?: true)
            editor.putInt(KEY_UPDATE_FREQUENCY, updateFrequency)
            
            editor.apply()
            Log.d(TAG, "✅ Settings saved successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving settings: ${e.message}")
        }
    }

    /**
     * 🔄 Restablecer a valores por defecto
     */
    private fun resetToDefaults() {
        Log.d(TAG, "Resetting settings to defaults")
        
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
        updateFrequencyValue?.text = "$updateFrequency seg"
        
        // Save defaults
        saveSettings()
        
        android.widget.Toast.makeText(
            context,
            "🔄 Configuración restablecida a valores por defecto",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        Log.d(TAG, "✅ Settings reset completed")
    }

    /**
     * 🔍 Obtener configuración actual de mock data
     */
    private fun getCurrentMockDataSetting(): Boolean {
        // TODO: Integrar con MockDataService.USE_MOCK_DATA
        return true
    }

    /**
     * 📊 Getters públicos para usar en otras partes de la app
     */
    fun getThresholds(): Triple<Int, Int, Int> {
        return Triple(thresholdModerate, thresholdPoor, thresholdCritical)
    }

    fun getNotificationCooldown(): Int {
        return notificationCooldown
    }

    fun isNotificationsEnabled(): Boolean {
        return switchNotificationsEnabled?.isChecked ?: true
    }
}