package com.ti3042.airmonitor.data.models

import java.util.*

data class GasReading(
    val timestamp: Date,
    val value: Double,
    val gasType: String,
    val unit: String
)

data class GasReadingStatistics(
    val gasType: String,
    val averageValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val totalReadings: Int,
    val timeRange: Pair<Date, Date>
)

data class DashboardMetrics(
    val totalReadings: Int,
    val criticalAlerts: Int,
    val warningAlerts: Int,
    val normalReadings: Int,
    val lastUpdateTime: Date,
    val airQualityIndex: Int,
    val dominantGas: String?
)

data class AlertInfo(
    val gasType: String,
    val currentValue: Double,
    val threshold: Double,
    val severity: AlertSeverity,
    val timestamp: Date,
    val message: String
)

enum class AlertSeverity {
    NORMAL,
    WARNING, 
    CRITICAL
}

data class CalibrationResult(
    val success: Boolean,
    val accuracyPercentage: Double,
    val calibrationFactor: Double,
    val notes: String,
    val timestamp: Date
)