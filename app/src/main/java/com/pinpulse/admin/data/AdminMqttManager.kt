package com.pinpulse.admin.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pinpulse.admin.model.AdminUser
import com.pinpulse.admin.model.FriendItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AdminMqttManager(private val context: Context) : MqttCallbackExtended {

    companion object {
        private const val TAG = "PinPulseAdminMqtt"
        private const val PREFS_NAME = "pinpulse_admin_cache"
        private const val KEY_CACHED_USERS = "cached_users_json"
        private const val KEY_CACHED_FRIENDS = "cached_friends_json"
        private const val DEFAULT_BROKER = "tcp://broker.emqx.io:1883"

        // Same-cluster EMQX transports: zero split-brain when TCP 1883 is blocked on Wi-Fi/carrier NAT
        private val EMQX_CLUSTER_URIS = arrayOf(
            "tcp://broker.emqx.io:1883",
            "ssl://broker.emqx.io:8883",
            "ws://broker.emqx.io:8083/mqtt",
            "wss://broker.emqx.io:8084/mqtt"
        )
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _users = MutableStateFlow<Map<String, AdminUser>>(emptyMap())
    val users: StateFlow<Map<String, AdminUser>> = _users.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    private var client: MqttAsyncClient? = null
    private val clientId = "PinPulse_AdminPhone_" + UUID.randomUUID().toString().take(8)
    @Volatile
    private var currentUriIndex = 0

    init {
        loadCache()
        connect()
    }

    private fun loadCache() {
        try {
            val json = prefs.getString(KEY_CACHED_USERS, null)
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<Map<String, AdminUser>>() {}.type
                val map: Map<String, AdminUser> = gson.fromJson(json, type) ?: emptyMap()
                _users.value = map
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached users: ${e.message}")
        }
    }

    private fun saveCache() {
        try {
            val map = _users.value
            val json = gson.toJson(map)
            prefs.edit().putString(KEY_CACHED_USERS, json).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save users cache: ${e.message}")
        }
    }

    fun connect(brokerUri: String = DEFAULT_BROKER) {
        scope.launch {
            connectWithFallback(brokerUri, attempt = 0)
        }
    }

    private fun connectWithFallback(brokerUri: String, attempt: Int) {
        try {
            try {
                if (client?.isConnected == true) {
                    client?.disconnect()
                }
                client?.close()
            } catch (_: Exception) {}

            val useClusterFallback = (brokerUri == DEFAULT_BROKER)
            val chosenUri = if (useClusterFallback) {
                EMQX_CLUSTER_URIS[currentUriIndex % EMQX_CLUSTER_URIS.size]
            } else {
                brokerUri
            }

            client = MqttAsyncClient(chosenUri, clientId, MemoryPersistence()).apply {
                setCallback(this@AdminMqttManager)
            }

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 25
                if (useClusterFallback) {
                    serverURIs = EMQX_CLUSTER_URIS
                }
            }

            client?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "Connected to EMQX broker via $chosenUri")
                    _isConnected.value = true
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connect failed on $chosenUri: ${exception?.message}")
                    _isConnected.value = false
                    if (useClusterFallback) {
                        currentUriIndex = (currentUriIndex + 1) % EMQX_CLUSTER_URIS.size
                        val nextDelayMs = if (attempt < EMQX_CLUSTER_URIS.size) 1200L else 4000L
                        scope.launch {
                            delay(nextDelayMs)
                            if (_isConnected.value.not()) {
                                connectWithFallback(DEFAULT_BROKER, attempt + 1)
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception on connect: ${e.message}")
            _isConnected.value = false
        }
    }

    private fun subscribeToTopics() {
        val topics = arrayOf(
            "pinpulse/v1/user/+/loc",
            "pinpulse/v1/user/+/status",
            "pinpulse/v1/registry/user/+",
            "pinpulse/v1/registry/friends/+",
            "pinpulse/v1/registry/friends/#",
            "pinpulse/v1/accounts/+/backup",
            "pinpulse/v1/admin/users/+",
            "pinpulse/v1/ring/+",
            "pinpulse/v1/alerts/+",
            "pinpulse/v1/admin/broadcast/silence"
        )
        val qos = IntArray(topics.size) { 1 }

        try {
            client?.subscribe(topics, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "Subscribed to fleet administration & siren topics successfully")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.w(TAG, "Failed to subscribe: ${exception?.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Subscribe error: ${e.message}")
        }
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        _isConnected.value = true
        Log.i(TAG, "MQTT connectComplete (reconnect=$reconnect, uri=$serverURI)")
        subscribeToTopics()
    }

    override fun connectionLost(cause: Throwable?) {
        _isConnected.value = false
        Log.w(TAG, "MQTT connectionLost: ${cause?.message}")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        if (topic == null || message == null) return
        val raw = String(message.payload, Charsets.UTF_8).trim()
        if (raw.isBlank()) return

        scope.launch {
            try {
                when {
                    topic.contains("/registry/friends/") -> handleFriendsRegistry(topic, raw)
                    topic.contains("/admin/users/") -> handleAdminPolicy(topic, raw)
                    topic.contains("/ring/") -> handleRingOrAlertTelemetry(topic, raw)
                    topic.contains("/alerts/") -> handleRingOrAlertTelemetry(topic, raw)
                    topic.contains("/loc") -> handleLocationPacket(topic, raw)
                    topic.contains("/status") -> handleStatusPacket(topic, raw)
                    topic.contains("/registry/user/") || topic.contains("/accounts/") -> handleUserRegistry(topic, raw)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error handling message on $topic: ${e.message}")
            }
        }
    }

    private fun handleRingOrAlertTelemetry(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val type = obj.optString("type", "")
        val caller = obj.optString("callerName", obj.optString("senderName", "")).ifBlank { "Device" }
        val target = topic.substringAfterLast("/")
        when {
            type == "SOS_EMERGENCY" -> addAuditLog("SOS_ALERT", "🆘 $caller triggered SOS ($target)")
            type == "STOP_RING" || type == "STOP_SIREN" -> {
                if (caller != "Master Admin Console") {
                    addAuditLog("STOP_RING", "$caller -> $target")
                }
            }
            type == "RING_PHONE" -> {
                if (caller != "Master Admin Console") {
                    addAuditLog("RING_EVENT", "$caller -> $target")
                }
            }
        }
    }

    private fun handleFriendsRegistry(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val targetId = obj.optString("userId", "").ifBlank { topic.substringAfterLast("/") }
        if (targetId.isBlank()) return

        val arr = obj.optJSONArray("friends") ?: JSONArray()
        val friendsList = mutableListOf<FriendItem>()
        for (i in 0 until arr.length()) {
            val fo = arr.optJSONObject(i) ?: continue
            val fid = fo.optString("id", "")
            if (fid.isNotBlank()) {
                friendsList.add(
                    FriendItem(
                        id = fid,
                        displayName = fo.optString("displayName", "Friend"),
                        email = fo.optString("email", ""),
                        friendCode = fo.optString("friendCode", ""),
                        avatarIndex = fo.optInt("avatarIndex", 0)
                    )
                )
            }
        }

        updateUser(targetId) { existing ->
            existing.copy(
                displayName = obj.optString("userName", existing.displayName).ifBlank { existing.displayName },
                email = obj.optString("userEmail", existing.email).ifBlank { existing.email },
                friendCode = obj.optString("friendCode", existing.friendCode).ifBlank { existing.friendCode },
                friends = friendsList,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun handleAdminPolicy(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val targetId = obj.optString("targetUserId", "").ifBlank { topic.substringAfterLast("/") }
        if (targetId.isBlank()) return

        if (obj.optString("accountStatus") == "DELETED" || obj.optString("action") == "DELETE_ACCOUNT") {
            val map = _users.value.toMutableMap()
            map.remove(targetId)
            _users.value = map
            saveCache()
            return
        }

        updateUser(targetId) { existing ->
            existing.copy(
                subscriptionPlan = obj.optString("subscriptionPlan", existing.subscriptionPlan),
                accountStatus = obj.optString("accountStatus", existing.accountStatus),
                maxFriends = obj.optInt("maxFriends", existing.maxFriends),
                maxGroups = obj.optInt("maxGroups", existing.maxGroups),
                rateLimitSeconds = obj.optInt("rateLimitSeconds", existing.rateLimitSeconds)
            )
        }
    }

    private fun handleLocationPacket(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val targetId = obj.optString("userId", obj.optString("id", "")).ifBlank { topic.split("/").getOrNull(3) ?: "" }
        if (targetId.isBlank()) return

        val lat = obj.optDouble("latitude", obj.optDouble("lat", 0.0))
        val lng = obj.optDouble("longitude", obj.optDouble("lng", 0.0))
        val speed = obj.optDouble("speedKmh", obj.optDouble("speed", 0.0))
        val batt = obj.optInt("batteryPercent", obj.optInt("battery", 100))
        val charging = obj.optBoolean("isCharging", false)
        val name = obj.optString("displayName", obj.optString("userName", ""))
        val email = obj.optString("email", "")
        val code = obj.optString("friendCode", "")

        updateUser(targetId) { existing ->
            existing.copy(
                displayName = name.ifBlank { existing.displayName },
                email = email.ifBlank { existing.email },
                friendCode = code.ifBlank { existing.friendCode },
                latitude = if (lat != 0.0) lat else existing.latitude,
                longitude = if (lng != 0.0) lng else existing.longitude,
                speedKmh = speed,
                batteryPercent = batt,
                isCharging = charging,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun handleStatusPacket(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val targetId = obj.optString("userId", obj.optString("id", "")).ifBlank { topic.split("/").getOrNull(3) ?: "" }
        if (targetId.isBlank()) return

        val batt = obj.optInt("batteryPercent", 100)
        val charging = obj.optBoolean("isCharging", false)
        val gps = obj.optBoolean("isGpsEnabled", true)
        val sharing = obj.optBoolean("isSharingLocation", true)

        updateUser(targetId) { existing ->
            existing.copy(
                batteryPercent = batt,
                isCharging = charging,
                isGpsEnabled = gps,
                isSharingLocation = sharing,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun handleUserRegistry(topic: String, raw: String) {
        val obj = JSONObject(raw)
        val userObj = obj.optJSONObject("user") ?: obj
        val targetId = userObj.optString("id", userObj.optString("userId", "")).ifBlank { topic.substringAfterLast("/") }
        if (targetId.isBlank()) return

        val friendsArr = obj.optJSONArray("friends") ?: userObj.optJSONArray("friends")
        val parsedFriends = mutableListOf<FriendItem>()
        if (friendsArr != null) {
            for (i in 0 until friendsArr.length()) {
                val f = friendsArr.optJSONObject(i) ?: continue
                val fid = f.optString("id", "")
                if (fid.isNotBlank()) {
                    parsedFriends.add(
                        FriendItem(
                            id = fid,
                            displayName = f.optString("displayName", "Friend"),
                            email = f.optString("email", ""),
                            friendCode = f.optString("friendCode", ""),
                            avatarIndex = f.optInt("avatarIndex", 0)
                        )
                    )
                }
            }
        }

        updateUser(targetId) { existing ->
            existing.copy(
                displayName = userObj.optString("displayName", userObj.optString("name", existing.displayName)).ifBlank { existing.displayName },
                email = userObj.optString("email", existing.email).ifBlank { existing.email },
                friendCode = userObj.optString("friendCode", existing.friendCode).ifBlank { existing.friendCode },
                avatarIndex = userObj.optInt("avatarIndex", existing.avatarIndex),
                subscriptionPlan = userObj.optString("subscriptionPlan", existing.subscriptionPlan),
                friends = if (parsedFriends.isNotEmpty()) parsedFriends else existing.friends,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun updateUser(id: String, transform: (AdminUser) -> AdminUser) {
        val map = _users.value.toMutableMap()
        val current = map[id] ?: AdminUser(id = id)
        val updated = transform(current)
        map[id] = updated
        _users.value = map
        saveCache()
    }

    /**
     * ⚡ Instant Force-Pair: Remotely links two users without requiring friend codes or acceptance.
     */
    fun forcePairFriends(userA: AdminUser, userB: AdminUser, onComplete: (Boolean) -> Unit = {}) {
        if (userA.id == userB.id) {
            onComplete(false)
            return
        }

        val friendForA = JSONObject().apply {
            put("id", userB.id)
            put("displayName", userB.effectiveName)
            put("friendCode", userB.friendCode)
            put("email", userB.email)
            put("avatarIndex", userB.avatarIndex)
        }

        val friendForB = JSONObject().apply {
            put("id", userA.id)
            put("displayName", userA.effectiveName)
            put("friendCode", userA.friendCode)
            put("email", userA.email)
            put("avatarIndex", userA.avatarIndex)
        }

        val cmdToA = JSONObject().apply {
            put("type", "FORCE_ADD_FRIEND")
            put("friend", friendForA)
            put("timestamp", System.currentTimeMillis())
        }

        val cmdToB = JSONObject().apply {
            put("type", "FORCE_ADD_FRIEND")
            put("friend", friendForB)
            put("timestamp", System.currentTimeMillis())
        }

        publishRetained("pinpulse/v1/admin/users/${userA.id}", cmdToA.toString())
        publishRetained("pinpulse/v1/admin/users/${userB.id}", cmdToB.toString())

        updateUser(userA.id) { u ->
            val list = u.friends.toMutableList()
            if (list.none { it.id == userB.id }) {
                list.add(FriendItem(userB.id, userB.effectiveName, userB.email, userB.friendCode, userB.avatarIndex))
            }
            u.copy(friends = list)
        }

        updateUser(userB.id) { u ->
            val list = u.friends.toMutableList()
            if (list.none { it.id == userA.id }) {
                list.add(FriendItem(userA.id, userA.effectiveName, userA.email, userA.friendCode, userA.avatarIndex))
            }
            u.copy(friends = list)
        }

        addAuditLog("FORCE_PAIR", "${userA.effectiveName} ⇆ ${userB.effectiveName}")
        onComplete(true)
    }

    /**
     * Remotely disconnects a friendship between two users.
     */
    fun forceUnpairFriends(userAId: String, userBId: String, onComplete: (Boolean) -> Unit = {}) {
        val cmdToA = JSONObject().apply {
            put("type", "FORCE_REMOVE_FRIEND")
            put("friendId", userBId)
            put("timestamp", System.currentTimeMillis())
        }
        val cmdToB = JSONObject().apply {
            put("type", "FORCE_REMOVE_FRIEND")
            put("friendId", userAId)
            put("timestamp", System.currentTimeMillis())
        }

        publishRetained("pinpulse/v1/admin/users/$userAId", cmdToA.toString())
        publishRetained("pinpulse/v1/admin/users/$userBId", cmdToB.toString())

        updateUser(userAId) { u -> u.copy(friends = u.friends.filter { it.id != userBId }) }
        updateUser(userBId) { u -> u.copy(friends = u.friends.filter { it.id != userAId }) }

        addAuditLog("FORCE_UNPAIR", "$userAId ✕ $userBId")
        onComplete(true)
    }

    /**
     * Matches PinPulse client RealtimeService.cleanEmail (_at_ and _dot_).
     */
    fun cleanEmail(email: String): String =
        email.trim().lowercase().replace("@", "_at_").replace(".", "_dot_")

    /**
     * Legacy underscore encoding for backward compatibility with older admin policy topics.
     */
    private fun legacyCleanEmail(email: String): String =
        email.trim().lowercase().replace("@", "_at_").replace(".", "_")

    /**
     * Triggers remote emergency siren (RING_PHONE) on target user's phone, including 45s ring_pending catch-up.
     */
    fun ringUser(user: AdminUser): Boolean {
        if (client?.isConnected != true) {
            connect()
            return false
        }

        val now = System.currentTimeMillis()
        val ringId = "admin_ring_${now}_${UUID.randomUUID().toString().take(6)}"
        val payload = JSONObject().apply {
            put("type", "RING_PHONE")
            put("ringId", ringId)
            put("callerName", "Master Admin Console")
            put("callerId", "MASTER_ADMIN")
            put("callerCode", "ADMIN")
            put("callerEmail", "admin@pinpulse.app")
            put("adminOverride", true)
            put("isAdmin", true)
            put("targetUserId", user.id)
            put("timestamp", now)
        }.toString()

        val cleanId = user.id.trim()
        val rawCode = user.friendCode.trim().uppercase()
        val validCode = if (rawCode.isNotBlank() && rawCode != "----") rawCode else ""
        val rawEmail = user.email.trim().lowercase()
        val hasEmail = rawEmail.contains("@")
        val dotEmail = if (hasEmail) cleanEmail(rawEmail) else ""
        val legacyEmail = if (hasEmail) legacyCleanEmail(rawEmail) else ""

        if (cleanId.isNotBlank()) {
            publish("pinpulse/v1/ring/$cleanId", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$cleanId", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$cleanId", payload)
        }
        if (validCode.isNotBlank()) {
            publish("pinpulse/v1/ring/$validCode", payload, qos = 1, retained = false)
            val noDash = validCode.replace("-", "")
            if (noDash != validCode) {
                publish("pinpulse/v1/ring/$noDash", payload, qos = 1, retained = false)
            }
            publish("pinpulse/v1/ring/${validCode.lowercase()}", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$validCode", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$validCode", payload)
        }
        if (dotEmail.isNotBlank()) {
            publish("pinpulse/v1/ring/$dotEmail", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$dotEmail", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$dotEmail", payload)
            if (legacyEmail.isNotBlank() && legacyEmail != dotEmail) {
                publish("pinpulse/v1/ring/$legacyEmail", payload, qos = 1, retained = false)
                publish("pinpulse/v1/alerts/$legacyEmail", payload, qos = 1, retained = false)
                publishRetained("pinpulse/v1/ring_pending/$legacyEmail", payload)
            }
            if (rawEmail != dotEmail) {
                publish("pinpulse/v1/ring/$rawEmail", payload, qos = 1, retained = false)
                publish("pinpulse/v1/alerts/$rawEmail", payload, qos = 1, retained = false)
            }
        }

        addAuditLog("RING_PHONE", "🔊 Dispatched siren to ${user.effectiveName}")
        return true
    }

    /**
     * Remotely stops siren (STOP_RING) on target user's phone and clears retained ring_pending topics.
     */
    fun stopRingUser(user: AdminUser): Boolean {
        if (client?.isConnected != true) {
            connect()
            return false
        }

        val now = System.currentTimeMillis()
        val ringId = "admin_stop_${now}_${UUID.randomUUID().toString().take(6)}"
        val payload = JSONObject().apply {
            put("type", "STOP_RING")
            put("action", "STOP_SIREN")
            put("ringId", ringId)
            put("callerName", "Master Admin Console")
            put("callerId", "MASTER_ADMIN")
            put("callerCode", "ADMIN")
            put("callerEmail", "admin@pinpulse.app")
            put("adminOverride", true)
            put("isAdmin", true)
            put("targetUserId", user.id)
            put("timestamp", now)
        }.toString()

        val cleanId = user.id.trim()
        val rawCode = user.friendCode.trim().uppercase()
        val validCode = if (rawCode.isNotBlank() && rawCode != "----") rawCode else ""
        val rawEmail = user.email.trim().lowercase()
        val hasEmail = rawEmail.contains("@")
        val dotEmail = if (hasEmail) cleanEmail(rawEmail) else ""
        val legacyEmail = if (hasEmail) legacyCleanEmail(rawEmail) else ""

        if (cleanId.isNotBlank()) {
            publish("pinpulse/v1/ring/$cleanId", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$cleanId", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$cleanId", "")
        }
        if (validCode.isNotBlank()) {
            publish("pinpulse/v1/ring/$validCode", payload, qos = 1, retained = false)
            val noDash = validCode.replace("-", "")
            if (noDash != validCode) {
                publish("pinpulse/v1/ring/$noDash", payload, qos = 1, retained = false)
            }
            publish("pinpulse/v1/ring/${validCode.lowercase()}", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$validCode", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$validCode", "")
        }
        if (dotEmail.isNotBlank()) {
            publish("pinpulse/v1/ring/$dotEmail", payload, qos = 1, retained = false)
            publish("pinpulse/v1/alerts/$dotEmail", payload, qos = 1, retained = false)
            publishRetained("pinpulse/v1/ring_pending/$dotEmail", "")
            if (legacyEmail.isNotBlank() && legacyEmail != dotEmail) {
                publish("pinpulse/v1/ring/$legacyEmail", payload, qos = 1, retained = false)
                publish("pinpulse/v1/alerts/$legacyEmail", payload, qos = 1, retained = false)
                publishRetained("pinpulse/v1/ring_pending/$legacyEmail", "")
            }
            if (rawEmail != dotEmail) {
                publish("pinpulse/v1/ring/$rawEmail", payload, qos = 1, retained = false)
                publish("pinpulse/v1/alerts/$rawEmail", payload, qos = 1, retained = false)
            }
        }

        addAuditLog("STOP_SIREN", "🛑 Silenced siren on ${user.effectiveName}")
        return true
    }

    /**
     * Updates subscription plan and limits.
     */
    fun updateUserPlan(userId: String, plan: String, maxFriends: Int, maxGroups: Int, rateLimit: Int) {
        val targetUser = _users.value[userId]
        val codeUpper = targetUser?.friendCode?.trim()?.uppercase().orEmpty()
        val email = targetUser?.email?.trim().orEmpty()
        val dotEmailTag = if (email.isNotBlank()) cleanEmail(email) else ""
        val legacyEmailTag = if (email.isNotBlank()) legacyCleanEmail(email) else ""

        val cmd = JSONObject().apply {
            put("type", "ADMIN_UPDATE_PLAN")
            put("targetUserId", userId)
            put("userId", userId)
            if (codeUpper.isNotBlank() && codeUpper != "----") {
                put("targetFriendCode", codeUpper)
                put("friendCode", codeUpper)
            }
            if (email.isNotBlank()) {
                put("targetEmail", email)
                put("email", email)
            }
            put("subscriptionPlan", plan)
            put("maxFriends", maxFriends)
            put("maxGroups", maxGroups)
            put("rateLimitSeconds", rateLimit)
            put("rateLimitSec", rateLimit)
            put("timestamp", System.currentTimeMillis())
        }
        val payloadStr = cmd.toString()
        publishRetained("pinpulse/v1/admin/users/$userId", payloadStr)
        if (codeUpper.isNotBlank() && codeUpper != "----" && codeUpper != userId) {
            publishRetained("pinpulse/v1/admin/users/$codeUpper", payloadStr)
        }
        if (dotEmailTag.isNotBlank() && dotEmailTag != userId) {
            publishRetained("pinpulse/v1/admin/users/$dotEmailTag", payloadStr)
        }
        if (legacyEmailTag.isNotBlank() && legacyEmailTag != dotEmailTag && legacyEmailTag != userId) {
            publishRetained("pinpulse/v1/admin/users/$legacyEmailTag", payloadStr)
        }
        publish("pinpulse/v1/admin/broadcast/user_update", payloadStr, qos = 1, retained = false)

        updateUser(userId) { u ->
            u.copy(
                subscriptionPlan = plan,
                maxFriends = maxFriends,
                maxGroups = maxGroups,
                rateLimitSeconds = rateLimit
            )
        }
        addAuditLog("UPDATE_PLAN", "$userId -> $plan")
    }

    /**
     * Updates account status (ACTIVE, LOCKED_PAYMENT, BANNED, DELETED).
     */
    fun updateUserStatus(userId: String, newStatus: String) {
        val targetUser = _users.value[userId]
        val codeUpper = targetUser?.friendCode?.trim()?.uppercase().orEmpty()
        val email = targetUser?.email?.trim().orEmpty()
        val dotEmailTag = if (email.isNotBlank()) cleanEmail(email) else ""
        val legacyEmailTag = if (email.isNotBlank()) legacyCleanEmail(email) else ""

        val cmd = JSONObject().apply {
            put("type", "ADMIN_UPDATE_STATUS")
            put("targetUserId", userId)
            put("userId", userId)
            if (codeUpper.isNotBlank() && codeUpper != "----") {
                put("targetFriendCode", codeUpper)
                put("friendCode", codeUpper)
            }
            if (email.isNotBlank()) {
                put("targetEmail", email)
                put("email", email)
            }
            put("accountStatus", newStatus)
            put("timestamp", System.currentTimeMillis())
        }
        val payloadStr = cmd.toString()
        publishRetained("pinpulse/v1/admin/users/$userId", payloadStr)
        if (codeUpper.isNotBlank() && codeUpper != "----" && codeUpper != userId) {
            publishRetained("pinpulse/v1/admin/users/$codeUpper", payloadStr)
        }
        if (dotEmailTag.isNotBlank() && dotEmailTag != userId) {
            publishRetained("pinpulse/v1/admin/users/$dotEmailTag", payloadStr)
        }
        if (legacyEmailTag.isNotBlank() && legacyEmailTag != dotEmailTag && legacyEmailTag != userId) {
            publishRetained("pinpulse/v1/admin/users/$legacyEmailTag", payloadStr)
        }
        publish("pinpulse/v1/admin/broadcast/user_update", payloadStr, qos = 1, retained = false)

        updateUser(userId) { u -> u.copy(accountStatus = newStatus) }
        addAuditLog("UPDATE_STATUS", "$userId -> $newStatus")
    }

    fun silenceAllSirens() {
        val now = System.currentTimeMillis()
        val ringId = "admin_fleet_silence_${now}_${UUID.randomUUID().toString().take(6)}"
        val silencePayload = JSONObject().apply {
            put("type", "SILENCE_ALL_SIRENS")
            put("action", "SILENCE")
            put("ringId", ringId)
            put("callerName", "Master Admin Console")
            put("callerId", "MASTER_ADMIN")
            put("timestamp", now)
        }.toString()

        val stopBroadcastPayload = JSONObject().apply {
            put("type", "STOP_RING")
            put("action", "STOP_SIREN")
            put("ringId", ringId)
            put("callerName", "Master Admin Console")
            put("callerId", "MASTER_ADMIN")
            put("adminOverride", true)
            put("timestamp", now)
        }.toString()

        publish("pinpulse/v1/admin/broadcast/silence", silencePayload, qos = 1, retained = false)
        publish("pinpulse/v1/ring/broadcast", stopBroadcastPayload, qos = 1, retained = false)
        publish("pinpulse/v1/alerts/broadcast", stopBroadcastPayload, qos = 1, retained = false)

        // Clear any retained ring_pending packets across known fleet devices
        _users.value.values.forEach { u ->
            if (u.id.isNotBlank()) {
                publishRetained("pinpulse/v1/ring_pending/${u.id.trim()}", "")
            }
            val code = u.friendCode.trim().uppercase()
            if (code.isNotBlank() && code != "----") {
                publishRetained("pinpulse/v1/ring_pending/$code", "")
            }
            if (u.email.contains("@")) {
                val dotTag = cleanEmail(u.email)
                val legacyTag = legacyCleanEmail(u.email)
                publishRetained("pinpulse/v1/ring_pending/$dotTag", "")
                if (legacyTag != dotTag) {
                    publishRetained("pinpulse/v1/ring_pending/$legacyTag", "")
                }
            }
        }

        addAuditLog("FLEET_SILENCE", "All sirens stopped & ring_pending cleared")
    }

    private fun publishRetained(topic: String, message: String) {
        publish(topic, message, qos = 1, retained = true)
    }

    private fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val mqttMsg = MqttMessage(message.toByteArray(Charsets.UTF_8)).apply {
                this.qos = qos
                this.isRetained = retained
            }
            client?.publish(topic, mqttMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing to $topic: ${e.message}")
        }
    }

    private fun addAuditLog(action: String, details: String) {
        val log = "[$action] $details"
        val list = _auditLogs.value.toMutableList()
        list.add(0, log)
        if (list.size > 100) list.removeAt(list.size - 1)
        _auditLogs.value = list
    }
}
