// FeedbackViewModel.kt
package com.example.taiwanesehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.taiwanesehouse.repository.FeedbackRepository
import com.example.taiwanesehouse.database.entities.FeedbackEntity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModelProvider

enum class ConnectionState {
    CHECKING, ONLINE, OFFLINE
}

data class FeedbackUIState(
    val connectionState: ConnectionState = ConnectionState.CHECKING,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val canSubmitFeedback: Boolean = true,
    val remainingSubmissions: Int = 3,
    val pendingSyncCount: Int = 0,
    val errorMessage: String = "",
    val successMessage: String = ""
)

data class FeedbackFormState(
    val rating: Int = 0,
    val feedbackType: String = "general",
    val title: String = "",
    val message: String = "",
    val contactEmail: String = ""
)
// Create a ViewModelFactory
class FeedbackViewModelFactory(
    private val repository: FeedbackRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedbackViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class FeedbackViewModel(
    private val repository: FeedbackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUIState())
    val uiState: StateFlow<FeedbackUIState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(FeedbackFormState())
    val formState: StateFlow<FeedbackFormState> = _formState.asStateFlow()

    fun getUserFeedbackFlow(userId: String) = repository.getUserFeedbackFlow(userId)

    // Initialize connection and check limits
    fun initializeConnection(context: Context, userId: String?) {
        viewModelScope.launch {
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.OFFLINE,
                    canSubmitFeedback = true,
                    remainingSubmissions = 3
                )
                return@launch
            }

            try {
                _uiState.value = _uiState.value.copy(connectionState = ConnectionState.CHECKING)

                // Check network first
                val isOnline = isNetworkAvailable(context)

                if (!isOnline) {
                    // If no network, get cached data and set offline
                    val weeklyCount = repository.getWeeklySubmissionCount(userId)
                    val pendingCount = repository.getPendingFeedbackCount(userId)

                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.OFFLINE,
                        remainingSubmissions = maxOf(0, 3 - weeklyCount),
                        canSubmitFeedback = true, // Allow offline submissions
                        pendingSyncCount = pendingCount
                    )
                    return@launch
                }

                // Network is available, try Firebase operations
                try {
                    // Get cached data first
                    val weeklyCount = repository.getWeeklySubmissionCount(userId)
                    val pendingCount = repository.getPendingFeedbackCount(userId)

                    // Try to sync with Firebase
                    repository.syncFeedbackFromFirebase(userId)

                    // Sync pending feedback
                    val syncedCount = repository.syncPendingFeedback(userId)
                    val newPendingCount = maxOf(0, pendingCount - syncedCount)

                    // Recalculate limits after sync
                    val updatedWeeklyCount = repository.getWeeklySubmissionCount(userId)

                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.ONLINE,
                        remainingSubmissions = maxOf(0, 3 - updatedWeeklyCount),
                        canSubmitFeedback = maxOf(0, 3 - updatedWeeklyCount) > 0,
                        pendingSyncCount = newPendingCount
                    )
                } catch (e: Exception) {
                    // Firebase failed but network is available - treat as offline
                    val weeklyCount = repository.getWeeklySubmissionCount(userId)
                    val pendingCount = repository.getPendingFeedbackCount(userId)

                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.OFFLINE,
                        remainingSubmissions = maxOf(0, 3 - weeklyCount),
                        canSubmitFeedback = true,
                        pendingSyncCount = pendingCount
                    )
                }
            } catch (e: Exception) {
                // Any other error - set offline
                _uiState.value = _uiState.value.copy(
                    connectionState = ConnectionState.OFFLINE,
                    canSubmitFeedback = true,
                    remainingSubmissions = 3,
                    pendingSyncCount = 0
                )
            }
        }
    }

    // Load feedback history
    fun loadFeedbackHistory(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                if (_uiState.value.connectionState == ConnectionState.ONLINE) {
                    repository.syncFeedbackFromFirebase(userId)
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading feedback: ${e.localizedMessage}"
                )
            }
        }
    }

    // Submit feedback
    fun submitFeedback(context: Context, userId: String, userEmail: String?, userName: String?) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = "")
                val form = _formState.value

                // Validation
                if (form.rating == 0) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Please select a rating"
                    )
                    return@launch
                }

                if (form.title.trim().isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Please enter a feedback title"
                    )
                    return@launch
                }

                if (form.message.trim().isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Please enter your feedback message"
                    )
                    return@launch
                }

                if (form.message.trim().length < 10) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Feedback message should be at least 10 characters"
                    )
                    return@launch
                }

                if (form.message.length > 500) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Feedback message cannot exceed 500 characters"
                    )
                    return@launch
                }

                // Check rate limits for online submissions
                if (_uiState.value.connectionState == ConnectionState.ONLINE && !_uiState.value.canSubmitFeedback) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "You've reached the weekly feedback limit (3 submissions). Please try again next week."
                    )
                    return@launch
                }

                // Prepare feedback data
                val feedbackData: HashMap<String, Any> = hashMapOf(
                    "userId" to userId,
                    "userEmail" to (userEmail ?: form.contactEmail.takeIf { it.isNotEmpty() } ?: ""),
                    "userName" to (userName ?: "Anonymous User"),
                    "rating" to form.rating,
                    "feedbackType" to form.feedbackType,
                    "title" to form.title.trim(),
                    "message" to form.message.trim(),
                    "contactEmail" to (form.contactEmail.trim().takeIf { it.isNotEmpty() } ?: ""),
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending",
                    "platform" to "android",
                    "appVersion" to "1.0.0"
                )

                if (_uiState.value.connectionState == ConnectionState.ONLINE) {
                    // Submit online
                    repository.submitFeedback(feedbackData)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Feedback submitted successfully!"
                    )
                } else {
                    // Queue locally when offline
                    val feedbackEntity = FeedbackEntity(
                        id = "local_${System.currentTimeMillis()}",
                        userId = userId,
                        userName = userName ?: "Anonymous User",
                        rating = form.rating,
                        feedbackType = form.feedbackType,
                        title = form.title.trim(),
                        message = form.message.trim(),
                        timestamp = System.currentTimeMillis(),
                        status = "pending",
                        isSynced = false
                    )

                    repository.enqueueFeedbackLocal(feedbackEntity)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        pendingSyncCount = _uiState.value.pendingSyncCount + 1,
                        successMessage = "Feedback saved locally. It will sync when you're back online."
                    )
                }

                // Reset form
                _formState.value = FeedbackFormState()

                // Update limits
                initializeConnection(context, userId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Failed to submit feedback: ${e.localizedMessage}"
                )
            }
        }
    }

    // Sync pending feedback
    fun syncPendingFeedback(context: Context, userId: String) {
        viewModelScope.launch {
            try {
                if (_uiState.value.connectionState != ConnectionState.ONLINE) {
                    val isOnline = isNetworkAvailable(context)
                    if (!isOnline) {
                        _uiState.value = _uiState.value.copy(errorMessage = "No internet connection available")
                        return@launch
                    }
                }

                val syncedCount = repository.trySyncNow(userId)
                if (syncedCount > 0) {
                    _uiState.value = _uiState.value.copy(
                        pendingSyncCount = maxOf(0, _uiState.value.pendingSyncCount - syncedCount),
                        successMessage = "Synced $syncedCount pending feedback items"
                    )
                    initializeConnection(context, userId) // Refresh limits after sync
                } else {
                    _uiState.value = _uiState.value.copy(successMessage = "No pending items to sync")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sync failed: ${e.localizedMessage}")
            }
        }
    }

    // Form state updates
    fun updateRating(rating: Int) {
        _formState.value = _formState.value.copy(rating = rating)
        clearErrorMessage()
    }

    fun updateFeedbackType(type: String) {
        _formState.value = _formState.value.copy(feedbackType = type)
    }

    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title)
        clearErrorMessage()
    }

    fun updateMessage(message: String) {
        _formState.value = _formState.value.copy(message = message)
        clearErrorMessage()
    }

    fun updateContactEmail(email: String) {
        _formState.value = _formState.value.copy(contactEmail = email)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = "")
    }

    private fun handleError(error: Throwable, operation: String) {
        val errorMessage = when (error) {
            is java.net.UnknownHostException -> "No internet connection"
            is java.net.SocketTimeoutException -> "Connection timeout"
            is com.google.firebase.firestore.FirebaseFirestoreException ->
                "Database error: ${error.localizedMessage}"
            else -> "Failed to $operation: ${error.localizedMessage}"
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSubmitting = false,
            errorMessage = errorMessage
        )
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            // Simplified check - just check if we have internet capability
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}