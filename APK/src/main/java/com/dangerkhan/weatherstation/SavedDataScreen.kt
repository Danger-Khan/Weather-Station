package com.dangerkhan.weatherstation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDataScreen(dao: TelemetryDao, moduleDao: ModuleDao, scope: CoroutineScope) {
    val records by dao.getAllRecords().collectAsState(initial = emptyList())
    val modules by moduleDao.getAllModules().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAVED RECORDS", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { scope.launch { dao.deleteAll() } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF151D2A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No records found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    RecordCard(record, modules, onDelete = { scope.launch { dao.delete(record) } })
                }
            }
        }
    }
}

@Composable
fun RecordCard(record: TelemetryRecord, modules: List<Module>, onDelete: () -> Unit) {
    val jsonData = remember(record.valuesJson) {
        try { JSONObject(record.valuesJson) } catch (e: Exception) { JSONObject() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.timestamp,
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val details = modules.filter { it.isEnabled }.joinToString("  |  ") { module ->
                    val value = jsonData.opt(module.jsonKey)
                    val displayValue = when (value) {
                        is Number -> String.format("%.1f", value.toFloat())
                        is Boolean -> if (value) "ON" else "OFF"
                        else -> value?.toString() ?: "N/A"
                    }
                    "${module.name.take(3)}: $displayValue${module.unit}"
                }
                
                Text(
                    text = details,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171))
            }
        }
    }
}
