package com.pinpulse.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinpulse.admin.model.AdminUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanSheet(
    user: AdminUser,
    onSave: (plan: String, maxFriends: Int, maxGroups: Int, rateLimit: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(user.subscriptionPlan.uppercase()) }
    var maxFriends by remember { mutableStateOf(user.maxFriends.toString()) }
    var maxGroups by remember { mutableStateOf(user.maxGroups.toString()) }
    var rateLimit by remember { mutableStateOf(user.rateLimitSeconds.toString()) }

    fun selectPreset(plan: String, f: Int, g: Int, r: Int) {
        selectedPlan = plan
        maxFriends = f.toString()
        maxGroups = g.toString()
        rateLimit = r.toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Subscription & Quotas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = user.effectiveName + (if (user.friendCode.isNotBlank()) " (${user.friendCode})" else ""),
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SUBSCRIPTION TIER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("DEMO", 1, 0),
                    Triple("FAMILY", 10, 5),
                    Triple("BUSINESS", 50, 20)
                ).forEach { (planName, defFriends, defGroups) ->
                    val isChosen = selectedPlan == planName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectPreset(planName, defFriends, defGroups, if (planName == "DEMO") 10 else 3) }
                            .background(
                                if (isChosen) Color(0x2010B981) else Color(0xFF1E293B),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isChosen) Color(0xFF10B981) else Color(0xFF334155),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = planName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChosen) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = maxFriends,
                    onValueChange = { maxFriends = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Friends") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )

                OutlinedTextField(
                    value = maxGroups,
                    onValueChange = { maxGroups = it.filter { c -> c.isDigit() } },
                    label = { Text("Max Groups") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rateLimit,
                onValueChange = { rateLimit = it.filter { c -> c.isDigit() } },
                label = { Text("Update Rate Limit (seconds)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val f = maxFriends.toIntOrNull() ?: user.maxFriends
                    val g = maxGroups.toIntOrNull() ?: user.maxGroups
                    val r = rateLimit.toIntOrNull() ?: user.rateLimitSeconds
                    onSave(selectedPlan, f, g, r)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "APPLY & PUSH TO FLEET",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
