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

    // Dialog states
    var confirmationDialogState by remember {
        mutableStateOf(
            ConfirmationDialogState(
                type = ConfirmationType.UPDATE_USERNAME,
                title = "Update Username",
                message = "Are you sure you want to update?",
                isVisible = false
            )
        )
    }
    var successDialogVisible by remember { mutableStateOf(false) }

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
                    currentName = userData?.get("fullName")?.toString() ?: currentUser.displayName ?: ""
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

                if (trimmedName.isBlank()) {
                    errorMessage = "Name cannot be empty"
                    return@launch
                }

                if (trimmedName.length < 2) {
                    errorMessage = "Name must be at least 2 characters"
                    return@launch
                }

                if (trimmedName.length > 50) {
                    errorMessage = "Name must be less than 50 characters"
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
                    .update("fullName", trimmedName)
                    .await()

                // Update local state
                currentName = trimmedName

                // Show success dialog instead of snackbar
                successDialogVisible = true

            } catch (e: Exception) {
                errorMessage = "Failed to update name: ${e.localizedMessage ?: "Unknown error"}"
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
                        "Edit Name",
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
            message = "Update Profile Successful",
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
            // Main content - Using LazyColumn for scrollable content
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
                                text = "Update Your Name",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Enter your new display name below",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Current name display
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
                                            text = "Current Name",
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

                            // New name input
                            OutlinedTextField(
                                value = newName,
                                onValueChange = {
                                    newName = it
                                    // Clear error when user starts typing
                                    if (errorMessage.isNotEmpty()) {
                                        errorMessage = ""
                                    }
                                },
                                label = { Text("New Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                isError = errorMessage.isNotEmpty(),
                                placeholder = { Text("Enter your new name") }
                            )

                            // Character count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${newName.length}/50",
                                    fontSize = 12.sp,
                                    color = if (newName.length > 50) Color.Red else Color.Gray
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
                                        .padding(top = 8.dp),
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Update button - now shows confirmation dialog instead of directly updating
                            Button(
                                onClick = {
                                    // Show confirmation dialog instead of directly updating
                                    confirmationDialogState = confirmationDialogState.copy(
                                        isVisible = true,
                                        onConfirm = { updateName() }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading && newName.trim() != currentName && newName.trim().isNotEmpty()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Update Name",
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

                    // Help text
                    Text(
                        text = "Your name will be updated across all parts of the app",
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