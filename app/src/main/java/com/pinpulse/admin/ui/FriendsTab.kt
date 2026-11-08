package com.pinpulse.admin.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinpulse.admin.data.AdminMqttManager
import com.pinpulse.admin.model.AdminUser

@Composable
fun FriendsTab(
    mqttManager: AdminMqttManager,
    onShowToast: (String) -> Unit
) {
    val usersMap by mqttManager.users.collectAsState()
    val allUsers = remember(usersMap) { usersMap.values.toList().sortedBy { it.effectiveName.lowercase() } }

    var selectedUserA by remember { mutableStateOf<AdminUser?>(null) }
    var selectedUserB by remember { mutableStateOf<AdminUser?>(null) }
    var pickingForA by remember { mutableStateOf(false) }
    var pickingForB by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var unpairConfirmTarget by remember { mutableStateOf<Pair<AdminUser, String>?>(null) }

    // Keep selections up-to-date with live state updates
    LaunchedEffect(usersMap) {
        selectedUserA?.let { a -> selectedUserA = usersMap[a.id] ?: a }
        selectedUserB?.let { b -> selectedUserB = usersMap[b.id] ?: b }
    }

    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) allUsers
        else {
            val q = searchQuery.trim().lowercase()
            allUsers.filter { u ->
                u.effectiveName.lowercase().contains(q) ||
                    u.email.lowercase().contains(q) ||
                    u.friendCode.lowercase().contains(q) ||
                    u.friends.any { f ->
                        f.displayName.lowercase().contains(q) ||
                            f.friendCode.lowercase().contains(q) ||
                            f.email.lowercase().contains(q)
                    }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // --- INSTANT FORCE-PAIR ACTION CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x2010B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Instant Force-Pairing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Links two users without friend codes or accept prompt",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User A Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { pickingForA = true }
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selectedUserA != null) Color(0xFF10B981) else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "USER 1",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedUserA?.effectiveName ?: "+ Select User",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedUserA != null) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                                if (selectedUserA != null) {
                                    Text(
                                        text = selectedUserA?.friendCode?.ifBlank { selectedUserA?.email } ?: "",
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Pairing with",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // User B Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { pickingForB = true }
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (selectedUserB != null) Color(0xFF10B981) else Color(0xFF334155),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "USER 2",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedUserB?.effectiveName ?: "+ Select User",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedUserB != null) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                                if (selectedUserB != null) {
                                    Text(
                                        text = selectedUserB?.friendCode?.ifBlank { selectedUserB?.email } ?: "",
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val canPair = selectedUserA != null &&
                        selectedUserB != null &&
                        selectedUserA?.id != selectedUserB?.id

                    Button(
                        onClick = {
                            val a = selectedUserA ?: return@Button
                            val b = selectedUserB ?: return@Button
                            mqttManager.forcePairFriends(a, b) { success ->
                                if (success) {
                                    onShowToast("⚡ Force-paired ${a.effectiveName} and ${b.effectiveName}!")
                                    selectedUserA = null
                                    selectedUserB = null
                                } else {
                                    onShowToast("Pairing failed.")
                                }
                            }
                        },
                        enabled = canPair,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF1E293B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = if (canPair) Color.Black else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FORCE PAIR FRIENDS NOW",
                            fontWeight = FontWeight.Bold,
                            color = if (canPair) Color.Black else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Friends Network (${allUsers.size} Users)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter users or friend names...", color = Color(0xFF64748B)) },
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allUsers.isEmpty()) "Waiting for users to report on MQTT..." else "No matching users found",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
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
                        // User Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.effectiveName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.effectiveName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.friendCode.ifBlank { user.id.take(8) },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    if (user.email.isNotBlank()) {
                                        Text(
                                            text = " • ${user.email}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Quick "Set as User 1" button
                            IconButton(
                                onClick = {
                                    selectedUserA = user
                                    onShowToast("Set as User 1")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddLink,
                                    contentDescription = "Set as User 1",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Friends List
                        Text(
                            text = "FRIENDS (${user.friends.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (user.friends.isEmpty()) {
                            Text(
                                text = "Solo user — No friends linked",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            user.friends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = friend.displayName.ifBlank { "Friend" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        if (friend.friendCode.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${friend.friendCode})",
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }

                                    // Touch-friendly Unpair Button
                                    IconButton(
                                        onClick = {
                                            unpairConfirmTarget = Pair(user, friend.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unpair friend",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unpair Confirmation Dialog
    unpairConfirmTarget?.let { (user, friendId) ->
        val friendObj = user.friends.find { it.id == friendId }
        val friendName = friendObj?.displayName ?: "this friend"
        AlertDialog(
            onDismissRequest = { unpairConfirmTarget = null },
            title = { Text("Unpair Friends?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to disconnect ${user.effectiveName} and $friendName? They will be removed from each other's circles."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mqttManager.forceUnpairFriends(user.id, friendId) {
                            onShowToast("Unpaired ${user.effectiveName} and $friendName")
                        }
                        unpairConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("UNPAIR NOW", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { unpairConfirmTarget = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }

    // Bottom Sheets for selecting User A or User B
    if (pickingForA) {
        UserPickerSheet(
            title = "Select User 1",
            users = allUsers.filter { it.id != selectedUserB?.id },
            selectedUserId = selectedUserA?.id,
            onUserSelected = { selectedUserA = it },
            onDismiss = { pickingForA = false }
        )
    }

    if (pickingForB) {
        UserPickerSheet(
            title = "Select User 2",
            users = allUsers.filter { it.id != selectedUserA?.id },
            selectedUserId = selectedUserB?.id,
            onUserSelected = { selectedUserB = it },
            onDismiss = { pickingForB = false }
        )
    }
}
