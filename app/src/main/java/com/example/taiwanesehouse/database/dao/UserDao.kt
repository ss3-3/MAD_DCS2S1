// UserDao.kt
package com.example.taiwanesehouse.database.dao

import androidx.room.*
import com.example.taiwanesehouse.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhone(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE rememberMe = 1 LIMIT 1")
    suspend fun getRememberedUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0, rememberMe = 0")
    suspend fun logoutAllUsers()

    @Query("UPDATE users SET isLoggedIn = 1, lastLoginAt = :timestamp WHERE uid = :uid")
    suspend fun loginUser(uid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET rememberMe = :remember WHERE uid = :uid")
    suspend fun setRememberMe(uid: String, remember: Boolean)

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)

    @Query("SELECT COUNT(*) FROM users WHERE email = :email OR phoneNumber = :phone")
    suspend fun checkUserExists(email: String, phone: String?): Int
}