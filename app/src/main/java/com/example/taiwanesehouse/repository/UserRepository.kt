package com.example.taiwanesehouse.repository

import android.util.Log
import com.example.taiwanesehouse.database.dao.UserDao
import com.google.firebase.firestore.FirebaseFirestore
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

    // Get user's current coin balance
    suspend fun getCurrentUserCoins(userId: String): Int {
        return try {
            // First check local database (faster)
            val coins = userDao.getUserCoins(userId) ?: 0
            coins
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user coins: ${e.message}")
            0
        }
    }

    // Add coins to user (after successful payment)
    suspend fun addCoinsToUser(userId: String, coinsToAdd: Int): Result<Boolean> {
        return try {
            val currentCoins = getCurrentUserCoins(userId)
            val newCoinsTotal = currentCoins + coinsToAdd

            // Update Firebase first (align field name with cart: memberCoins)
            usersCollection.document(userId)
                .update("memberCoins", newCoinsTotal)
                .await()

            // Update local database
            userDao.updateUserCoins(userId, newCoinsTotal)

            Log.d(TAG, "Added $coinsToAdd coins to user $userId. New total: $newCoinsTotal")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding coins to user: ${e.message}")
            Result.failure(e)
        }
    }

    // Deduct coins from user (when using coins in order)
    suspend fun deductCoinsFromUser(userId: String, coinsToDeduct: Int): Result<Boolean> {
        return try {
            val currentCoins = getCurrentUserCoins(userId)

            if (currentCoins < coinsToDeduct) {
                return Result.failure(Exception("Insufficient coins. Available: $currentCoins, Required: $coinsToDeduct"))
            }

            val newCoinsTotal = currentCoins - coinsToDeduct

            // Update Firebase first (align field name with cart: memberCoins)
            usersCollection.document(userId)
                .update("memberCoins", newCoinsTotal)
                .await()

            // Update local database
            userDao.updateUserCoins(userId, newCoinsTotal)

            Log.d(TAG, "Deducted $coinsToDeduct coins from user $userId. New total: $newCoinsTotal")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deducting coins from user: ${e.message}")
            Result.failure(e)
        }
    }
}