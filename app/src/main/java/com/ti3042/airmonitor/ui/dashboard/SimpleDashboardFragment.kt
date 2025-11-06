package com.ti3042.airmonitor.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ti3042.airmonitor.R
import kotlin.random.Random

class SimpleDashboardFragment : Fragment() {
    
    private val tag = "SimpleDashboard"
    private var mainTextView: TextView? = null
    private var btnToggleFan: Button? = null
    private var btnToggleMode: Button? = null
    private var btnControls: Button? = null
    private val dataHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    // Datos simulados
    private var currentPPM = 245
    private var currentTemp = 23.5f
    private var currentHumidity = 62
    private var fanEnabled = true
    private var autoMode = true
    private var warningThreshold = 200
    private var criticalThreshold = 400
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateSimulatedData()
                dataHandler.postDelayed(this, 2000) // Actualizar cada 2 segundos
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Crear layout principal
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#F8F9FA"))
        }
        
        // TextView principal para datos
        mainTextView = TextView(requireContext()).apply {
            textSize = 14f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
            setTextColor(Color.parseColor("#212529"))
        }
        
        // Crear botones de control
        createControlButtons(mainLayout)
        
        // Agregar componentes al layout
        mainLayout.addView(mainTextView)
        
        updateDisplay()
        startSimulation()
        
        return mainLayout
    }
    
    private fun createControlButtons(parentLayout: LinearLayout) {
        // Layout horizontal para botones
        val buttonLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        
        // Botón toggle ventilador
        btnToggleFan = Button(requireContext()).apply {
            text = "🌀 Ventilador: ON"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setBackgroundColor(Color.parseColor("#28A745"))
            setTextColor(Color.WHITE)
            setOnClickListener { toggleFan() }
        }
        
        // Botón toggle modo
        btnToggleMode = Button(requireContext()).apply {
            text = "🤖 Modo: AUTO"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8
            }
            setBackgroundColor(Color.parseColor("#007BFF"))
            setTextColor(Color.WHITE)
            setOnClickListener { toggleMode() }
        }
        
        buttonLayout.addView(btnToggleFan)
        buttonLayout.addView(btnToggleMode)
        
        // Layout para botón de controles
        val controlsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }
        
        // Botón de controles avanzados
        btnControls = Button(requireContext()).apply {
            text = "⚙️ Configurar Umbrales"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.parseColor("#6C757D"))
            setTextColor(Color.WHITE)
            setOnClickListener { showControlsDialog() }
        }
        
        controlsLayout.addView(btnControls)
        
        // Agregar layouts de botones al layout principal
        parentLayout.addView(buttonLayout)
        parentLayout.addView(controlsLayout)
    }
    
    private fun startSimulation() {
        isRunning = true
        dataHandler.post(updateRunnable)
        Log.d(tag, "Simulación iniciada")
    }
    
    private fun updateSimulatedData() {
        // Simular variaciones naturales en los datos
        currentPPM += Random.nextInt(-15, 15)
        currentPPM = currentPPM.coerceIn(80, 500)
        
        currentTemp += Random.nextDouble(-0.5, 0.5).toFloat()
        currentTemp = currentTemp.coerceIn(18.0f, 28.0f)
        
        currentHumidity += Random.nextInt(-3, 3)
        currentHumidity = currentHumidity.coerceIn(40, 80)
        
        // Lógica automática del ventilador
        if (autoMode) {
            fanEnabled = currentPPM >= warningThreshold
        }
        
        activity?.runOnUiThread {
            updateDisplay()
            updateButtons()
        }
        
        Log.d(tag, "Datos actualizados - PPM: $currentPPM, Temp: $currentTemp, Fan: $fanEnabled, Auto: $autoMode")
    }
    
    private fun updateDisplay() {
        val airLevel = when {
            currentPPM <= 150 -> "BUENA" to "#4CAF50"
            currentPPM <= 300 -> "MODERADA" to "#FF9800"
            currentPPM <= 500 -> "MALA" to "#F44336"
            else -> "CRÍTICA" to "#9C27B0"
        }
        
        val status = if (fanEnabled) "⚡ ENCENDIDO" else "⏸️ APAGADO"
        val mode = if (autoMode) "🤖 AUTOMÁTICO" else "👤 MANUAL"
        val connection = "🟢 CONECTADO (Simulación)"
        
        val displayText = """
🌬️ Air Monitor TI3042 - TIEMPO REAL

$connection

📊 CALIDAD DEL AIRE
• PPM: $currentPPM (${airLevel.first})
• Temperatura: %.1f°C
• Humedad: $currentHumidity%%

🔧 SISTEMA
• Ventilador: $status
• Modo: $mode
• Tiempo activo: ${getUptime()}

⚙️ UMBRALES
• Advertencia: 200 PPM
• Crítico: 400 PPM

⚙️ UMBRALES
• Advertencia: $warningThreshold PPM
• Crítico: $criticalThreshold PPM

🔄 Actualizando cada 2 segundos...

💡 CONTROLES:
${if (autoMode) "• Modo automático activo" else "• Modo manual - Usa los botones"}
${if (autoMode) "• Ventilador se controla automáticamente" else "• Controla el ventilador manualmente"}
        """.trimIndent().format(currentTemp)
        
        mainTextView?.text = displayText
    }
    
    private fun updateButtons() {
        btnToggleFan?.apply {
            text = if (fanEnabled) "🌀 Ventilador: ON" else "⏸️ Ventilador: OFF"
            setBackgroundColor(
                if (fanEnabled) Color.parseColor("#28A745") 
                else Color.parseColor("#DC3545")
            )
            isEnabled = !autoMode // Solo habilitado en modo manual
            alpha = if (autoMode) 0.5f else 1.0f
        }
        
        btnToggleMode?.apply {
            text = if (autoMode) "🤖 Modo: AUTO" else "👤 Modo: MANUAL"
            setBackgroundColor(
                if (autoMode) Color.parseColor("#007BFF") 
                else Color.parseColor("#FFC107")
            )
            setTextColor(
                if (autoMode) Color.WHITE 
                else Color.parseColor("#212529")
            )
        }
    }
    
    private fun getUptime(): String {
        val uptimeMs = System.currentTimeMillis() % 3600000 // Reset cada hora para demo
        val minutes = (uptimeMs / 60000).toInt()
        val seconds = ((uptimeMs % 60000) / 1000).toInt()
        return "${minutes}m ${seconds}s"
    }
    
    private fun toggleFan() {
        if (!autoMode) {
            fanEnabled = !fanEnabled
            updateButtons()
            Log.d(tag, "Ventilador ${if (fanEnabled) "encendido" else "apagado"} manualmente")
        }
    }
    
    private fun toggleMode() {
        autoMode = !autoMode
        updateButtons()
        Log.d(tag, "Modo cambiado a ${if (autoMode) "automático" else "manual"}")
        
        // Mostrar mensaje informativo
        val message = if (autoMode) {
            "Modo automático activado\nEl ventilador se controlará según PPM ≥ $warningThreshold"
        } else {
            "Modo manual activado\nUsa el botón del ventilador para controlarlo"
        }
        
        showToast(message)
    }
    
    private fun showControlsDialog() {
        // Crear un diálogo simple para configurar umbrales
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val titleText = TextView(requireContext()).apply {
            text = "⚙️ Configuración de Umbrales"
            textSize = 18f
            setPadding(0, 0, 0, 16)
            setTextColor(Color.parseColor("#212529"))
        }
        
        val infoText = TextView(requireContext()).apply {
            text = "Umbral actual de advertencia: $warningThreshold PPM\nUmbral actual crítico: $criticalThreshold PPM\n\nEn modo automático, el ventilador se activa cuando PPM ≥ umbral de advertencia."
            textSize = 14f
            setPadding(0, 0, 0, 16)
            setTextColor(Color.parseColor("#6C757D"))
        }
        
        val btnPreset1 = Button(requireContext()).apply {
            text = "🟢 Configuración Estricta (150/300 PPM)"
            setOnClickListener { 
                setThresholds(150, 300)
                showToast("Configuración estricta aplicada")
            }
        }
        
        val btnPreset2 = Button(requireContext()).apply {
            text = "🟡 Configuración Normal (200/400 PPM)"
            setOnClickListener { 
                setThresholds(200, 400)
                showToast("Configuración normal aplicada")
            }
        }
        
        val btnPreset3 = Button(requireContext()).apply {
            text = "🔴 Configuración Permisiva (300/500 PPM)"
            setOnClickListener { 
                setThresholds(300, 500)
                showToast("Configuración permisiva aplicada")
            }
        }
        
        dialogLayout.addView(titleText)
        dialogLayout.addView(infoText)
        dialogLayout.addView(btnPreset1)
        dialogLayout.addView(btnPreset2)
        dialogLayout.addView(btnPreset3)
        
        // Mostrar en un AlertDialog simple
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .setNegativeButton("Cerrar", null)
            .create()
        
        dialog.show()
    }
    
    private fun setThresholds(warning: Int, critical: Int) {
        warningThreshold = warning
        criticalThreshold = critical
        Log.d(tag, "Umbrales actualizados - Warning: $warning, Critical: $critical")
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        dataHandler.removeCallbacks(updateRunnable)
        Log.d(tag, "Simulación detenida")
    }
}