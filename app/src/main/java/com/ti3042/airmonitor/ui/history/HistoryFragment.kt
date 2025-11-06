package com.ti3042.airmonitor.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ti3042.airmonitor.R

/**
 * 📊 HistoryFragment - Historial y Gráficos TI3042
 * 
 * Pantalla para mostrar:
 * - Gráficos de tendencias PPM
 * - Estadísticas históricas 
 * - Log de eventos recientes
 * - Controles de tiempo (1H, 6H, 24H, 7D)
 */
class HistoryFragment : Fragment() {

    companion object {
        private const val TAG = "HistoryFragment"
        
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    // UI References
    private var btnBack: TextView? = null
    private var btnExportData: TextView? = null
    
    // Time range buttons
    private var btnRange1h: TextView? = null
    private var btnRange6h: TextView? = null
    private var btnRange24h: TextView? = null
    private var btnRange7d: TextView? = null
    
    // Statistics
    private var tvStatsAverage: TextView? = null
    private var tvStatsMaximum: TextView? = null
    private var tvStatsMinimum: TextView? = null
    
    // Events container
    private var eventsContainer: LinearLayout? = null
    
    // Current selected range
    private var selectedTimeRange = "1H"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "Creating HistoryFragment view")
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        loadHistoryData()
        
        Log.d(TAG, "✅ HistoryFragment initialized successfully")
    }

    private fun initViews(view: View) {
        // Navigation
        btnBack = view.findViewById(R.id.btn_back_history)
        btnExportData = view.findViewById(R.id.btn_export_data)
        
        // Time range buttons
        btnRange1h = view.findViewById(R.id.btn_range_1h)
        btnRange6h = view.findViewById(R.id.btn_range_6h)
        btnRange24h = view.findViewById(R.id.btn_range_24h)
        btnRange7d = view.findViewById(R.id.btn_range_7d)
        
        // Statistics
        tvStatsAverage = view.findViewById(R.id.tvStatsAverage)
        tvStatsMaximum = view.findViewById(R.id.tvStatsMaximum)
        tvStatsMinimum = view.findViewById(R.id.tvStatsMinimum)
        
        // Events
        eventsContainer = view.findViewById(R.id.events_container)
        
        Log.d(TAG, "Views initialized")
    }

    private fun setupListeners() {
        // Back button
        btnBack?.setOnClickListener {
            Log.d(TAG, "Back button pressed")
            parentFragmentManager.popBackStack()
        }
        
        // Export data button
        btnExportData?.setOnClickListener {
            Log.d(TAG, "Export data pressed")
            exportHistoryData()
        }
        
        // Time range buttons
        btnRange1h?.setOnClickListener { selectTimeRange("1H") }
        btnRange6h?.setOnClickListener { selectTimeRange("6H") }
        btnRange24h?.setOnClickListener { selectTimeRange("24H") }
        btnRange7d?.setOnClickListener { selectTimeRange("7D") }
        
        Log.d(TAG, "Listeners configured")
    }

    /**
     * 📊 Seleccionar rango de tiempo para análisis
     */
    private fun selectTimeRange(range: String) {
        Log.d(TAG, "Time range selected: $range")
        selectedTimeRange = range
        
        // Reset all buttons
        resetTimeRangeButtons()
        
        // Highlight selected button
        when (range) {
            "1H" -> highlightButton(btnRange1h)
            "6H" -> highlightButton(btnRange6h)
            "24H" -> highlightButton(btnRange24h)
            "7D" -> highlightButton(btnRange7d)
        }
        
        // Reload data for selected range
        loadHistoryData()
    }

    private fun resetTimeRangeButtons() {
        val buttons = listOf(btnRange1h, btnRange6h, btnRange24h, btnRange7d)
        buttons.forEach { button ->
            button?.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface_variant)
            )
            button?.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
    }

    private fun highlightButton(button: TextView?) {
        button?.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_50)
        )
        button?.setTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
        )
    }

    /**
     * 📊 Cargar datos históricos simulados
     */
    private fun loadHistoryData() {
        try {
            Log.d(TAG, "Loading history data for range: $selectedTimeRange")
            
            // Simular datos según el rango seleccionado
            val (average, maximum, minimum) = getSimulatedStats(selectedTimeRange)
            
            // Actualizar estadísticas
            tvStatsAverage?.text = average.toString()
            tvStatsMaximum?.text = maximum.toString()
            tvStatsMinimum?.text = minimum.toString()
            
            // Actualizar color del máximo según el nivel
            tvStatsMaximum?.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    requireContext(), 
                    when {
                        maximum >= 400 -> R.color.air_quality_critical
                        maximum >= 250 -> R.color.air_quality_poor
                        maximum >= 150 -> R.color.air_quality_moderate
                        else -> R.color.air_quality_good
                    }
                )
            )
            
            // Generar eventos recientes simulados
            generateSimulatedEvents()
            
            Log.d(TAG, "✅ History data loaded - Avg: $average, Max: $maximum, Min: $minimum")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading history data: ${e.message}")
        }
    }

    /**
     * 📊 Generar estadísticas simuladas según el rango
     */
    private fun getSimulatedStats(range: String): Triple<Int, Int, Int> {
        return when (range) {
            "1H" -> Triple(187, 245, 125)   // Última hora - datos variables
            "6H" -> Triple(195, 280, 118)   // 6 horas - más variabilidad
            "24H" -> Triple(205, 320, 105)  // 24 horas - incluye picos nocturnos
            "7D" -> Triple(225, 450, 95)    // 7 días - máximos críticos incluidos
            else -> Triple(187, 245, 125)
        }
    }

    /**
     * 📝 Generar eventos simulados recientes
     */
    private fun generateSimulatedEvents() {
        try {
            eventsContainer?.removeAllViews()
            
            // Eventos simulados basados en el rango seleccionado
            val events = when (selectedTimeRange) {
                "1H" -> listOf(
                    Event("🚨", "15:32", "Alerta: PPM crítico detectado (420 PPM)", R.color.air_quality_critical),
                    Event("🌪️", "15:30", "Sistema: Ventilador activado automáticamente", R.color.secondary),
                    Event("✅", "15:25", "Calidad: Vuelta a nivel normal (145 PPM)", R.color.air_quality_good),
                    Event("ℹ️", "15:20", "Info: Modo automático activado por usuario", R.color.text_secondary)
                )
                "6H" -> listOf(
                    Event("🚨", "15:32", "Alerta: PPM crítico detectado (420 PPM)", R.color.air_quality_critical),
                    Event("⚠️", "12:45", "Advertencia: PPM moderado mantenido (280 PPM)", R.color.air_quality_moderate),
                    Event("🌪️", "11:30", "Sistema: Ventilador activación automática", R.color.secondary),
                    Event("📊", "10:15", "Reporte: Promedio 6h = 195 PPM", R.color.text_secondary),
                    Event("✅", "09:45", "Calidad: Nivel bueno recuperado", R.color.air_quality_good)
                )
                "24H" -> listOf(
                    Event("🚨", "15:32", "Alerta: PPM crítico detectado (420 PPM)", R.color.air_quality_critical),
                    Event("🌙", "03:20", "Nocturno: Pico de contaminación detectado (320 PPM)", R.color.air_quality_poor),
                    Event("📈", "22:15", "Tendencia: Incremento gradual durante noche", R.color.air_quality_moderate),
                    Event("☀️", "08:30", "Matutino: Mejora calidad aire (150 PPM)", R.color.air_quality_good),
                    Event("🔄", "00:00", "Sistema: Reporte diario generado", R.color.text_secondary)
                )
                else -> listOf(
                    Event("📊", "Hoy", "Reporte semanal: 2 alertas críticas registradas", R.color.air_quality_critical),
                    Event("📈", "Ayer", "Tendencia: Incremento promedio +15 PPM/día", R.color.air_quality_moderate),
                    Event("⚠️", "3 días", "Patrón: Picos nocturnos recurrentes identificados", R.color.air_quality_poor),
                    Event("✅", "5 días", "Sistema: Funcionamiento óptimo ventilación", R.color.secondary),
                    Event("🔧", "7 días", "Mantenimiento: Calibración sensores completada", R.color.text_secondary)
                )
            }
            
            // Agregar eventos al container
            events.forEach { event ->
                addEventToContainer(event)
            }
            
            Log.d(TAG, "✅ Generated ${events.size} events for $selectedTimeRange")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating events: ${e.message}")
        }
    }

    private fun addEventToContainer(event: Event) {
        val context = this.context ?: return
        
        val eventView = TextView(context).apply {
            text = "${event.icon} ${event.time} - ${event.message}"
            textSize = 12f
            setPadding(24, 24, 24, 24)
            
            // Background color with alpha
            val color = androidx.core.content.ContextCompat.getColor(context, event.colorRes)
            setBackgroundColor(android.graphics.Color.argb(32, 
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color)))
            
            // Text color
            setTextColor(androidx.core.content.ContextCompat.getColor(context, event.colorRes))
            
            // Layout params
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        
        eventsContainer?.addView(eventView)
    }

    /**
     * 📤 Exportar datos históricos
     */
    private fun exportHistoryData() {
        try {
            Log.d(TAG, "Exporting history data for range: $selectedTimeRange")
            
            // TODO: Implementar exportación real de datos
            // Por ahora solo mostrar confirmación
            
            android.widget.Toast.makeText(
                context, 
                "📤 Exportando datos de $selectedTimeRange...\n(Funcionalidad en desarrollo)", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            Log.d(TAG, "✅ Export initiated")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error exporting data: ${e.message}")
        }
    }

    /**
     * 📊 Data class para eventos
     */
    private data class Event(
        val icon: String,
        val time: String,
        val message: String,
        val colorRes: Int
    )
}