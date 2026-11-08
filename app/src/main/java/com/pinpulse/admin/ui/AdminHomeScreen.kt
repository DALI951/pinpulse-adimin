package com.pinpulse.admin.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.pinpulse.admin.security.AdminSecurityManager

enum class AdminNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RADAR("Live Map", Icons.Default.Map),
    FRIENDS("Friends", Icons.Default.People),
    CRM("CRM Sheet", Icons.Default.Badge),
    SECURITY("Security", Icons.Default.Security)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    securityManager: AdminSecurityManager,
    mqttManager: AdminMqttManager,
    onLock: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by mqttManager.isConnected.collectAsState()
    var currentTab by remember { mutableStateOf(AdminNavTab.RADAR) }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    BackHandler {
        if (currentTab != AdminNavTab.FRIENDS) {
            currentTab = AdminNavTab.FRIENDS
        } else {
            onLock()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PinPulse Master Admin",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isConnected) "EMQX Fleet Broker Live" else "Connecting...",
                                fontSize = 11.sp,
                                color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            mqttManager.silenceAllSirens()
                            showToast("🚨 Silenced all sirens fleet-wide!")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Silence All Sirens",
                            tint = Color(0xFFEF4444)
                        )
                    }
                    IconButton(onClick = onLock) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Console",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF090D16))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                tonalElevation = 8.dp
            ) {
                AdminNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFF10B981) else Color(0xFF64748B)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF64748B)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0x2010B981)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AdminNavTab.FRIENDS -> FriendsTab(mqttManager = mqttManager, onShowToast = { showToast(it) })
                AdminNavTab.CRM -> CrmTab(mqttManager = mqttManager, onShowToast = { showToast(it) })
                AdminNavTab.RADAR -> RadarTab(mqttManager = mqttManager, onShowToast = { showToast(it) })
                AdminNavTab.SECURITY -> VaultTab(
                    securityManager = securityManager,
                    mqttManager = mqttManager,
                    onLockConsole = onLock,
                    onShowToast = { showToast(it) }
                )
            }
        }
    }
}
