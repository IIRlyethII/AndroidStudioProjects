package com.ti3042.airmonitor.feature.control

/**
 * 🔧 Configuración del sistema de control
 */
data class SystemConfiguration(
    val bluetoothEnabled: Boolean = true,
    val wifiP2PEnabled: Boolean = false,
    val encryptionEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val sensitivity: Int = 75, // 0-100%
    val dataInterval: Int = 30 // segundos
)

/**
 * 📊 Estado de dispositivos conectados
 */
data class DeviceStatus(
    val connectedDevices: Int = 0,
    val signalStrength: Int = 0, // 0-100%
    val dataRate: Double = 0.0, // KB/s
    val uptime: Long = System.currentTimeMillis()
) {
    val formattedUptime: String
        get() {
            val currentTime = System.currentTimeMillis()
            val uptimeMs = currentTime - uptime
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val minutes = (uptimeMs / (1000 * 60)) % 60
            return String.format("%02d:%02d", hours, minutes)
        }
}