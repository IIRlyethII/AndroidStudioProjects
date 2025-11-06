package com.ti3042.airmonitor.bluetooth

import com.ti3042.airmonitor.models.SensorData

/**
 * Interface para callbacks de conexión Bluetooth
 */
interface ConnectionCallback {
    fun onConnected()
    fun onDisconnected()
    fun onDataReceived(sensorData: SensorData)
    fun onError(error: String)
    fun onConnectionStateChanged(isConnected: Boolean)
}

