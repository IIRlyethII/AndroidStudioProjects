package com.ti3042.airmonitor.feature.monitoring

import java.text.SimpleDateFormat
import java.util.*

/**
 * 📊 Datos principales de monitoreo
 */
data class MonitoringData(
    val totalReadings: Int,
    val averagePPM: Double,
    val maxPPM: Double,
    val minPPM: Double,
    val lastUpdateTime: Long
)

/**
 * 📈 Datos para gráficos
 */
data class ChartData(
    val timeLabels: List<String>,
    val ppmValues: List<Float>,
    val temperatureValues: List<Float>,
    val humidityValues: List<Float>
)

/**
 * 📊 Lectura histórica individual
 */
data class HistoricalReading(
    val timestamp: Long,
    val ppm: Int,
    val temperature: Double,
    val humidity: Int,
    val airQualityLevel: String
) {
    val formattedDate: String
        get() {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    
    val formattedTime: String
        get() {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
}

/**
 * 🧪 Datos de análisis estadístico
 */
data class AnalyticsData(
    val weeklyAverage: Double,
    val weeklyTrend: String, // "+12.5%", "-5.2%"
    val criticalHours: Int,
    val bestHour: String,
    val worstHour: String,
    val gasComposition: Map<String, Float>,
    val recommendations: List<String>
)

/**
 * 📋 Configuración de reporte
 */
data class ReportConfig(
    val title: String,
    val dateRange: Pair<Long, Long>,
    val includeCharts: Boolean = true,
    val includeStatistics: Boolean = true,
    val includeRecommendations: Boolean = true,
    val format: ReportFormat = ReportFormat.PDF
)

/**
 * 📄 Formatos de reporte disponibles
 */
enum class ReportFormat(val extension: String, val mimeType: String) {
    PDF(".pdf", "application/pdf"),
    EXCEL(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV(".csv", "text/csv")
}