package com.pinpulse.admin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pinpulse.admin.model.AdminUser
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

// Google Hybrid Satellite Tile Source
val GoogleHybridTileSource = object : OnlineTileSourceBase(
    "GoogleHybrid", 0, 20, 256, ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=y&",
        "https://mt1.google.com/vt/lyrs=y&",
        "https://mt2.google.com/vt/lyrs=y&",
        "https://mt3.google.com/vt/lyrs=y&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$z"
    }
}

// CartoDB Dark Matter Tile Source for sleek fleet dashboard
val CartoDarkTileSource = object : OnlineTileSourceBase(
    "CartoDark", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$z/$x/$y.png"
    }
}

@Composable
fun AdminMapComponent(
    users: List<AdminUser>,
    selectedUser: AdminUser?,
    onSelectUser: (AdminUser?) -> Unit,
    onEditPlan: (AdminUser) -> Unit,
    onToggleLock: (AdminUser) -> Unit,
    onCopyCoords: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize OSMDroid cache configuration
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapStyle by remember { mutableStateOf("hybrid") } // "hybrid", "dark", "osm"
    var autoFitDone by remember { mutableStateOf(false) }

    // Manage MapView lifecycle with Android Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    fun fitAllUsers(mapView: MapView, animate: Boolean = true) {
        val validUsers = users.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        if (validUsers.isEmpty()) {
            mapView.controller.setZoom(3.0)
            mapView.controller.setCenter(GeoPoint(20.0, 0.0))
            return
        }
        if (validUsers.size == 1) {
            val u = validUsers.first()
            val pt = GeoPoint(u.latitude, u.longitude)
            if (animate) mapView.controller.animateTo(pt, 15.0, 800L)
            else {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(pt)
            }
            return
        }
        val minLat = validUsers.minOf { it.latitude }
        val maxLat = validUsers.maxOf { it.latitude }
        val minLng = validUsers.minOf { it.longitude }
        val maxLng = validUsers.maxOf { it.longitude }

        val latMargin = ((maxLat - minLat) * 0.25).coerceAtLeast(0.01)
        val lngMargin = ((maxLng - minLng) * 0.25).coerceAtLeast(0.01)

        val box = BoundingBox(
            maxLat + latMargin,
            maxLng + lngMargin,
            minLat - latMargin,
            minLng - lngMargin
        )
        mapView.zoomToBoundingBox(box, animate)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Map View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 2.0
                    maxZoomLevel = 20.0
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setTileSource(GoogleHybridTileSource)
                    controller.setZoom(4.0)
                    controller.setCenter(GeoPoint(36.8, 10.2))

                    mapViewRef = this
                }
            }
        )

        // Switch Tile Source
        LaunchedEffect(mapViewRef, mapStyle) {
            val mapView = mapViewRef ?: return@LaunchedEffect
            val src = when (mapStyle) {
                "dark" -> CartoDarkTileSource
                "osm" -> TileSourceFactory.MAPNIK
                else -> GoogleHybridTileSource
            }
            mapView.setTileSource(src)
            mapView.invalidate()
        }

        // Render User Markers
        LaunchedEffect(mapViewRef, users, selectedUser) {
            val mapView = mapViewRef ?: return@LaunchedEffect
            val validUsers = users.filter { it.latitude != 0.0 || it.longitude != 0.0 }

            // Clear old markers (keep base overlay index 0)
            mapView.overlays.removeAll { it is Marker }

            validUsers.forEach { user ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(user.latitude, user.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = null
                    val isSelected = selectedUser?.id == user.id
                    icon = BitmapDrawable(
                        context.resources,
                        createAdminMarkerBitmap(context, user, isSelected)
                    )
                    setOnMarkerClickListener { _, _ ->
                        onSelectUser(user)
                        mapView.controller.animateTo(position, 16.0, 600L)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }

            mapView.invalidate()

            // Initial auto-fit once when valid users are loaded
            if (!autoFitDone && validUsers.isNotEmpty()) {
                autoFitDone = true
                fitAllUsers(mapView, animate = false)
            }
        }

        // Focus selected user when changed from outside
        LaunchedEffect(selectedUser) {
            val u = selectedUser ?: return@LaunchedEffect
            val mapView = mapViewRef ?: return@LaunchedEffect
            if (u.latitude != 0.0 || u.longitude != 0.0) {
                mapView.controller.animateTo(GeoPoint(u.latitude, u.longitude), 16.0, 600L)
            }
        }

        // Floating Top-Right Controls (Style Switcher + Fleet Extents + Zoom)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Style Switcher Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE60F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "hybrid" to "Satellite",
                        "dark" to "Dark",
                        "osm" to "Streets"
                    ).forEach { (key, label) ->
                        val isChosen = mapStyle == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) Color(0xFF10B981) else Color.Transparent)
                                .clickable { mapStyle = key }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChosen) Color.Black else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Fit Fleet Button
            FloatingActionButton(
                onClick = { mapViewRef?.let { fitAllUsers(it, animate = true) } },
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF10B981),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit Fleet", modifier = Modifier.size(20.dp))
            }

            // Zoom In (+)
            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Zoom Out (-)
            FloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
        }

        // Active Devices Counter Overlay (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE60F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val onlineCount = users.count { it.isOnline && (it.latitude != 0.0 || it.longitude != 0.0) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$onlineCount/${users.size} Live on Map",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Selected User Live Telemetry Sheet (Bottom)
        selectedUser?.let { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (user.isOnline) Color(0x2010B981) else Color(0x2064748B),
                                        CircleShape
                                    )
                                    .border(
                                        2.dp,
                                        if (user.isOnline) Color(0xFF10B981) else Color(0xFF64748B),
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
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
                                    text = if (user.friendCode.isNotBlank()) "Code: ${user.friendCode}" else user.email,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(onClick = { onSelectUser(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry Row: Speed, Battery, Status, Coordinates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ${user.speedKmh.toInt()} km/h  •  🔋 ${user.batteryPercent}%  •  Plan: ${user.subscriptionPlan}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )

                        val isLocked = user.accountStatus.uppercase().contains("LOCK")
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isLocked) Color(0x20EF4444) else Color(0x2010B981),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isLocked) "LOCKED" else "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocked) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Coordinates with 1-tap Copy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCopyCoords("${user.latitude}, ${user.longitude}")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS: ${String.format("%.5f", user.latitude)}, ${String.format("%.5f", user.longitude)} (Tap to copy)",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Actions: Edit Plan & Lock/Unlock Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEditPlan(user) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val isLocked = user.accountStatus.uppercase().contains("LOCK")
                        Button(
                            onClick = { onToggleLock(user) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLocked) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        ) {
                            Icon(
                                if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isLocked) "Unlock" else "Lock Access",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates custom crisp marker bitmap with user initial, status ring, battery badge, and name pill.
 */
private fun createAdminMarkerBitmap(context: Context, user: AdminUser, isSelected: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (90 * density).toInt()
    val height = (70 * density).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isOnline = user.isOnline
    val isLocked = user.accountStatus.uppercase().contains("LOCK")

    val ringColor = when {
        isLocked -> android.graphics.Color.parseColor("#EF4444")
        isOnline -> android.graphics.Color.parseColor("#10B981")
        else -> android.graphics.Color.parseColor("#64748B")
    }

    // Avatar Circle
    val avatarRadius = 18f * density
    val centerX = width / 2f
    val centerY = 24f * density

    // Outer Selection Glow Ring
    if (isSelected) {
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
        }
        canvas.drawCircle(centerX, centerY, avatarRadius + 5f * density, glowPaint)
    }

    // Background circle
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, avatarRadius, bgPaint)

    // Status Ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ringColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawCircle(centerX, centerY, avatarRadius, ringPaint)

    // User Initial
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isOnline) ringColor else android.graphics.Color.WHITE
        textSize = 15f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val initial = user.effectiveName.take(1).uppercase()
    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(initial, 0, initial.length, textBounds)
    canvas.drawText(initial, centerX, centerY + (textBounds.height() / 2f), textPaint)

    // Battery & Speed Badge (Top Right)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (user.batteryPercent <= 20) android.graphics.Color.parseColor("#EF4444") else android.graphics.Color.parseColor("#059669")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX + avatarRadius - 2f * density, centerY - avatarRadius + 4f * density, 6f * density, badgePaint)

    // Bottom Name Pill
    val name = user.effectiveName.take(10)
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E60F172A")
        style = Paint.Style.FILL
    }
    val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#38BDF8") else ringColor
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    val nameTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val nameWidth = nameTextPaint.measureText(name) + (12f * density)
    val pillRect = RectF(
        centerX - (nameWidth / 2f),
        centerY + avatarRadius + (2f * density),
        centerX + (nameWidth / 2f),
        centerY + avatarRadius + (18f * density)
    )
    canvas.drawRoundRect(pillRect, 6f * density, 6f * density, pillPaint)
    canvas.drawRoundRect(pillRect, 6f * density, 6f * density, pillBorderPaint)
    canvas.drawText(name, centerX, pillRect.centerY() + (3.5f * density), nameTextPaint)

    return bitmap
}
