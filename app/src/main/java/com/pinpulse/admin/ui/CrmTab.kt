package com.pinpulse.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinpulse.admin.data.AdminMqttManager
import com.pinpulse.admin.model.AdminUser

@Composable
fun CrmTab(
    mqttManager: AdminMqttManager,
    onShowToast: (String) -> Unit
) {
    val usersMap by mqttManager.users.collectAsState()
    val allUsers = remember(usersMap) { usersMap.values.toList().sortedBy { it.effectiveName.lowercase() } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var editingUser by remember { mutableStateOf<AdminUser?>(null) }

    val totalUsers = allUsers.size
    val onlineCount = allUsers.count { it.isOnline }
    val demoCount = allUsers.count { it.subscriptionPlan.equals("DEMO", ignoreCase = true) }
    val familyCount = allUsers.count { it.subscriptionPlan.equals("FAMILY", ignoreCase = true) }
    val businessCount = allUsers.count { it.subscriptionPlan.equals("BUSINESS", ignoreCase = true) }

    val filteredUsers = remember(allUsers, searchQuery, selectedFilter) {
        allUsers.filter { u ->
            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> u.accountStatus.equals("ACTIVE", ignoreCase = true)
                "LOCKED" -> u.accountStatus.equals("LOCKED_PAYMENT", ignoreCase = true)
                "DEMO" -> u.subscriptionPlan.equals("DEMO", ignoreCase = true)
                "FAMILY" -> u.subscriptionPlan.equals("FAMILY", ignoreCase = true)
                "BUSINESS" -> u.subscriptionPlan.equals("BUSINESS", ignoreCase = true)
                else -> true
            }
            if (!matchesFilter) return@filter false

            if (searchQuery.isBlank()) true
            else {
                val q = searchQuery.trim().lowercase()
                u.effectiveName.lowercase().contains(q) ||
                    u.email.lowercase().contains(q) ||
                    u.friendCode.lowercase().contains(q) ||
                    u.id.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // --- KPI OVERVIEW CARDS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(title = "TOTAL USERS", value = totalUsers.toString(), color = Color(0xFF38BDF8), icon = Icons.Default.People)
                KpiCard(title = "ONLINE NOW", value = onlineCount.toString(), color = Color(0xFF10B981), icon = Icons.Default.Wifi)
                KpiCard(title = "DEMO", value = demoCount.toString(), color = Color(0xFFFBBF24), icon = Icons.Default.Person)
                KpiCard(title = "FAMILY", value = familyCount.toString(), color = Color(0xFFA855F7), icon = Icons.Default.Group)
                KpiCard(title = "BUSINESS", value = businessCount.toString(), color = Color(0xFFEC4899), icon = Icons.Default.Business)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search CRM by name, email, code...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "ACTIVE", "LOCKED", "DEMO", "FAMILY", "BUSINESS").forEach { f ->
                    val isSelected = selectedFilter == f
                    Box(
                        modifier = Modifier
                            .clickable { selectedFilter = f }
                            .background(
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = f,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allUsers.isEmpty()) "Connecting to fleet registry..." else "No matching users found",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                val plan = user.subscriptionPlan.uppercase()
                val status = user.accountStatus.uppercase()
                val isLocked = status == "LOCKED_PAYMENT"

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (user.isOnline) Color(0x2010B981) else Color(0xFF1E293B),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.effectiveName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (user.isOnline) Color(0xFF10B981) else Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.effectiveName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (user.isOnline) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                    }
                                }
                                Text(
                                    text = user.email.ifBlank { "No email associated" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Code: ${user.friendCode.ifBlank { user.id.take(8) }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            // Plan Badge
                            val planBg = when (plan) {
                                "FAMILY" -> Color(0x20A855F7)
                                "BUSINESS" -> Color(0x20EC4899)
                                else -> Color(0x20FBBF24)
                            }
                            val planColor = when (plan) {
                                "FAMILY" -> Color(0xFFA855F7)
                                "BUSINESS" -> Color(0xFFEC4899)
                                else -> Color(0xFFFBBF24)
                            }
                            Box(
                                modifier = Modifier
                                    .background(planBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = plan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = planColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quotas & Hardware Telemetry Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Friends: ${user.friends.size}/${user.maxFriends} • Rate: ${user.rateLimitSeconds}s",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            // Status Tag
                            val (statusBg, statusColor) = if (isLocked) {
                                Pair(Color(0x20EF4444), Color(0xFFEF4444))
                            } else {
                                Pair(Color(0x2010B981), Color(0xFF10B981))
                            }
                            Box(
                                modifier = Modifier
                                    .background(statusBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isLocked) "LOCKED" else "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editingUser = user },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF38BDF8)
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val nextStatus = if (isLocked) "ACTIVE" else "LOCKED_PAYMENT"
                                    mqttManager.updateUserStatus(user.id, nextStatus)
                                    onShowToast(if (isLocked) "Unlocked ${user.effectiveName}" else "Locked ${user.effectiveName}")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLocked) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLocked) "Unlock" else "Lock Access",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingUser?.let { user ->
        EditPlanSheet(
            user = user,
            onSave = { plan, maxFriends, maxGroups, rateLimit ->
                mqttManager.updateUserPlan(user.id, plan, maxFriends, maxGroups, rateLimit)
                onShowToast("Updated ${user.effectiveName} to $plan!")
                editingUser = null
            },
            onDismiss = { editingUser = null }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(78.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
