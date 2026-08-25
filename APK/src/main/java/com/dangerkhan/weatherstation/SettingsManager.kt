package com.dangerkhan.weatherstation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val MQTT_BROKER = stringPreferencesKey("mqtt_broker")
        val MQTT_PORT = stringPreferencesKey("mqtt_port")
        val MQTT_TOPIC = stringPreferencesKey("mqtt_topic")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: true
    }

    val mqttBroker: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MQTT_BROKER] ?: "broker.hivemq.com"
    }

    val mqttPort: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MQTT_PORT] ?: "1883"
    }

    val mqttTopic: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MQTT_TOPIC] ?: "Danger-Khan/Weather-Station/telemetry"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    suspend fun setMqttBroker(broker: String) {
        context.dataStore.edit { preferences ->
            preferences[MQTT_BROKER] = broker
        }
    }

    suspend fun setMqttPort(port: String) {
        context.dataStore.edit { preferences ->
            preferences[MQTT_PORT] = port
        }
    }

    suspend fun setMqttTopic(topic: String) {
        context.dataStore.edit { preferences ->
            preferences[MQTT_TOPIC] = topic
        }
    }
}
