package com.dangerkhan.weatherstation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
fun ModuleManagerScreen(moduleDao: ModuleDao, scope: CoroutineScope) {
    val modules by moduleDao.getAllModules().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var moduleToEdit by remember { mutableStateOf<Module?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MANAGE MODULES", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF151D2A),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF38BDF8)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Module", tint = Color(0xFF0B0E14))
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modules) { module ->
                ModuleItem(
                    module = module,
                    onDelete = { scope.launch { moduleDao.delete(module) } },
                    onToggle = { scope.launch { moduleDao.update(module.copy(isEnabled = it)) } },
                    onEdit = { moduleToEdit = module }
                )
            }
        }

        if (showAddDialog) {
            ModuleDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { 
                    scope.launch { moduleDao.insert(it) }
                    showAddDialog = false
                }
            )
        }

        moduleToEdit?.let { module ->
            ModuleDialog(
                module = module,
                onDismiss = { moduleToEdit = null },
                onConfirm = { 
                    scope.launch { moduleDao.update(it) }
                    moduleToEdit = null
                }
            )
        }
    }
}

@Composable
fun ModuleItem(
    module: Module, 
    onDelete: () -> Unit, 
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(module.color), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(module.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Key: ${module.jsonKey} | Unit: ${module.unit}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = module.isEnabled,
                    onCheckedChange = onToggle,
                    scale = 0.8f
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, scale: Float) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDialog(
    module: Module? = null,
    onDismiss: () -> Unit,
    onConfirm: (Module) -> Unit
) {
    var name by remember { mutableStateOf(module?.name ?: "") }
    var jsonKey by remember { mutableStateOf(module?.jsonKey ?: "") }
    var unit by remember { mutableStateOf(module?.unit ?: "") }
    var type by remember { mutableStateOf(module?.type ?: "metric") }
    var selectedColor by remember { mutableStateOf(module?.color ?: 0xFF38BDF8) }

    val colors = listOf(0xFF38BDF8, 0xFFF97316, 0xFF10B981, 0xFFF87171, 0xFFA855F7, 0xFFEAB308)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151D2A),
        title = { Text(if (module == null) "Add Module" else "Edit Module", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jsonKey,
                    onValueChange = { jsonKey = it },
                    label = { Text("JSON Key (e.g. pressure)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g. hPa)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Type", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "metric", onClick = { type = "metric" })
                        Text("Metric", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == "status", onClick = { type = "status" })
                        Text("Status", color = Color.White)
                    }
                }

                Text("Color", color = Color.Gray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(color), CircleShape)
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    module?.copy(name = name, jsonKey = jsonKey, unit = unit, color = selectedColor, type = type)
                        ?: Module(name = name, jsonKey = jsonKey, unit = unit, color = selectedColor, type = type)
                )
            }) {
                Text("SAVE", color = Color(0xFF38BDF8))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}
