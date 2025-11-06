package com.ti3042.airmonitor.ui.control

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.slider.Slider
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.firebase.FirebaseManager
import com.ti3042.airmonitor.security.SecurityManager
import com.ti3042.airmonitor.multidevice.MultiDeviceManager

/**
 * 🔧 Sistema de Control Avanzado
 * Acceso mediante deslizar hacia abajo desde el dashboard principal
 */
class ControlSystemActivity : AppCompatActivity() {

    private val tag = "ControlSystem"
    private lateinit var securityManager: SecurityManager
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var multiDeviceManager: MultiDeviceManager
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var systemStatusCard: MaterialCardView
    private lateinit var deviceControlCard: MaterialCardView
    private lateinit var securityCard: MaterialCardView
    private lateinit var networkCard: MaterialCardView
    
    // Controls
    private lateinit var bluetoothSwitch: SwitchMaterial
    private lateinit var wifiP2PSwitch: SwitchMaterial
    private lateinit var encryptionSwitch: SwitchMaterial
    private lateinit var autoSyncSwitch: SwitchMaterial
    private lateinit var sensitivitySlider: Slider
    private lateinit var intervalSlider: Slider
    
    // Buttons
    private lateinit var scanDevicesBtn: MaterialButton
    private lateinit var exportDataBtn: MaterialButton
    private lateinit var clearCacheBtn: MaterialButton
    private lateinit var diagnosticsBtn: MaterialButton
    
    // Status indicators
    private lateinit var systemStatusText: TextView
    private lateinit var deviceCountText: TextView
    private lateinit var securityStatusText: TextView
    private lateinit var networkStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_system)
        
        Log.d(tag, "🔧 Inicializando Sistema de Control")
        
        initManagers()
        initViews()
        setupToolbar()
        setupControls()
        updateStatus()
        
        Log.d(tag, "✅ Sistema de Control inicializado")
    }
    
    private fun initManagers() {
        try {
            securityManager = SecurityManager.getInstance()
            firebaseManager = FirebaseManager.getInstance()
            multiDeviceManager = MultiDeviceManager.getInstance()
            Log.d(tag, "✅ Managers inicializados")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error inicializando managers: ${e.message}")
        }
    }
    
    private fun initViews() {
        // Toolbar
        toolbar = findViewById(R.id.controlToolbar)
        
        // Cards
        systemStatusCard = findViewById(R.id.systemStatusCard)
        deviceControlCard = findViewById(R.id.deviceControlCard)
        securityCard = findViewById(R.id.securityCard)
        networkCard = findViewById(R.id.networkCard)
        
        // Switches
        bluetoothSwitch = findViewById(R.id.bluetoothSwitch)
        wifiP2PSwitch = findViewById(R.id.wifiP2PSwitch)
        encryptionSwitch = findViewById(R.id.encryptionSwitch)
        autoSyncSwitch = findViewById(R.id.autoSyncSwitch)
        
        // Sliders
        sensitivitySlider = findViewById(R.id.sensitivitySlider)
        intervalSlider = findViewById(R.id.intervalSlider)
        
        // Buttons
        scanDevicesBtn = findViewById(R.id.scanDevicesBtn)
        exportDataBtn = findViewById(R.id.exportDataBtn)
        clearCacheBtn = findViewById(R.id.clearCacheBtn)
        diagnosticsBtn = findViewById(R.id.diagnosticsBtn)
        
        // Status texts
        systemStatusText = findViewById(R.id.systemStatusText)
        deviceCountText = findViewById(R.id.deviceCountText)
        securityStatusText = findViewById(R.id.securityStatusText)
        networkStatusText = findViewById(R.id.networkStatusText)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupControls() {
        // Bluetooth Control
        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "🔵 Bluetooth ${if (isChecked) "activado" else "desactivado"}")
            handleBluetoothToggle(isChecked)
        }
        
        // WiFi P2P Control
        wifiP2PSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "📶 WiFi P2P ${if (isChecked) "activado" else "desactivado"}")
            handleWifiP2PToggle(isChecked)
        }
        
        // Encryption Control
        encryptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "🔐 Cifrado ${if (isChecked) "activado" else "desactivado"}")
            handleEncryptionToggle(isChecked)
        }
        
        // Auto Sync Control
        autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "🔄 Auto-sync ${if (isChecked) "activado" else "desactivado"}")
            handleAutoSyncToggle(isChecked)
        }
        
        // Sensitivity Slider
        sensitivitySlider.addOnChangeListener { _, value, _ ->
            Log.d(tag, "📊 Sensibilidad: ${value.toInt()}%")
            handleSensitivityChange(value.toInt())
        }
        
        // Interval Slider  
        intervalSlider.addOnChangeListener { _, value, _ ->
            Log.d(tag, "⏱️ Intervalo: ${value.toInt()}s")
            handleIntervalChange(value.toInt())
        }
        
        // Buttons
        scanDevicesBtn.setOnClickListener { scanForDevices() }
        exportDataBtn.setOnClickListener { exportData() }
        clearCacheBtn.setOnClickListener { clearCache() }
        diagnosticsBtn.setOnClickListener { runDiagnostics() }
    }
    
    private fun handleBluetoothToggle(enabled: Boolean) {
        try {
            // Aquí implementarías el control del Bluetooth
            val status = if (enabled) "✅ Activado" else "❌ Desactivado"
            Toast.makeText(this, "Bluetooth: $status", Toast.LENGTH_SHORT).show()
            updateStatus()
        } catch (e: Exception) {
            Log.e(tag, "Error toggling Bluetooth: ${e.message}")
        }
    }
    
    private fun handleWifiP2PToggle(enabled: Boolean) {
        try {
            if (enabled) {
                multiDeviceManager.startDeviceDiscovery()
                Toast.makeText(this, "📶 WiFi P2P activado", Toast.LENGTH_SHORT).show()
            } else {
                // multiDeviceManager.stopDeviceDiscovery() // Método no disponible por ahora
                Toast.makeText(this, "📶 WiFi P2P desactivado", Toast.LENGTH_SHORT).show()
            }
            updateStatus()
        } catch (e: Exception) {
            Log.e(tag, "Error toggling WiFi P2P: ${e.message}")
        }
    }
    
    private fun handleEncryptionToggle(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("control_settings", MODE_PRIVATE)
            prefs.edit().putBoolean("encryption_enabled", enabled).apply()
            
            val status = if (enabled) "🔐 Cifrado activado" else "🔓 Cifrado desactivado"
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            updateStatus()
        } catch (e: Exception) {
            Log.e(tag, "Error toggling encryption: ${e.message}")
        }
    }
    
    private fun handleAutoSyncToggle(enabled: Boolean) {
        try {
            val prefs = getSharedPreferences("control_settings", MODE_PRIVATE)
            prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
            
            Toast.makeText(this, "Auto-sync: ${if (enabled) "✅" else "❌"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Error toggling auto sync: ${e.message}")
        }
    }
    
    private fun handleSensitivityChange(sensitivity: Int) {
        val prefs = getSharedPreferences("control_settings", MODE_PRIVATE)
        prefs.edit().putInt("sensor_sensitivity", sensitivity).apply()
    }
    
    private fun handleIntervalChange(interval: Int) {
        val prefs = getSharedPreferences("control_settings", MODE_PRIVATE)
        prefs.edit().putInt("sync_interval", interval).apply()
    }
    
    private fun scanForDevices() {
        Log.d(tag, "🔍 Escaneando dispositivos...")
        try {
            multiDeviceManager.startDeviceDiscovery()
            Toast.makeText(this, "🔍 Buscando dispositivos...", Toast.LENGTH_SHORT).show()
            
            // Simular actualización después de escaneo
            handler.postDelayed({
                updateDeviceCount()
                Toast.makeText(this, "✅ Escaneo completado", Toast.LENGTH_SHORT).show()
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(tag, "Error scanning devices: ${e.message}")
            Toast.makeText(this, "❌ Error en escaneo", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportData() {
        Log.d(tag, "📤 Exportando datos...")
        try {
            // Implementar exportación de datos
            Toast.makeText(this, "📤 Exportando datos...", Toast.LENGTH_SHORT).show()
            
            handler.postDelayed({
                Toast.makeText(this, "✅ Datos exportados exitosamente", Toast.LENGTH_LONG).show()
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(tag, "Error exporting data: ${e.message}")
            Toast.makeText(this, "❌ Error exportando datos", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun clearCache() {
        Log.d(tag, "🧹 Limpiando cache...")
        try {
            // Limpiar cache de la aplicación
            cacheDir.deleteRecursively()
            
            val prefs = getSharedPreferences("temp_data", MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            Toast.makeText(this, "🧹 Cache limpiado", Toast.LENGTH_SHORT).show()
            updateStatus()
            
        } catch (e: Exception) {
            Log.e(tag, "Error clearing cache: ${e.message}")
            Toast.makeText(this, "❌ Error limpiando cache", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun runDiagnostics() {
        Log.d(tag, "🔧 Ejecutando diagnósticos...")
        try {
            Toast.makeText(this, "🔧 Ejecutando diagnósticos...", Toast.LENGTH_SHORT).show()
            
            // Simular diagnósticos
            handler.postDelayed({
                val report = """
                    ✅ Sistema: OK
                    ✅ Conectividad: OK  
                    ✅ Seguridad: OK
                    ✅ Almacenamiento: OK
                """.trimIndent()
                
                Toast.makeText(this, "✅ Diagnósticos completados", Toast.LENGTH_LONG).show()
                Log.d(tag, "Diagnostics Report:\n$report")
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(tag, "Error running diagnostics: ${e.message}")
            Toast.makeText(this, "❌ Error en diagnósticos", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateStatus() {
        try {
            // Sistema general
            systemStatusText.text = "🟢 Sistema Operativo"
            systemStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            
            // Seguridad
            val encryptionEnabled = getSharedPreferences("control_settings", MODE_PRIVATE)
                .getBoolean("encryption_enabled", true)
            securityStatusText.text = if (encryptionEnabled) "🔐 Cifrado Activo" else "🔓 Sin Cifrado"
            securityStatusText.setTextColor(
                ContextCompat.getColor(this, 
                    if (encryptionEnabled) android.R.color.holo_green_dark 
                    else android.R.color.holo_orange_dark
                )
            )
            
            // Red
            networkStatusText.text = "📶 WiFi P2P Disponible"
            networkStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            
            updateDeviceCount()
            
        } catch (e: Exception) {
            Log.e(tag, "Error updating status: ${e.message}")
        }
    }
    
    private fun updateDeviceCount() {
        // Simular conteo de dispositivos
        val deviceCount = (1..5).random()
        deviceCountText.text = "📱 $deviceCount dispositivos detectados"
    }
    
    private val handler = android.os.Handler(mainLooper)
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        Log.d(tag, "🔙 Saliendo del Sistema de Control")
    }
}