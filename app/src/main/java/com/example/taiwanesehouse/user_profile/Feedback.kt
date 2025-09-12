package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Data classes for the feedback system
data class FeedbackItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val feedbackType: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Screen states
    var currentScreen by rememberSaveable { mutableStateOf("main") }
    var userFeedbackList by remember { mutableStateOf<List<FeedbackItem>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var canSubmitFeedback by remember { mutableStateOf(true) }
    var remainingSubmissions by remember { mutableStateOf(3) }
    var isOnline by remember { mutableStateOf(true) }

    // Form states
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var feedbackType by rememberSaveable { mutableStateOf("general") }
    var feedbackTitle by rememberSaveable { mutableStateOf("") }
    var feedbackMessage by rememberSaveable { mutableStateOf("") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val feedbackTypes = listOf(
        "general" to "General Feedback",
        "food" to "Food Quality",
        "service" to "Service Experience",
        "app" to "App Issues",
        "suggestion" to "Suggestions",
        "complaint" to "Complaint"
    )

    // Rate limiting check function
    suspend fun checkRateLimit() {
        try {
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val recentFeedbacks = firestore.collection("feedback")
                .whereEqualTo("userId", currentUser?.uid)
                .whereGreaterThan("timestamp", oneWeekAgo)
                .get()
                .await()

            val submissionCount = recentFeedbacks.size()
            remainingSubmissions = maxOf(0, 3 - submissionCount)
            canSubmitFeedback = remainingSubmissions > 0
        } catch (e: Exception) {
            // Error checking rate limit, allow submission
            canSubmitFeedback = true
            remainingSubmissions = 3
            isOnline = false
        }
    }

    // Load user's feedback history function
    suspend fun loadUserFeedbackHistory() {
        if (currentUser == null) return

        try {
            isLoadingHistory = true
            val feedbacks = firestore.collection("feedback")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            userFeedbackList = feedbacks.documents.mapNotNull { doc ->
                try {
                    FeedbackItem(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = doc.getLong("rating")?.toInt() ?: 0,
                        feedbackType = doc.getString("feedbackType") ?: "",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        status = doc.getString("status") ?: "pending"
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error loading feedback history: ${e.localizedMessage}")
            isOnline = false
        } finally {
            isLoadingHistory = false
        }
    }

    // Check network and sync on screen load
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            try {
                // Try to sync with Firebase
                checkRateLimit()
                loadUserFeedbackHistory()
                isOnline = true
            } catch (e: Exception) {
                // Offline mode
                isOnline = false
                canSubmitFeedback = false
            }
        }
    }

    // Submit feedback function
    fun submitFeedback() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                if (!isOnline) {
                    errorMessage = "Internet connection required to submit feedback"
                    return@launch
                }

                // Re-check rate limit before submission
                checkRateLimit()
                if (!canSubmitFeedback) {
                    errorMessage = "You've reached the weekly feedback limit. Please try again next week."
                    return@launch
                }

                // Validation
                if (rating == 0) {
                    errorMessage = "Please select a rating"
                    return@launch
                }

                if (feedbackTitle.isBlank()) {
                    errorMessage = "Please enter a feedback title"
                    return@launch
                }

                if (feedbackMessage.isBlank()) {
                    errorMessage = "Please enter your feedback message"
                    return@launch
                }

                if (feedbackMessage.length < 10) {
                    errorMessage = "Feedback message should be at least 10 characters"
                    return@launch
                }

                // Prepare feedback data
                val feedbackData = hashMapOf(
                    "userId" to (currentUser?.uid ?: "anonymous"),
                    "userEmail" to (currentUser?.email ?: contactEmail.takeIf { it.isNotEmpty() }),
                    "userName" to (currentUser?.displayName ?: "Anonymous User"),
                    "rating" to rating,
                    "feedbackType" to feedbackType,
                    "title" to feedbackTitle.trim(),
                    "message" to feedbackMessage.trim(),
                    "contactEmail" to contactEmail.trim().takeIf { it.isNotEmpty() },
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending",
                    "platform" to "android",
                    "appVersion" to "1.0.0"
                )

                // Submit to Firestore
                firestore.collection("feedback")
                    .add(feedbackData)
                    .await()

                // Reset form
                rating = 0
                feedbackType = "general"
                feedbackTitle = ""
                feedbackMessage = ""
                contactEmail = ""

                // Update counts and refresh history
                checkRateLimit()
                loadUserFeedbackHistory()

                snackbarHostState.showSnackbar("Feedback submitted successfully!")
                currentScreen = "history"

            } catch (e: Exception) {
                errorMessage = "Failed to submit feedback: ${e.localizedMessage ?: "Unknown error"}"
                isOnline = false // Network might be down
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (currentScreen) {
                                "form" -> "Submit Feedback"
                                "history" -> "My Feedback"
                                else -> "Feedback Center"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Black
                        )

                        // Offline indicator
                        if (!isOnline) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    fontSize = 10.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentScreen) {
                                "form", "history" -> currentScreen = "main"
                                else -> navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        when (currentScreen) {
            "main" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Feedback Center",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = if (isOnline) {
                                    "Share your thoughts and help us improve"
                                } else {
                                    "View your previous feedback • Connect to internet to submit new feedback"
                                },
                                fontSize = 14.sp,
                                color = if (isOnline) Color.Gray else Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Network status card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOnline) Color(0xFFF0F8FF) else Color(0xFFFFF0F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (isOnline) "Online Mode" else "Offline Mode",
                                fontWeight = FontWeight.Medium,
                                color = if (isOnline) Color(0xFF1976D2) else Color.Red
                            )

                            if (isOnline && currentUser != null) {
                                Text(
                                    text = "You can submit $remainingSubmissions more feedback(s) this week",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (!isOnline) {
                                Text(
                                    text = "You can view your previous feedback. Connect to internet to submit new feedback.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Action buttons
                    Button(
                        onClick = { currentScreen = "form" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            disabledContainerColor = Color.Gray
                        ),
                        enabled = canSubmitFeedback && isOnline
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Submit New Feedback",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canSubmitFeedback && isOnline) Color.Black else Color.White
                            )
                            if (!canSubmitFeedback) {
                                Text(
                                    text = "Weekly limit reached",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else if (!isOnline) {
                                Text(
                                    text = "Requires internet connection",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    if (currentUser != null) {
                        OutlinedButton(
                            onClick = { currentScreen = "history" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "View My Feedback History ${if (!isOnline) "(Offline)" else ""}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            "history" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Offline indicator in history
                    if (!isOnline) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                )
                            ) {
                                Text(
                                    text = "Viewing cached data. Connect to internet for latest updates.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6F00),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (isLoadingHistory) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (userFeedbackList.isEmpty()) {
                        item {
                            Card {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No Feedback Yet",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Your submitted feedback will appear here",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(userFeedbackList) { feedback ->
                            FeedbackHistoryCard(feedback = feedback)
                        }
                    }
                }
            }

            "form" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "We Value Your Feedback",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Help us improve by sharing your experience with Taiwanese House",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Rating section
                            Text(
                                text = "Overall Rating *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                repeat(5) { index ->
                                    IconButton(
                                        onClick = { rating = index + 1 },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = "Rate ${index + 1} star${if (index != 0) "s" else ""}",
                                            tint = if (index < rating) Color(0xFFFFC107) else Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                if (rating > 0) {
                                    Text(
                                        text = "($rating/5)",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // Feedback type selection
                            Text(
                                text = "Feedback Category",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                items(feedbackTypes.size) { index ->
                                    val (type, label) = feedbackTypes[index]
                                    FilterChip(
                                        onClick = { feedbackType = type },
                                        label = { Text(label, fontSize = 12.sp) },
                                        selected = feedbackType == type,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFC107),
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            // Feedback title
                            OutlinedTextField(
                                value = feedbackTitle,
                                onValueChange = {
                                    feedbackTitle = it
                                    if (errorMessage.isNotEmpty()) {
                                        errorMessage = ""
                                    }
                                },
                                label = { Text("Feedback Title *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                placeholder = { Text("Brief summary of your feedback") }
                            )

                            // Feedback message
                            OutlinedTextField(
                                value = feedbackMessage,
                                onValueChange = {
                                    feedbackMessage = it
                                    if (errorMessage.isNotEmpty()) {
                                        errorMessage = ""
                                    }
                                },
                                label = { Text("Your Feedback *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                minLines = 4,
                                maxLines = 8,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                placeholder = { Text("Please share your detailed feedback, suggestions, or concerns...") }
                            )

                            // Character count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${feedbackMessage.length}/500",
                                    fontSize = 12.sp,
                                    color = if (feedbackMessage.length > 500) Color.Red else Color.Gray
                                )
                            }

                            // Contact email (optional for non-authenticated users)
                            if (currentUser == null) {
                                OutlinedTextField(
                                    value = contactEmail,
                                    onValueChange = { contactEmail = it },
                                    label = { Text("Contact Email (Optional)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isLoading,
                                    placeholder = { Text("your.email@example.com") }
                                )
                            }

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )
                            }

                            // Submit button
                            Button(
                                onClick = { submitFeedback() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading &&
                                        rating > 0 &&
                                        feedbackTitle.isNotEmpty() &&
                                        feedbackMessage.isNotEmpty() &&
                                        feedbackMessage.length <= 500 &&
                                        isOnline
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = if (isOnline) "Submit Feedback" else "No Internet Connection",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cancel button
                            OutlinedButton(
                                onClick = { currentScreen = "main" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Gray
                                ),
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Privacy note
                    Text(
                        text = "Your feedback helps us improve our service. We respect your privacy and will only use this information to enhance your experience at Taiwanese House.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FeedbackHistoryCard(feedback: FeedbackItem) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val statusColor = when (feedback.status) {
        "pending" -> Color(0xFFFF9800)
        "reviewed" -> Color(0xFF2196F3)
        "resolved" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feedback.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = feedback.status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Rating and category
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < feedback.rating) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (index < feedback.rating) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = " • ${feedback.feedbackType.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Message preview
            Text(
                text = feedback.message,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                maxLines = 3,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Timestamp
            Text(
                text = dateFormat.format(Date(feedback.timestamp)),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}