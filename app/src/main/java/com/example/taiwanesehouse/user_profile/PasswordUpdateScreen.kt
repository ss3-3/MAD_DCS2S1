package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.utils.EmailVerificationRequiredDialog
import com.example.taiwanesehouse.utils.EmailVerificationUtils
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class PasswordUpdateMethod {
    object Selection : PasswordUpdateMethod()
    object EmailMethod : PasswordUpdateMethod()
    object CurrentPasswordMethod : PasswordUpdateMethod()
    object EmailSent : PasswordUpdateMethod()
    object Success : PasswordUpdateMethod()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordUpdateScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Form states
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmNewPassword by rememberSaveable { mutableStateOf("") }
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Process states
    var currentMethod by remember { mutableStateOf<PasswordUpdateMethod>(PasswordUpdateMethod.Selection) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf("") }

    // Email verification states
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isCheckingVerification by remember { mutableStateOf(false) }

    // Dialog states
    var confirmationDialogState by remember {
        mutableStateOf(
            ConfirmationDialogState(
                type = ConfirmationType.UPDATE_USERNAME,
                title = "",
                message = "",
                isVisible = false
            )
        )
    }
    var successDialogVisible by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }

    // Check if user is authenticated
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screen.Login.name) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Helper functions
    fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    fun clearMessages() {
        errorMessage = ""
        successMessage = ""
    }

    // Check email verification before allowing password changes
    fun checkEmailVerificationAndProceed(onProceed: () -> Unit) {
        scope.launch {
            try {
                isCheckingVerification = true

                // Reload user to get the latest verification status
                currentUser?.reload()?.await()

                val result = EmailVerificationUtils.checkEmailVerificationStatus()
                if (result.isVerified) {
                    onProceed()
                } else {
                    showVerificationDialog = true
                }
            } catch (e: Exception) {
                errorMessage = "Failed to check verification status: ${e.localizedMessage}"
            } finally {
                isCheckingVerification = false
            }
        }
    }

    // Send password reset email
    fun sendPasswordResetEmail() {
        confirmationDialogState = ConfirmationDialogState(
            type = ConfirmationType.UPDATE_USERNAME,
            title = "Send Password Reset Email",
            message = "We will send a secure password reset link to your email address. You will be logged out after sending the email. Continue?",
            isVisible = true,
            onConfirm = {
                checkEmailVerificationAndProceed {
                    scope.launch {
                        try {
                            isLoading = true
                            clearMessages()

                            currentUser?.email?.let { email ->
                                auth.sendPasswordResetEmail(email).await()
                                currentMethod = PasswordUpdateMethod.EmailSent
                                successDialogMessage = "Password reset email sent successfully! Please check your inbox."
                                successDialogVisible = true
                            } ?: run {
                                errorMessage = "No email address found for this account"
                            }
                        } catch (e: FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with this email address"
                        } catch (e: Exception) {
                            errorMessage = "Failed to send reset email: ${e.localizedMessage ?: "Please try again"}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            onDismiss = { /* User cancelled */ }
        )
    }

    // Update password using current password
    fun updatePasswordWithCurrentPassword() {
        confirmationDialogState = ConfirmationDialogState(
            type = ConfirmationType.UPDATE_USERNAME,
            title = "Update Password",
            message = "Are you sure you want to update your password? You will need to use the new password for future logins.",
            isVisible = true,
            onConfirm = {
                checkEmailVerificationAndProceed {
                    scope.launch {
                        try {
                            isLoading = true
                            clearMessages()

                            currentUser?.let { user ->
                                // Re-authenticate user with current password
                                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                                user.reauthenticate(credential).await()

                                // Update password
                                user.updatePassword(newPassword).await()

                                currentMethod = PasswordUpdateMethod.Success
                                successDialogMessage = "Password updated successfully!"
                                successDialogVisible = true
                            } ?: run {
                                errorMessage = "User not authenticated"
                            }
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Current password is incorrect"
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            errorMessage = "New password is too weak. Please choose a stronger password."
                        } catch (e: Exception) {
                            errorMessage = "Failed to update password: ${e.localizedMessage ?: "Please try again"}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        )
    }

    // Validation functions
    fun validateCurrentPasswordForm(): Boolean {
        return when {
            currentPassword.isBlank() -> {
                errorMessage = "Please enter your current password"
                false
            }
            newPassword.isBlank() -> {
                errorMessage = "Please enter a new password"
                false
            }
            !validatePassword(newPassword) -> {
                errorMessage = "New password must be at least 6 characters long"
                false
            }
            confirmNewPassword.isBlank() -> {
                errorMessage = "Please confirm your new password"
                false
            }
            newPassword != confirmNewPassword -> {
                errorMessage = "New passwords do not match"
                false
            }
            currentPassword == newPassword -> {
                errorMessage = "New password must be different from current password"
                false
            }
            else -> {
                clearMessages()
                true
            }
        }
    }

    // Show success/error messages
    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = successMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Confirmation Dialog
    ConfirmationDialog(
        state = confirmationDialogState,
        onDismissRequest = {
            confirmationDialogState = confirmationDialogState.copy(isVisible = false)
        }
    )

    // Success Dialog - only used for simple confirmations now
    SuccessDialog(
        isVisible = successDialogVisible,
        message = successDialogMessage,
        onDismissRequest = {
            successDialogVisible = false
            successDialogMessage = ""
        }
    )

    // Email verification required dialog
    EmailVerificationRequiredDialog(
        showDialog = showVerificationDialog,
        onDismiss = { showVerificationDialog = false },
        onNavigateToVerification = {
            showVerificationDialog = false
            navController.navigate("verify_email")
        },
        title = "Verify Email to Change Password",
        message = "For security reasons, please verify your email before changing your password."
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Update Password",
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
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (successMessage.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

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
                    when (currentMethod) {
                        PasswordUpdateMethod.Selection -> {
                            // Method selection
                            Text(
                                text = "Choose Update Method",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "How would you like to update your password?",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Email verification warning card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F7FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔒",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Email verification is required for password changes for security purposes.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF1976D2),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Email method
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isCheckingVerification) {
                                        currentMethod = PasswordUpdateMethod.EmailMethod
                                    }
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCheckingVerification) Color(0xFFF5F5F5) else Color(0xFFF8F9FA)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCheckingVerification) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(end = 16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Gray
                                        )
                                    } else {
                                        Text(
                                            text = "📧",
                                            fontSize = 28.sp,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Email Reset",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCheckingVerification) Color.Gray else Color.Black
                                        )
                                        Text(
                                            text = "Get a secure password reset link via email",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Text(
                                            text = "Recommended • Most secure",
                                            fontSize = 11.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "→",
                                        fontSize = 18.sp,
                                        color = if (isCheckingVerification) Color.Gray else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Current password method
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isCheckingVerification) {
                                        currentMethod = PasswordUpdateMethod.CurrentPasswordMethod
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCheckingVerification) Color(0xFFF5F5F5) else Color(0xFFF8F9FA)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔐",
                                        fontSize = 28.sp,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Current Password",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCheckingVerification) Color.Gray else Color.Black
                                        )
                                        Text(
                                            text = "Enter current password and set new one",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Text(
                                            text = "Quick update",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2196F3),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "→",
                                        fontSize = 18.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        PasswordUpdateMethod.EmailMethod -> {
                            // Email reset confirmation
                            Text(
                                text = "Email Password Reset",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "We'll send a secure password reset link to your registered email address.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Email display
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F7FF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Reset link will be sent to:",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = currentUser?.email ?: "Not available",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Warning
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "You will be logged out after sending the reset email for security reasons.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE65100),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Send email button
                            Button(
                                onClick = { sendPasswordResetEmail() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading && !isCheckingVerification
                            ) {
                                if (isLoading || isCheckingVerification) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Send Reset Email",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(
                                onClick = {
                                    currentMethod = PasswordUpdateMethod.Selection
                                    clearMessages()
                                }
                            ) {
                                Text(
                                    text = "← Back to Options",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        PasswordUpdateMethod.CurrentPasswordMethod -> {
                            // Current password form
                            Text(
                                text = "Update Password",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Enter your current password and choose a new one",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Current password field
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    clearMessages()
                                },
                                label = { Text("Current Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    TextButton(
                                        onClick = { currentPasswordVisible = !currentPasswordVisible }
                                    ) {
                                        Text(
                                            text = if (currentPasswordVisible) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                },
                                isError = errorMessage.isNotEmpty() && errorMessage.contains("current password", ignoreCase = true)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // New password field
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    clearMessages()
                                },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    TextButton(
                                        onClick = { newPasswordVisible = !newPasswordVisible }
                                    ) {
                                        Text(
                                            text = if (newPasswordVisible) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                },
                                supportingText = { Text("At least 6 characters", fontSize = 12.sp) },
                                isError = errorMessage.isNotEmpty() && (errorMessage.contains("new password", ignoreCase = true) || errorMessage.contains("weak", ignoreCase = true))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm new password field
                            OutlinedTextField(
                                value = confirmNewPassword,
                                onValueChange = {
                                    confirmNewPassword = it
                                    clearMessages()
                                },
                                label = { Text("Confirm New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    TextButton(
                                        onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                                    ) {
                                        Text(
                                            text = if (confirmPasswordVisible) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFF007AFF)
                                        )
                                    }
                                },
                                isError = errorMessage.isNotEmpty() && errorMessage.contains("match", ignoreCase = true)
                            )

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Red.copy(alpha = 0.1f)
                                    ),
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

                            Spacer(modifier = Modifier.height(24.dp))

                            // Update button
                            Button(
                                onClick = {
                                    if (validateCurrentPasswordForm()) {
                                        updatePasswordWithCurrentPassword()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107),
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading && !isCheckingVerification
                            ) {
                                if (isLoading || isCheckingVerification) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Update Password",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(
                                onClick = {
                                    currentMethod = PasswordUpdateMethod.Selection
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                    clearMessages()
                                }
                            ) {
                                Text(
                                    text = "← Back to Options",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        PasswordUpdateMethod.EmailSent -> {
                            // Email sent confirmation - handled by success dialog
                            SuccessDialog(
                                isVisible = successDialogVisible,
                                message = "Email sent Successful",
                                onDismissRequest = {
                                    successDialogVisible = false
                                    navController.popBackStack()
                                }
                            )
                        }

                        PasswordUpdateMethod.Success -> {
                            // Success - handled by success dialog
                            SuccessDialog(
                                isVisible = successDialogVisible,
                                message = "Update Password Successful",
                                onDismissRequest = {
                                    successDialogVisible = false
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }

            if (currentMethod == PasswordUpdateMethod.Selection) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Choose the method that works best for you. Both options are secure and reliable.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}