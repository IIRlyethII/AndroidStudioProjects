package com.ti3042.airmonitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ti3042.airmonitor.data.database.repository.SensorDataRepository
import com.ti3042.airmonitor.data.models.DashboardMetrics
import com.ti3042.airmonitor.data.models.AlertInfo
import com.ti3042.airmonitor.data.models.AlertSeverity
import com.ti3042.airmonitor.services.firebase.FirebaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {
    
    private val _dashboardMetrics = MutableStateFlow(
        DashboardMetrics(
            totalReadings = 0,
            criticalAlerts = 0,
            warningAlerts = 0,
            normalReadings = 0,
            lastUpdateTime = Date(),
            airQualityIndex = 0,
            dominantGas = null
        )
    )
    val dashboardMetrics: StateFlow<DashboardMetrics> = _dashboardMetrics.asStateFlow()
    
    private val _recentAlerts = MutableStateFlow<List<AlertInfo>>(emptyList())
    val recentAlerts: StateFlow<List<AlertInfo>> = _recentAlerts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    init {
        // Start monitoring data changes
        startRealTimeUpdates()
    }
    
    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            // Monitor all readings and update metrics
            sensorDataRepository.getAllReadings()
                .collect { readings ->
                    updateDashboardMetrics(readings)
                    checkForAlerts(readings.take(10)) // Last 10 readings for alerts
                }
        }
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get latest metrics
                val latestReading = sensorDataRepository.getLatestReading()
                
                if (latestReading != null) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _isMonitoring.value = true
                } else {
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                    _isMonitoring.value = false
                }
                
                // Update last seen time
                _dashboardMetrics.value = _dashboardMetrics.value.copy(
                    lastUpdateTime = latestReading?.timestamp ?: Date()
                )
                
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.ERROR
                _isMonitoring.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun toggleMonitoring() {
        viewModelScope.launch {
            _isMonitoring.value = !_isMonitoring.value
            // Here you would typically start/stop the actual monitoring service
        }
    }
    
    fun syncWithCloud() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            
            try {
                // Get unsynced readings
                val unsyncedReadings = sensorDataRepository.getUnsyncedReadings()
                
                if (unsyncedReadings.isNotEmpty()) {
                    val syncedCount = firebaseService.batchSyncReadings(unsyncedReadings)
                    
                    // Mark as synced
                    val syncedIds = unsyncedReadings.take(syncedCount).map { it.id }
                    sensorDataRepository.markAsSynced(syncedIds)
                    
                    _syncStatus.value = SyncStatus.Success(syncedCount)
                } else {
                    _syncStatus.value = SyncStatus.Success(0)
                }
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun updateDashboardMetrics(readings: List<com.ti3042.airmonitor.data.database.entities.SensorReading>) {
        if (readings.isEmpty()) return
        
        var criticalCount = 0
        var warningCount = 0
        var normalCount = 0
        
        var dominantGas: String? = null
        var maxGasValue = 0.0
        
        // Analyze readings from last 24 hours
        val oneDayAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.time
        
        val recentReadings = readings.filter { it.timestamp.after(oneDayAgo) }
        
        recentReadings.forEach { reading ->
            // Check each gas level
            val gasLevels = mapOf(
                "CO2" to reading.co2Ppm,
                "CO" to reading.coPpm,
                "NH3" to reading.nh3Ppm,
                "NOX" to reading.noxPpm,
                "Alcohol" to reading.alcoholPpm,
                "Benceno" to reading.benzenePpm,
                "Tolueno" to reading.toluenePpm,
                "Acetona" to reading.acetonePpm
            )
            
            var hasAlert = false
            
            gasLevels.forEach { (gas, value) ->
                val threshold = getGasThreshold(gas)
                
                when {
                    value > threshold.critical -> {
                        criticalCount++
                        hasAlert = true
                        if (value > maxGasValue) {
                            maxGasValue = value
                            dominantGas = gas
                        }
                    }
                    value > threshold.warning -> {
                        warningCount++
                        hasAlert = true
                    }
                }
            }
            
            if (!hasAlert) {
                normalCount++
            }
        }
        
        // Calculate Air Quality Index (simplified)
        val latestReading = readings.firstOrNull()
        val aqi = latestReading?.let { 
            calculateAirQualityIndex(it)
        } ?: 0
        
        _dashboardMetrics.value = DashboardMetrics(
            totalReadings = readings.size,
            criticalAlerts = criticalCount,
            warningAlerts = warningCount,
            normalReadings = normalCount,
            lastUpdateTime = latestReading?.timestamp ?: Date(),
            airQualityIndex = aqi,
            dominantGas = dominantGas
        )
    }
    
    private fun checkForAlerts(recentReadings: List<com.ti3042.airmonitor.data.database.entities.SensorReading>) {
        val alerts = mutableListOf<AlertInfo>()
        
        recentReadings.forEach { reading ->
            val gasLevels = mapOf(
                "CO2" to reading.co2Ppm,
                "CO" to reading.coPpm, 
                "NH3" to reading.nh3Ppm,
                "NOX" to reading.noxPpm
            )
            
            gasLevels.forEach { (gas, value) ->
                val threshold = getGasThreshold(gas)
                
                when {
                    value > threshold.critical -> {
                        alerts.add(
                            AlertInfo(
                                gasType = gas,
                                currentValue = value,
                                threshold = threshold.critical,
                                severity = AlertSeverity.CRITICAL,
                                timestamp = reading.timestamp,
                                message = "Nivel crítico de $gas detectado: ${value.toInt()} ppm"
                            )
                        )
                    }
                    value > threshold.warning -> {
                        alerts.add(
                            AlertInfo(
                                gasType = gas,
                                currentValue = value,
                                threshold = threshold.warning,
                                severity = AlertSeverity.WARNING,
                                timestamp = reading.timestamp,
                                message = "Nivel de advertencia de $gas: ${value.toInt()} ppm"
                            )
                        )
                    }
                }
            }
        }
        
        _recentAlerts.value = alerts.take(5) // Last 5 alerts
    }
    
    private fun getGasThreshold(gasType: String): GasThreshold {
        return when (gasType) {
            "CO2" -> GasThreshold(warning = 1000.0, critical = 5000.0)
            "CO" -> GasThreshold(warning = 30.0, critical = 100.0)
            "NH3" -> GasThreshold(warning = 25.0, critical = 50.0)
            "NOX" -> GasThreshold(warning = 200.0, critical = 400.0)
            "Alcohol" -> GasThreshold(warning = 400.0, critical = 1000.0)
            "Benceno" -> GasThreshold(warning = 5.0, critical = 15.0)
            "Tolueno" -> GasThreshold(warning = 300.0, critical = 1000.0)
            "Acetona" -> GasThreshold(warning = 1000.0, critical = 2400.0)
            else -> GasThreshold(warning = 100.0, critical = 500.0)
        }
    }
    
    private fun calculateAirQualityIndex(reading: com.ti3042.airmonitor.data.database.entities.SensorReading): Int {
        // Simplified AQI calculation based on multiple gas readings
        val co2Factor = (reading.co2Ppm / 5000.0) * 50  // CO2 influence
        val coFactor = (reading.coPpm / 100.0) * 100     // CO influence (higher weight)
        val nh3Factor = (reading.nh3Ppm / 50.0) * 75     // NH3 influence
        val noxFactor = (reading.noxPpm / 400.0) * 75    // NOX influence
        
        val combinedIndex = (co2Factor + coFactor + nh3Factor + noxFactor).toInt()
        
        return combinedIndex.coerceIn(0, 500) // AQI scale 0-500
    }
}

data class GasThreshold(
    val warning: Double,
    val critical: Double
)

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val recordsSynced: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}