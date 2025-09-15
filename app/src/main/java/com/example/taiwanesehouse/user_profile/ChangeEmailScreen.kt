package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Form states
    var currentEmail by rememberSaveable { mutableStateOf("") }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var isLoadingUserData by rememberSaveable { mutableStateOf(true) }

    // Error states
    var emailError by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf("") }
    var generalError by rememberSaveable { mutableStateOf("") }

    // Dialog states
    var confirmationDialogVisible by remember { mutableStateOf(false) }
    var successDialogVisible by remember { mutableStateOf(false) }

    // Load current user data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentEmail = currentUser.email ?: ""
        }
        isLoadingUserData = false
    }

    // Email validation function
    fun isValidEmail(email: String): Boolean {
        return Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        ).matcher(email).matches()
    }

    // Check for email duplication
    suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email).await()
            methods.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            false // Assume email is available if check fails
        }
    }

    // Update email function
    fun updateEmail() {
        scope.launch {
            try {
                isProcessing = true
                emailError = ""
                passwordError = ""
                generalError = ""

                val user = auth.currentUser
                if (user == null) {
                    generalError = "User not authenticated"
                    return@launch
                }

                val trimmedEmail = newEmail.trim().lowercase()

                // Validation
                if (trimmedEmail.isBlank()) {
                    emailError = "Email cannot be empty"
                    return@launch
                }

                if (!isValidEmail(trimmedEmail)) {
                    emailError = "Please enter a valid email address"
                    return@launch
                }

                if (trimmedEmail == currentEmail.lowercase()) {
                    emailError = "New email must be different from current email"
                    return@launch
                }

                if (currentPassword.isBlank()) {
                    passwordError = "Current password is required"
                    return@launch
                }

                // Check if email already exists
                if (checkEmailExists(trimmedEmail)) {
                    emailError = "This email is already registered with another account"
                    return@launch
                }

                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                try {
                    user.reauthenticate(credential).await()
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    passwordError = "Current password is incorrect"
                    return@launch
                } catch (e: Exception) {
                    passwordError = "Failed to verify password: ${e.localizedMessage}"
                    return@launch
                }

                val oldEmail = currentEmail

                try {
                    // Update email in Firebase Auth
                    user.updateEmail(trimmedEmail).await()

                    // Send notification to old email (best effort)
                    try {
                        // Note: This sends a password reset email as notification
                        // In a real app, you'd want a custom email template
                        auth.sendPasswordResetEmail(oldEmail).await()
                    } catch (_: Exception) {
                        // Ignore if this fails
                    }

                    // Send verification to new email
                    try {
                        user.sendEmailVerification().await()
                    } catch (_: Exception) {
                        // Continue even if verification email fails
                    }

                    // Update Firestore document
                    firestore.collection("users").document(user.uid)
                        .update(
                            mapOf(
                                "email" to trimmedEmail,
                                "emailVerified" to false,
                                "lastEmailChange" to System.currentTimeMillis()
                            )
                        ).await()

                    // Update local state
                    currentEmail = trimmedEmail

                    // Show success dialog
                    successDialogVisible = true

                } catch (e: FirebaseAuthUserCollisionException) {
                    emailError = "This email is already registered with another account"
                } catch (e: Exception) {
                    generalError = "Failed to update email: ${e.localizedMessage ?: "Unknown error"}"
                }

            } catch (e: Exception) {
                generalError = "An unexpected error occurred: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Change Email",
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

        // Confirmation Dialog
        if (confirmationDialogVisible) {
            AlertDialog(
                onDismissRequest = { confirmationDialogVisible = false },
                title = {
                    Text(
                        text = "Confirm Email Change",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Are you sure you want to change your email address?",
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "From: $currentEmail",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "To: ${newEmail.trim()}",
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A verification email will be sent to your new address.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmationDialogVisible = false
                            updateEmail()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { confirmationDialogVisible = false }
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Success Dialog
        if (successDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    successDialogVisible = false
                    navController.popBackStack()
                },
                title = {
                    Text(
                        text = "Email Updated Successfully!",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Your email address has been updated successfully.",
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• A verification email has been sent to your new address",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "• A notification has been sent to your old address",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "• Please verify your new email address",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            successDialogVisible = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Continue")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

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
                            // Email icon
                            Card(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(40.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFC107).copy(alpha = 0.2f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Change Email Address",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Update your account's email address",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Current email display
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
                                        text = "Current Email",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = currentEmail,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // New email input
                            OutlinedTextField(
                                value = newEmail,
                                onValueChange = {
                                    newEmail = it
                                    emailError = ""
                                },
                                label = { Text("New Email Address") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isProcessing,
                                isError = emailError.isNotEmpty(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                placeholder = { Text("Enter new email address") }
                            )

                            if (emailError.isNotEmpty()) {
                                Text(
                                    text = emailError,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Current password input
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    passwordError = ""
                                },
                                label = { Text("Current Password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Password"
                                    )
                                },
                                trailingIcon = {
                                    TextButton(
                                        onClick = { passwordVisible = !passwordVisible }
                                    ) {
                                        Text(
                                            text = if (passwordVisible) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFFC107)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isProcessing,
                                isError = passwordError.isNotEmpty(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                placeholder = { Text("Enter current password") }
                            )

                            if (passwordError.isNotEmpty()) {
                                Text(
                                    text = passwordError,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    textAlign = TextAlign.Start
                                )
                            }

                            // General error message
                            if (generalError.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFDE8E8)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = generalError,
                                        color = Color(0xFFD32F2F),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Update button
                            Button(
                                onClick = {
                                    confirmationDialogVisible = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                enabled = !isProcessing &&
                                        newEmail.trim().isNotEmpty() &&
                                        currentPassword.isNotEmpty() &&
                                        newEmail.trim().lowercase() != currentEmail.lowercase()
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Update Email",
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
                                enabled = !isProcessing
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

                    // Security notice
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8F9FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Security Notice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "• You'll need to verify your new email address",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "• Your old email will receive a security notification",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "• Make sure you have access to your new email",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "• This action cannot be undone without verification",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}