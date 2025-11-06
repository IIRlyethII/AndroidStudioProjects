package com.ti3042.airmonitor.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.ti3042.airmonitor.models.SensorData
import com.ti3042.airmonitor.models.ControlCommand

/**
 * Utilidades para parsear JSON entre la app y el ESP32
 */
object JsonParser {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Convierte un JSON string a SensorData
     */
    fun parseJsonToSensorData(json: String): SensorData? {
        return try {
            gson.fromJson(json, SensorData::class.java)
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("JsonParser", "Error parsing SensorData: ${e.message}")
            null
        }
    }

    /**
     * Convierte un ControlCommand a JSON string
     */
    fun commandToJson(command: ControlCommand): String {
        return gson.toJson(command)
    }

    /**
     * Valida si un string es un JSON válido
     */
    fun isValidJson(json: String): Boolean {
        return try {
            gson.fromJson(json, Any::class.java)
            true
        } catch (e: JsonSyntaxException) {
            false
        }
    }

    /**
     * Formatea un JSON string para logging
     */
    fun prettyPrintJson(json: String): String {
        return try {
            val jsonObject = gson.fromJson(json, Any::class.java)
            gson.toJson(jsonObject)
        } catch (e: JsonSyntaxException) {
            json // Retorna el original si no es válido
        }
    }
}

