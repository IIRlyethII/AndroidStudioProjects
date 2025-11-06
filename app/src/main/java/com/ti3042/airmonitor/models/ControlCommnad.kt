package com.ti3042.airmonitor.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para enviar comandos de control al ESP32
 */
data class ControlCommand(
    @SerializedName("action")
    val action: String = "control",
    
    @SerializedName("fan")
    val fan: FanControl? = null,
    
    @SerializedName("buzzer") 
    val buzzer: BuzzerControl? = null,
    
    @SerializedName("auto_mode")
    val autoMode: Boolean? = null,
    
    @SerializedName("thresholds")
    val thresholds: Thresholds? = null
)

data class FanControl(
    @SerializedName("enable")
    val enable: Boolean
)

data class BuzzerControl(
    @SerializedName("enable")
    val enable: Boolean
)

/**
 * Comandos predefinidos para facilitar el uso
 */
object ControlCommands {
    fun turnOnFan() = ControlCommand(fan = FanControl(enable = true))
    
    fun turnOffFan() = ControlCommand(fan = FanControl(enable = false))
    
    fun turnOnBuzzer() = ControlCommand(buzzer = BuzzerControl(enable = true))
    
    fun turnOffBuzzer() = ControlCommand(buzzer = BuzzerControl(enable = false))
    
    fun enableAutoMode() = ControlCommand(autoMode = true)
    
    fun disableAutoMode() = ControlCommand(autoMode = false)
    
    fun setThresholds(warning: Int, critical: Int) = ControlCommand(
        thresholds = Thresholds(warning = warning, critical = critical)
    )
}
