package com.dangerkhan.weatherstation

import org.json.JSONObject

data class TelemetryData(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val battery: Int = 0,
    val rain: Boolean = false,
    val timestamp: String = ""
) {
    companion object {
        fun parseJson(jsonStr: String, timeStr: String): TelemetryData? {
            return try {
                val json = JSONObject(jsonStr)
                TelemetryData(
                    temperature = json.optDouble("temperature", 0.0).toFloat(),
                    humidity = json.optDouble("humidity", 0.0).toFloat(),
                    battery = json.optInt("battery", 0),
                    rain = json.optBoolean("rain", false) || json.optString("rain") == "raining",
                    timestamp = timeStr
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
