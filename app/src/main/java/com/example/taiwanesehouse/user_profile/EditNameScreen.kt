package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.utils.UsernameValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Form states
    var currentName by rememberSaveable { mutableStateOf("") }
    var newName by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isLoadingUserData by rememberSaveable { mutableStateOf(true) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf("") }

    // Dialog states
    var confirmationDialogState by remember {
        mutableStateOf(
            ConfirmationDialogState(
                type = ConfirmationType.UPDATE_USERNAME,
                title = "Update Username",
                message = "Are you sure you want to update your username?",
                isVisible = false
            )
        )
    }
    var successDialogVisible by remember { mutableStateOf(false) }

    // Real-time validation
    LaunchedEffect(newName) {
        if (newName.trim().isNotEmpty() && newName.trim() != currentName) {
            val validation = UsernameValidator.validateUsername(newName)
            validationMessage = if (validation.isValid) {
                "Username is available"
            } else {
                validation.errorMessage
            }
        } else {
            validationMessage = ""
        }
    }

    // Load current user data
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                isLoadingUserData = true

                // Get user data from Firestore
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val userData = userDoc.data
                    currentName = userData?.get("username")?.toString() ?: currentUser.displayName ?: ""
                    newName = currentName
                } else {
                    // Fallback to Firebase Auth display name
                    currentName = currentUser.displayName ?: ""
                    newName = currentName
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load user data: ${e.localizedMessage}"
                currentName = currentUser.displayName ?: ""
                newName = currentName
            } finally {
                isLoadingUserData = false
            }
        } else {
            // User not logged in, redirect to login
            navController.navigate(Screen.Login.name) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Update name function
    fun updateName() {
        if (currentUser == null) {
            errorMessage = "User not authenticated"
            return
        }

        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                val trimmedName = newName.trim()

                // Final validation
                val validationResult = UsernameValidator.validateUsername(trimmedName)
                if (!validationResult.isValid) {
                    errorMessage = validationResult.errorMessage
                    return@launch
                }

                if (trimmedName == currentName) {
                    errorMessage = "New username must be different from current username"
                    return@launch
                }

                // Update Firebase Auth profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()

                currentUser.updateProfile(profileUpdates).await()

                // Update Firestore document
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("username", trimmedName)
                    .await()

                // Update local state
                currentName = trimmedName

                // Show success dialog
                successDialogVisible = true

            } catch (e: Exception) {
                errorMessage = "Failed to update username: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Edit Username",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // Confirmation dialog
        ConfirmationDialog(
            state = confirmationDialogState,
            onDismissRequest = {
                confirmationDialogState = confirmationDialogState.copy(isVisible = false)
            }
        )

        // Success dialog
        SuccessDialog(
            isVisible = successDialogVisible,
            message = "Username Updated Successfully",
            onDismissRequest = {
                successDialogVisible = false
                navController.popBackStack()
            }
        )

        if (isLoadingUserData) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFC107),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading user data...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Main content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    // Main card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Update Your Username",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Choose a unique username that represents you",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Current username display
                            if (currentName.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Current Username",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentName,
                                            fontSize = 16.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // New username input
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { input ->
                                    // Filter out excessive special characters while typing
                                    val filtered = input.take(30).filter { char ->
                                        char.isLetterOrDigit() || char.isWhitespace() ||
                                                char in ".-_"
                                    }
                                    newName = filtered

                                    // Clear previous errors when user types
                                    if (errorMessage.isNotEmpty()) {
                                        errorMessage = ""
                                    }
                                },
                                label = { Text("New Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                isError = errorMessage.isNotEmpty() ||
                                        (validationMessage.isNotEmpty() && !validationMessage.contains("available")),
                                placeholder = { Text("Enter your username") },
                                supportingText = {
                                    if (validationMessage.isNotEmpty()) {
                                        Text(
                                            text = validationMessage,
                                            color = if (validationMessage.contains("available"))
                                                Color(0xFF4CAF50) else Color.Red,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            )

                            // Character count and guidelines
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${newName.length}/30",
                                    fontSize = 12.sp,
                                    color = if (newName.length > 30) Color.Red else Color.Gray
                                )

                                // Validation status indicator
                                if (newName.trim().isNotEmpty() && newName.trim() != currentName) {
                                    val isValid = UsernameValidator.validateUsername(newName).isValid
                                    Text(
                                        text = if (isValid) "✓ Valid" else "✗ Invalid",
                                        fontSize = 12.sp,
                                        color = if (isValid) Color(0xFF4CAF50) else Color.Red,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Username guidelines
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F8FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Username Guidelines:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = "• 2-30 characters long\n• Letters, numbers, spaces, dots, dashes, underscores only\n• No inappropriate or offensive content\n• Cannot be a system reserved name",
                                        fontSize = 11.sp,
                                        color = Color(0xFF424242),
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF0F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = errorMessage,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // Update button
                            val canUpdate = !isLoading &&
                                    newName.trim() != currentName &&
                                    newName.trim().isNotEmpty() &&
                                    UsernameValidator.validateUsername(newName.trim()).isValid

                            Button(
                                onClick = {
                                    // Final validation before showing confirmation
                                    val validation = UsernameValidator.validateUsername(newName.trim())
                                    if (!validation.isValid) {
                                        errorMessage = validation.errorMessage
                                        return@Button
                                    }

                                    confirmationDialogState = confirmationDialogState.copy(
                                        isVisible = true,
                                        message = "Update username to \"${newName.trim()}\"?",
                                        onConfirm = { updateName() }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.Gray,
                                    disabledContentColor = Color.White
                                ),
                                enabled = !isLoading &&
                                        newName.trim() != currentName &&
                                        newName.trim().isNotEmpty() &&
                                        UsernameValidator.validateUsername(newName.trim()).isValid
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Update Username",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cancel button
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
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
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Privacy and policy note
                    Text(
                        text = "Your username will be visible to other users and will be updated across the app. Please choose responsibly and follow our community guidelines.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}