package com.dangerkhan.weatherstation

import org.json.JSONObject

data class TelemetryData(
    val values: Map<String, Any> = emptyMap(),
    val timestamp: String = ""
) {
    companion object {
        fun parseJson(jsonStr: String, timeStr: String): TelemetryData? {
            return try {
                val json = JSONObject(jsonStr)
                val values = mutableMapOf<String, Any>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.get(key)
                    
                    // Normalize boolean/numeric values if needed (similar to old rain logic)
                    if (key == "rain") {
                        values[key] = json.optBoolean("rain", false) || 
                                     (json.optString("rain") == "raining") || 
                                     (json.optInt("rain", 0) == 1)
                    } else {
                        values[key] = value
                    }
                }
                TelemetryData(values = values, timestamp = timeStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Helper to get typed values easily
    fun getFloat(key: String): Float = (values[key] as? Number)?.toFloat() ?: 0f
    fun getInt(key: String): Int = (values[key] as? Number)?.toInt() ?: 0
    fun getBoolean(key: String): Boolean = values[key] as? Boolean ?: false
}
