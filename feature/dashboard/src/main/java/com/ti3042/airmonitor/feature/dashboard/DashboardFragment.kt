package com.ti3042.airmonitor.feature.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ti3042.airmonitor.feature.dashboard.databinding.FragmentDashboardBinding
import com.ti3042.airmonitor.domain.model.SensorData
import com.ti3042.airmonitor.domain.model.AirQualityLevel
import kotlinx.coroutines.launch

/**
 * 📊 Dashboard principal - Monitoreo de calidad del aire
 * 
 * Responsabilidades:
 * - Mostrar datos de sensores en tiempo real
 * - Controlar ventilador y alertas
 * - Navegación a otras funcionalidades
 * - Notificaciones de calidad del aire
 */
class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory()
    }
    
    private val tag = "DashboardFragment"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(tag, "📱 Creating DashboardFragment view")
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
        
        // Iniciar monitoreo
        viewModel.startMonitoring()
        
        Log.d(tag, "✅ DashboardFragment configurado correctamente")
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar datos del sensor
            viewModel.sensorData.collect { sensorData ->
                sensorData?.let { updateUI(it) }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar estado de conexión
            viewModel.connectionState.collect { isConnected ->
                updateConnectionStatus(isConnected)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar errores
            viewModel.errors.collect { error ->
                error?.let { showError(it) }
            }
        }
    }
    
    private fun setupClickListeners() {
        with(binding) {
            // Controles de sistema
            switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAutoMode(isChecked)
            }
            
            switchFan.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setFanState(isChecked)
            }
            
            switchAlert.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAlertState(isChecked)
            }
            
            // Navegación
            btnGasAnalysis.setOnClickListener {
                // TODO: Navegar a análisis de gases detallado
                Log.d(tag, "🧪 Navigating to gas analysis")
            }
            
            btnHistory.setOnClickListener {
                // TODO: Navegar a historial
                Log.d(tag, "📊 Navigating to history")
            }
            
            btnSettings.setOnClickListener {
                // TODO: Navegar a configuración
                Log.d(tag, "⚙️ Navigating to settings")
            }
        }
    }
    
    private fun updateUI(sensorData: SensorData) {
        with(binding) {
            // Datos principales
            tvPPM.text = "${sensorData.airQuality.ppm} PPM"
            tvAirLevel.text = sensorData.airQuality.level.name
            tvTemperature.text = String.format("%.1f°C", sensorData.airQuality.temperature)
            tvHumidity.text = "${sensorData.airQuality.humidity}%"
            
            // Colores según nivel de calidad
            val color = getAirQualityColor(sensorData.airQuality.level)
            tvPPM.setTextColor(color)
            tvAirLevel.setTextColor(color)
            
            // Estado del sistema
            tvFanStatus.text = if (sensorData.systemStatus.fanActive) "ON" else "OFF"
            tvBuzzerStatus.text = if (sensorData.systemStatus.buzzerActive) "ON" else "OFF"
            
            // Composición de gases
            updateGasComposition(sensorData.airQuality.gasComposition)
            
            // Uptime
            tvUptime.text = "Sistema operativo - ${sensorData.systemStatus.formattedUptime}"
        }
        
        Log.d(tag, "✅ UI actualizada - PPM: ${sensorData.airQuality.ppm}")
    }
    
    private fun updateConnectionStatus(isConnected: Boolean) {
        with(binding) {
            if (isConnected) {
                tvConnectionStatus.text = "✅ Conectado"
                tvConnectionStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        requireContext(),
                        com.ti3042.airmonitor.core.ui.R.color.status_connected
                    )
                )
            } else {
                tvConnectionStatus.text = "❌ Desconectado"
                tvConnectionStatus.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        requireContext(),
                        com.ti3042.airmonitor.core.ui.R.color.status_disconnected
                    )
                )
            }
        }
    }
    
    private fun updateGasComposition(gasComposition: Map<String, Float>) {
        // TODO: Actualizar barras de composición de gases
        with(binding) {
            val oxygen = gasComposition["oxygen"] ?: 0f
            val co2 = gasComposition["co2"] ?: 0f
            val smoke = gasComposition["smoke"] ?: 0f
            val vapor = gasComposition["vapor"] ?: 0f
            val others = gasComposition["others"] ?: 0f
            
            tvGasOxygenValue.text = "${oxygen.toInt()}%"
            tvGasCo2Value.text = "${co2.toInt()}%"
            tvGasSmokeValue.text = "${smoke.toInt()}%"
            tvGasVaporValue.text = "${vapor.toInt()}%"
            tvGasOthersValue.text = "${others.toInt()}%"
        }
    }
    
    private fun getAirQualityColor(level: AirQualityLevel): Int {
        val colorRes = when (level) {
            AirQualityLevel.GOOD -> 
                com.ti3042.airmonitor.core.ui.R.color.air_quality_good
            AirQualityLevel.MODERATE -> 
                com.ti3042.airmonitor.core.ui.R.color.air_quality_moderate
            AirQualityLevel.POOR -> 
                com.ti3042.airmonitor.core.ui.R.color.air_quality_poor
            AirQualityLevel.CRITICAL -> 
                com.ti3042.airmonitor.core.ui.R.color.air_quality_critical
        }
        
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
    }
    
    private fun showError(error: String) {
        // TODO: Mostrar error con Snackbar o similar
        Log.e(tag, "❌ Error: $error")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopMonitoring()
        _binding = null
        Log.d(tag, "🗑️ DashboardFragment destroyed")
    }
    
    companion object {
        fun newInstance() = DashboardFragment()
    }
}