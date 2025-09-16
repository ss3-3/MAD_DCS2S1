// HybridAuthManager.kt
package com.example.taiwanesehouse.auth

import android.content.Context
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.database.entities.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.regex.Pattern

class HybridAuthManager private constructor(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()

    companion object {
        @Volatile
        private var INSTANCE: HybridAuthManager? = null

        fun getInstance(context: Context): HybridAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HybridAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Get current user as Flow for reactive UI
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow()
    }

    /**
     * Get current user (suspend function)
     */
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }

    /**
     * Check if user is logged in
     */
    suspend fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Hash password for offline storage
     */
    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return emailPattern.matcher(email).matches()
    }

    /**
     * Validate phone number
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s-+]"), "")
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() }
    }

    /**
     * Clean phone number format
     */
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[\\s-]"), "").trim()
    }

    /**
     * Register new user (Hybrid approach)
     */
    suspend fun registerUser(
        username: String,
        email: String,
        phoneNumber: String?,
        password: String,
        securityQuestion: String,
        securityAnswer: String
    ): AuthResult {
        return try {
            // Check if user exists in Room first (offline check)
            val existingUser = userDao.checkUserExists(email.trim().lowercase(), phoneNumber?.let { cleanPhoneNumber(it) })
            if (existingUser > 0) {
                return AuthResult.Error("User already exists")
            }

            // Online uniqueness checks (Firestore)
            try {
                val emailDup = firestore.collection("users")
                    .whereEqualTo("email", email.trim().lowercase())
                    .limit(1)
                    .get()
                    .await()
                if (!emailDup.isEmpty) return AuthResult.Error("Email already in use")

                if (!phoneNumber.isNullOrBlank()) {
                    val phoneDup = firestore.collection("users")
                        .whereEqualTo("phoneNumber", cleanPhoneNumber(phoneNumber))
                        .limit(1)
                        .get()
                        .await()
                    if (!phoneDup.isEmpty) return AuthResult.Error("Phone number already in use")
                }
            } catch (_: Exception) {
                // Ignore network errors here; Firebase Auth will still enforce email uniqueness
            }

            // Try Firebase registration (online)
            val authResult = try {
                auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
            } catch (e: Exception) {
                // Firebase failed, but we can still create offline user for later sync
                null
            }

            val uid = authResult?.user?.uid ?: "offline_${System.currentTimeMillis()}"
            val user = UserEntity(
                uid = uid,
                username = username.trim(),
                email = email.trim().lowercase(),
                phoneNumber = phoneNumber?.let { cleanPhoneNumber(it) },
                passwordHash = hashPassword(password),
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer.trim().lowercase(),
                emailVerified = authResult?.user?.isEmailVerified ?: false,
                syncStatus = if (authResult != null) "synced" else "pending"
            )

            // Save to Room (always works offline)
            userDao.insertUser(user)

            // Try to save to Firebase (online)
            if (authResult != null) {
                try {
                    val userData = hashMapOf(
                        "username" to user.username,
                        "email" to user.email,
                        "phoneNumber" to user.phoneNumber,
                        "securityQuestion" to user.securityQuestion,
                        "securityAnswer" to user.securityAnswer,
                        "emailVerified" to user.emailVerified,
                        "createdAt" to user.createdAt,
                        "lastLoginAt" to user.lastLoginAt
                    )
                    firestore.collection("users").document(uid).set(userData).await()

                    // Update sync status
                    userDao.updateUser(user.copy(syncStatus = "synced"))

                    // Send verification email
                    try {
                        authResult.user?.sendEmailVerification()?.await()
                    } catch (_: Exception) {}
                } catch (e: Exception) {
                    // Firebase save failed, mark for later sync
                    userDao.updateUser(user.copy(syncStatus = "pending"))
                }
            }

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Registration failed: ${e.localizedMessage}")
        }
    }

    /**
     * Login user (Hybrid approach with Remember Me)
     */
    suspend fun loginUser(
        emailOrPhone: String,
        password: String,
        rememberMe: Boolean = false
    ): AuthResult {
        return try {
            val input = emailOrPhone.trim()
            val isEmail = input.contains("@")

            // First, try to find user in local database
            val localUser = if (isEmail) {
                userDao.getUserByEmail(input.lowercase())
            } else {
                userDao.getUserByPhone(cleanPhoneNumber(input))
            }

            // Verify password locally first (for offline capability)
            if (localUser != null) {
                val passwordHash = hashPassword(password)
                if (localUser.passwordHash == passwordHash) {
                    // Password matches locally, login user
                    userDao.logoutAllUsers() // Logout any other users
                    userDao.loginUser(localUser.uid)
                    userDao.setRememberMe(localUser.uid, rememberMe)

                    // Try Firebase authentication in background (don't block login)
                    tryFirebaseLogin(localUser.email, password)

                    return AuthResult.Success(localUser.copy(isLoggedIn = true, rememberMe = rememberMe))
                }
            }

            // If local authentication fails, try Firebase (online only)
            val firebaseUser = if (isEmail) {
                auth.signInWithEmailAndPassword(input.lowercase(), password).await()
            } else {
                // For phone login, find email first
                val userByPhone = findUserByPhone(cleanPhoneNumber(input))
                if (userByPhone != null) {
                    auth.signInWithEmailAndPassword(userByPhone, password).await()
                } else {
                    throw Exception("No account found with this phone number")
                }
            }

            firebaseUser.user?.let { fbUser ->
                // Sync Firebase user to Room
                val userData = getUserDataFromFirestore(fbUser.uid)
                val user = UserEntity(
                    uid = fbUser.uid,
                    username = userData?.get("fullName") as? String ?: fbUser.displayName ?: "",
                    email = fbUser.email ?: "",
                    phoneNumber = userData?.get("phoneNumber") as? String,
                    passwordHash = hashPassword(password),
                    securityQuestion = userData?.get("securityQuestion") as? String,
                    securityAnswer = userData?.get("securityAnswer") as? String,
                    emailVerified = fbUser.isEmailVerified,
                    createdAt = userData?.get("createdAt") as? Long ?: System.currentTimeMillis(),
                    isLoggedIn = true,
                    rememberMe = rememberMe,
                    syncStatus = "synced"
                )

                userDao.logoutAllUsers()
                userDao.insertUser(user)

                AuthResult.Success(user)
            } ?: AuthResult.Error("Login failed")

        } catch (e: Exception) {
            when {
                e.message?.contains("password", ignoreCase = true) == true ->
                    AuthResult.Error("Invalid email or password")
                e.message?.contains("network", ignoreCase = true) == true ->
                    AuthResult.Error("Network error. Please check your connection")
                e.message?.contains("user not found", ignoreCase = true) == true ->
                    AuthResult.Error("No account found with this email/phone")
                else -> AuthResult.Error("Login failed: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Auto-login with Remember Me
     */
    suspend fun autoLogin(): AuthResult? {
        val rememberedUser = userDao.getRememberedUser()
        return if (rememberedUser != null) {
            userDao.loginUser(rememberedUser.uid)
            AuthResult.Success(rememberedUser.copy(isLoggedIn = true))
        } else {
            null
        }
    }

    /**
     * Logout user
     */
    suspend fun logout() {
        userDao.logoutAllUsers()
        auth.signOut()
    }

    /**
     * Helper function to try Firebase login in background
     */
    private suspend fun tryFirebaseLogin(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            // Silent fail - user is already logged in locally
        }
    }

    /**
     * Find user email by phone number from Firebase
     */
    private suspend fun findUserByPhone(phoneNumber: String): String? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].getString("email")
            } else {
                // Check local database as fallback
                userDao.getUserByPhone(phoneNumber)?.email
            }
        } catch (e: Exception) {
            // Fallback to local database
            userDao.getUserByPhone(phoneNumber)?.email
        }
    }

    /**
     * Get user data from Firestore
     */
    private suspend fun getUserDataFromFirestore(uid: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) document.data else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sync pending users to Firebase when online
     */
    suspend fun syncPendingUsers(): Boolean {
        return try {
            // Implementation for syncing pending users when connection is available
            // This would be called when app detects internet connection
            true
        } catch (e: Exception) {
            false
        }
    }
    /*
    * Check email duplication
     */
    suspend fun checkEmailExists(email: String): Boolean {
        return userDao.checkEmailExists(email) > 0
    }
}

/**
 * Sealed class for authentication results
 */
sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}