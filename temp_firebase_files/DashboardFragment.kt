package com.ti3042.airmonitor.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.databinding.FragmentDashboardBinding
import com.ti3042.airmonitor.bluetooth.BluetoothManager
import com.ti3042.airmonitor.bluetooth.ConnectionCallback
import com.ti3042.airmonitor.models.SensorData
import com.ti3042.airmonitor.ui.control.ControlFragment
import com.ti3042.airmonitor.firebase.FirebaseManager
import com.ti3042.airmonitor.ui.adapters.MetricsCardAdapter
import com.ti3042.airmonitor.ui.adapters.QuickActionsAdapter
import com.ti3042.airmonitor.ui.adapters.RecentAlertsAdapter
import com.ti3042.airmonitor.data.models.DashboardMetrics
import com.ti3042.airmonitor.data.models.AlertInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

//@AndroidEntryPoint
class DashboardFragment : Fragment(), ConnectionCallback {
    
    private val tag = "DashboardFragment"
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var firebaseManager: FirebaseManager
    
    // New professional dashboard components
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var metricsAdapter: MetricsCardAdapter
    private lateinit var quickActionsAdapter: QuickActionsAdapter
    private lateinit var alertsAdapter: RecentAlertsAdapter
    
    // Referencias a las vistas (legacy support)
    private var tvConnectionStatus: TextView? = null
    private var connectionIndicator: View? = null
    private var tvPPM: TextView? = null
    private var tvAirLevel: TextView? = null
    private var tvTemperature: TextView? = null
    private var tvHumidity: TextView? = null
    private var tvUptime: TextView? = null
    private var fabControls: FloatingActionButton? = null
    
    // Status views que ya no están en el layout pero mantenemos para compatibilidad
    private var tvConnectionStatus2: TextView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Try to use new professional dashboard layout first
        return try {
            _binding = FragmentDashboardBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            // Fallback to legacy layout
            Log.w(tag, "Using legacy dashboard layout: ${e.message}")
            val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
            
            initViews(view)
            setupBluetoothManager()
            setupClickListeners()
            
            view
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (_binding != null) {
            // New professional dashboard
            setupProfessionalDashboard()
        } else {
            // Legacy dashboard already setup in onCreateView
            Log.d(tag, "Using legacy dashboard")
        }
    }
    
    private fun setupProfessionalDashboard() {
        setupRecyclerViews()
        setupObservers()
        setupSwipeRefresh()
        setupToolbar()
        
        // Initialize existing bluetooth and firebase managers
        setupBluetoothManager()
        
        // Initial data load
        viewModel.loadDashboardData()
    }
    
    private fun initViews(view: View) {
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        connectionIndicator = view.findViewById(R.id.connectionIndicator)
        tvPPM = view.findViewById(R.id.tvPPM)
        tvAirLevel = view.findViewById(R.id.tvAirLevel)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvUptime = view.findViewById(R.id.tvUptime)
        fabControls = view.findViewById(R.id.fabControls)
        
        // Status secundario
        tvConnectionStatus2 = view.findViewById(R.id.tvConnectionStatus2)
    }
    
    private fun setupBluetoothManager() {
        try {
            bluetoothManager = BluetoothManager.getInstance()
            bluetoothManager.initialize(requireContext(), useSimulation = true)
            bluetoothManager.setConnectionCallback(this)
            
            // Inicializar Firebase Manager
            firebaseManager = FirebaseManager.getInstance()
            
            // Conectar automáticamente con delay para evitar problemas
            view?.postDelayed({
                try {
                    bluetoothManager.connect()
                } catch (e: Exception) {
                    Log.e(tag, "Error conectando Bluetooth: ${e.message}")
                }
            }, 1000)
            
        } catch (e: Exception) {
            Log.e(tag, "Error configurando BluetoothManager: ${e.message}")
        }
    }
    
    private fun setupClickListeners() {
        fabControls?.setOnClickListener {
            try {
                // Navegar al fragment de control
                val controlFragment = ControlFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, controlFragment)
                    .addToBackStack(null)
                    .commit()
                    
                // Registrar navegación en Firebase
                if (::firebaseManager.isInitialized) {
                    firebaseManager.logScreenView("Control")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error navegando a Control: ${e.message}")
            }
        }
    }
    
    override fun onConnected() {
        activity?.runOnUiThread {
            updateConnectionStatus("Conectado", true)
            firebaseManager.logBluetoothConnection(success = true, deviceName = "AirMonitor_TI3042")
            Log.d(tag, "Bluetooth conectado")
        }
    }
    
    override fun onDisconnected() {
        activity?.runOnUiThread {
            updateConnectionStatus("Desconectado", false)
            clearDataDisplays()
            Log.d(tag, "Bluetooth desconectado")
        }
    }
    
    override fun onDataReceived(sensorData: SensorData) {
        activity?.runOnUiThread {
            updateDataDisplays(sensorData)
            firebaseManager.logSensorData(sensorData)
            
            // Registrar alertas si es necesario
            if (sensorData.airQuality.ppm >= 400) {
                firebaseManager.logAirQualityAlert(sensorData.airQuality.ppm, "critical")
            } else if (sensorData.airQuality.ppm >= 200) {
                firebaseManager.logAirQualityAlert(sensorData.airQuality.ppm, "warning")
            }
        }
    }
    
    override fun onError(error: String) {
        activity?.runOnUiThread {
            updateConnectionStatus("Error: $error", false)
            Log.e(tag, "Error Bluetooth: $error")
        }
    }
    
    override fun onConnectionStateChanged(isConnected: Boolean) {
        activity?.runOnUiThread {
            if (isConnected) {
                updateConnectionStatus("Conectado", true)
            } else {
                updateConnectionStatus("Conectando...", false)
            }
        }
    }
    
    private fun updateConnectionStatus(status: String, isConnected: Boolean) {
        tvConnectionStatus?.text = status
        
        val color = when {
            isConnected -> Color.parseColor("#4CAF50") // Verde
            status.contains("Error") -> Color.parseColor("#F44336") // Rojo
            else -> Color.parseColor("#FF9800") // Naranja
        }
        
        connectionIndicator?.setBackgroundColor(color)
    }
    
    private fun updateDataDisplays(sensorData: SensorData) {
        // Actualizar PPM y nivel de aire
        tvPPM?.text = "${sensorData.airQuality.ppm} PPM"
        tvAirLevel?.text = sensorData.airQuality.level.uppercase()
        
        // Cambiar color según el nivel
        val levelColor = Color.parseColor(sensorData.airQuality.getLevelColor())
        tvPPM?.setTextColor(levelColor)
        tvAirLevel?.setTextColor(levelColor)
        
        // Actualizar temperatura y humedad
        tvTemperature?.text = String.format("%.1f°C", sensorData.airQuality.temperature)
        tvHumidity?.text = "${sensorData.airQuality.humidity}%"
        
        // Actualizar estado de conexión secundario si existe
        tvConnectionStatus2?.text = if (sensorData.systemStatus.fanStatus) "Conectado" else "Desconectado"
        
        tvUptime?.text = sensorData.systemStatus.getFormattedUptime()
        
        Log.d(tag, "Datos actualizados - PPM: ${sensorData.airQuality.ppm}, Nivel: ${sensorData.airQuality.level}")
    }
    
    private fun clearDataDisplays() {
        tvPPM?.text = "--- PPM"
        tvAirLevel?.text = "---"
        tvTemperature?.text = "--°C"
        tvHumidity?.text = "--%"
        tvUptime?.text = "---"
        
        // Restaurar colores por defecto
        tvPPM?.setTextColor(Color.parseColor("#9E9E9E"))
        tvAirLevel?.setTextColor(Color.parseColor("#9E9E9E"))
    }
    
    // Professional dashboard methods
    private fun setupRecyclerViews() {
        if (_binding == null) return
        
        // Metrics cards (2x2 grid)
        metricsAdapter = MetricsCardAdapter { metric ->
            when (metric.type) {
                "total_readings" -> navigateToHistory()
                "critical_alerts" -> navigateToAlerts()
                "air_quality" -> navigateToAnalysis()
                "last_update" -> viewModel.refreshData()
            }
        }
        
        binding.recyclerViewMetrics.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = metricsAdapter
        }
        
        // Quick actions (horizontal)
        quickActionsAdapter = QuickActionsAdapter { action ->
            when (action.id) {
                "start_monitoring" -> viewModel.toggleMonitoring()
                "calibrate_sensor" -> navigateToCalibration()
                "generate_report" -> navigateToReports()
                "sync_data" -> viewModel.syncWithCloud()
                "settings" -> navigateToSettings()
            }
        }
        
        binding.recyclerViewQuickActions.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = quickActionsAdapter
        }
        
        // Recent alerts
        alertsAdapter = RecentAlertsAdapter { alert ->
            showAlertDetails(alert)
        }
        
        binding.recyclerViewRecentAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }
    }
    
    private fun setupObservers() {
        if (_binding == null) return
        
        // Dashboard metrics
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboardMetrics.collect { metrics ->
                updateMetricsCards(metrics)
                updateOverallStatus(metrics)
            }
        }
        
        // Recent alerts
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentAlerts.collect { alerts ->
                alertsAdapter.submitList(alerts)
                
                binding.textViewNoAlerts.visibility = 
                    if (alerts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        // Loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }
        
        // Connection status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectionStatus.collect { status ->
                updateConnectionStatusProfessional(status)
            }
        }
        
        // Monitoring status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isMonitoring.collect { isMonitoring ->
                updateMonitoringButton(isMonitoring)
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        if (_binding == null) return
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
        
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorSecondary,
            R.color.colorAccent
        )
    }
    
    private fun setupToolbar() {
        if (_binding == null) return
        
        binding.toolbar.apply {
            setTitle(R.string.dashboard_title)
            inflateMenu(R.menu.dashboard_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_sync -> {
                        viewModel.syncWithCloud()
                        true
                    }
                    R.id.action_export -> {
                        navigateToReports()
                        true
                    }
                    R.id.action_settings -> {
                        navigateToSettings()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private fun updateMetricsCards(metrics: DashboardMetrics) {
        val metricsList = listOf(
            MetricCard(
                type = "total_readings",
                title = getString(R.string.total_readings),
                value = metrics.totalReadings.toString(),
                icon = R.drawable.ic_database,
                trend = TrendType.NEUTRAL
            ),
            MetricCard(
                type = "critical_alerts", 
                title = getString(R.string.critical_alerts),
                value = metrics.criticalAlerts.toString(),
                icon = R.drawable.ic_warning,
                trend = if (metrics.criticalAlerts > 0) TrendType.NEGATIVE else TrendType.POSITIVE
            ),
            MetricCard(
                type = "air_quality",
                title = getString(R.string.air_quality_index),
                value = metrics.airQualityIndex.toString(),
                icon = R.drawable.ic_air,
                trend = when {
                    metrics.airQualityIndex <= 50 -> TrendType.POSITIVE
                    metrics.airQualityIndex <= 100 -> TrendType.NEUTRAL
                    else -> TrendType.NEGATIVE
                }
            ),
            MetricCard(
                type = "last_update",
                title = getString(R.string.last_update),
                value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(metrics.lastUpdateTime),
                icon = R.drawable.ic_sync,
                trend = TrendType.NEUTRAL
            )
        )
        
        metricsAdapter.submitList(metricsList)
    }
    
    private fun updateOverallStatus(metrics: DashboardMetrics) {
        if (_binding == null) return
        
        val statusText = when {
            metrics.criticalAlerts > 0 -> getString(R.string.status_critical)
            metrics.warningAlerts > 0 -> getString(R.string.status_warning)
            else -> getString(R.string.status_normal)
        }
        
        val statusColor = when {
            metrics.criticalAlerts > 0 -> R.color.status_critical
            metrics.warningAlerts > 0 -> R.color.status_warning
            else -> R.color.status_normal
        }
        
        binding.textViewOverallStatus.text = statusText
        binding.textViewOverallStatus.setTextColor(
            resources.getColor(statusColor, requireContext().theme)
        )
        
        if (metrics.dominantGas != null) {
            binding.textViewDominantGas.text = 
                getString(R.string.dominant_gas_format, metrics.dominantGas)
            binding.textViewDominantGas.visibility = View.VISIBLE
        } else {
            binding.textViewDominantGas.visibility = View.GONE
        }
    }
    
    private fun updateConnectionStatusProfessional(status: ConnectionStatus) {
        if (_binding == null) return
        
        val (statusText, statusColor) = when (status) {
            ConnectionStatus.CONNECTED -> Pair(
                getString(R.string.connection_online),
                R.color.status_normal
            )
            ConnectionStatus.OFFLINE -> Pair(
                getString(R.string.connection_offline),
                R.color.status_warning
            )
            ConnectionStatus.ERROR -> Pair(
                getString(R.string.connection_error),
                R.color.status_critical
            )
        }
        
        binding.textViewConnectionStatus.text = statusText
        binding.textViewConnectionStatus.setTextColor(
            resources.getColor(statusColor, requireContext().theme)
        )
    }
    
    private fun updateMonitoringButton(isMonitoring: Boolean) {
        if (::quickActionsAdapter.isInitialized) {
            quickActionsAdapter.updateMonitoringStatus(isMonitoring)
        }
    }
    
    private fun showAlertDetails(alert: AlertInfo) {
        // Show alert details dialog
    }
    
    private fun navigateToHistory() {
        findNavController().navigate(R.id.action_dashboard_to_history)
    }
    
    private fun navigateToAlerts() {
        findNavController().navigate(R.id.action_dashboard_to_alerts)
    }
    
    private fun navigateToAnalysis() {
        findNavController().navigate(R.id.action_dashboard_to_gas_analysis)
    }
    
    private fun navigateToCalibration() {
        findNavController().navigate(R.id.action_dashboard_to_calibration)
    }
    
    private fun navigateToReports() {
        findNavController().navigate(R.id.action_dashboard_to_reports)
    }
    
    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_dashboard_to_settings)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::bluetoothManager.isInitialized) {
                bluetoothManager.disconnect()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error en onDestroy: ${e.message}")
        }
    }
}

// Data classes for dashboard components
data class MetricCard(
    val type: String,
    val title: String,
    val value: String,
    val icon: Int,
    val trend: TrendType
)

enum class TrendType {
    POSITIVE, NEGATIVE, NEUTRAL
}

enum class ConnectionStatus {
    CONNECTED, OFFLINE, ERROR
}
