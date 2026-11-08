package com.pinpulse.admin.model

data class AdminUser(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val friendCode: String = "",
    val avatarIndex: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Double = 0.0,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isSharingLocation: Boolean = true,
    val lastUpdated: Long = 0L,
    val subscriptionPlan: String = "DEMO",
    val maxFriends: Int = 1,
    val maxGroups: Int = 0,
    val accountStatus: String = "ACTIVE",
    val rateLimitSeconds: Int = 5,
    val friends: List<FriendItem> = emptyList()
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastUpdated) < 300_000L // online if heard within 5 minutes

    val effectiveName: String
        get() = displayName.ifBlank { "User " + id.take(5) }
}
