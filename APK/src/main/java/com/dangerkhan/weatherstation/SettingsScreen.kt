package com.dangerkhan.weatherstation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager, scope: CoroutineScope) {
    val isDarkMode by settingsManager.isDarkMode.collectAsState(initial = true)
    val mqttBroker by settingsManager.mqttBroker.collectAsState(initial = "broker.hivemq.com")
    val mqttPort by settingsManager.mqttPort.collectAsState(initial = "1883")
    val mqttTopic by settingsManager.mqttTopic.collectAsState(initial = "Danger-Khan/Weather-Station/telemetry")

    var brokerInput by remember(mqttBroker) { mutableStateOf(mqttBroker) }
    var portInput by remember(mqttPort) { mutableStateOf(mqttPort) }
    var topicInput by remember(mqttTopic) { mutableStateOf(mqttTopic) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF151D2A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("APPEARANCE", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode", color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { scope.launch { settingsManager.setDarkMode(it) } }
                )
            }

            HorizontalDivider(color = Color(0xFF263347))

            Text("MQTT CONFIGURATION", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)

            OutlinedTextField(
                value = brokerInput,
                onValueChange = { brokerInput = it },
                label = { Text("Broker URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF263347)
                )
            )

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF263347)
                )
            )

            OutlinedTextField(
                value = topicInput,
                onValueChange = { topicInput = it },
                label = { Text("Topic") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF263347)
                )
            )

            Button(
                onClick = {
                    scope.launch {
                        settingsManager.setMqttBroker(brokerInput)
                        settingsManager.setMqttPort(portInput)
                        settingsManager.setMqttTopic(topicInput)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("SAVE MQTT SETTINGS", color = Color(0xFF0B0E14), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            Text("@Modified by Imaad Yameen", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
