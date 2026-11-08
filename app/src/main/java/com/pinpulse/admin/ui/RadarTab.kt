package com.pinpulse.admin.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
    var viewMode by remember { mutableStateOf("MAP") } // "MAP" or "LIST"
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var editingUserForPlan by remember { mutableStateOf<AdminUser?>(null) }

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

    fun toggleUserLock(user: AdminUser) {
        val isLocked = user.accountStatus.uppercase().contains("LOCK")
        val nextStatus = if (isLocked) "ACTIVE" else "LOCKED_PAYMENT"
        mqttManager.updateUserStatus(user.id, nextStatus)
        onShowToast(if (isLocked) "Unlocked ${user.effectiveName}" else "Locked ${user.effectiveName}")
        selectedUser = user.copy(accountStatus = nextStatus)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Navigation & Mode Switcher Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090D16))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fleet GPS Radar & Telemetry",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${allUsers.count { it.isOnline }} Active • ${allUsers.size} Total Devices",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // View Mode Toggle Switch (MAP vs LIST)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewMode == "MAP") Color(0xFF10B981) else Color.Transparent)
                                .clickable { viewMode = "MAP" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (viewMode == "MAP") Color.Black else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Map",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == "MAP") Color.Black else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewMode == "LIST") Color(0xFF10B981) else Color.Transparent)
                                .clickable { viewMode = "LIST" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (viewMode == "LIST") Color.Black else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "List",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewMode == "LIST") Color.Black else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            if (viewMode == "LIST") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter devices by name, code...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
            }
        }

        // Body Content (Interactive Map or Telemetry List)
        Box(modifier = Modifier.weight(1f)) {
            if (viewMode == "MAP") {
                AdminMapComponent(
                    users = filteredUsers,
                    selectedUser = selectedUser,
                    onSelectUser = { selectedUser = it },
                    onEditPlan = { editingUserForPlan = it },
                    onToggleLock = { toggleUserLock(it) },
                    onCopyCoords = { copyToClipboard("GPS Coordinates", it) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (allUsers.isEmpty()) "Waiting for device telemetry packets from broker..." else "No matching devices found",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
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
                                diffSec < 5 -> "Live now"
                                diffSec < 60 -> "${diffSec}s ago"
                                diffSec < 3600 -> "${diffSec / 60}m ago"
                                else -> "${diffSec / 3600}h ago"
                            }
                            val isLocked = user.accountStatus.uppercase().contains("LOCK")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Header: Online dot + Name + Time
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
                                            fontSize = 14.sp,
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

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Coordinates Box
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (hasLocation) {
                                                    copyToClipboard("Coordinates", "${user.latitude}, ${user.longitude}")
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = if (hasLocation) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (hasLocation) {
                                                    "%.5f, %.5f".format(user.latitude, user.longitude)
                                                } else {
                                                    "GPS Searching / No Fix"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (hasLocation) Color.White else Color(0xFF94A3B8)
                                            )
                                        }
                                        if (hasLocation) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy coordinates",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Telemetry stats row + Locate button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🔋 ${user.batteryPercent}%",
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                            Text(
                                                text = "⚡ ${user.speedKmh.toInt()} km/h",
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                            Text(
                                                text = user.subscriptionPlan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }

                                        // View on Map Button
                                        Button(
                                            onClick = {
                                                selectedUser = user
                                                viewMode = "MAP"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Locate", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Editing Plan from Map or List
    editingUserForPlan?.let { user ->
        EditPlanSheet(
            user = user,
            onSave = { plan, maxFriends, maxGroups, rateLimit ->
                mqttManager.updateUserPlan(user.id, plan, maxFriends, maxGroups, rateLimit)
                onShowToast("Updated ${user.effectiveName} to $plan!")
                selectedUser = user.copy(
                    subscriptionPlan = plan,
                    maxFriends = maxFriends,
                    maxGroups = maxGroups,
                    rateLimitSeconds = rateLimit
                )
                editingUserForPlan = null
            },
            onDismiss = { editingUserForPlan = null }
        )
    }
}
