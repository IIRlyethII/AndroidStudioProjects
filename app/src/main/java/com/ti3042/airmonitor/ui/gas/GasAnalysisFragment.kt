package com.ti3042.airmonitor.ui.gas

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ti3042.airmonitor.R
import com.ti3042.airmonitor.data.MockDataService
import com.ti3042.airmonitor.models.SensorData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class GasAnalysisFragment : Fragment() {
    
    private val tag = "GasAnalysisFragment"
    private lateinit var mockDataService: MockDataService
    
    // Data class for gas information
    data class GasData(
        val id: String,
        val name: String,
        val symbol: String,
        val current: Float,
        val normalRange: Pair<Float, Float>,
        val warningRange: Pair<Float, Float>,
        val criticalRange: Pair<Float, Float>,
        val category: GasCategory,
        val description: String,
        val sources: String,
        val effects: String,
        val safety: String,
        val unit: String = "%"
    )
    
    enum class GasCategory {
        COMMON, UNCOMMON, UNKNOWN
    }
    
    enum class GasStatus {
        NORMAL, WARNING, CRITICAL
    }
    
    // Active filters
    private var activeCategory: GasCategory? = null
    private var activeStatus: GasStatus? = null
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    // Referencias simples basadas en IDs del layout actual
    private var btnBackAnalysis: TextView? = null
    private var tvOverallStatus: TextView? = null
    private var tvAnalysisTimestamp: TextView? = null
    private var tvTotalPpm: TextView? = null
    private var tvDetectedGases: TextView? = null
    private var tvAlertCount: TextView? = null
    
    // Filter buttons (usando IDs del layout actual)
    private var btnFilterCommon: TextView? = null
    private var btnFilterUncommon: TextView? = null
    private var btnFilterUnknown: TextView? = null
    private var btnFilterNormal: TextView? = null
    private var btnFilterWarning: TextView? = null
    private var btnFilterCritical: TextView? = null
    
    // Gas analysis elements (usando IDs del layout actual)
    private var tvO2Value: TextView? = null
    private var progressO2Detailed: View? = null
    private var tvCO2ValueDetailed: TextView? = null
    private var progressCO2Detailed: View? = null
    private var gasContainer: LinearLayout? = null
    
    // Gases detectables por el MQ-135 según especificaciones técnicas
    private val gasDatabase = mutableMapOf<String, GasData>(
        // SOLO VAPOR DE AGUA COMO GAS ATMOSFÉRICO BASE (COMMON)
        "vapor" to GasData(
            "vapor", "Vapor de Agua", "H₂O", 2.5f,
            Pair(1f, 4f), Pair(0.5f, 1f), Pair(0f, 0.5f),
            GasCategory.COMMON, "Humedad ambiental esencial para confort",
            "Respiración, evaporación, actividades domésticas, clima",
            "Niveles bajos causan sequedad. Niveles altos favorecen moho",
            "Mantener entre 40-60% humedad relativa para confort óptimo"
        ),
        
        // GASES PELIGROSOS Y ATMOSFÉRICOS (UNCOMMON)
        "o2" to GasData(
            "o2", "Oxígeno", "O₂", 20.9f, 
            Pair(19f, 22f), Pair(16f, 19f), Pair(0f, 16f),
            GasCategory.UNCOMMON, "Gas vital para respiración celular y combustión",
            "Fotosíntesis de plantas, liberación desde cuerpos de agua",
            "Esencial para la vida. Concentraciones bajas causan asfixia",
            "Niveles inferiores al 16% son peligrosos para humanos"
        ),
        "co2" to GasData(
            "co2", "Dióxido de Carbono", "CO₂", 0.04f,
            Pair(0.03f, 0.1f), Pair(0.1f, 0.5f), Pair(0.5f, 5f),
            GasCategory.UNCOMMON, "Subproducto de respiración y combustión",
            "Respiración, combustión, fermentación, actividad volcánica",
            "Concentraciones altas causan somnolencia, mareos y asfixia",
            "Niveles superiores al 0.5% son peligrosos en espacios cerrados"
        ),
        "co_ppm" to GasData(
            "co_ppm", "Monóxido de Carbono", "CO", 0.001f,
            Pair(0f, 0.01f), Pair(0.01f, 0.05f), Pair(0.05f, 1f),
            GasCategory.UNCOMMON, "Gas tóxico invisible e inodoro de combustión incompleta",
            "Vehículos, calentadores defectuosos, braseros, chimeneas mal ventiladas",
            "Se une a la hemoglobina impidiendo transporte de oxígeno. Puede ser letal",
            "Cualquier concentración detectable requiere ventilación inmediata"
        ),

        "ammonia" to GasData(
            "ammonia", "Amoníaco", "NH₃", 0.003f,
            Pair(0f, 0.002f), Pair(0.002f, 0.01f), Pair(0.01f, 0.05f),
            GasCategory.UNCOMMON, "Gas alcalino con olor penetrante característico",
            "Productos de limpieza, fertilizantes, procesos industriales",
            "Irritación de ojos, nariz y garganta. Quemaduras químicas",
            "Uso con ventilación adecuada. Evitar inhalación directa"
        ),
        "nox" to GasData(
            "nox", "Óxidos de Nitrógeno", "NOₓ", 0.002f,
            Pair(0f, 0.005f), Pair(0.005f, 0.02f), Pair(0.02f, 0.1f),
            GasCategory.UNCOMMON, "Gases reactivos de combustión a alta temperatura",
            "Vehículos, plantas de energía, procesos industriales",
            "Irritación respiratoria, contribuye al smog y lluvia ácida",
            "Indicador de contaminación vehicular. Ventilación necesaria"
        ),
        
        // COMPUESTOS VOLÁTILES Y OTROS (UNKNOWN)
        "smoke" to GasData(
            "smoke", "Humo/Partículas", "PM", 0.05f,
            Pair(0f, 0.1f), Pair(0.1f, 0.5f), Pair(0.5f, 5f),
            GasCategory.UNKNOWN, "Partículas sólidas y gases de combustión incompleta",
            "Cigarrillos, incendios, motores diésel, cocina",
            "Irritación respiratoria, problemas cardiovasculares",
            "Evacuar área si se detectan niveles altos. Buscar fuente"
        ),
        "toluene" to GasData(
            "toluene", "Tolueno", "C₇H₈", 0.001f,
            Pair(0f, 0.003f), Pair(0.003f, 0.01f), Pair(0.01f, 0.1f),
            GasCategory.UNKNOWN, "Solvente aromático derivado del petróleo. Neurotóxico",
            "Pinturas, adhesivos, combustibles, productos de limpieza",
            "Mareos, dolor de cabeza, efectos en sistema nervioso central",
            "Usar en áreas ventiladas. Evitar exposición prolongada"
        ),
    )
    
    companion object {
        fun newInstance(): GasAnalysisFragment {
            return GasAnalysisFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(tag, "🧪 Creating Gas Analysis Fragment")
        val view = inflater.inflate(R.layout.fragment_gas_analysis, container, false)
        
        initViews(view)
        setupDataServices()
        setupNavigationListeners()
        loadGasData()
        
        return view
    }
    
    private fun initViews(view: View) {
        Log.d(tag, "🔧 Initializing views")
        
        // Usar IDs que realmente existen en el layout
        btnBackAnalysis = view.findViewById(R.id.btn_back_analysis)
        tvOverallStatus = view.findViewById(R.id.tv_overall_status)
        tvAnalysisTimestamp = view.findViewById(R.id.tv_analysis_timestamp)
        tvTotalPpm = view.findViewById(R.id.tv_total_ppm)
        tvDetectedGases = view.findViewById(R.id.tv_detected_gases)
        tvAlertCount = view.findViewById(R.id.tv_alert_count)
        
        // Filter buttons
        btnFilterCommon = view.findViewById(R.id.btn_filter_common)
        btnFilterUncommon = view.findViewById(R.id.btn_filter_uncommon)
        btnFilterUnknown = view.findViewById(R.id.btn_filter_unknown)
        btnFilterNormal = view.findViewById(R.id.btn_filter_normal)
        btnFilterWarning = view.findViewById(R.id.btn_filter_warning)
        btnFilterCritical = view.findViewById(R.id.btn_filter_critical)
        
        // Gas elements
        tvO2Value = view.findViewById(R.id.tv_o2_value)
        progressO2Detailed = view.findViewById(R.id.progress_o2_detailed)
        tvCO2ValueDetailed = view.findViewById(R.id.tv_co2_value_detailed)
        progressCO2Detailed = view.findViewById(R.id.progress_co2_detailed)
        
        // Create dynamic gas container after the existing cards
        createDynamicGasContainer(view)
        
        Log.d(tag, "✅ Views initialized successfully")
    }
    
    private fun setupDataServices() {
        try {
            Log.d(tag, "🔧 Setting up data services")
            
            // Initialize MockDataService
            mockDataService = MockDataService.getInstance()
            mockDataService.initialize(requireContext())
            
            // SOLO iniciar simulación si está habilitada en configuración
            if (MockDataService.shouldUseMockData(requireContext())) {
                Log.d(tag, "✅ Simulación habilitada - iniciando datos simulados")
                mockDataService.startDataSimulation(
                    onDataReceived = { sensorData ->
                        activity?.runOnUiThread {
                            updateAnalysisSummary(sensorData)
                            updateDetailedAnalysis(sensorData)
                        }
                    },
                    onConnected = {
                        Log.d(tag, "📡 Data simulation connected")
                    }
                )
            } else {
                Log.d(tag, "❌ Simulación deshabilitada - usando datos reales del ESP32")
                // TODO: Conectar con datos reales del ESP32
                setupRealDataConnection()
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error setting up data services: ${e.message}")
        }
    }
    
    /**
     * 🔌 Configurar conexión con datos reales del ESP32
     */
    private fun setupRealDataConnection() {
        // Por ahora, mostrar datos estáticos cuando no hay simulación
        Log.d(tag, "🔌 Configurando conexión real con ESP32...")
        // Aquí iría la lógica de conexión Bluetooth/WiFi real
        
        // Mostrar estado de "esperando conexión real"
        updateAnalysisSummaryStatic()
        updateDetailedAnalysisStatic()
    }
    

    
    private fun setupNavigationListeners() {
        btnBackAnalysis?.setOnClickListener {
            Log.d(tag, "🔙 Navigating back to dashboard")
            parentFragmentManager.popBackStack()
        }
        
        // Category filters
        btnFilterCommon?.setOnClickListener {
            toggleCategoryFilter(GasCategory.COMMON)
        }
        
        btnFilterUncommon?.setOnClickListener {
            toggleCategoryFilter(GasCategory.UNCOMMON)
        }
        
        btnFilterUnknown?.setOnClickListener {
            toggleCategoryFilter(GasCategory.UNKNOWN)
        }
        
        // Status filters
        btnFilterNormal?.setOnClickListener {
            toggleStatusFilter(GasStatus.NORMAL)
        }
        
        btnFilterWarning?.setOnClickListener {
            toggleStatusFilter(GasStatus.WARNING)
        }
        
        btnFilterCritical?.setOnClickListener {
            toggleStatusFilter(GasStatus.CRITICAL)
        }
    }
    
    private fun loadGasData() {
        Log.d(tag, "🧪 Loading gas analysis data")
        // This method will be called to load and display gas data
    }
    
    private fun updateAnalysisSummary(sensorData: SensorData) {
        try {
            // Overall status based on PPM
            val overallStatus = when {
                sensorData.airQuality.ppm >= 400 -> "🔴 Crítico"
                sensorData.airQuality.ppm >= 300 -> "🟠 Alerta"
                sensorData.airQuality.ppm >= 200 -> "🟡 Moderado"
                else -> "🟢 Normal"
            }
            tvOverallStatus?.text = overallStatus
            
            // Last updated timestamp
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            tvAnalysisTimestamp?.text = "Actualizado: ${dateFormat.format(Date())}"
            
            // Analysis statistics
            tvTotalPpm?.text = "${sensorData.airQuality.ppm}"
            tvDetectedGases?.text = "6"
            tvAlertCount?.text = if (sensorData.airQuality.ppm > 300) "2" else "0"
            
            Log.d(tag, "📊 Analysis summary updated")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error updating analysis summary: ${e.message}")
        }
    }
    
    private fun updateDetailedAnalysis(sensorData: SensorData) {
        try {
            val ppm = sensorData.airQuality.ppm
            
            // Use values from gas database to ensure consistency
            val oxygenData = gasDatabase["o2"]
            val co2Data = gasDatabase["co2"]
            
            val oxygenLevel = oxygenData?.current ?: 20.9f
            val co2Level = co2Data?.current ?: 0.04f
            
            tvO2Value?.text = String.format("%.1f%%", oxygenLevel)
            tvCO2ValueDetailed?.text = String.format("%.3f%%", co2Level)
            
            // Update progress bars based on values
            progressO2Detailed?.layoutParams = progressO2Detailed?.layoutParams?.apply {
                width = ((oxygenLevel / 25.0f) * 200).toInt().coerceIn(20, 200)
            }
            
            progressCO2Detailed?.layoutParams = progressCO2Detailed?.layoutParams?.apply {
                width = ((co2Level / 1.0f) * 200).toInt().coerceIn(5, 200)
            }
            
            Log.d(tag, "📈 Detailed analysis updated")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error updating detailed analysis: ${e.message}")
        }
    }
    

    
    /**
     * 🏗️ Crear contenedor dinámico para todas las tarjetas de gases
     */
    private fun createDynamicGasContainer(view: View) {
        try {
            val mainLayout = view.findViewById<LinearLayout>(R.id.main_container) 
                ?: (view as? ScrollView)?.getChildAt(0) as? LinearLayout
                ?: (view as? ViewGroup)?.getChildAt(0) as? LinearLayout
            
            if (mainLayout == null) {
                Log.e(tag, "❌ No se pudo encontrar el contenedor principal")
                return
            }
            
            // Crear nuevo contenedor para los gases
            gasContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Agregar el contenedor antes del último elemento (sensor info)
            val insertIndex = maxOf(0, mainLayout.childCount - 1)
            mainLayout.addView(gasContainer, insertIndex)
            
            // Generar todas las tarjetas de gases
            generateAllGasCards()
            
            Log.d(tag, "✅ Contenedor dinámico de gases creado")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error creando contenedor dinámico: ${e.message}")
        }
    }
    
    /**
     * 🃏 Generar todas las tarjetas de gases
     */
    private fun generateAllGasCards() {
        try {
            gasContainer?.removeAllViews()
            
            // Agrupar por categorías, excluyendo gases que ya están en el layout
            val excludedGases = setOf("o2", "co2") // Estos ya están hardcodeados en el layout
            val categorizedGases = gasDatabase.values
                .filter { it.id !in excludedGases }
                .groupBy { it.category }
            
            categorizedGases.forEach { (category, gases) ->
                val categoryTitle = when (category) {
                    GasCategory.COMMON -> "� Humedad Ambiental"
                    GasCategory.UNCOMMON -> "⚠️ Gases Peligrosos y Atmosféricos"
                    GasCategory.UNKNOWN -> "🔍 Compuestos Volátiles"
                }
                
                // Crear tarjeta de categoría
                val categoryCard = createCategoryCard(categoryTitle, gases)
                gasContainer?.addView(categoryCard)
            }
            
            // Iniciar actualizaciones en tiempo real
            startRealTimeUpdates()
            
            Log.d(tag, "✅ ${gasDatabase.size} tarjetas de gases generadas")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error generando tarjetas: ${e.message}")
        }
    }
    
    /**
     * 🃏 Crear tarjeta de categoría con gases
     */
    private fun createCategoryCard(title: String, gases: List<GasData>): CardView {
        val categoryCard = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            radius = 12.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
        }
        
        val categoryLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }
        
        // Título de categoría
        val categoryTitle = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dpToPx()
            }
        }
        categoryLayout.addView(categoryTitle)
        
        // Agregar cada gas
        gases.forEach { gas ->
            val gasCard = createGasItemCard(gas)
            categoryLayout.addView(gasCard)
        }
        
        categoryCard.addView(categoryLayout)
        return categoryCard
    }
    
    /**
     * 🧪 Crear tarjeta individual de gas
     */
    private fun createGasItemCard(gas: GasData): View {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_variant))
            setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
            tag = gas.id // Para identificar en filtros
        }
        
        // Header con nombre y valor
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }
        
        val gasName = TextView(requireContext()).apply {
            text = "${gas.name} (${gas.symbol})"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        val gasValue = TextView(requireContext()).apply {
            text = "${String.format("%.2f", gas.current)}${gas.unit}"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
            tag = "gas_value_${gas.id}" // Para actualizar dinámicamente
        }
        
        headerLayout.addView(gasName)
        headerLayout.addView(gasValue)
        
        // Indicador de rango (ProgressBar personalizada)
        val rangeIndicator = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8.dpToPx()
            ).apply {
                bottomMargin = 4.dpToPx()
            }
            tag = "progress_${gas.id}"
        }
        
        // Etiquetas de rangos
        val rangeLabels = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }
        
        val labels = listOf(
            "Normal: ${gas.normalRange.first}-${gas.normalRange.second}${gas.unit}",
            "Alerta: ${gas.warningRange.first}-${gas.warningRange.second}${gas.unit}",
            "Crítico: >${gas.criticalRange.first}${gas.unit}"
        )
        
        labels.forEach { label ->
            val labelView = TextView(requireContext()).apply {
                text = label
                textSize = 9f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    when (labels.indexOf(label)) {
                        0 -> gravity = android.view.Gravity.START
                        1 -> gravity = android.view.Gravity.CENTER
                        2 -> gravity = android.view.Gravity.END
                    }
                }
            }
            rangeLabels.addView(labelView)
        }
        
        // Descripción y botón info
        val descriptionLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val status = getGasStatus(gas)
        val statusIcon = when (status) {
            GasStatus.NORMAL -> "🟢"
            GasStatus.WARNING -> "🟡"
            GasStatus.CRITICAL -> "🔴"
        }
        val statusText = when (status) {
            GasStatus.NORMAL -> "NORMAL"
            GasStatus.WARNING -> "ALERTA"
            GasStatus.CRITICAL -> "CRÍTICO"
        }
        
        val description = TextView(requireContext()).apply {
            text = "$statusIcon $statusText • ${gas.description}"
            textSize = 11f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            tag = "gas_desc_${gas.id}" // Tag para poder actualizar dinámicamente
        }
        
        val infoButton = Button(requireContext()).apply {
            text = "ℹ"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.filter_button)
            layoutParams = LinearLayout.LayoutParams(
                32.dpToPx(),
                32.dpToPx()
            ).apply {
                setMargins(8.dpToPx(), 0, 0, 0)
            }
            setPadding(0, 0, 0, 0)
            elevation = 4.dpToPx().toFloat()
            setOnClickListener {
                showGasInfo(gas)
            }
        }
        
        descriptionLayout.addView(description)
        descriptionLayout.addView(infoButton)
        
        // Ensamblar tarjeta
        itemLayout.addView(headerLayout)
        itemLayout.addView(rangeIndicator)
        itemLayout.addView(rangeLabels)
        itemLayout.addView(descriptionLayout)
        
        // Actualizar colores según estado
        updateGasItemColors(gas, itemLayout)
        
        return itemLayout
    }
    
    /**
     * 🎨 Actualizar colores de tarjeta según estado del gas
     */
    private fun updateGasItemColors(gas: GasData, itemLayout: View) {
        val status = getGasStatus(gas)
        val valueView = itemLayout.findViewWithTag<TextView>("gas_value_${gas.id}")
        val progressView = itemLayout.findViewWithTag<ProgressBar>("progress_${gas.id}")
        
        val (textColor, backgroundColor, progressColor) = when (status) {
            GasStatus.NORMAL -> Triple(
                ContextCompat.getColor(requireContext(), R.color.air_quality_good),
                Color.parseColor("#4CAF5020"),
                ContextCompat.getColor(requireContext(), R.color.air_quality_good)
            )
            GasStatus.WARNING -> Triple(
                ContextCompat.getColor(requireContext(), R.color.air_quality_moderate),
                Color.parseColor("#FF980020"),
                ContextCompat.getColor(requireContext(), R.color.air_quality_moderate)
            )
            GasStatus.CRITICAL -> Triple(
                ContextCompat.getColor(requireContext(), R.color.air_quality_critical),
                Color.parseColor("#F4433620"),
                ContextCompat.getColor(requireContext(), R.color.air_quality_critical)
            )
        }
        
        valueView?.apply {
            setTextColor(textColor)
            setBackgroundColor(backgroundColor)
        }
        
        progressView?.apply {
            progressTintList = ColorStateList.valueOf(progressColor)
            // Calcular progreso más inteligente basado en el estado actual
            val progressPercent = when {
                gas.current <= gas.normalRange.second -> {
                    // En rango normal: 0-33%
                    ((gas.current / gas.normalRange.second) * 33f).toInt().coerceIn(0, 33)
                }
                gas.current <= gas.warningRange.second -> {
                    // En rango de alerta: 34-66%
                    val warningProgress = ((gas.current - gas.normalRange.second) / 
                        (gas.warningRange.second - gas.normalRange.second)) * 33f
                    (33 + warningProgress).toInt().coerceIn(34, 66)
                }
                else -> {
                    // En rango crítico: 67-100%
                    val criticalMax = maxOf(gas.criticalRange.second, gas.current * 1.2f)
                    val criticalProgress = ((gas.current - gas.warningRange.second) / 
                        (criticalMax - gas.warningRange.second)) * 34f
                    (66 + criticalProgress).toInt().coerceIn(67, 100)
                }
            }
            progress = progressPercent
            max = 100
        }
    }
    
    /**
     * 📊 Obtener estado actual del gas
     */
    private fun getGasStatus(gas: GasData): GasStatus {
        return when {
            gas.current >= gas.criticalRange.first -> GasStatus.CRITICAL
            gas.current >= gas.warningRange.first -> GasStatus.WARNING  
            gas.current >= gas.normalRange.first && gas.current <= gas.normalRange.second -> GasStatus.NORMAL
            else -> GasStatus.NORMAL // Por defecto si está fuera de todos los rangos
        }
    }
    
    /**
     * 🔄 Iniciar actualizaciones en tiempo real (SOLO si simulación está activa)
     */
    private fun startRealTimeUpdates() {
        if (MockDataService.shouldUseMockData(requireContext())) {
            Log.d(tag, "🎮 Iniciando actualizaciones simuladas")
            updateRunnable = object : Runnable {
                override fun run() {
                    // Solo actualizar si la simulación sigue activa
                    if (MockDataService.shouldUseMockData(requireContext())) {
                        updateGasValues()
                        updateHandler.postDelayed(this, 5000) // Actualizar cada 5 segundos
                    } else {
                        Log.d(tag, "⏹️ Simulación deshabilitada - deteniendo actualizaciones")
                    }
                }
            }
            updateHandler.post(updateRunnable!!)
        } else {
            Log.d(tag, "🔌 Modo real activo - no hay actualizaciones simuladas")
        }
    }
    
    /**
     * 📈 Actualizar valores de gases con variación realista (SOLO EN SIMULACIÓN)
     */
    private fun updateGasValues() {
        try {
            // VERIFICAR si la simulación está activa antes de actualizar
            if (!MockDataService.shouldUseMockData(requireContext())) {
                Log.d(tag, "🔌 Simulación deshabilitada - no actualizando valores")
                return
            }
            
            gasDatabase.forEach { (id, gas) ->
                // Agregar variación realista SOLO en modo simulación
                val variation = Random.nextFloat() * 0.1f - 0.05f // ±5%
                val newValue = (gas.current + variation).coerceAtLeast(0f)
                gasDatabase[id] = gas.copy(current = newValue)
                
                // Actualizar UI - valor
                gasContainer?.findViewWithTag<TextView>("gas_value_$id")?.apply {
                    text = "${String.format("%.2f", newValue)}${gas.unit}"
                }
                
                // Actualizar UI - descripción con estado
                gasContainer?.findViewWithTag<TextView>("gas_desc_$id")?.apply {
                    val updatedGas = gasDatabase[id]!!
                    val status = getGasStatus(updatedGas)
                    val statusIcon = when (status) {
                        GasStatus.NORMAL -> "🟢"
                        GasStatus.WARNING -> "🟡"
                        GasStatus.CRITICAL -> "🔴"
                    }
                    val statusText = when (status) {
                        GasStatus.NORMAL -> "NORMAL"
                        GasStatus.WARNING -> "ALERTA"
                        GasStatus.CRITICAL -> "CRÍTICO"
                    }
                    text = "$statusIcon $statusText • ${updatedGas.description}"
                }
                
                // Actualizar colores si cambió el estado
                gasContainer?.findViewWithTag<View>(id)?.let { itemView ->
                    updateGasItemColors(gasDatabase[id]!!, itemView)
                }
            }
            
            // Actualizar gases del layout (O2 y CO2)
            updateLayoutGasValues()
            
            // Actualizar estadísticas del resumen
            updateStatsSummary()
            
            Log.d(tag, "🎮 Valores simulados actualizados")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando valores: ${e.message}")
        }
    }
    
    /**
     * 📱 Actualizar gases hardcodeados en el layout
     */
    private fun updateLayoutGasValues() {
        try {
            val oxygenData = gasDatabase["o2"]
            val co2Data = gasDatabase["co2"]
            
            oxygenData?.let { gas ->
                val variation = Random.nextFloat() * 0.1f - 0.05f // ±5%
                val newValue = (gas.current + variation).coerceAtLeast(0f)
                gasDatabase["o2"] = gas.copy(current = newValue)
                tvO2Value?.text = String.format("%.1f%%", newValue)
            }
            
            co2Data?.let { gas ->
                val variation = Random.nextFloat() * 0.002f - 0.001f // ±0.001%
                val newValue = (gas.current + variation).coerceAtLeast(0f)
                gasDatabase["co2"] = gas.copy(current = newValue)
                tvCO2ValueDetailed?.text = String.format("%.3f%%", newValue)
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando gases del layout: ${e.message}")
        }
    }
    
    /**
     * 📊 Actualizar estadísticas del resumen
     */
    private fun updateStatsSummary() {
        try {
            val totalAlerts = gasDatabase.values.count { getGasStatus(it) != GasStatus.NORMAL }
            val detectedGases = gasDatabase.size
            val criticalCount = gasDatabase.values.count { getGasStatus(it) == GasStatus.CRITICAL }
            
            tvAlertCount?.text = totalAlerts.toString()
            tvDetectedGases?.text = detectedGases.toString()
            
            // Actualizar estado general
            val overallStatus = when {
                criticalCount > 0 -> "🔴 SITUACIÓN CRÍTICA"
                totalAlerts > 2 -> "🟡 ATENCIÓN REQUERIDA"
                else -> "🟢 AMBIENTE NORMAL"
            }
            tvOverallStatus?.text = overallStatus
            
        } catch (e: Exception) {
            Log.d(tag, "❌ Error actualizando estadísticas: ${e.message}")
        }
    }
    
    /**
     * 📊 Actualizar resumen con datos estáticos (modo real)
     */
    private fun updateAnalysisSummaryStatic() {
        try {
            tvOverallStatus?.text = "🔌 Esperando ESP32..."
            
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            tvAnalysisTimestamp?.text = "Última conexión: ${dateFormat.format(Date())}"
            
            tvTotalPpm?.text = "---"
            tvDetectedGases?.text = "0"
            tvAlertCount?.text = "0"
            
            Log.d(tag, "📊 Resumen estático actualizado (modo real)")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando resumen estático: ${e.message}")
        }
    }
    
    /**
     * 📈 Actualizar análisis detallado con datos estáticos (modo real)
     */
    private fun updateDetailedAnalysisStatic() {
        try {
            tvO2Value?.text = "---"
            tvCO2ValueDetailed?.text = "---%"
            
            // Resetear barras de progreso
            progressO2Detailed?.layoutParams = progressO2Detailed?.layoutParams?.apply {
                width = 0
            }
            
            progressCO2Detailed?.layoutParams = progressCO2Detailed?.layoutParams?.apply {
                width = 0
            }
            
            Log.d(tag, "📈 Análisis detallado estático actualizado")
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error actualizando análisis estático: ${e.message}")
        }
    }
    
    /**
     * 🔍 Alternar filtro de categoría
     */
    private fun toggleCategoryFilter(category: GasCategory) {
        activeCategory = if (activeCategory == category) null else category
        applyFilters()
        updateFilterButtons()
    }
    
    /**
     * 🔍 Alternar filtro de estado
     */
    private fun toggleStatusFilter(status: GasStatus) {
        activeStatus = if (activeStatus == status) null else status
        applyFilters()
        updateFilterButtons()
    }
    
    /**
     * 🎯 Aplicar filtros activos
     */
    private fun applyFilters() {
        try {
            gasContainer?.let { container ->
                for (i in 0 until container.childCount) {
                    val categoryCard = container.getChildAt(i) as? CardView ?: continue
                    val categoryLayout = categoryCard.getChildAt(0) as? LinearLayout ?: continue
                    
                    var hasVisibleItems = false
                    
                    // Iterar sobre items de gas (saltar el título en índice 0)
                    for (j in 1 until categoryLayout.childCount) {
                        val gasItem = categoryLayout.getChildAt(j)
                        val gasId = gasItem.tag as? String ?: continue
                        val gas = gasDatabase[gasId] ?: continue
                        
                        val categoryMatch = activeCategory == null || gas.category == activeCategory
                        val statusMatch = activeStatus == null || getGasStatus(gas) == activeStatus
                        
                        gasItem.visibility = if (categoryMatch && statusMatch) {
                            hasVisibleItems = true
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                    
                    // Mostrar/ocultar categoría completa
                    categoryCard.visibility = if (hasVisibleItems) View.VISIBLE else View.GONE
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error aplicando filtros: ${e.message}")
        }
    }
    
    /**
     * 🎨 Actualizar apariencia de botones de filtro
     */
    private fun updateFilterButtons() {
        // Resetear todos los botones
        listOf(btnFilterCommon, btnFilterUncommon, btnFilterUnknown).forEach { btn ->
            btn?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            btn?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        
        listOf(btnFilterNormal, btnFilterWarning, btnFilterCritical).forEach { btn ->
            btn?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            btn?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        
        // Resaltar botón activo de categoría
        when (activeCategory) {
            GasCategory.COMMON -> btnFilterCommon
            GasCategory.UNCOMMON -> btnFilterUncommon  
            GasCategory.UNKNOWN -> btnFilterUnknown
            null -> null
        }?.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            setTextColor(Color.WHITE)
        }
        
        // Resaltar botón activo de estado
        when (activeStatus) {
            GasStatus.NORMAL -> btnFilterNormal
            GasStatus.WARNING -> btnFilterWarning
            GasStatus.CRITICAL -> btnFilterCritical
            null -> null
        }?.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            setTextColor(Color.WHITE)
        }
    }
    
    /**
     * ℹ️ Mostrar información detallada del gas
     */
    private fun showGasInfo(gas: GasData) {
        val status = getGasStatus(gas)
        val statusIcon = when (status) {
            GasStatus.NORMAL -> "🟢"
            GasStatus.WARNING -> "🟡"
            GasStatus.CRITICAL -> "🔴"
        }
        val statusText = when (status) {
            GasStatus.NORMAL -> "NORMAL"
            GasStatus.WARNING -> "ALERTA"
            GasStatus.CRITICAL -> "CRÍTICO"
        }
        
        val simulationStatus = if (MockDataService.shouldUseMockData(requireContext())) {
            "🎮 SIMULACIÓN ACTIVA"
        } else {
            "🔌 DATOS REALES ESP32"
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("$statusIcon ${gas.name} (${gas.symbol})")
            .setMessage("""
                � ESTADO ACTUAL: $statusText
                📈 Valor: ${String.format("%.2f", gas.current)}${gas.unit}
                🔄 Fuente: $simulationStatus
                
                📋 DESCRIPCIÓN:
                ${gas.description}
                
                🏭 FUENTES COMUNES:
                ${gas.sources}
                
                ⚠️ EFECTOS EN LA SALUD:
                ${gas.effects}
                
                🛡️ MEDIDAS DE SEGURIDAD:
                ${gas.safety}
                
                � RANGOS DE REFERENCIA:
                🟢 Normal: ${gas.normalRange.first}-${gas.normalRange.second}${gas.unit}
                🟡 Alerta: ${gas.warningRange.first}-${gas.warningRange.second}${gas.unit}  
                🔴 Crítico: >${gas.criticalRange.first}${gas.unit}
                
                🏷️ Categoría: ${when(gas.category) {
                    GasCategory.COMMON -> "Atmosférico Común"
                    GasCategory.UNCOMMON -> "Contaminante Industrial"
                    GasCategory.UNKNOWN -> "Compuesto Desconocido"
                }}
            """.trimIndent())
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("⚠️ Más Info") { _, _ ->
                showExtendedGasInfo(gas)
            }
            .create()
        
        dialog.show()
    }
    
    /**
     * 📚 Mostrar información extendida del gas
     */
    private fun showExtendedGasInfo(gas: GasData) {
        val detailedInfo = when (gas.id) {
            "o2" -> """
                🫁 INFORMACIÓN MÉDICA:
                • Concentración atmosférica normal: 20.9%
                • Mínimo para supervivencia: 16%
                • Deficiencia causa: hipoxia, mareos, muerte
                
                🔬 PROPIEDADES QUÍMICAS:
                • Fórmula: O₂ (diatómico)
                • Peso molecular: 32 g/mol
                • Incoloro, inodoro, insípido
                
                📡 SENSOR MQ-135:
                • Detección: indirecta por combustión
                • Precisión: ±3%
                • Tiempo respuesta: <10s
            """
            
            "co2" -> """
                🌍 INFORMACIÓN AMBIENTAL:
                • Concentración atmosférica: ~420 ppm
                • Nivel indoor seguro: <1000 ppm
                • Contribuye al efecto invernadero
                
                🫁 EFECTOS FISIOLÓGICOS:
                • 1000-5000 ppm: somnolencia
                • 5000-40000 ppm: asfixia
                • >40000 ppm: inmediatamente peligroso
                
                📡 DETECCIÓN MQ-135:
                • Alta sensibilidad al CO₂
                • Respuesta rápida <30s
                • Calibración requerida cada 6 meses
            """
            
            "co" -> """
                ☠️ PELIGRO EXTREMO:
                • Conocido como "asesino silencioso"
                • Se une a hemoglobina 200x más que O₂
                • Cualquier concentración es peligrosa
                
                🚨 SÍNTOMAS DE ENVENENAMIENTO:
                • Dolor de cabeza, náuseas
                • Confusión, pérdida de conciencia
                • Muerte por asfixia celular
                
                🚑 ACCIÓN INMEDIATA:
                • Ventilar área inmediatamente
                • Buscar atención médica urgente
                • Usar detectores de CO en el hogar
            """
            
            else -> """
                📋 INFORMACIÓN GENERAL:
                Este compuesto requiere atención según su nivel detectado.
                
                🔬 PROPIEDADES:
                • Compuesto orgánico volátil
                • Puede ser tóxico en concentraciones altas
                • Requiere ventilación adecuada
                
                📞 CONTACTO DE EMERGENCIA:
                • Bomberos: 911
                • Centro de Toxicología: 7834-9898
                • Cruz Roja: 911
            """
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("📚 ${gas.name} - Información Extendida")
            .setMessage(detailedInfo.trimIndent())
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    /**
     * 📏 Convertir dp a px
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            updateRunnable?.let { updateHandler.removeCallbacks(it) }
            if (::mockDataService.isInitialized) {
                Log.d(tag, "🧪 Gas Analysis Fragment destroyed")
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error in onDestroy: ${e.message}")
        }
    }
}