package com.pinpulse.admin.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class AdminSecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pinpulse_admin_secure_vault", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PASSWORD_HASH = "admin_master_hash"
        private const val KEY_SALT = "admin_master_salt"
        private const val KEY_CREATED_AT = "admin_master_created_at"
        private const val KEY_LAST_LOGIN = "admin_master_last_login"
    }

    fun isPasswordSet(): Boolean {
        val hash = prefs.getString(KEY_PASSWORD_HASH, null)
        val salt = prefs.getString(KEY_SALT, null)
        return !hash.isNullOrBlank() && !salt.isNullOrBlank()
    }

    fun setMasterPassword(password: String): Boolean {
        if (password.length < 4) return false

        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPassword(password, salt)

        prefs.edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .putString(KEY_SALT, salt)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()

        return true
    }

    fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val computedHash = hashPassword(password, salt)
        val matches = storedHash == computedHash

        if (matches) {
            prefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
        }

        return matches
    }

    fun changeMasterPassword(oldPass: String, newPass: String): Boolean {
        if (!verifyPassword(oldPass)) return false
        return setMasterPassword(newPass)
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
