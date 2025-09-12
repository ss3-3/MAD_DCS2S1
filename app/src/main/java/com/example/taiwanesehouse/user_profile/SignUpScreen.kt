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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Security questions list
    val securityQuestions = listOf(
        "What is your mother's name?",
        "What is your favorite song?",
        "Where were you born?",
        "What is the name of your first pet?",
        "What is your favorite food?"
    )

    // Form states
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var agreeToTerms by rememberSaveable { mutableStateOf(false) }

    // Security question states
    var selectedQuestion by rememberSaveable { mutableStateOf("") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // Process states
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // Email validation state
    var isEmailValid by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Helper function to clean phone number
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").trim()
    }

    // Email validation function
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
        val isValid = emailPattern.matcher(email).matches()
        isEmailValid = isValid
        return isValid
    }

    // Phone validation function
    fun validatePhoneNumber(phone: String): Boolean {
        val cleanPhone = cleanPhoneNumber(phone).replace("+", "")
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() }
    }

    // Email + Password + Phone registration function (UNIFIED APPROACH)
    fun createAccount() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                // Validate phone number format
                if (phoneNumber.isNotEmpty() && !validatePhoneNumber(phoneNumber)) {
                    errorMessage = "Please enter a valid phone number"
                    return@launch
                }

                // Check if email already exists
                val existingEmailUsers = firestore.collection("users")
                    .whereEqualTo("email", email.trim().lowercase())
                    .get()
                    .await()

                if (!existingEmailUsers.isEmpty) {
                    errorMessage = "An account with this email already exists"
                    return@launch
                }

                // Check if phone number already exists (if provided)
                if (phoneNumber.isNotEmpty()) {
                    val cleanPhone = cleanPhoneNumber(phoneNumber)
                    val existingPhoneUsers = firestore.collection("users")
                        .whereEqualTo("phoneNumber", cleanPhone)
                        .get()
                        .await()

                    if (!existingPhoneUsers.isEmpty) {
                        errorMessage = "An account with this phone number already exists"
                        return@launch
                    }
                }

                // Create user with email/password authentication
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
                val user = authResult.user

                if (user != null) {
                    // Update user profile with display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()

                    // Prepare user data for Firestore
                    val userData = hashMapOf(
                        "fullName" to fullName.trim(),
                        "email" to email.trim().lowercase(),
                        "securityQuestion" to selectedQuestion,
                        "securityAnswer" to securityAnswer.trim().lowercase(),
                        "emailVerified" to false,
                        "authMethod" to "email_password",
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )

                    // Add phone number if provided
                    if (phoneNumber.isNotEmpty()) {
                        userData["phoneNumber"] = cleanPhoneNumber(phoneNumber)
                    }

                    // Save user data to Firestore using UID as document ID
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .await()

                    // Send email verification (optional)
                    try {
                        user.sendEmailVerification().await()
                        snackbarHostState.showSnackbar("Account created! Please check your email for verification.")
                    } catch (e: Exception) {
                        // Email verification failed but account is created
                        snackbarHostState.showSnackbar("Account created successfully!")
                    }

                    // Navigate to success or menu
                    navController.navigate(Screen.Menu.name) {
                        popUpTo(Screen.Signup.name) { inclusive = true }
                    }
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                errorMessage = "Password is too weak. Please choose a stronger password (at least 6 characters)."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "Invalid email format. Please check and try again."
            } catch (e: FirebaseAuthUserCollisionException) {
                errorMessage = "An account with this email already exists. Please use a different email."
            } catch (e: Exception) {
                errorMessage = "Registration failed: ${e.localizedMessage ?: "Unknown error occurred"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Form validation
    fun validateForm(): Boolean {
        return when {
            fullName.isBlank() -> {
                errorMessage = "Please enter your full name"
                false
            }
            email.isBlank() -> {
                errorMessage = "Please enter your email address"
                false
            }
            !validateEmail(email) -> {
                errorMessage = "Please enter a valid email address"
                false
            }
            phoneNumber.isNotEmpty() && !validatePhoneNumber(phoneNumber) -> {
                errorMessage = "Please enter a valid phone number or leave it empty"
                false
            }
            password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                false
            }
            password != confirmPassword -> {
                errorMessage = "Passwords do not match"
                false
            }
            selectedQuestion.isBlank() -> {
                errorMessage = "Please select a security question"
                false
            }
            securityAnswer.isBlank() -> {
                errorMessage = "Please provide an answer to the security question"
                false
            }
            !agreeToTerms -> {
                errorMessage = "Please agree to the terms and conditions"
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
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
                                text = "Join Us Today",
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
                            // Registration Form
                            Text(
                                text = "Create Account",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Please fill in your details to create an account.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            // Full Name
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Email field (Required)
                            Column {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { newValue ->
                                        email = newValue
                                        // Real-time validation
                                        if (newValue.isNotEmpty()) {
                                            validateEmail(newValue)
                                        }
                                    },
                                    label = { Text("Email Address *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    placeholder = { Text("example@email.com") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    isError = email.isNotEmpty() && !isEmailValid,
                                    trailingIcon = {
                                        if (email.isNotEmpty()) {
                                            Text(
                                                text = if (isEmailValid) "✅" else "❌",
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                )

                                // Show email validation message
                                if (email.isNotEmpty()) {
                                    Text(
                                        text = if (isEmailValid) "✓ Valid email address" else "Please enter a valid email address",
                                        color = if (isEmailValid) Color.Green else Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Phone Number (Optional)
                            Column {
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone Number (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    placeholder = { Text("+60123456789") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    isError = phoneNumber.isNotEmpty() && !validatePhoneNumber(phoneNumber),
                                    trailingIcon = {
                                        if (phoneNumber.isNotEmpty()) {
                                            Text(
                                                text = if (validatePhoneNumber(phoneNumber)) "✅" else "❌",
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                )

                                // Show phone validation message
                                if (phoneNumber.isNotEmpty()) {
                                    Text(
                                        text = if (validatePhoneNumber(phoneNumber)) "✓ Valid phone number" else "Please enter a valid phone number",
                                        color = if (validatePhoneNumber(phoneNumber)) Color.Green else Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    )
                                }

                                // Helper text for phone number
                                Text(
                                    text = "Adding a phone number allows you to login using either email or phone",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
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

                            // Confirm Password
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
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
                                shape = RoundedCornerShape(8.dp),
                                isError = confirmPassword.isNotEmpty() && password != confirmPassword
                            )

                            if (confirmPassword.isNotEmpty()) {
                                Text(
                                    text = if (password == confirmPassword) "✓ Passwords match" else "Passwords do not match",
                                    color = if (password == confirmPassword) Color.Green else Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Security Question Dropdown
                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedQuestion,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Security Question") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    placeholder = { Text("Select a security question") }
                                )

                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    securityQuestions.forEach { question ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = question,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                selectedQuestion = question
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Security Answer
                            OutlinedTextField(
                                value = securityAnswer,
                                onValueChange = { securityAnswer = it },
                                label = { Text("Security Answer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                placeholder = { Text("Enter your answer") },
                                enabled = selectedQuestion.isNotEmpty()
                            )

                            if (selectedQuestion.isNotEmpty()) {
                                Text(
                                    text = "This will help you recover your password if needed",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Terms and conditions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { agreeToTerms = !agreeToTerms }
                            ) {
                                Checkbox(
                                    checked = agreeToTerms,
                                    onCheckedChange = { agreeToTerms = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFFD700)
                                    )
                                )
                                Text(
                                    text = "I agree to the Terms and Conditions",
                                    fontSize = 14.sp,
                                    color = Color(0xFF616161),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Create Account button
                            Button(
                                onClick = {
                                    if (validateForm()) {
                                        createAccount()
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
                                enabled = !isLoading && isEmailValid && agreeToTerms
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Create Account",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Back to login prompt
                item { Spacer(modifier = Modifier.height(30.dp)) }

                item {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Already have an account? ",
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