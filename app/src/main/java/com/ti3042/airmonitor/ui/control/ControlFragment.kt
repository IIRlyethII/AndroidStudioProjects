package com.ti3042.airmonitor.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.bluetooth.BluetoothManager
import com.ti3042.airmonitor.bluetooth.ConnectionCallback
import com.ti3042.airmonitor.models.ControlCommands
import com.ti3042.airmonitor.models.SensorData
import com.ti3042.airmonitor.models.Thresholds
import com.ti3042.airmonitor.firebase.FirebaseManager

class ControlFragment : Fragment(), ConnectionCallback {
    
    private val tag = "ControlFragment"
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var firebaseManager: FirebaseManager
    
    // Referencias a las vistas
    private lateinit var btnBack: MaterialButton
    private lateinit var switchAutoMode: MaterialSwitch
    private lateinit var switchFan: MaterialSwitch
    private lateinit var switchBuzzer: MaterialSwitch
    private lateinit var sliderWarning: Slider
    private lateinit var sliderCritical: Slider
    private lateinit var tvWarningValue: TextView
    private lateinit var tvCriticalValue: TextView
    private lateinit var tvFanDescription: TextView
    private lateinit var tvBuzzerDescription: TextView
    private lateinit var btnApplyChanges: MaterialButton
    
    // Estado actual del sistema
    private var currentAutoMode = true
    private var currentFanStatus = false
    private var currentBuzzerStatus = false
    private var currentWarningThreshold = 200
    private var currentCriticalThreshold = 400
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_control, container, false)
        
        initViews(view)
        setupBluetoothManager()
        setupClickListeners()
        setupSliders()
        
        return view
    }
    
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        switchAutoMode = view.findViewById(R.id.switchAutoMode)
        switchFan = view.findViewById(R.id.switchFan)
        switchBuzzer = view.findViewById(R.id.switchBuzzer)
        sliderWarning = view.findViewById(R.id.sliderWarning)
        sliderCritical = view.findViewById(R.id.sliderCritical)
        tvWarningValue = view.findViewById(R.id.tvWarningValue)
        tvCriticalValue = view.findViewById(R.id.tvCriticalValue)
        tvFanDescription = view.findViewById(R.id.tvFanDescription)
        tvBuzzerDescription = view.findViewById(R.id.tvBuzzerDescription)
        btnApplyChanges = view.findViewById(R.id.btnApplyChanges)
    }
    
    private fun setupBluetoothManager() {
        bluetoothManager = BluetoothManager.getInstance()
        bluetoothManager.setConnectionCallback(this)
        
        // Inicializar Firebase Manager
        firebaseManager = FirebaseManager.getInstance()
        
        // Solicitar datos actuales del sistema
        requestCurrentSystemState()
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
            currentAutoMode = isChecked
            updateControlsVisibility()
            updateDescriptions()
        }
        
        switchFan.setOnCheckedChangeListener { _, isChecked ->
            currentFanStatus = isChecked
            updateDescriptions()
        }
        
        switchBuzzer.setOnCheckedChangeListener { _, isChecked ->
            currentBuzzerStatus = isChecked
            updateDescriptions()
        }
        
        btnApplyChanges.setOnClickListener {
            applyChangesToESP32()
        }
    }
    
    private fun setupSliders() {
        sliderWarning.addOnChangeListener { _, value, _ ->
            currentWarningThreshold = value.toInt()
            tvWarningValue.text = "${value.toInt()} PPM"
            
            // Asegurar que el umbral crítico sea mayor que el de advertencia
            if (sliderCritical.value <= value) {
                sliderCritical.value = value + 50f
                currentCriticalThreshold = (value + 50f).toInt()
                tvCriticalValue.text = "${(value + 50f).toInt()} PPM"
            }
        }
        
        sliderCritical.addOnChangeListener { _, value, _ ->
            currentCriticalThreshold = value.toInt()
            tvCriticalValue.text = "${value.toInt()} PPM"
            
            // Asegurar que sea mayor que el umbral de advertencia
            if (value <= sliderWarning.value) {
                sliderWarning.value = value - 50f
                currentWarningThreshold = (value - 50f).toInt()
                tvWarningValue.text = "${(value - 50f).toInt()} PPM"
            }
        }
        
        // Inicializar valores
        updateSliderValues()
    }
    
    private fun updateControlsVisibility() {
        val enableManualControls = !currentAutoMode
        
        switchFan.isEnabled = enableManualControls
        switchBuzzer.isEnabled = enableManualControls
        
        // Cambiar opacidad visual
        switchFan.alpha = if (enableManualControls) 1.0f else 0.5f
        switchBuzzer.alpha = if (enableManualControls) 1.0f else 0.5f
    }
    
    private fun updateDescriptions() {
        if (currentAutoMode) {
            tvFanDescription.text = "Control automático - Se activa según umbrales de PPM"
            tvBuzzerDescription.text = "Control automático - Se activa cuando PPM > umbral advertencia"
        } else {
            tvFanDescription.text = if (currentFanStatus) {
                "Control manual - Ventilador encendido"
            } else {
                "Control manual - Ventilador apagado"
            }
            
            tvBuzzerDescription.text = if (currentBuzzerStatus) {
                "Control manual - Buzzer activo"
            } else {
                "Control manual - Buzzer inactivo"
            }
        }
    }
    
    private fun updateSliderValues() {
        sliderWarning.value = currentWarningThreshold.toFloat()
        sliderCritical.value = currentCriticalThreshold.toFloat()
        tvWarningValue.text = "$currentWarningThreshold PPM"
        tvCriticalValue.text = "$currentCriticalThreshold PPM"
    }
    
    private fun requestCurrentSystemState() {
        // En modo simulación, obtener datos del MockService
        val mockService = bluetoothManager.getCurrentService()
        if (mockService is com.ti3042.airmonitor.bluetooth.MockBluetoothService) {
            val currentData = mockService.getCurrentData()
            currentData?.let { updateControlsFromSensorData(it) }
        }
    }
    
    private fun updateControlsFromSensorData(sensorData: SensorData) {
        currentAutoMode = sensorData.systemStatus.autoMode
        currentFanStatus = sensorData.systemStatus.fanStatus
        currentBuzzerStatus = sensorData.systemStatus.buzzerActive
        currentWarningThreshold = sensorData.thresholds.warning
        currentCriticalThreshold = sensorData.thresholds.critical
        
        // Actualizar UI
        activity?.runOnUiThread {
            switchAutoMode.isChecked = currentAutoMode
            switchFan.isChecked = currentFanStatus
            switchBuzzer.isChecked = currentBuzzerStatus
            
            updateSliderValues()
            updateControlsVisibility()
            updateDescriptions()
        }
        
        Log.d(tag, "Controles actualizados desde datos del sensor")
    }
    
    private fun applyChangesToESP32() {
        if (!bluetoothManager.isConnected()) {
            showMessage("Error: No hay conexión con el dispositivo")
            return
        }
        
        try {
            // Enviar comando de modo automático
            bluetoothManager.sendCommand(
                if (currentAutoMode) ControlCommands.enableAutoMode() 
                else ControlCommands.disableAutoMode()
            )
            firebaseManager.logControlCommand("auto_mode", currentAutoMode)
            
            // Si está en modo manual, enviar estados específicos
            if (!currentAutoMode) {
                if (currentFanStatus) {
                    bluetoothManager.sendCommand(ControlCommands.turnOnFan())
                } else {
                    bluetoothManager.sendCommand(ControlCommands.turnOffFan())
                }
                firebaseManager.logControlCommand("fan_control", currentFanStatus)
                
                if (currentBuzzerStatus) {
                    bluetoothManager.sendCommand(ControlCommands.turnOnBuzzer())
                } else {
                    bluetoothManager.sendCommand(ControlCommands.turnOffBuzzer())
                }
                firebaseManager.logControlCommand("buzzer_control", currentBuzzerStatus)
            }
            
            // Enviar umbrales actualizados
            bluetoothManager.sendCommand(
                ControlCommands.setThresholds(currentWarningThreshold, currentCriticalThreshold)
            )
            
            showMessage("Cambios aplicados correctamente")
            Log.d(tag, "Comandos enviados - Auto: $currentAutoMode, Fan: $currentFanStatus, Buzzer: $currentBuzzerStatus, Umbrales: $currentWarningThreshold/$currentCriticalThreshold")
            
        } catch (e: Exception) {
            Log.e(tag, "Error aplicando cambios: ${e.message}")
            showMessage("Error aplicando cambios: ${e.message}")
        }
    }
    
    private fun showMessage(message: String) {
        view?.let { 
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }
    
    // Implementación de ConnectionCallback
    override fun onConnected() {
        activity?.runOnUiThread {
            btnApplyChanges.isEnabled = true
            showMessage("Conectado al dispositivo")
        }
    }
    
    override fun onDisconnected() {
        activity?.runOnUiThread {
            btnApplyChanges.isEnabled = false
            showMessage("Desconectado del dispositivo")
        }
    }
    
    override fun onDataReceived(sensorData: SensorData) {
        updateControlsFromSensorData(sensorData)
    }
    
    override fun onError(error: String) {
        activity?.runOnUiThread {
            showMessage("Error: $error")
        }
    }
    
    override fun onConnectionStateChanged(isConnected: Boolean) {
        activity?.runOnUiThread {
            btnApplyChanges.isEnabled = isConnected
        }
    }
}

