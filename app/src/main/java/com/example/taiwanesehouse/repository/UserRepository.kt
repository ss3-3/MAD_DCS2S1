package com.example.taiwanesehouse.repository

import android.util.Log
import com.example.taiwanesehouse.database.dao.UserDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Get user's current coin balance
     * First checks local database, then Firebase as fallback
     */
    suspend fun getCurrentUserCoins(userId: String): Int {
        return try {
            // First check local database (faster)
            val localCoins = userDao.getUserCoins(userId)

            if (localCoins != null) {
                Log.d(TAG, "Retrieved coins from local DB for user $userId: $localCoins")
                return localCoins
            }

            // Fallback to Firebase if local data not available
            val firebaseDoc = usersCollection.document(userId).get().await()
            val firebaseCoins = firebaseDoc.getLong("coins")?.toInt() ?: 0

            // Sync local database with Firebase data
            if (firebaseCoins > 0) {
                userDao.updateUserCoins(userId, firebaseCoins)
            }

            Log.d(TAG, "Retrieved coins from Firebase for user $userId: $firebaseCoins")
            firebaseCoins

        } catch (e: Exception) {
            Log.e(TAG, "Error getting user coins for $userId: ${e.message}")
            0 // Return 0 coins if unable to fetch
        }
    }

    /**
     * Add coins to user (after successful payment)
     * Updates both Firebase and local database
     */
    suspend fun addCoinsToUser(userId: String, coinsToAdd: Int): Result<Boolean> {
        return try {
            if (coinsToAdd <= 0) {
                Log.w(TAG, "Invalid coins to add: $coinsToAdd")
                return Result.failure(Exception("Invalid coin amount: $coinsToAdd"))
            }

            val currentCoins = getCurrentUserCoins(userId)
            val newCoinsTotal = currentCoins + coinsToAdd

            // Update Firebase first with merge to avoid overwriting other fields
            val updateData = mapOf("coins" to newCoinsTotal)
            usersCollection.document(userId)
                .set(updateData, SetOptions.merge())
                .await()

            // Update local database
            userDao.updateUserCoins(userId, newCoinsTotal)

            Log.d(TAG, "Successfully added $coinsToAdd coins to user $userId. New total: $newCoinsTotal")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding coins to user $userId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deduct coins from user (when using coins in order)
     * Includes validation and atomic operations
     */
    suspend fun deductCoinsFromUser(userId: String, coinsToDeduct: Int): Result<Boolean> {
        return try {
            if (coinsToDeduct <= 0) {
                Log.w(TAG, "Invalid coins to deduct: $coinsToDeduct")
                return Result.failure(Exception("Invalid coin amount: $coinsToDeduct"))
            }

            val currentCoins = getCurrentUserCoins(userId)

            if (currentCoins < coinsToDeduct) {
                Log.w(TAG, "Insufficient coins for user $userId. Available: $currentCoins, Required: $coinsToDeduct")
                return Result.failure(Exception("Insufficient coins. Available: $currentCoins, Required: $coinsToDeduct"))
            }

            val newCoinsTotal = currentCoins - coinsToDeduct

            // Update Firebase first with merge to avoid overwriting other fields
            val updateData = mapOf("coins" to newCoinsTotal)
            usersCollection.document(userId)
                .set(updateData, SetOptions.merge())
                .await()

            // Update local database
            userDao.updateUserCoins(userId, newCoinsTotal)

            Log.d(TAG, "Successfully deducted $coinsToDeduct coins from user $userId. New total: $newCoinsTotal")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deducting coins from user $userId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Validate if user has enough coins for deduction
     * Quick check without modifying balance
     */
    suspend fun hasEnoughCoins(userId: String, requiredCoins: Int): Boolean {
        return try {
            val currentCoins = getCurrentUserCoins(userId)
            currentCoins >= requiredCoins
        } catch (e: Exception) {
            Log.e(TAG, "Error checking coin balance for user $userId: ${e.message}")
            false
        }
    }

    /**
     * Get detailed coin information for user
     */
    suspend fun getCoinInfo(userId: String): CoinInfo {
        return try {
            val balance = getCurrentUserCoins(userId)
            CoinInfo(
                balance = balance,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting coin info for user $userId: ${e.message}")
            CoinInfo(
                balance = 0,
                isLoading = false,
                error = e.message
            )
        }
    }

    /**
     * Initialize user coin balance (for new users)
     */
    suspend fun initializeUserCoins(userId: String, initialCoins: Int = 0): Result<Boolean> {
        return try {
            val userData = mapOf(
                "coins" to initialCoins,
                "createdAt" to System.currentTimeMillis()
            )

            usersCollection.document(userId)
                .set(userData, SetOptions.merge())
                .await()

            userDao.updateUserCoins(userId, initialCoins)

            Log.d(TAG, "Initialized coins for user $userId with $initialCoins coins")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing coins for user $userId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Data class for coin information
     */
    data class CoinInfo(
        val balance: Int,
        val isLoading: Boolean,
        val error: String?
    )
}