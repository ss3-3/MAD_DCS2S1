package com.example.taiwanesehouse.database.dao
// DAO (Data Access Object)
import androidx.room.*
import com.example.taiwanesehouse.database.entities.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserFeedback(userId: String): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedback WHERE userId = :userId ORDER BY timestamp DESC LIMIT 20")
    suspend fun getUserFeedbackList(userId: String): List<FeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleFeedback(feedbacks: List<FeedbackEntity>)

    @Query("DELETE FROM feedback WHERE userId = :userId")
    suspend fun clearUserFeedback(userId: String)

    @Query("SELECT COUNT(*) FROM feedback WHERE userId = :userId AND timestamp > :weekAgo")
    suspend fun getWeeklySubmissionCount(userId: String, weekAgo: Long): Int

    @Query("SELECT * FROM feedback WHERE userId = :userId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingFeedback(userId: String): List<FeedbackEntity>

    @Query("UPDATE feedback SET isSynced = 1, status = :status WHERE id = :id")
    suspend fun markFeedbackSynced(id: String, status: String)

    @Query("SELECT COUNT(*) FROM feedback WHERE userId = :userId AND isSynced = 0")
    suspend fun getPendingFeedbackCount(userId: String): Int
}
