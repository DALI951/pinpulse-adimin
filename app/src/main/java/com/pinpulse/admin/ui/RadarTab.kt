package com.pinpulse.admin.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinpulse.admin.data.AdminMqttManager
import com.pinpulse.admin.model.AdminUser

@Composable
fun RadarTab(
    mqttManager: AdminMqttManager,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val usersMap by mqttManager.users.collectAsState()
    val allUsers = remember(usersMap) {
        usersMap.values.toList().sortedWith(
            compareByDescending<AdminUser> { it.isOnline }
                .thenByDescending { it.lastUpdated }
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) allUsers
        else {
            val q = searchQuery.trim().lowercase()
            allUsers.filter {
                it.effectiveName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    it.friendCode.lowercase().contains(q)
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        cm?.setPrimaryClip(clip)
        onShowToast("Copied $label to clipboard")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fleet Radar Telemetry",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${allUsers.count { it.isOnline }} Active • ${allUsers.size} Total Tracked",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter devices by name, code...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allUsers.isEmpty()) "Waiting for device telemetry packets..." else "No matching devices found",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                val hasLocation = user.latitude != 0.0 || user.longitude != 0.0
                val now = System.currentTimeMillis()
                val diffSec = if (user.lastUpdated > 0L) (now - user.lastUpdated) / 1000L else -1L
                val timeLabel = when {
                    diffSec < 0 -> "Never"
                    diffSec < 5 -> "Live right now"
                    diffSec < 60 -> "${diffSec}s ago"
                    diffSec < 3600 -> "${diffSec / 60}m ago"
                    else -> "${diffSec / 3600}h ago"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (user.isOnline) Color(0xFF10B981) else Color(0xFF64748B),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.effectiveName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = timeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (user.isOnline) Color(0xFF10B981) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // GPS Coordinates Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .clickable {
                                    if (hasLocation) {
                                        copyToClipboard("Coordinates", "${user.latitude}, ${user.longitude}")
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (hasLocation) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasLocation) {
                                        "%.5f, %.5f".format(user.latitude, user.longitude)
                                    } else {
                                        "GPS Searching / No Fix"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasLocation) Color.White else Color(0xFF94A3B8)
                                )
                            }
                            if (hasLocation) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy coordinates",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Telemetry details (Battery, Speed, GPS state)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Battery
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (user.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                    contentDescription = null,
                                    tint = when {
                                        user.batteryPercent > 50 -> Color(0xFF10B981)
                                        user.batteryPercent > 20 -> Color(0xFFFBBF24)
                                        else -> Color(0xFFEF4444)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${user.batteryPercent}%${if (user.isCharging) " (⚡)" else ""}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            // Speed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f km/h".format(user.speedKmh),
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            // GPS Provider Status
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (user.isGpsEnabled) Color(0x2010B981) else Color(0x20EF4444),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (user.isGpsEnabled) "GPS ACTIVE" else "GPS OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isGpsEnabled) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
