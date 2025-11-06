package com.ti3042.airmonitor.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.bluetooth.BluetoothManager
import com.ti3042.airmonitor.bluetooth.ConnectionCallback
import com.ti3042.airmonitor.models.SensorData
import com.ti3042.airmonitor.firebase.FirebaseManager
import com.ti3042.airmonitor.data.MockDataService
import com.ti3042.airmonitor.data.FirestoreDataManager
import com.ti3042.airmonitor.notifications.NotificationHelper
import com.ti3042.airmonitor.ui.gas.GasAnalysisFragment
import com.ti3042.airmonitor.ui.history.HistoryFragment
import com.ti3042.airmonitor.ui.settings.SettingsManagerFragment
import com.ti3042.airmonitor.ui.settings.SettingsFragment

class DashboardSimpleFragment : Fragment(), ConnectionCallback {
    
    private val tag = "DashboardSimpleFragment"
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var mockDataService: MockDataService
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var dataManager: FirestoreDataManager
    
    // 📊 Control de notificaciones (evitar spam)
    private var lastNotificationPPM = -1
    private var lastNotificationTime = 0L
    private val notificationCooldown = 30000L // 30 segundos entre notificaciones
    
    // Referencias simples a las vistas
    private var tvConnectionStatus: TextView? = null
    private var tvPPM: TextView? = null
    private var tvAirLevel: TextView? = null
    private var tvTemperature: TextView? = null
    private var tvHumidity: TextView? = null
    private var tvUptime: TextView? = null
    
    // Referencias a controles
    private var switchAutoMode: android.widget.Switch? = null
    private var switchFan: android.widget.Switch? = null
    private var switchAlert: android.widget.Switch? = null
    private var layoutFanControl: android.widget.LinearLayout? = null
    private var layoutAlertControl: android.widget.LinearLayout? = null
    
    // Referencias a status
    private var tvFanStatus: TextView? = null
    private var tvBuzzerStatus: TextView? = null
    
    // Referencias a gas composition (XML-based)
    private var gasBarsContainer: android.widget.LinearLayout? = null
    
    // 📱 Referencias de navegación
    private var btnToggleGases: TextView? = null
    private var btnHistory: TextView? = null
    private var btnSettings: TextView? = null
    

    
    // 📈 Referencias Trending
    private var tvPPMTrend: TextView? = null
    private var tvTempTrend: TextView? = null
    private var tvAlertStatus: TextView? = null
    private var tvLast24h: TextView? = null
    
    // 🎨 Header References
    private var headerStatus: TextView? = null
    

    
    // 📊 Referencias directas a gas bars XML
    private var tvGasOxygenValue: TextView? = null
    private var tvGasCo2Value: TextView? = null  
    private var tvGasSmokeValue: TextView? = null
    private var tvGasVaporValue: TextView? = null
    private var tvGasOthersValue: TextView? = null
    
    private var progressOxygen: View? = null
    private var progressCo2: View? = null
    private var progressSmoke: View? = null
    private var progressVapor: View? = null
    private var progressOthers: View? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(tag, "Creando vista simple")
        val view = inflater.inflate(R.layout.fragment_dashboard_simple, container, false)
        
        initViews(view)
        setupDataServices()
        
        return view
    }
    
    private fun initViews(view: View) {
        Log.d(tag, "Inicializando vistas")
        
        // Inicializar NotificationHelper
        notificationHelper = NotificationHelper.getInstance(requireContext())
        
        // Vistas de datos
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        tvPPM = view.findViewById(R.id.tvPPM)
        tvAirLevel = view.findViewById(R.id.tvAirLevel)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvUptime = view.findViewById(R.id.tvUptime)
        
        // Controles
        switchAutoMode = view.findViewById(R.id.switch_auto_mode)
        switchFan = view.findViewById(R.id.switch_fan)
        switchAlert = view.findViewById(R.id.switch_alert)
        layoutFanControl = view.findViewById(R.id.layout_fan_control)
        layoutAlertControl = view.findViewById(R.id.layout_alert_control)
        
        // Status
        tvFanStatus = view.findViewById(R.id.tvFanStatus)
        tvBuzzerStatus = view.findViewById(R.id.tvBuzzerStatus)
        
        // Gas composition - XML based references
        gasBarsContainer = view.findViewById(R.id.gas_bars_container)
        
        // 📊 Referencias directas a elementos XML de gas bars
        tvGasOxygenValue = view.findViewById(R.id.tv_gas_oxygen_value)
        tvGasCo2Value = view.findViewById(R.id.tv_gas_co2_value)
        tvGasSmokeValue = view.findViewById(R.id.tv_gas_smoke_value)
        tvGasVaporValue = view.findViewById(R.id.tv_gas_vapor_value)
        tvGasOthersValue = view.findViewById(R.id.tv_gas_others_value)
        
        progressOxygen = view.findViewById(R.id.progress_oxygen)
        progressCo2 = view.findViewById(R.id.progress_co2)
        progressSmoke = view.findViewById(R.id.progress_smoke)
        progressVapor = view.findViewById(R.id.progress_vapor)
        progressOthers = view.findViewById(R.id.progress_others)
        
        // 📱 Navigation buttons
        btnToggleGases = view.findViewById(R.id.btn_toggle_gases)
        btnHistory = view.findViewById(R.id.btn_history)
        btnSettings = view.findViewById(R.id.btn_settings)
        

        
        // 📈 Trending elements
        tvPPMTrend = view.findViewById(R.id.tvPPMTrend)
        tvTempTrend = view.findViewById(R.id.tvTempTrend)
        tvAlertStatus = view.findViewById(R.id.tvAlertStatus)
        tvLast24h = view.findViewById(R.id.tvLast24h)
        
        // 🎨 Header elements
        headerStatus = view.findViewById(R.id.header_status)
        

        
        // �📊 Inicializar gas bars con valores por defecto
        initializeGasBars()
        
        // 🔗 Setup navigation
        setupNavigationListeners()
        
        // Configurar listeners
        setupSwitchListeners()
        
        Log.d(tag, "Vistas inicializadas correctamente")
    }
    
    /**
     * 📱 Configurar navegación a pantallas adicionales
     */
    private fun setupNavigationListeners() {
        btnToggleGases?.setOnClickListener {
            Log.d(tag, "🧪 Navigating to detailed gas analysis")
            try {
                val gasAnalysisFragment = GasAnalysisFragment.newInstance()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, gasAnalysisFragment)
                    .addToBackStack("gas_analysis")
                    .commit()
            } catch (e: Exception) {
                Log.e(tag, "❌ Error navigating to gas analysis: ${e.message}")
            }
        }
        
        btnHistory?.setOnClickListener {
            Log.d(tag, "📊 Navigating to History")
            try {
                val historyFragment = com.ti3042.airmonitor.ui.history.HistoryFragment.newInstance()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, historyFragment)
                    .addToBackStack("history")
                    .commit()
            } catch (e: Exception) {
                Log.e(tag, "❌ Error navigating to history: ${e.message}")
                android.widget.Toast.makeText(context, "Error abriendo historial", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        btnSettings?.setOnClickListener {
            Log.d(tag, "⚙️ Navigating to Settings")
            try {
                val settingsFragment = SettingsFragment.newInstance()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, settingsFragment)
                    .addToBackStack("settings")
                    .commit()
            } catch (e: Exception) {
                Log.e(tag, "❌ Error navigating to settings: ${e.message}")
                android.widget.Toast.makeText(context, "Error abriendo configuración", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        Log.d(tag, "📱 Navigation listeners configured")
    }
    
    private fun setupSwitchListeners() {
        switchAutoMode?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "Modo automático: $isChecked")
            updateManualControlsState(!isChecked)
            
            // 📱 Notificación de cambio de modo
            try {
                val uptime = tvUptime?.text?.toString()?.substringAfter("- ") ?: "Sistema activo"
                notificationHelper.showSystemStatusUpdate(
                    fanStatus = switchFan?.isChecked ?: false,
                    buzzerActive = switchAlert?.isChecked ?: false,
                    uptime = uptime
                )
                
                val message = if (isChecked) {
                    "🤖 Modo Automático ACTIVADO"
                } else {
                    "⚙️ Control Manual HABILITADO"
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(tag, "Error en switch auto: ${e.message}")
            }
        }
        
        switchFan?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "Ventilador manual: $isChecked")
            
            try {
                // 📱 Notificación específica para ventilador crítico
                if (isChecked && (tvPPM?.text?.toString()?.replace(" PPM", "")?.toIntOrNull() ?: 0) > 300) {
                    // Usar notificación de sistema para cambios críticos
                    val uptime = tvUptime?.text?.toString()?.substringAfter("- ") ?: "Sistema activo"
                    notificationHelper.showSystemStatusUpdate(
                        fanStatus = isChecked,
                        buzzerActive = switchAlert?.isChecked ?: false,
                        uptime = uptime
                    )
                }
                
                val message = if (isChecked) "🌪️ Ventilador ENCENDIDO" else "🌪️ Ventilador APAGADO"
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(tag, "Error en switch fan: ${e.message}")
            }
        }
        
        switchAlert?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(tag, "Alerta manual: $isChecked")
            
            try {
                // 🚨 Notificación específica para alerta activada
                if (isChecked) {
                    val uptime = tvUptime?.text?.toString()?.substringAfter("- ") ?: "Sistema activo"
                    notificationHelper.showSystemStatusUpdate(
                        fanStatus = switchFan?.isChecked ?: false,
                        buzzerActive = isChecked,
                        uptime = uptime
                    )
                }
                
                val message = if (isChecked) "🔊 Alerta ACTIVADA" else "🔊 Alerta DESACTIVADA"
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(tag, "Error en switch alert: ${e.message}")
            }
        }
    }
    
    private fun updateManualControlsState(enabled: Boolean) {
        // Habilitar/deshabilitar controles manuales
        switchFan?.isEnabled = enabled
        switchAlert?.isEnabled = enabled
        
        // Cambiar opacidad visual
        val alpha = if (enabled) 1.0f else 0.5f
        layoutFanControl?.alpha = alpha
        layoutAlertControl?.alpha = alpha
        
        Log.d(tag, "Controles manuales ${if (enabled) "habilitados" else "deshabilitados"}")
    }
    
    private fun setupDataServices() {
        try {
            Log.d(tag, "Configurando servicios de datos")
            
            // Inicializar Firebase Manager
            firebaseManager = FirebaseManager.getInstance()
            
            // Inicializar Firestore DataManager
            dataManager = FirestoreDataManager.getInstance()
            
            // Inicializar MockDataService
            mockDataService = MockDataService.getInstance()
            mockDataService.initialize(requireContext())
            
            // Decidir qué servicio usar
            if (MockDataService.shouldUseMockData(requireContext())) {
                Log.d(tag, "🎭 Usando datos simulados")
                setupMockDataService()
            } else {
                Log.d(tag, "📡 Usando BluetoothManager real")
                setupBluetoothManager()
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error configurando servicios: ${e.message}")
        }
    }
    
    private fun setupMockDataService() {
        try {
            Log.d(tag, "Iniciando simulación de datos")
            
            mockDataService.startDataSimulation(
                onDataReceived = { sensorData ->
                    Log.d(tag, "📊 Datos simulados recibidos: PPM=${sensorData.airQuality.ppm}")
                    updateDataDisplays(sensorData)
                    
                    // Log a Firebase
                    if (::firebaseManager.isInitialized) {
                        firebaseManager.logSensorData(sensorData)
                    }
                },
                onConnected = {
                    Log.d(tag, "🎭 Conexión simulada establecida")
                    activity?.runOnUiThread {
                        tvConnectionStatus?.text = "🎭 Conectado (Simulación)"
                        tvConnectionStatus?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_simulation))
                        
                        // 📱 Notificación de conexión establecida
                        notificationHelper.showConnectionStatus(true, "ESP32 (Simulación)")
                    }
                }
            )
            
        } catch (e: Exception) {
            Log.e(tag, "Error configurando MockDataService: ${e.message}")
        }
    }
    
    private fun setupBluetoothManager() {
        try {
            Log.d(tag, "Configurando BluetoothManager real")
            bluetoothManager = BluetoothManager.getInstance()
            bluetoothManager.initialize(requireContext(), useSimulation = true)
            bluetoothManager.setConnectionCallback(this)
            
            // Conectar con delay
            view?.postDelayed({
                try {
                    bluetoothManager.connect()
                    Log.d(tag, "Bluetooth conectando...")
                } catch (e: Exception) {
                    Log.e(tag, "Error conectando Bluetooth: ${e.message}")
                }
            }, 1000)
            
        } catch (e: Exception) {
            Log.e(tag, "Error configurando BluetoothManager: ${e.message}")
        }
    }
    
    override fun onConnected() {
        Log.d(tag, "Bluetooth conectado")
        activity?.runOnUiThread {
            tvConnectionStatus?.text = "✅ Conectado a ESP32"
            tvConnectionStatus?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_connected))
            
            // 📱 Notificación de conexión real
            notificationHelper.showConnectionStatus(true, "ESP32")
        }
    }
    
    override fun onDisconnected() {
        Log.d(tag, "Bluetooth desconectado")
        activity?.runOnUiThread {
            tvConnectionStatus?.text = "❌ Desconectado"
            tvConnectionStatus?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_disconnected))
            
            // 📱 Notificación de desconexión
            notificationHelper.showConnectionStatus(false, "ESP32")
        }
    }
    
    override fun onDataReceived(sensorData: SensorData) {
        Log.d(tag, "Datos recibidos: PPM=${sensorData.airQuality.ppm}")
        activity?.runOnUiThread {
            updateDataDisplays(sensorData)
        }
    }
    
    override fun onError(error: String) {
        Log.e(tag, "Error Bluetooth: $error")
        activity?.runOnUiThread {
            tvConnectionStatus?.text = "⚠️ Error: $error"
            tvConnectionStatus?.setTextColor(Color.parseColor("#FF9800"))
        }
    }
    
    override fun onConnectionStateChanged(isConnected: Boolean) {
        Log.d(tag, "Estado conexión cambiado: $isConnected")
        activity?.runOnUiThread {
            if (isConnected) {
                tvConnectionStatus?.text = "🔗 Conectado"
                tvConnectionStatus?.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                tvConnectionStatus?.text = "🔄 Conectando..."
                tvConnectionStatus?.setTextColor(Color.parseColor("#FF9800"))
            }
        }
    }
    
    private fun updateDataDisplays(sensorData: SensorData) {
        Log.d(tag, "Actualizando displays")
        
        // Actualizar PPM y nivel
        tvPPM?.text = "${sensorData.airQuality.ppm} PPM"
        tvAirLevel?.text = sensorData.airQuality.level.uppercase()
        
        // Cambiar colores según el nivel
        val levelColor = Color.parseColor(sensorData.airQuality.getLevelColor())
        tvPPM?.setTextColor(levelColor)
        tvAirLevel?.setTextColor(levelColor)
        
        // Actualizar temperatura y humedad
        tvTemperature?.text = String.format("%.1f°C", sensorData.airQuality.temperature)
        tvHumidity?.text = "${sensorData.airQuality.humidity}%"
        
        // Actualizar uptime
        tvUptime?.text = "Sistema operativo - ${sensorData.systemStatus.getFormattedUptime()}"
        
        // 🎛️ NUEVO: Actualizar estado de dispositivos
        updateDeviceStatus(sensorData.systemStatus)
        
        // 🎨 NUEVO: Actualizar colores de cards dinámicamente
        updateCardColors(sensorData.airQuality.ppm, sensorData.airQuality.level)
        
        // 🔔 NUEVO: Mostrar notificaciones según el nivel
        showAirQualityNotification(sensorData.airQuality.ppm, sensorData.airQuality.level)
        
        // 📊 NUEVO: Actualizar gas composition bars
        updateGasBars(sensorData.airQuality.ppm)
        

        
        // 📈 NUEVO: Actualizar trending info
        updateTrendingInfo(sensorData.airQuality.ppm, sensorData.airQuality.temperature)
        
        // 🎨 Actualizar header status
        updateHeaderStatus(sensorData.airQuality.ppm, sensorData.airQuality.level)
        
        // 💾 NUEVO: Guardar datos en Firestore (cada 30 segundos)
        saveSensorDataToFirestore(sensorData)
        
        Log.d(tag, "Displays actualizados correctamente - PPM: ${sensorData.airQuality.ppm}")
    }
    
    private fun updateCardColors(ppm: Int, level: String) {
        try {
            val view = this.view ?: return
            
            // Obtener las cards
            val airQualityCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.card_air_quality)
            val tempCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.card_temperature)
            val humidityCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.card_humidity)
            
            // Determinar color según PPM usando recursos
            val cardColorRes = when {
                ppm < 150 -> R.color.air_quality_good
                ppm < 250 -> R.color.air_quality_moderate  
                ppm < 400 -> R.color.air_quality_poor
                else -> R.color.air_quality_critical
            }
            val cardColor = androidx.core.content.ContextCompat.getColor(requireContext(), cardColorRes)
            
            // Aplicar color a las cards (simulando strokeColor)
            airQualityCard?.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            
            // Como CardView no tiene strokeColor en esta versión, usamos un enfoque alternativo
            // Cambiar el fondo del layout interno para simular borde
            val airQualityLayout = airQualityCard?.getChildAt(0) as? android.widget.LinearLayout
            airQualityLayout?.setBackgroundColor(Color.argb(20, Color.red(cardColor), Color.green(cardColor), Color.blue(cardColor)))
            
            Log.d(tag, "Colores de cards actualizados para PPM: $ppm")
            
        } catch (e: Exception) {
            Log.e(tag, "Error actualizando colores: ${e.message}")
        }
    }
    
    private fun showAirQualityNotification(ppm: Int, level: String) {
        try {
            val context = this.context ?: return
            val currentTime = System.currentTimeMillis()
            
            // 🚦 Lógica de control de notificaciones
            val shouldNotify = when {
                ppm >= 400 -> true // Siempre notificar crítico
                ppm >= 300 -> shouldSendNotification(ppm, currentTime, 15000) // Cada 15s para malo
                ppm >= 200 -> shouldSendNotification(ppm, currentTime, 45000) // Cada 45s para moderado
                ppm < 150 && lastNotificationPPM >= 200 -> true // Notificar mejora significativa
                else -> false
            }
            
            if (shouldNotify) {
                // 📱 Obtener datos de sensor actuales
                val temperature = tvTemperature?.text?.toString()?.replace("°C", "")?.toFloatOrNull() ?: 0f
                val humidity = tvHumidity?.text?.toString()?.replace("%", "")?.toIntOrNull() ?: 0
                
                // 🔔 Enviar notificación real
                notificationHelper.showAirQualityAlert(ppm, level, temperature, humidity)
                
                // 📊 Actualizar control
                lastNotificationPPM = ppm
                lastNotificationTime = currentTime
                
                Log.d(tag, "📱 Notificación enviada - PPM: $ppm, Nivel: $level")
            } else {
                Log.d(tag, "🔕 Notificación omitida - PPM: $ppm (cooldown activo)")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error mostrando notificación: ${e.message}")
        }
    }
    
    /**
     * 🕐 Determinar si enviar notificación según cooldown y cambio significativo
     */
    private fun shouldSendNotification(currentPPM: Int, currentTime: Long, cooldownMs: Long): Boolean {
        val timeSinceLastNotification = currentTime - lastNotificationTime
        val ppmDifference = kotlin.math.abs(currentPPM - lastNotificationPPM)
        
        return when {
            // Primera notificación
            lastNotificationPPM == -1 -> true
            
            // Cooldown no cumplido
            timeSinceLastNotification < cooldownMs -> false
            
            // Cambio significativo de PPM (>20)
            ppmDifference > 20 -> true
            
            // Cooldown cumplido
            timeSinceLastNotification >= cooldownMs -> true
            
            else -> false
        }
    }
    
    private fun updateDeviceStatus(systemStatus: com.ti3042.airmonitor.models.SystemStatus) {
        try {
            // Actualizar status text
            tvFanStatus?.text = if (systemStatus.fanStatus) "ON" else "OFF"
            tvFanStatus?.setTextColor(
                if (systemStatus.fanStatus) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E")
            )
            
            tvBuzzerStatus?.text = if (systemStatus.buzzerActive) "ON" else "OFF"  
            tvBuzzerStatus?.setTextColor(
                if (systemStatus.buzzerActive) Color.parseColor("#FF9800") else Color.parseColor("#9E9E9E")
            )
            
            // Sincronizar switches con el estado del sistema (solo si está en modo automático)
            if (switchAutoMode?.isChecked == true) {
                // En modo automático, los switches reflejan el estado real
                switchFan?.isChecked = systemStatus.fanStatus
                switchAlert?.isChecked = systemStatus.buzzerActive
            }
            
            Log.d(tag, "Estado de dispositivos actualizado - Fan: ${systemStatus.fanStatus}, Buzzer: ${systemStatus.buzzerActive}")
            
        } catch (e: Exception) {
            Log.e(tag, "Error actualizando estado dispositivos: ${e.message}")
        }
    }
    
    /**
     * 📊 Inicializar gas bars con valores por defecto (XML-based implementation)
     */
    private fun initializeGasBars() {
        try {
            // Valores iniciales por defecto
            val defaultGasData = listOf(78, 15, 3, 2, 2) // Aire normal
            updateGasDisplays(defaultGasData)
            
            Log.d(tag, "✅ Gas bars XML inicializadas")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error inicializando gas bars: ${e.message}")
        }
    }
    
    /**
     * 📊 Actualizar gas bars usando elementos XML (Método simplificado)
     */
    private fun updateGasBars(ppm: Int) {
        try {
            // Simular composición basada en PPM
            val gasData = when {
                ppm < 150 -> listOf(78, 15, 3, 2, 2)    // Buena calidad - O2, CO2, Humo, Vapor, Otros
                ppm < 250 -> listOf(72, 20, 4, 2, 2)    // Moderada 
                ppm < 400 -> listOf(65, 28, 4, 2, 1)    // Mala
                else -> listOf(62, 30, 5, 2, 1)         // Crítica
            }
            
            updateGasDisplays(gasData)
            Log.d(tag, "📊 Gas bars actualizadas para PPM: $ppm")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando gas bars: ${e.message}")
        }
    }
    
    /**
     * 📊 Actualizar displays y barras de gas (Método centralizado XML-based)
     */
    private fun updateGasDisplays(gasData: List<Int>) {
        try {
            val container = gasBarsContainer ?: return
            val containerWidth = container.width.takeIf { it > 0 } ?: 800 // Fallback width
            
            // Actualizar valores de texto
            tvGasOxygenValue?.text = "${gasData[0]}%"
            tvGasCo2Value?.text = "${gasData[1]}%"
            tvGasSmokeValue?.text = "${gasData[2]}%"
            tvGasVaporValue?.text = "${gasData[3]}%"
            tvGasOthersValue?.text = "${gasData[4]}%"
            
            // Actualizar barras de progreso con width proporcional
            val baseWidth = (containerWidth - 32) // Descontar padding
            
            progressOxygen?.layoutParams = android.widget.FrameLayout.LayoutParams(
                (baseWidth * gasData[0] / 100).coerceAtLeast(4),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            progressCo2?.layoutParams = android.widget.FrameLayout.LayoutParams(
                (baseWidth * gasData[1] / 100).coerceAtLeast(4),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            progressSmoke?.layoutParams = android.widget.FrameLayout.LayoutParams(
                (baseWidth * gasData[2] / 100).coerceAtLeast(4),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            progressVapor?.layoutParams = android.widget.FrameLayout.LayoutParams(
                (baseWidth * gasData[3] / 100).coerceAtLeast(4),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            progressOthers?.layoutParams = android.widget.FrameLayout.LayoutParams(
                (baseWidth * gasData[4] / 100).coerceAtLeast(4),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            Log.d(tag, "✅ Gas displays actualizados: O2=${gasData[0]}%, CO2=${gasData[1]}%")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando gas displays: ${e.message}")
        }
    }
    

    
    /**
     * 📈 Actualizar información de tendencias
     */
    private fun updateTrendingInfo(currentPPM: Int, currentTemp: Float) {
        try {
            // Simular tendencia PPM (en app real sería calculado desde historial)
            val ppmTrendValue = when {
                currentPPM > 300 -> "+${(10..25).random()}"
                currentPPM > 200 -> "+${(5..15).random()}" 
                currentPPM > 150 -> "${(-5..10).random()}"
                else -> "${(-15..-5).random()}"
            }
            
            val ppmTrendIcon = if (ppmTrendValue.startsWith("+")) "↗️" else if (ppmTrendValue.startsWith("-")) "↘️" else "➡️"
            tvPPMTrend?.text = "$ppmTrendIcon $ppmTrendValue"
            
            val ppmTrendColorRes = when {
                ppmTrendValue.startsWith("+") -> R.color.air_quality_moderate
                ppmTrendValue.startsWith("-") -> R.color.air_quality_good
                else -> R.color.text_secondary
            }
            tvPPMTrend?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), ppmTrendColorRes))
            
            // Simular tendencia temperatura
            val tempTrendValue = String.format("%.1f°C", (currentTemp - 25.0f))
            val tempTrendIcon = if (currentTemp > 25.0f) "↗️" else "↘️"
            tvTempTrend?.text = "$tempTrendIcon $tempTrendValue"
            
            val tempTrendColorRes = if (currentTemp > 25.0f) R.color.air_quality_moderate else R.color.air_quality_good
            tvTempTrend?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), tempTrendColorRes))
            
            // Actualizar estado de alerta
            val alertText = when {
                currentPPM >= 400 -> "🚨 Crítica"
                currentPPM >= 300 -> "⚠️ Alta"
                currentPPM >= 200 -> "⚠️ Moderada"
                else -> "✅ Normal"
            }
            
            tvAlertStatus?.text = alertText
            
            val alertColorRes = when {
                currentPPM >= 400 -> R.color.air_quality_critical
                currentPPM >= 300 -> R.color.air_quality_poor
                currentPPM >= 200 -> R.color.air_quality_moderate
                else -> R.color.air_quality_good
            }
            tvAlertStatus?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), alertColorRes))
            
            // Actualizar resumen 24h (simulado)
            val avgPPM = (currentPPM * 0.85).toInt()
            val maxPPM = (currentPPM * 1.2).toInt()
            val minPPM = (currentPPM * 0.6).toInt()
            
            tvLast24h?.text = "📊 Últimas 24h: Promedio $avgPPM PPM • Máx: $maxPPM PPM • Mín: $minPPM PPM"
            
            Log.d(tag, "✅ Trending info updated")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error updating trending info: ${e.message}")
        }
    }
    
    /**
     * 🎨 Actualizar estado del header
     */
    private fun updateHeaderStatus(currentPPM: Int, level: String) {
        try {
            val statusText = when {
                currentPPM >= 400 -> "⚠️ Nivel Crítico"
                currentPPM >= 300 -> "⚠️ Nivel Alto" 
                currentPPM >= 200 -> "⚠️ Moderado"
                currentPPM >= 150 -> "✅ Buena Calidad"
                else -> "✅ Excelente"
            }
            
            headerStatus?.text = statusText
            Log.d(tag, "✅ Header status updated: $statusText")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error updating header status: ${e.message}")
        }
    }
    

    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Destruyendo fragment")
        try {
            // Detener servicio apropiado
            if (MockDataService.shouldUseMockData(requireContext()) && ::mockDataService.isInitialized) {
                mockDataService.stopDataSimulation()
                Log.d(tag, "MockDataService detenido")
            } else if (::bluetoothManager.isInitialized) {
                bluetoothManager.disconnect()
                Log.d(tag, "BluetoothManager desconectado")
            }
            
            // 🗑️ Limpiar notificaciones de conexión al salir
            if (::notificationHelper.isInitialized) {
                notificationHelper.clearConnectionNotifications()
                Log.d(tag, "📱 Notificaciones de conexión limpiadas")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error en onDestroy: ${e.message}")
        }
    }
    
    // 📊 Control de guardado (evitar spam a Firestore)
    private var lastSaveTime = 0L
    private val saveInterval = 30000L // 30 segundos
    
    /**
     * 💾 Guardar datos de sensor en Firestore con control de intervalo
     */
    private fun saveSensorDataToFirestore(sensorData: SensorData) {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastSaveTime < saveInterval) {
            return // Skip saving if interval hasn't passed
        }
        
        try {
            if (::dataManager.isInitialized) {
                dataManager.saveSensorReading(sensorData) { success, error ->
                    if (success) {
                        Log.d(tag, "✅ Datos guardados en Firestore")
                        lastSaveTime = currentTime
                    } else {
                        Log.e(tag, "❌ Error guardando en Firestore: $error")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Exception saving to Firestore: ${e.message}")
        }
    }
}