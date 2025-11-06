package com.ti3042.airmonitor.feature.control

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ti3042.airmonitor.feature.control.databinding.ActivityControlBinding
import kotlinx.coroutines.launch

/**
 * 🔧 Sistema de Control Avanzado
 * 
 * Responsabilidades:
 * - Control de dispositivos conectados
 * - Configuración de sensibilidad y intervalos
 * - Gestión de seguridad y cifrado
 * - Diagnósticos del sistema
 * - Exportación de datos
 */
class ControlActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityControlBinding
    private val viewModel: ControlViewModel by viewModels {
        ControlViewModelFactory()
    }
    
    private val tag = "ControlActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "🔧 Creating ControlActivity")
        
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupObservers()
        setupClickListeners()
        
        // Cargar configuración actual
        viewModel.loadCurrentConfiguration()
        
        Log.d(tag, "✅ ControlActivity configurado correctamente")
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "🔧 Control del Sistema"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar configuración del sistema
            viewModel.systemConfiguration.collect { config ->
                config?.let { updateUI(it) }
            }
        }
        
        lifecycleScope.launch {
            // Observar estado de dispositivos
            viewModel.deviceStatus.collect { status ->
                updateDeviceStatus(status)
            }
        }
        
        lifecycleScope.launch {
            // Observar errores
            viewModel.errors.collect { error ->
                error?.let { showError(it) }
            }
        }
    }
    
    private fun setupClickListeners() {
        with(binding) {
            // Controles de conectividad
            switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setBluetoothEnabled(isChecked)
            }
            
            switchWifiP2P.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setWifiP2PEnabled(isChecked)
            }
            
            switchEncryption.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setEncryptionEnabled(isChecked)
            }
            
            switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAutoSyncEnabled(isChecked)
            }
            
            // Sliders de configuración
            sliderSensitivity.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.setSensitivity(value.toInt())
                }
            }
            
            sliderInterval.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.setDataInterval(value.toInt())
                }
            }
            
            // Botones de acción
            btnScanDevices.setOnClickListener {
                viewModel.scanForDevices()
            }
            
            btnExportData.setOnClickListener {
                viewModel.exportData()
            }
            
            btnClearCache.setOnClickListener {
                viewModel.clearCache()
            }
            
            btnDiagnostics.setOnClickListener {
                viewModel.runDiagnostics()
            }
            
            btnResetSettings.setOnClickListener {
                viewModel.resetToDefaults()
            }
        }
    }
    
    private fun updateUI(config: SystemConfiguration) {
        with(binding) {
            // Actualizar switches sin disparar listeners
            switchBluetooth.isChecked = config.bluetoothEnabled
            switchWifiP2P.isChecked = config.wifiP2PEnabled
            switchEncryption.isChecked = config.encryptionEnabled
            switchAutoSync.isChecked = config.autoSyncEnabled
            
            // Actualizar sliders
            sliderSensitivity.value = config.sensitivity.toFloat()
            sliderInterval.value = config.dataInterval.toFloat()
            
            // Actualizar labels
            tvSensitivityValue.text = "${config.sensitivity}%"
            tvIntervalValue.text = "${config.dataInterval}s"
            
            // Actualizar status
            tvBluetoothStatus.text = if (config.bluetoothEnabled) "ACTIVO" else "INACTIVO"
            tvWifiStatus.text = if (config.wifiP2PEnabled) "ACTIVO" else "INACTIVO"
            tvEncryptionStatus.text = if (config.encryptionEnabled) "HABILITADO" else "DESHABILITADO"
        }
        
        Log.d(tag, "✅ UI actualizada con configuración")
    }
    
    private fun updateDeviceStatus(status: DeviceStatus) {
        with(binding) {
            tvConnectedDevices.text = "${status.connectedDevices}"
            tvSignalStrength.text = "${status.signalStrength}%"
            tvDataRate.text = "${status.dataRate} KB/s"
            tvUptime.text = status.formattedUptime
            
            // Actualizar colores de status
            val statusColor = when {
                status.connectedDevices > 0 -> 
                    androidx.core.content.ContextCompat.getColor(this@ControlActivity, 
                        com.ti3042.airmonitor.core.ui.R.color.status_connected)
                else -> 
                    androidx.core.content.ContextCompat.getColor(this@ControlActivity, 
                        com.ti3042.airmonitor.core.ui.R.color.status_disconnected)
            }
            
            tvConnectedDevices.setTextColor(statusColor)
        }
    }
    
    private fun showError(error: String) {
        // TODO: Mostrar error con Snackbar
        Log.e(tag, "❌ Error: $error")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    companion object {
        const val EXTRA_SECTION = "section"
        const val SECTION_DEVICES = "devices"
        const val SECTION_SECURITY = "security"
        const val SECTION_NETWORK = "network"
    }
}