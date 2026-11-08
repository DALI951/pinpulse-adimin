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
            try {
                if (client?.isConnected == true) {
                    client?.disconnect()
                }

                client = MqttAsyncClient(brokerUri, clientId, MemoryPersistence()).apply {
                    setCallback(this@AdminMqttManager)
                }

                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = false
                    connectionTimeout = 15
                    keepAliveInterval = 30
                }

                client?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(TAG, "Connected to MQTT broker: $brokerUri")
                        _isConnected.value = true
                        subscribeToTopics()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "Connection failed: ${exception?.message}")
                        _isConnected.value = false
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Exception on connect: ${e.message}")
                _isConnected.value = false
            }
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
            "pinpulse/v1/admin/users/+"
        )
        val qos = IntArray(topics.size) { 1 }

        try {
            client?.subscribe(topics, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "Subscribed to fleet administration topics successfully")
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
        Log.i(TAG, "MQTT connectComplete (reconnect=$reconnect)")
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
                    topic.contains("/loc") -> handleLocationPacket(topic, raw)
                    topic.contains("/status") -> handleStatusPacket(topic, raw)
                    topic.contains("/registry/user/") || topic.contains("/accounts/") -> handleUserRegistry(topic, raw)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error handling message on $topic: ${e.message}")
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

        updateUser(targetId) { existing ->
            existing.copy(
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

        // Update local memory representation immediately
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
     * Updates subscription plan and limits.
     */
    fun updateUserPlan(userId: String, plan: String, maxFriends: Int, maxGroups: Int, rateLimit: Int) {
        val cmd = JSONObject().apply {
            put("type", "ADMIN_UPDATE_PLAN")
            put("subscriptionPlan", plan)
            put("maxFriends", maxFriends)
            put("maxGroups", maxGroups)
            put("rateLimitSeconds", rateLimit)
            put("timestamp", System.currentTimeMillis())
        }
        publishRetained("pinpulse/v1/admin/users/$userId", cmd.toString())
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
        val cmd = JSONObject().apply {
            put("type", "ADMIN_UPDATE_STATUS")
            put("accountStatus", newStatus)
            put("timestamp", System.currentTimeMillis())
        }
        publishRetained("pinpulse/v1/admin/users/$userId", cmd.toString())
        updateUser(userId) { u -> u.copy(accountStatus = newStatus) }
        addAuditLog("UPDATE_STATUS", "$userId -> $newStatus")
    }

    fun silenceAllSirens() {
        val payload = JSONObject().apply {
            put("type", "SILENCE_ALL_SIRENS")
            put("action", "SILENCE")
            put("timestamp", System.currentTimeMillis())
        }
        publish("pinpulse/v1/admin/broadcast/silence", payload.toString(), qos = 1, retained = false)
        addAuditLog("FLEET_SILENCE", "All sirens stopped")
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
