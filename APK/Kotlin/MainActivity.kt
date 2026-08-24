package com.dangerkhan.weatherstation

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.delay
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val mqttTopic = "Danger-Khan/Weather-Station/telemetry"
    private val brokerUrl = "ssl://broker.hivemq.com:8883" // Secure SSL port

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WeatherDashboardScreen(brokerUrl, mqttTopic)
        }
    }
}

@Composable
fun WeatherDashboardScreen(brokerUrl: String, topic: String) {
    var pktTime by remember { mutableStateOf("--:--:--") }
    var isConnected by remember { mutableStateOf(false) }
    var telemetry by remember { mutableStateOf(TelemetryData()) }
    val historyList = remember { mutableStateListOf<TelemetryData>() }

    // PKT Time Interval Timer (UTC+5)
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Karachi")
            pktTime = sdf.format(Date())
            delay(1000)
        }
    }

    // MQTT Connection Handler
    DisposableEffect(Unit) {
        val clientId = "android_hmi_" + UUID.randomUUID().toString().take(8)
        val client = MqttClient(brokerUrl, clientId, MemoryPersistence())

        try {
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
            }

            client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    isConnected = false
                }

                override fun messageArrived(t: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: return
                    val timeStr = SimpleDateFormat("HH:mm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Karachi")
                    }.format(Date())

                    TelemetryData.parseJson(payload, timeStr)?.let { data ->
                        telemetry = data
                        historyList.add(data)
                        if (historyList.size > 10) historyList.removeAt(0)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client.connect(options)
            client.subscribe(topic)
            isConnected = true
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
        }

        onDispose {
            try {
                if (client.isConnected) client.disconnect()
            } catch (_: Exception) {}
        }
    }

    // Modern Dark UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, Color(0xFF263347), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DANGER-KHAN WEATHER STATION",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PKT $pktTime",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    BadgeStatus(isConnected)
                }
            }
        }

        // Metrics Grid (Cards)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "TEMPERATURE",
                value = "${telemetry.temperature} °C",
                subtext = "Sensor: DHT11 / BMP280",[cite: 1, 3]
                valueColor = Color(0xFFF97316),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "HUMIDITY",
                value = "${telemetry.humidity} %",
                subtext = "Sensor: DHT11",[cite: 1]
                valueColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RainStatusCard(
                isRaining = telemetry.rain,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "BATTERY STATUS",
                value = "${telemetry.battery} %",
                subtext = "Power: Li-Ion / Solar",
                valueColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Telemetry Graph
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF263347), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LIVE DATA TREND",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LineChartView(historyList)
            }
        }
    }
}

@Composable
fun BadgeStatus(online: Boolean) {
    Text(
        text = if (online) "LIVE CLOUD LINK ONLINE" else "CLOUD DISCONNECTED",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (online) Color(0xFF34D399) else Color(0xFFF87171),
        modifier = Modifier
            .background(
                if (online) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, Color(0xFF263347), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = valueColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(text = subtext, fontSize = 10.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
fun RainStatusCard(isRaining: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, Color(0xFF263347), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "RAIN METER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRaining) "🌧️ RAINING" else "☀️ NO RAIN",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRaining) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                modifier = Modifier
                    .background(
                        if (isRaining) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Precipitation Status", fontSize = 10.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
fun LineChartView(history: List<TelemetryData>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                axisRight.isEnabled = false
                xAxis.textColor = AndroidColor.GRAY
                axisLeft.textColor = AndroidColor.GRAY
                legend.textColor = AndroidColor.WHITE
            }
        },
        update = { chart ->
            val tempEntries = history.mapIndexed { index, data -> Entry(index.toFloat(), data.temperature) }
            val humidEntries = history.mapIndexed { index, data -> Entry(index.toFloat(), data.humidity) }

            val tempDataSet = LineDataSet(tempEntries, "Temp (°C)").apply {
                color = AndroidColor.parseColor("#F97316")
                setDrawCircles(false)
                lineWidth = 2f
            }

            val humidDataSet = LineDataSet(humidEntries, "Humidity (%)").apply {
                color = AndroidColor.parseColor("#38BDF8")
                setDrawCircles(false)
                lineWidth = 2f
            }

            chart.data = LineData(tempDataSet, humidDataSet)
            chart.invalidate()
        }
    )
}
