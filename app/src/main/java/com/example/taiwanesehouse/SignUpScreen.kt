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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var phoneNumber by rememberSaveable { mutableStateOf("+60") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var agreeToTerms by rememberSaveable { mutableStateOf(false) }

    // Security question states
    var selectedQuestion by rememberSaveable { mutableStateOf("") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // Process states
    var currentStep by rememberSaveable { mutableIntStateOf(1) } // 1: Form, 2: Phone Verification, 3: Complete
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // Authentication method selection
    var authMethod by rememberSaveable { mutableStateOf("phone_verification") } // "phone_verification" or "phone_password"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Initialize PhoneAuth
    val phoneAuth = remember { PhoneAuth(activity) }

    // Helper function to clean and format phone number
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").trim()
    }

    // Helper function to convert phone number to pseudo-email
    fun phoneNumberToPseudoEmail(phoneNumber: String): String {
        val cleanPhone = cleanPhoneNumber(phoneNumber).replace("+", "")
        return "$cleanPhone@taiwanesehouse.com"
    }

    // Complete registration function for phone verification method
    fun completeRegistration() {
        scope.launch {
            try {
                isLoading = true

                // Create user with phone number authentication
                val user = auth.currentUser

                if (user != null) {
                    val cleanPhone = cleanPhoneNumber(phoneNumber)

                    // Save user data to Firestore using UID as document ID
                    val userData = hashMapOf(
                        "fullName" to fullName.trim(),
                        "phoneNumber" to cleanPhone,
                        "phoneVerified" to true,
                        "securityQuestion" to selectedQuestion,
                        "securityAnswer" to securityAnswer.trim().lowercase(), // Store in lowercase for easier comparison
                        "authMethod" to "phone_verification", // Set auth method
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )

                    // Use UID as document ID instead of storing uid field
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .await()

                    // Update user profile
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()

                    user.updateProfile(profileUpdates).await()

                    currentStep = 3
                    snackbarHostState.showSnackbar("Account created successfully!")
                }
            } catch (e: Exception) {
                errorMessage = "Registration failed: ${e.localizedMessage}"
                snackbarHostState.showSnackbar(errorMessage)
            } finally {
                isLoading = false
            }
        }
    }

    // Phone + Password registration function
    fun createAccountWithPassword() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = ""

                val cleanPhone = cleanPhoneNumber(phoneNumber)
                val pseudoEmail = phoneNumberToPseudoEmail(cleanPhone)

                // Check if phone number already exists in Firestore
                val existingUsers = firestore.collection("users")
                    .whereEqualTo("phoneNumber", cleanPhone)
                    .get()
                    .await()

                if (!existingUsers.isEmpty) {
                    errorMessage = "An account with this phone number already exists"
                    return@launch
                }

                // Create user with email/password authentication using pseudo-email
                val authResult = auth.createUserWithEmailAndPassword(pseudoEmail, password.trim()).await()
                val user = authResult.user

                if (user != null) {
                    // Update user profile with display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()

                    // Save user data to Firestore using UID as document ID
                    val userData = hashMapOf(
                        "fullName" to fullName.trim(),
                        "phoneNumber" to cleanPhone,
                        "pseudoEmail" to pseudoEmail, // Store the pseudo-email for reference
                        "securityQuestion" to selectedQuestion,
                        "securityAnswer" to securityAnswer.trim().lowercase(), // Store in lowercase for easier comparison
                        "phoneVerified" to false, // Can be updated later if phone verification is implemented
                        "authMethod" to "phone_password", // Set auth method
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )

                    // Use UID as document ID instead of storing uid field
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData)
                        .await()

                    // Skip verification step and go to success
                    currentStep = 3
                    snackbarHostState.showSnackbar("Account created successfully!")
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                errorMessage = "Password is too weak. Please choose a stronger password."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "Invalid phone number format. Please check and try again."
            } catch (e: FirebaseAuthUserCollisionException) {
                errorMessage = "An account with this phone number already exists."
            } catch (e: Exception) {
                errorMessage = "Registration failed: ${e.localizedMessage ?: "Unknown error occurred"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Set up phone auth listener
    LaunchedEffect(phoneAuth) {
        phoneAuth.setPhoneAuthListener(object : PhoneAuth.PhoneAuthListener {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                scope.launch {
                    snackbarHostState.showSnackbar("Phone verified automatically!")
                    completeRegistration()
                }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                isLoading = false
                errorMessage = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> "Please enter a valid phone number"
                    else -> "Verification failed. Please try again."
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isLoading = false
                currentStep = 2
                errorMessage = ""
                scope.launch {
                    snackbarHostState.showSnackbar("Verification code sent to your phone")
                }
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                // Auto retrieval timeout
            }

            override fun onSignInSuccess(user: FirebaseUser?) {
                // This won't be used in signup - we handle it manually
            }

            override fun onSignInFailure(exception: Exception) {
                // This won't be used in signup
            }
        })
    }

    // Form validation
    fun validateForm(): Boolean {
        return when {
            fullName.isBlank() -> {
                errorMessage = "Please enter your full name"
                false
            }
            phoneNumber.isBlank() || cleanPhoneNumber(phoneNumber).length < 10 -> {
                errorMessage = "Please enter a valid phone number"
                false
            }
            authMethod == "phone_password" && password.length < 6 -> {
                errorMessage = "Password must be at least 6 characters"
                false
            }
            authMethod == "phone_password" && password != confirmPassword -> {
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
                            when (currentStep) {
                                1 -> {
                                    // Step 1: Registration Form
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

                                    // Authentication Method Selection
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
                                                text = "Choose Registration Method",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { authMethod = "phone_verification" }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = authMethod == "phone_verification",
                                                    onClick = { authMethod = "phone_verification" },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = Color(0xFFFFD700)
                                                    )
                                                )
                                                Column(
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "Phone Verification (OTP)",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "Verify your phone number with SMS code",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { authMethod = "phone_password" }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = authMethod == "phone_password",
                                                    onClick = { authMethod = "phone_password" },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = Color(0xFFFFD700)
                                                    )
                                                )
                                                Column(
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "Phone + Password",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "Use phone number and password to login",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }

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

                                    // Phone Number
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("Phone Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        placeholder = { Text("+60123456789") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Password fields (only show for phone_password method)
                                    if (authMethod == "phone_password") {
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
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

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
                                        Text(
                                            text = errorMessage,
                                            color = Color.Red,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                        )
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

                                    // Continue button
                                    Button(
                                        onClick = {
                                            if (validateForm()) {
                                                if (authMethod == "phone_verification") {
                                                    isLoading = true
                                                    phoneAuth.verifyPhoneNumber(cleanPhoneNumber(phoneNumber))
                                                } else {
                                                    createAccountWithPassword()
                                                }
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
                                                text = if (authMethod == "phone_verification") "Verify Phone Number" else "Create Account",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                2 -> {
                                    // Step 2: Phone Verification (only for phone_verification method)
                                    Text(
                                        text = "Verify Phone Number",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Please enter the 6-digit code sent to\n${cleanPhoneNumber(phoneNumber)}",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Verification code input
                                    OutlinedTextField(
                                        value = verificationCode,
                                        onValueChange = { if (it.length <= 6) verificationCode = it },
                                        label = { Text("Verification Code") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("123456") },
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Resend code button
                                    TextButton(
                                        onClick = {
                                            isLoading = true
                                            phoneAuth.verifyPhoneNumber(cleanPhoneNumber(phoneNumber), forceResend = true)
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Text(
                                            text = "Resend Code",
                                            color = Color(0xFF007AFF)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Verify button
                                    Button(
                                        onClick = {
                                            if (verificationCode.length == 6) {
                                                isLoading = true
                                                phoneAuth.verifyCode(verificationCode)
                                                scope.launch {
                                                    kotlinx.coroutines.delay(1000) // Give time for verification
                                                }
                                                completeRegistration()
                                            } else {
                                                errorMessage = "Please enter a 6-digit verification code"
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
                                                text = "Verify & Create Account",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Back to form
                                    TextButton(
                                        onClick = {
                                            currentStep = 1
                                            verificationCode = ""
                                            errorMessage = ""
                                            phoneAuth.clearVerificationData()
                                        }
                                    ) {
                                        Text(
                                            text = "← Back to Form",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                3 -> {
                                    // Step 3: Success
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
                                            text = "Welcome to Taiwanese House!",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = "Your account has been created successfully.\nYou can now start exploring our delicious menu!",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 32.dp)
                                        )

                                        Button(
                                            onClick = {
                                                navController.navigate(Screen.Menu.name) {
                                                    popUpTo(Screen.Signup.name) { inclusive = true }
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
                                                text = "Explore Menu",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        OutlinedButton(
                                            onClick = {
                                                navController.navigate(Screen.Login.name) {
                                                    popUpTo(Screen.Signup.name) { inclusive = true }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Text(
                                                text = "Go to Login",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
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
}