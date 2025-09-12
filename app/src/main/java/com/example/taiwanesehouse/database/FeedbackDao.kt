// DAO (Data Access Object)
package com.example.taiwanesehouse.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback_cache WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserFeedback(userId: String): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback_cache WHERE userId = :userId ORDER BY timestamp DESC LIMIT 20")
    suspend fun getUserFeedbackList(userId: String): List<FeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleFeedback(feedbacks: List<FeedbackEntity>)

    @Query("DELETE FROM feedback_cache WHERE userId = :userId")
    suspend fun clearUserFeedback(userId: String)

    @Query("SELECT COUNT(*) FROM feedback_cache WHERE userId = :userId AND timestamp > :weekAgo")
    suspend fun getWeeklySubmissionCount(userId: String, weekAgo: Long): Int
}
