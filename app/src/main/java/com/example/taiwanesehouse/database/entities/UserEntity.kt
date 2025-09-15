// UserEntity.kt
package com.example.taiwanesehouse.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String? = null,
    val passwordHash: String? = null, // For offline login
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val rememberMe: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profileImageUrl: String? = null,
    val syncStatus: String = "synced", // "synced", "pending", "failed"
    val coins: Int = 0
)