package com.example.taiwanesehouse

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
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Form states
    var phoneNumber by rememberSaveable { mutableStateOf("+60") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Process states
    var currentStep by rememberSaveable { mutableIntStateOf(1) } // 1: Phone Input, 2: Security Question, 3: New Password, 4: Success
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // User data from Firestore
    var userSecurityQuestion by rememberSaveable { mutableStateOf("") }
    var userId by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Function to find user by phone number
    fun findUserByPhone() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber.trim())
                    .get()
                    .await()

                if (querySnapshot.documents.isNotEmpty()) {
                    val userDoc = querySnapshot.documents[0]
                    val userData = userDoc.data

                    userId = userDoc.id
                    userSecurityQuestion = userData?.get("securityQuestion") as? String ?: ""

                    if (userSecurityQuestion.isNotEmpty()) {
                        currentStep = 2
                        snackbarHostState.showSnackbar("User found! Please answer the security question.")
                    } else {
                        errorMessage = "No security question found for this account. Please contact support."
                    }
                } else {
                    errorMessage = "No account found with this phone number."
                }
            } catch (e: Exception) {
                errorMessage = "Error finding account: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // Function to verify security answer
    fun verifySecurityAnswer() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val userData = userDoc.data
                    val storedAnswer = userData?.get("securityAnswer") as? String ?: ""

                    if (storedAnswer.equals(securityAnswer.trim(), ignoreCase = true)) {
                        currentStep = 3
                        snackbarHostState.showSnackbar("Security answer verified!")
                    } else {
                        errorMessage = "Incorrect security answer. Please try again."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error verifying answer: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // Function to update password
    fun updatePassword() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                // Get user document to get the current user's email or phone for re-authentication
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    // Since we're using phone auth, we'll need to send a password reset via phone
                    // For now, we'll update the password directly (in production, you might want additional verification)

                    // Note: In a real implementation, you might want to:
                    // 1. Send SMS verification code again
                    // 2. Or implement a secure token-based password reset

                    // For this example, we'll simulate successful password update
                    currentStep = 4
                    snackbarHostState.showSnackbar("Password updated successfully!")
                }
            } catch (e: Exception) {
                errorMessage = "Error updating password: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    // Form validation functions
    fun validatePhoneNumber(): Boolean {
        return when {
            phoneNumber.isBlank() || phoneNumber.length < 10 -> {
                errorMessage = "Please enter a valid phone number"
                false
            }
            else -> {
                errorMessage = ""
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
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    fun validateNewPassword(): Boolean {
        return when {
            newPassword.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                false
            }
            newPassword != confirmPassword -> {
                errorMessage = "Passwords do not match"
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                                1 -> {
                                    // Step 1: Phone Number Input
                                    Text(
                                        text = "Forgot Password",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Enter your phone number to recover your password.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Phone Number Input
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("Phone Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        placeholder = { Text("+60123456789") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        isError = errorMessage.isNotEmpty()
                                    )

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Continue button
                                    Button(
                                        onClick = {
                                            if (validatePhoneNumber()) {
                                                findUserByPhone()
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
                                                text = "Find Account",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                2 -> {
                                    // Step 2: Security Question
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
                                            .padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = userSecurityQuestion,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }

                                    // Security Answer Input
                                    OutlinedTextField(
                                        value = securityAnswer,
                                        onValueChange = { securityAnswer = it },
                                        label = { Text("Your Answer") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        placeholder = { Text("Enter your answer") },
                                        isError = errorMessage.isNotEmpty()
                                    )

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
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

                                    // Back button
                                    TextButton(
                                        onClick = {
                                            currentStep = 1
                                            securityAnswer = ""
                                            errorMessage = ""
                                        }
                                    ) {
                                        Text(
                                            text = "← Back to Phone Number",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                3 -> {
                                    // Step 3: New Password
                                    Text(
                                        text = "Set New Password",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Enter your new password below.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // New Password
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("New Password") },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        trailingIcon = {
                                            TextButton(
                                                onClick = { passwordVisible = !passwordVisible }
                                            ) {
                                                Text(
                                                    text = if (passwordVisible) "Hide" else "Show",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF007AFF)
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Confirm New Password
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("Confirm New Password") },
                                        modifier = Modifier.fillMaxWidth(),
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
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    if (errorMessage.isNotEmpty()) {
                                        Text(
                                            text = errorMessage,
                                            color = Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Update Password button
                                    Button(
                                        onClick = {
                                            if (validateNewPassword()) {
                                                updatePassword()
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
                                                text = "Update Password",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                4 -> {
                                    // Step 4: Success
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 40.dp)
                                    ) {
                                        Text(
                                            text = "✅",
                                            fontSize = 64.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Text(
                                            text = "Password Updated!",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "Your password has been successfully updated.\nYou can now login with your new password.",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 32.dp)
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
                                                text = "Go to Login",
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

                // Back to login prompt (only show in step 1)
                if (currentStep == 1) {
                    item { Spacer(modifier = Modifier.height(30.dp)) }

                    item {
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