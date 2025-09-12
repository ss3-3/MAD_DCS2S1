package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

sealed class ForgotPasswordStep {
    object InputCredentials : ForgotPasswordStep()
    object SelectRecoveryMethod : ForgotPasswordStep()
    object SecurityQuestion : ForgotPasswordStep()
    object NewPassword : ForgotPasswordStep()
    object EmailSent : ForgotPasswordStep()
    object Success : ForgotPasswordStep()
}

data class UserAccount(
    val uid: String,
    val email: String,
    val securityQuestion: String = "",
    val hasSecurityQuestion: Boolean = false
)

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Form states
    var emailOrPhone by rememberSaveable { mutableStateOf("") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Process states
    var currentStep by remember { mutableStateOf<ForgotPasswordStep>(ForgotPasswordStep.InputCredentials) }
    var selectedRecoveryMethod by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var successMessage by rememberSaveable { mutableStateOf("") }

    // User data
    var userAccount by remember { mutableStateOf<UserAccount?>(null) }

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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Helper functions
    fun validateEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return emailPattern.matcher(email).matches()
    }

    fun validatePhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(" ", "").replace("-", "").replace("+", "").trim()
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() }
    }

    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").trim()
    }

    fun clearMessages() {
        errorMessage = ""
        successMessage = ""
    }

    // Find user account function
    fun findUserAccount() {
        scope.launch {
            try {
                isLoading = true
                clearMessages()

                val input = emailOrPhone.trim()
                val isEmail = validateEmail(input)
                val isPhone = validatePhoneNumber(input)

                if (!isEmail && !isPhone) {
                    errorMessage = "Please enter a valid email address or phone number"
                    return@launch
                }

                val querySnapshot = if (isEmail) {
                    firestore.collection("users")
                        .whereEqualTo("email", input.lowercase())
                        .get()
                        .await()
                } else {
                    val cleanPhone = cleanPhoneNumber(input)
                    firestore.collection("users")
                        .whereEqualTo("phoneNumber", cleanPhone)
                        .get()
                        .await()
                }

                if (querySnapshot.documents.isNotEmpty()) {
                    val userDoc = querySnapshot.documents[0]
                    val userData = userDoc.data

                    val securityQuestion = userData?.get("securityQuestion") as? String ?: ""
                    val hasSecurityQuestion = securityQuestion.isNotEmpty()

                    userAccount = UserAccount(
                        uid = userDoc.id,
                        email = userData?.get("email") as? String ?: "",
                        securityQuestion = securityQuestion,
                        hasSecurityQuestion = hasSecurityQuestion
                    )

                    currentStep = ForgotPasswordStep.SelectRecoveryMethod
                    successMessage = "Account found! Choose your preferred recovery method."
                } else {
                    errorMessage = "No account found with this ${if (isEmail) "email address" else "phone number"}. Please check and try again."
                }
            } catch (e: Exception) {
                errorMessage = "Error finding account: ${e.localizedMessage ?: "Please try again"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Send password reset email with confirmation
    fun sendPasswordResetEmail() {
        confirmationDialogState = ConfirmationDialogState(
            type = ConfirmationType.UPDATE_USERNAME,
            title = "Send Password Reset Email",
            message = "We will send a secure password reset link to your email address. Continue?",
            isVisible = true,
            onConfirm = {
                scope.launch {
                    try {
                        isLoading = true
                        clearMessages()

                        userAccount?.email?.let { email ->
                            auth.sendPasswordResetEmail(email).await()
                            currentStep = ForgotPasswordStep.EmailSent
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
            },
            onDismiss = { /* User cancelled */ }
        )
    }

    // Verify security answer
    fun verifySecurityAnswer() {
        scope.launch {
            try {
                isLoading = true
                clearMessages()

                userAccount?.uid?.let { uid ->
                    val userDoc = firestore.collection("users")
                        .document(uid)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        val userData = userDoc.data
                        val storedAnswer = userData?.get("securityAnswer") as? String ?: ""

                        if (storedAnswer.equals(securityAnswer.trim(), ignoreCase = true)) {
                            currentStep = ForgotPasswordStep.NewPassword
                            successMessage = "Security answer verified successfully!"
                        } else {
                            errorMessage = "Incorrect security answer. Please try again or use email recovery."
                        }
                    } else {
                        errorMessage = "Account not found. Please try again."
                    }
                } ?: run {
                    errorMessage = "Session expired. Please start over."
                }
            } catch (e: Exception) {
                errorMessage = "Error verifying answer: ${e.localizedMessage ?: "Please try again"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Update password through security question recovery with confirmation
    fun updatePasswordViaSecurityQuestion() {
        confirmationDialogState = ConfirmationDialogState(
            type = ConfirmationType.UPDATE_USERNAME,
            title = "Complete Password Reset",
            message = "We will send a secure password reset link to your email to complete the process. Continue?",
            isVisible = true,
            onConfirm = {
                scope.launch {
                    try {
                        isLoading = true
                        clearMessages()

                        userAccount?.email?.let { email ->
                            // Send password reset email for secure password change
                            auth.sendPasswordResetEmail(email).await()

                            currentStep = ForgotPasswordStep.Success
                            successDialogMessage = "Password reset email sent! Please check your email to complete the password change."
                            successDialogVisible = true
                        } ?: run {
                            errorMessage = "Session expired. Please start over."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error updating password: ${e.localizedMessage ?: "Please try again"}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }

    // Validation functions
    fun validateInput(): Boolean {
        val input = emailOrPhone.trim()
        return when {
            input.isBlank() -> {
                errorMessage = "Please enter your email address or phone number"
                false
            }
            !validateEmail(input) && !validatePhoneNumber(input) -> {
                errorMessage = "Please enter a valid email address or phone number"
                false
            }
            else -> {
                clearMessages()
                true
            }
        }
    }

    fun validateSecurityAnswer(): Boolean {
        return when {
            securityAnswer.isBlank() -> {
                errorMessage = "Please provide an answer to the security question"
                false
            }
            securityAnswer.trim().length < 2 -> {
                errorMessage = "Answer seems too short. Please provide a complete answer"
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

    // Success Dialog
    SuccessDialog(
        isVisible = successDialogVisible,
        message = successDialogMessage,
        onDismissRequest = {
            successDialogVisible = false
            successDialogMessage = ""
        }
    )

    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background image
            Image(
                painter = coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.3f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    // Logo and title
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = logoImage,
                            contentDescription = "Taiwanese House Logo",
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Taiwanese House",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Password Recovery",
                                fontSize = 10.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }

                item {
                    // Main card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (currentStep) {
                                ForgotPasswordStep.InputCredentials -> {
                                    // Step 1: Input Credentials
                                    Text(
                                        text = "Forgot Password",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Enter your email address or phone number to recover your password.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    OutlinedTextField(
                                        value = emailOrPhone,
                                        onValueChange = {
                                            emailOrPhone = it
                                            clearMessages()
                                        },
                                        label = { Text("Email or Phone Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("example@email.com or +60123456789") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        isError = errorMessage.isNotEmpty(),
                                        trailingIcon = {
                                            if (emailOrPhone.isNotEmpty()) {
                                                val isValid = validateEmail(emailOrPhone.trim()) || validatePhoneNumber(emailOrPhone.trim())
                                                Text(
                                                    text = if (isValid) "✅" else "❌",
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    )

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

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            if (validateInput()) {
                                                findUserAccount()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFD700),
                                            contentColor = Color.Black
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.Black,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Find My Account",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                ForgotPasswordStep.SelectRecoveryMethod -> {
                                    // Step 2: Select Recovery Method
                                    Text(
                                        text = "Choose Recovery Method",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "We found your account! How would you like to recover your password?",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Important message card
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF0F8FF)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "💡",
                                                fontSize = 20.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = "Choose the method that works best for you. Both options are secure and reliable.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF1976D2),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    // Email Recovery Option
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedRecoveryMethod = "email"
                                                sendPasswordResetEmail()
                                            }
                                            .padding(bottom = 12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF8F9FA)
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
                                                text = "📧",
                                                fontSize = 28.sp,
                                                modifier = Modifier.padding(end = 16.dp)
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Email Recovery",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Black
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
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Security Question Option (if available)
                                    if (userAccount?.hasSecurityQuestion == true) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedRecoveryMethod = "security"
                                                    currentStep = ForgotPasswordStep.SecurityQuestion
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF8F9FA)
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
                                                        text = "Security Question",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        text = "Answer your security question to reset password",
                                                        fontSize = 13.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                    Text(
                                                        text = "Alternative method",
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
                                    } else {
                                        // Show message if no security question is set
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
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
                                                    text = "ℹ️",
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    text = "Security question not available for this account. Please use email recovery.",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFE65100),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Back button
                                    TextButton(
                                        onClick = {
                                            currentStep = ForgotPasswordStep.InputCredentials
                                            selectedRecoveryMethod = ""
                                            userAccount = null
                                            clearMessages()
                                        }
                                    ) {
                                        Text(
                                            text = "← Use Different Account",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                ForgotPasswordStep.SecurityQuestion -> {
                                    // Step 3: Security Question
                                    Text(
                                        text = "Security Question",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Please answer your security question to verify your identity.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Display Security Question
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF0F7FF)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Security Question:",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = userAccount?.securityQuestion ?: "",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    // Important note
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFF8E1)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚠️",
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = "Answer must match exactly as you set it during registration.",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    // Security Answer Input
                                    OutlinedTextField(
                                        value = securityAnswer,
                                        onValueChange = {
                                            securityAnswer = it
                                            clearMessages()
                                        },
                                        label = { Text("Your Answer") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        placeholder = { Text("Enter your answer") },
                                        isError = errorMessage.isNotEmpty()
                                    )

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

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Verify button
                                    Button(
                                        onClick = {
                                            if (validateSecurityAnswer()) {
                                                verifySecurityAnswer()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFD700),
                                            contentColor = Color.Black
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.Black,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Verify Answer",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Alternative options
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                selectedRecoveryMethod = "email"
                                                sendPasswordResetEmail()
                                            },
                                            enabled = !isLoading
                                        ) {
                                            Text(
                                                text = "Use Email Recovery Instead",
                                                color = Color(0xFF007AFF),
                                                fontSize = 14.sp
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                currentStep = ForgotPasswordStep.SelectRecoveryMethod
                                                securityAnswer = ""
                                                clearMessages()
                                            }
                                        ) {
                                            Text(
                                                text = "← Back to Recovery Options",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                ForgotPasswordStep.NewPassword -> {
                                    // Step 4: Password Reset Success via Security Question
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 20.dp)
                                    ) {
                                        Text(
                                            text = "✅",
                                            fontSize = 64.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Text(
                                            text = "Identity Verified",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "Your identity has been verified successfully. We'll send a secure password reset link to your email.",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )

                                        // Important security notice
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF0F8FF)
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
                                                    text = "For security reasons, we still require email verification to complete your password reset.",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1976D2),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                updatePasswordViaSecurityQuestion()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFD700),
                                                contentColor = Color.Black
                                            ),
                                            enabled = !isLoading
                                        ) {
                                            if (isLoading) {
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
                                    }
                                }

                                ForgotPasswordStep.EmailSent -> {
                                    // Step 5: Email Sent Confirmation
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 40.dp)
                                    ) {
                                        Text(
                                            text = "📧",
                                            fontSize = 64.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Text(
                                            text = "Check Your Email",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "We've sent a password reset link to your email address. Click the link in the email to create a new password.",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )

                                        // Email instructions
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF8F9FA)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "What's Next:",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                Text(
                                                    text = "1. Check your email inbox\n2. Look for an email from Taiwanese House\n3. Click the reset link in the email\n4. Create your new password",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF666666),
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Didn't receive the email? Check your spam folder or wait a few minutes before trying again.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(bottom = 20.dp)
                                        )

                                        // Action buttons
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Button(
                                                onClick = {
                                                    navController.navigate(Screen.Login.name) {
                                                        popUpTo(Screen.ForgotPassword.name) { inclusive = true }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFFD700),
                                                    contentColor = Color.Black
                                                )
                                            ) {
                                                Text(
                                                    text = "Back to Login",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            TextButton(
                                                onClick = {
                                                    // Resend email
                                                    sendPasswordResetEmail()
                                                }
                                            ) {
                                                Text(
                                                    text = "Resend Email",
                                                    color = Color(0xFF007AFF),
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                ForgotPasswordStep.Success -> {
                                    // Step 6: Final Success
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 40.dp)
                                    ) {
                                        Text(
                                            text = "🎉",
                                            fontSize = 64.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Text(
                                            text = "Password Reset Complete",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "Your identity has been verified and a password reset email has been sent. Please check your email to complete the process.",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )

                                        Button(
                                            onClick = {
                                                navController.navigate(Screen.Login.name) {
                                                    popUpTo(Screen.ForgotPassword.name) { inclusive = true }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFD700),
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Text(
                                                text = "Back to Login",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Back to login prompt (always visible at bottom)
                item { Spacer(modifier = Modifier.height(30.dp)) }

                item {
                    if (currentStep == ForgotPasswordStep.InputCredentials) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Remember your password? ",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Login",
                                fontSize = 14.sp,
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.Login.name)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}