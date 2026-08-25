package com.dangerkhan.weatherstation

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import android.widget.Toast
import kotlinx.coroutines.awaitCancellation
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val db = remember { AppDatabase.getDatabase(context) }
            val isDarkModePref by settingsManager.isDarkMode.collectAsState(initial = true)

            WeatherStationTheme(darkTheme = isDarkModePref) {
                MainScreen(settingsManager, db)
            }
        }
    }
}

@Composable
fun WeatherStationTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF38BDF8),
            background = Color(0xFF0B0E14),
            surface = Color(0xFF151D2A).copy(alpha = 0.6f) // Glassy surface
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0284C7),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF).copy(alpha = 0.6f) // Glassy surface
        )
    }
    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

@Composable
fun GridBackground(
    gridTransparency: Float = 0.05f,
    gridSize: Dp = 30.dp,
    content: @Composable () -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.onBackground
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val color = gridColor.copy(alpha = gridTransparency)
            val sizePx = gridSize.toPx()

            var x = 0f
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                x += sizePx
            }

            var y = 0f
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
                y += sizePx
            }
        }
        content()
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Records : Screen("records", "Records", Icons.Default.List)
    object Modules : Screen("modules", "Modules", Icons.Default.Build)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(settingsManager: SettingsManager, db: AppDatabase) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val dao = db.telemetryDao()
    val moduleDao = db.moduleDao()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = Color.Transparent // Allow background to show through
    ) { padding ->
        GridBackground {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(settingsManager, dao, moduleDao)
                }
                composable(Screen.Records.route) {
                    SavedDataScreen(dao, moduleDao, scope)
                }
                composable(Screen.Modules.route) {
                    ModuleManagerScreen(moduleDao, scope)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsManager, scope)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Records, Screen.Modules, Screen.Settings)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, fontSize = 10.sp) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun DashboardScreen(settingsManager: SettingsManager, dao: TelemetryDao, moduleDao: ModuleDao) {
    val context = LocalContext.current
    var pktTime by remember { mutableStateOf("--:--:--") }
    var isConnected by remember { mutableStateOf(false) }
    var telemetry by remember { mutableStateOf(TelemetryData()) }
    val historyList = remember { mutableStateListOf<TelemetryData>() }
    val scope = rememberCoroutineScope()
    
    val modules by moduleDao.getAllModules().collectAsState(initial = emptyList())
    val enabledModules = modules.filter { it.isEnabled }

    val mqttBroker by settingsManager.mqttBroker.collectAsState(initial = "broker.hivemq.com")
    val mqttPort by settingsManager.mqttPort.collectAsState(initial = "1883")
    val mqttTopic by settingsManager.mqttTopic.collectAsState(initial = "Danger-Khan/Weather-Station/telemetry")

    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Karachi")
            pktTime = sdf.format(Date())
            delay(1000)
        }
    }

    LaunchedEffect(mqttBroker, mqttPort, mqttTopic) {
        val brokerUrl = "tcp://$mqttBroker:$mqttPort"
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
                    scope.launch { isConnected = false }
                }
                override fun messageArrived(t: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: return
                    val timeStr = SimpleDateFormat("HH:mm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Karachi")
                    }.format(Date())

                    TelemetryData.parseJson(payload, timeStr)?.let { data ->
                        scope.launch {
                            telemetry = data
                            historyList.add(data)
                            if (historyList.size > 10) historyList.removeAt(0)
                        }
                    }
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            withContext(Dispatchers.IO) {
                client.connect(options)
                client.subscribe(mqttTopic)
            }
            isConnected = true
            
            // Keep the connection alive until the effect is cancelled
            awaitCancellation()
            
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "MQTT Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            try {
                if (client.isConnected) client.disconnect()
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DANGER-KHAN WEATHER STATION", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "PKT $pktTime", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    BadgeStatus(isConnected)
                }
            }
        }

        // Dynamic Modules
        enabledModules.chunked(2).forEach { rowModules ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowModules.forEach { module ->
                    val value = telemetry.values[module.jsonKey]
                    if (module.type == "status") {
                        val isTrue = when (value) {
                            is Boolean -> value
                            is Number -> value.toInt() == 1
                            is String -> value.lowercase() == "true" || value.lowercase() == "raining"
                            else -> false
                        }
                        RainStatusCard(module.name, isTrue, Modifier.weight(1f))
                    } else {
                        val displayValue = when (value) {
                            is Number -> String.format("%.1f", value.toFloat())
                            else -> value?.toString() ?: "0.0"
                        }
                        MetricCard(
                            module.name, 
                            "$displayValue ${module.unit}", 
                            "Key: ${module.jsonKey}", 
                            Color(module.color), 
                            Modifier.weight(1f)
                        )
                    }
                }
                // Add empty spacer if odd number of items in row
                if (rowModules.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                scope.launch {
                    val json = JSONObject()
                    telemetry.values.forEach { (k, v) -> json.put(k, v) }
                    
                    dao.insert(TelemetryRecord(
                        valuesJson = json.toString(),
                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    ))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("SAVE CURRENT RECORD", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("LIVE DATA TREND", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                LineChartView(historyList)
            }
        }
    }
}

@Composable
fun BadgeStatus(online: Boolean) {
    Text(
        text = if (online) "ONLINE" else "OFFLINE",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .background(if (online) Color(0xFF064E3B) else Color(0xFF7F1D1D), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun MetricCard(title: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(vertical = 4.dp))
            Text(sub, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RainStatusCard(title: String, isRaining: Boolean, modifier: Modifier) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRaining) "🌧️ RAINING" else "☀️ NO RAIN",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRaining) Color(0xFF60A5FA) else Color.Gray,
                modifier = Modifier.background(if (isRaining) Color(0xFF1E3A8A) else Color.DarkGray, RoundedCornerShape(6.dp)).padding(8.dp)
            )
        }
    }
}

@Composable
fun LineChartView(history: List<TelemetryData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                axisRight.isEnabled = false
                xAxis.textColor = AndroidColor.GRAY
                axisLeft.textColor = AndroidColor.GRAY
                legend.textColor = AndroidColor.GRAY
            }
        },
        update = { chart ->
            val tempEntries = history.mapIndexed { index, data -> Entry(index.toFloat(), data.getFloat("temperature")) }
            val humidEntries = history.mapIndexed { index, data -> Entry(index.toFloat(), data.getFloat("humidity")) }
            val tempDataSet = LineDataSet(tempEntries, "Temp").apply { color = AndroidColor.rgb(249, 115, 22); setDrawCircles(false) }
            val humidDataSet = LineDataSet(humidEntries, "Humid").apply { color = AndroidColor.rgb(56, 189, 248); setDrawCircles(false) }
            chart.data = LineData(tempDataSet, humidDataSet)
            chart.invalidate()
        }
    )
}
