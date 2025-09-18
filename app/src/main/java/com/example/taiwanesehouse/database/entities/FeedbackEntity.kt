package com.example.taiwanesehouse.database.entities

// Feedback Entity for Room Database
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val feedbackType: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val status: String,
    val isSynced: Boolean = true // Track if synced with Firebase
)