//Repository for Data Management
package com.example.taiwanesehouse.repository

import com.example.taiwanesehouse.database.dao.FeedbackDao
import com.example.taiwanesehouse.database.entities.FeedbackEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FeedbackRepository(
    private val feedbackDao: FeedbackDao,
    private val firestore: FirebaseFirestore
) {

    // Add this method to FeedbackRepository
    suspend fun getPendingFeedbackCount(userId: String): Int {
        return feedbackDao.getPendingFeedbackCount(userId)
    }

    // Get user feedback from local cache (for offline viewing)
    fun getUserFeedbackFlow(userId: String): Flow<List<FeedbackEntity>> {
        return feedbackDao.getUserFeedback(userId)
    }

    // Sync feedback from Firebase to local cache
    suspend fun syncFeedbackFromFirebase(userId: String) {
        try {
            // Avoid composite index requirement by removing orderBy; sort client-side
            val firebaseFeedbacks = firestore.collection("feedback")
                .whereEqualTo("userId", userId)
                .limit(50)
                .get()
                .await()

            val feedbackEntities = firebaseFeedbacks.documents.mapNotNull { doc ->
                try {
                    FeedbackEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = doc.getLong("rating")?.toInt() ?: 0,
                        feedbackType = doc.getString("feedbackType") ?: "",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        status = doc.getString("status") ?: "pending",
                        isSynced = true
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timestamp }

            // Clear old cache and insert new data
            feedbackDao.clearUserFeedback(userId)
            feedbackDao.insertMultipleFeedback(feedbackEntities)

        } catch (e: Exception) {
            // Handle sync error - user can still view cached data
            throw e
        }
    }

    // Submit feedback (online) and cache locally
    suspend fun submitFeedback(feedbackData: HashMap<String, Any>): String {
        // Submit to Firebase
        val docRef = firestore.collection("feedback")
            .add(feedbackData)
            .await()

        // Cache locally
        val feedbackEntity = FeedbackEntity(
            id = docRef.id,
            userId = feedbackData["userId"] as String,
            userName = (feedbackData["userName"] as? String) ?: "",
            rating = feedbackData["rating"] as Int,
            feedbackType = (feedbackData["feedbackType"] as? String) ?: "",
            title = (feedbackData["title"] as? String) ?: "",
            message = (feedbackData["message"] as? String) ?: "",
            timestamp = feedbackData["timestamp"] as Long,
            status = (feedbackData["status"] as? String) ?: "pending",
            isSynced = true
        )

        feedbackDao.insertFeedback(feedbackEntity)

        return docRef.id
    }

    // Enqueue feedback locally when offline
    suspend fun enqueueFeedbackLocal(feedbackEntity: FeedbackEntity) {
        feedbackDao.insertFeedback(feedbackEntity.copy(isSynced = false))
    }

    // Sync pending local feedback to Firestore
    suspend fun syncPendingFeedback(userId: String): Int {
        val pending = feedbackDao.getPendingFeedback(userId)
        var successCount = 0
        for (fb in pending) {
            try {
                val data = hashMapOf(
                    "userId" to fb.userId,
                    "userName" to fb.userName,
                    "rating" to fb.rating,
                    "feedbackType" to fb.feedbackType,
                    "title" to fb.title,
                    "message" to fb.message,
                    "timestamp" to fb.timestamp,
                    "status" to fb.status
                )
                firestore.collection("feedback").add(data).await()
                feedbackDao.markFeedbackSynced(fb.id, fb.status)
                successCount++
            } catch (_: Exception) {
                // keep pending
            }
        }
        return successCount
    }

    // Get weekly submission count (from local cache for performance)
    suspend fun getWeeklySubmissionCount(userId: String): Int {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return feedbackDao.getWeeklySubmissionCount(userId, oneWeekAgo)
    }

    // Helper to try syncing cached feedback on demand
    suspend fun trySyncNow(userId: String): Int {
        return syncPendingFeedback(userId)
    }
}
