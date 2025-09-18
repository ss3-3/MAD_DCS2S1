// FeedbackScreen.kt
package com.example.taiwanesehouse.user_profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.taiwanesehouse.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.example.taiwanesehouse.viewmodel.FeedbackViewModel
import com.example.taiwanesehouse.viewmodel.ConnectionState
import com.example.taiwanesehouse.database.entities.FeedbackEntity
import com.example.taiwanesehouse.repository.FeedbackRepository
import com.example.taiwanesehouse.viewmodel.EnhancedFeedbackHistoryCard
import com.example.taiwanesehouse.viewmodel.FeedbackViewModelFactory
import com.example.taiwanesehouse.viewmodel.FormScreen
import com.google.firebase.firestore.FirebaseFirestore

// Data classes
data class FeedbackItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val feedbackType: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "pending",
    val isSynced: Boolean = true
)

fun FeedbackEntity.toFeedbackItem(): FeedbackItem {
    return FeedbackItem(
        id = this.id,
        userId = this.userId,
        userName = this.userName,
        rating = this.rating,
        feedbackType = this.feedbackType,
        title = this.title,
        message = this.message,
        timestamp = this.timestamp,
        status = this.status,
        isSynced = this.isSynced
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Create repository dependencies
    val database = remember {
        AppDatabase.getDatabase(context) // Use your existing AppDatabase
    }
    val repository = remember {
        FeedbackRepository(database.feedbackDao(), FirebaseFirestore.getInstance())
    }
    val factory = remember { FeedbackViewModelFactory(repository) }
    val viewModel: FeedbackViewModel = viewModel(factory = factory)

    // Firebase
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Screen state
    var currentScreen by rememberSaveable { mutableStateOf("main") }

    // Collect states
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    // Observe feedback
    val userFeedbackFlow by viewModel.getUserFeedbackFlow(currentUser?.uid ?: "")
        .collectAsState(initial = emptyList())
    val userFeedbackList = userFeedbackFlow.map { it.toFeedbackItem() }

    val feedbackTypes = listOf(
        "general" to "General Feedback",
        "food" to "Food Quality",
        "service" to "Service Experience",
        "app" to "App Issues",
        "suggestion" to "Suggestions",
        "complaint" to "Complaint"
    )

    // Handle messages
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.errorMessage)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.successMessage)
            viewModel.clearSuccessMessage()
        }
    }

    // Initialize on startup
    LaunchedEffect(Unit) {
        viewModel.initializeConnection(context, currentUser?.uid)
    }

    // Load history when needed
    LaunchedEffect(currentScreen) {
        if (currentScreen == "history" && currentUser != null) {
            viewModel.loadFeedbackHistory(currentUser.uid)
        }
    }

    // Auto-load history once online so user sees existing Firebase records
    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState == ConnectionState.ONLINE && currentUser != null) {
            viewModel.loadFeedbackHistory(currentUser.uid)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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

                        Spacer(modifier = Modifier.width(8.dp))

                        // Connection status indicator
                        when (uiState.connectionState) {
                            ConnectionState.CHECKING -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF3C4)
                                    )
                                ) {
                                    Text(
                                        text = "CHECKING",
                                        fontSize = 10.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            ConnectionState.OFFLINE -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE3F2FD)
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "OFFLINE",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1976D2),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        if (uiState.pendingSyncCount > 0) {
                                            Text(
                                                text = " (${uiState.pendingSyncCount})",
                                                fontSize = 8.sp,
                                                color = Color(0xFF1976D2),
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            ConnectionState.ONLINE -> {
                                if (uiState.pendingSyncCount > 0) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8F5E8)
                                        )
                                    ) {
                                        Text(
                                            text = "SYNC (${uiState.pendingSyncCount})",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentScreen) {
                                "form", "history" -> {
                                    currentScreen = "main"
                                }
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
            "main" -> MainScreen(
                innerPadding = innerPadding,
                uiState = uiState,
                currentUser = currentUser,
                onNavigateToForm = { currentScreen = "form" },
                onNavigateToHistory = { currentScreen = "history" },
                onSyncPending = {
                    currentUser?.uid?.let { viewModel.syncPendingFeedback(context, it) }
                }
            )

            "history" -> HistoryScreen(
                innerPadding = innerPadding,
                uiState = uiState,
                feedbackList = userFeedbackList,
                onRefresh = {
                    currentUser?.uid?.let { viewModel.loadFeedbackHistory(it) }
                },
                onSyncPending = {
                    currentUser?.uid?.let { viewModel.syncPendingFeedback(context, it) }
                },
                onNavigateToForm = { currentScreen = "form" }
            )

            "form" -> FormScreen(
                innerPadding = innerPadding,
                uiState = uiState,
                formState = formState,
                feedbackTypes = feedbackTypes,
                currentUser = currentUser,
                onUpdateRating = viewModel::updateRating,
                onUpdateFeedbackType = viewModel::updateFeedbackType,
                onUpdateTitle = viewModel::updateTitle,
                onUpdateMessage = viewModel::updateMessage,
                onUpdateContactEmail = viewModel::updateContactEmail,
                onSubmitFeedback = {
                    currentUser?.let { user ->
                        viewModel.submitFeedback(
                            context = context,
                            userId = user.uid,
                            userEmail = user.email,
                            userName = user.displayName
                        )
                        currentScreen = "history"
                    }
                },
                onCancel = { currentScreen = "main" }
            )
        }
    }
}

@Composable
fun MainScreen(
    innerPadding: PaddingValues,
    uiState: com.example.taiwanesehouse.viewmodel.FeedbackUIState,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onNavigateToForm: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSyncPending: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Feedback Center",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (uiState.connectionState) {
                        ConnectionState.CHECKING -> "Initializing connection..."
                        ConnectionState.ONLINE -> "Share your thoughts and help us improve"
                        ConnectionState.OFFLINE -> "Offline mode • Your feedback will be saved locally"
                    },
                    fontSize = 14.sp,
                    color = when (uiState.connectionState) {
                        ConnectionState.CHECKING -> Color(0xFFE65100)
                        ConnectionState.ONLINE -> Color.Gray
                        ConnectionState.OFFLINE -> Color(0xFF1976D2)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (uiState.connectionState) {
                    ConnectionState.CHECKING -> Color(0xFFFFF8E1)
                    ConnectionState.ONLINE -> Color(0xFFF1F8E9)
                    ConnectionState.OFFLINE -> Color(0xFFE3F2FD)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (uiState.connectionState) {
                        ConnectionState.CHECKING -> "⚡ Initializing System"
                        ConnectionState.ONLINE -> "✅ Online & Ready"
                        ConnectionState.OFFLINE -> "💾 Offline Mode Active"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (uiState.connectionState) {
                        ConnectionState.CHECKING -> Color(0xFFE65100)
                        ConnectionState.ONLINE -> Color(0xFF2E7D32)
                        ConnectionState.OFFLINE -> Color(0xFF1976D2)
                    },
                    textAlign = TextAlign.Center
                )

                if (currentUser != null && uiState.connectionState != ConnectionState.CHECKING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (uiState.connectionState) {
                            ConnectionState.ONLINE -> {
                                "You can submit ${uiState.remainingSubmissions} more feedback(s) this week"
                            }
                            ConnectionState.OFFLINE -> {
                                if (uiState.pendingSyncCount > 0) {
                                    "You have ${uiState.pendingSyncCount} feedback(s) waiting to sync"
                                } else {
                                    "You can still submit feedback offline"
                                }
                            }
                            else -> ""
                        },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    disabledContainerColor = Color.Gray
                ),
                enabled = currentUser != null &&
                        uiState.connectionState != ConnectionState.CHECKING &&
                        (uiState.connectionState == ConnectionState.OFFLINE || uiState.canSubmitFeedback)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Submit New Feedback",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    when {
                        uiState.connectionState == ConnectionState.CHECKING -> {
                            Text("Please wait...", fontSize = 12.sp, color = Color.Gray)
                        }
                        !uiState.canSubmitFeedback && uiState.connectionState == ConnectionState.ONLINE -> {
                            Text("Weekly limit reached", fontSize = 12.sp, color = Color.Gray)
                        }
                        uiState.connectionState == ConnectionState.OFFLINE -> {
                            Text("Will save locally", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            if (currentUser != null) {
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = uiState.connectionState != ConnectionState.CHECKING
                ) {
                    Text(
                        text = "View My Feedback History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (uiState.pendingSyncCount > 0) {
                    Button(
                        onClick = onSyncPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            text = "Sync ${uiState.pendingSyncCount} Pending Feedback",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    innerPadding: PaddingValues,
    uiState: com.example.taiwanesehouse.viewmodel.FeedbackUIState,
    feedbackList: List<FeedbackItem>,
    onRefresh: () -> Unit,
    onSyncPending: () -> Unit,
    onNavigateToForm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Status indicator
        item {
            if (uiState.connectionState == ConnectionState.OFFLINE || uiState.pendingSyncCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.pendingSyncCount > 0) Color(0xFFE3F2FD) else Color(0xFFF0F8FF)
                    )
                ) {
                    Text(
                        text = when {
                            uiState.pendingSyncCount > 0 -> "💾 ${uiState.pendingSyncCount} feedback(s) pending sync"
                            uiState.connectionState == ConnectionState.OFFLINE -> "💾 Offline mode. Showing cached data."
                            else -> "📱 Showing local cache"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFC107),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Loading feedback history...",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (feedbackList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No Feedback History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Your submitted feedback will appear here once you submit your first feedback",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onNavigateToForm,
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Submit Your First Feedback",
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            items(feedbackList) { feedback ->
                EnhancedFeedbackHistoryCard(feedback = feedback)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh History")
                    }

                    if (uiState.pendingSyncCount > 0) {
                        Button(
                            onClick = onSyncPending,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text(
                                "Sync ${uiState.pendingSyncCount} Pending Items",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}