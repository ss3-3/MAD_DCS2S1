package com.example.taiwanesehouse

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get user data from Firestore
     */
    suspend fun getUserData(uid: String? = null): UserData? {
        return try {
            val userId = uid ?: getCurrentUser()?.uid ?: return null
            val document = firestore.collection("users").document(userId).get().await()

            if (document.exists()) {
                UserData(
                    uid = document.getString("uid") ?: "",
                    fullName = document.getString("fullName") ?: "",
                    email = document.getString("email") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",
                    phoneVerified = document.getBoolean("phoneVerified") ?: false,
                    createdAt = document.getLong("createdAt") ?: 0L
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update user data in Firestore
     */
    suspend fun updateUserData(userData: Map<String, Any>): Boolean {
        return try {
            val userId = getCurrentUser()?.uid ?: return false
            firestore.collection("users").document(userId).update(userData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if phone number exists in database
     */
    suspend fun isPhoneNumberRegistered(phoneNumber: String): Boolean {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber.trim())
                .limit(1)
                .get()
                .await()

            !querySnapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get email by phone number for login
     */
    suspend fun getEmailByPhoneNumber(phoneNumber: String): String? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber.trim())
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].getString("email")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Login with phone number and password
     */
    suspend fun loginWithPhoneAndPassword(phoneNumber: String, password: String): LoginResult {
        return try {
            // Get email associated with phone number
            val email = getEmailByPhoneNumber(phoneNumber)
                ?: return LoginResult.Error("No account found with this phone number")

            // Sign in with email and password
            auth.signInWithEmailAndPassword(email, password).await()

            LoginResult.Success(getCurrentUser())
        } catch (e: Exception) {
            when {
                e.message?.contains("password", ignoreCase = true) == true ->
                    LoginResult.Error("Incorrect password")
                e.message?.contains("network", ignoreCase = true) == true ->
                    LoginResult.Error("Network error. Please check your connection")
                else -> LoginResult.Error("Login failed: ${e.localizedMessage}")
            }
        }
    }
}

/**
 * Data class representing user information
 */
data class UserData(
    val uid: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val phoneVerified: Boolean,
    val createdAt: Long
)

/**
 * Sealed class for login results
 */
sealed class LoginResult {
    data class Success(val user: FirebaseUser?) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * Helper functions for phone number validation
 */
object ValidationUtils {
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\+[1-9]\\d{1,14}$")) ||
                phoneNumber.matches(Regex("^\\+?[1-9][\\d\\s-]{7,17}$"))
    }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun formatPhoneNumber(phoneNumber: String): String {
        // Remove spaces and dashes, ensure starts with +
        val cleaned = phoneNumber.replace(Regex("[\\s-]"), "")
        return if (cleaned.startsWith("+")) cleaned else "+$cleaned"
    }
}