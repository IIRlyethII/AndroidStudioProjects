package com.ti3042.airmonitor.models

/**
 * Configuraciones de la aplicación
 */
data class AppSettings(
    val bluetoothDeviceName: String = "AirMonitor_TI3042",
    val bluetoothDeviceAddress: String? = null,
    val autoConnect: Boolean = true,
    val dataRefreshInterval: Long = 2000L, // 2 segundos
    val notificationsEnabled: Boolean = true,
    val warningThreshold: Int = 200,
    val criticalThreshold: Int = 400,
    val simulationMode: Boolean = true, // Por defecto en simulación
    val darkMode: Boolean = false
) {
    companion object {
        const val PREFS_NAME = "air_monitor_settings"
        const val KEY_BLUETOOTH_NAME = "bluetooth_device_name"
        const val KEY_BLUETOOTH_ADDRESS = "bluetooth_device_address"
        const val KEY_AUTO_CONNECT = "auto_connect"
        const val KEY_REFRESH_INTERVAL = "data_refresh_interval"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_WARNING_THRESHOLD = "warning_threshold"
        const val KEY_CRITICAL_THRESHOLD = "critical_threshold"
        const val KEY_SIMULATION_MODE = "simulation_mode"
        const val KEY_DARK_MODE = "dark_mode"
    }
}

