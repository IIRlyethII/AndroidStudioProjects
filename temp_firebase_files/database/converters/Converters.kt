package com.ti3042.airmonitor.data.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class GasThresholdsConverter {
    @TypeConverter
    fun fromGasThresholds(value: GasThresholds): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toGasThresholds(value: String): GasThresholds {
        return Gson().fromJson(value, GasThresholds::class.java)
    }
}

// Data classes para thresholds
data class GasThresholds(
    val normal: Pair<Float, Float>,
    val warning: Pair<Float, Float>, 
    val critical: Pair<Float, Float>
)

data class ThresholdRange(
    val min: Float,
    val max: Float
)

// Converter para listas simples
class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}