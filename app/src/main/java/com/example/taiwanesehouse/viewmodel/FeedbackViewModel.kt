package com.example.taiwanesehouse.viewmodel

import androidx.lifecycle.ViewModel
import com.example.taiwanesehouse.repository.FeedbackRepository

class FeedbackViewModel(private val repository: FeedbackRepository) : ViewModel() {

    fun getUserFeedbackFlow(userId: String) = repository.getUserFeedbackFlow(userId)

    suspend fun syncFeedback(userId: String) = repository.syncFeedbackFromFirebase(userId)

    suspend fun submitFeedback(feedbackData: HashMap<String, Any>) = repository.submitFeedback(feedbackData)

    suspend fun getWeeklyCount(userId: String) = repository.getWeeklySubmissionCount(userId)
}