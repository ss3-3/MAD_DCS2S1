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
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Login states
    var phoneNumber by rememberSaveable { mutableStateOf("+60") }
    var password by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var loginMethod by rememberSaveable { mutableStateOf("auto") } // "auto", "password", "otp"
    var currentStep by rememberSaveable { mutableIntStateOf(1) } // 1: Method selection, 2: OTP verification
    var userAuthMethod by rememberSaveable { mutableStateOf("") } // Detected user's auth method

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val phoneAuth = remember { PhoneAuth(activity) }

    // Helper functions
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").trim()
    }

    fun phoneNumberToPseudoEmail(phoneNumber: String): String {
        val cleanPhone = cleanPhoneNumber(phoneNumber).replace("+", "")
        return "$cleanPhone@taiwanesehouse.com"
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val cleanPhone = cleanPhoneNumber(phone).replace("+", "")
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() }
    }

    // Check user's authentication method
    fun checkUserAuthMethod() {
        if (!isValidPhoneNumber(phoneNumber)) {
            errorMessage = "Please enter a valid phone number"
            return
        }

        isLoading = true
        errorMessage = ""

        scope.launch {
            try {
                val cleanPhone = cleanPhoneNumber(phoneNumber)
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", cleanPhone)
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    errorMessage = "No account found with this phone number"
                    loginMethod = "auto"
                } else {
                    val userDoc = querySnapshot.documents[0]
                    val authMethod = userDoc.getString("authMethod") ?: "phone_verification"
                    userAuthMethod = authMethod

                    when (authMethod) {
                        "phone_password" -> {
                            loginMethod = "password"
                            errorMessage = ""
                        }
                        "phone_verification" -> {
                            loginMethod = "otp"
                            errorMessage = ""
                        }
                        else -> {
                            errorMessage = "Unknown authentication method"
                            loginMethod = "auto"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to check account: ${e.localizedMessage}"
                loginMethod = "auto"
            } finally {
                isLoading = false
            }
        }
    }

    // Login with password
    fun loginWithPassword() {
        if (password.isBlank()) {
            errorMessage = "Please enter your password"
            return
        }

        isLoading = true
        errorMessage = ""

        scope.launch {
            try {
                val cleanPhone = cleanPhoneNumber(phoneNumber)
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", cleanPhone)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]
                    val pseudoEmail = userDoc.getString("pseudoEmail")
                        ?: phoneNumberToPseudoEmail(cleanPhone)

                    auth.signInWithEmailAndPassword(pseudoEmail, password).await()

                    // Update last login time
                    firestore.collection("users")
                        .document(userDoc.id)
                        .update("lastLoginAt", System.currentTimeMillis())
                        .await()

                    snackbarHostState.showSnackbar("Login successful!")
                    navController.navigate(Screen.Menu.name) {
                        popUpTo(Screen.Login.name) { inclusive = true }
                    }
                } else {
                    errorMessage = "Account not found"
                }

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "Incorrect password. Please try again."
            } catch (e: Exception) {
                errorMessage = "Login failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Start OTP verification
    fun startOTPVerification() {
        isLoading = true
        errorMessage = ""
        phoneAuth.verifyPhoneNumber(cleanPhoneNumber(phoneNumber))
    }

    // Complete OTP login
    fun completeOTPLogin() {
        scope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    // Update last login time
                    val cleanPhone = cleanPhoneNumber(phoneNumber)
                    val querySnapshot = firestore.collection("users")
                        .whereEqualTo("phoneNumber", cleanPhone)
                        .limit(1)
                        .get()
                        .await()

                    if (!querySnapshot.isEmpty) {
                        firestore.collection("users")
                            .document(querySnapshot.documents[0].id)
                            .update("lastLoginAt", System.currentTimeMillis())
                            .await()
                    }

                    snackbarHostState.showSnackbar("Login successful!")
                    navController.navigate(Screen.Menu.name) {
                        popUpTo(Screen.Login.name) { inclusive = true }
                    }
                } else {
                    errorMessage = "Login failed. Please try again."
                    currentStep = 1
                }
            } catch (e: Exception) {
                errorMessage = "Login failed: ${e.localizedMessage}"
                currentStep = 1
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
                    completeOTPLogin()
                }
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                isLoading = false
                currentStep = 1
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
                completeOTPLogin()
            }

            override fun onSignInFailure(exception: Exception) {
                isLoading = false
                currentStep = 1
                errorMessage = "Login failed: ${exception.localizedMessage}"
            }
        })
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(52.dp))
                }

                // Logo and title section
                item {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = logoImage,
                            contentDescription = "Taiwanese House Logo",
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Taiwanese House",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Authentic Taiwanese Cuisine",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Login card section
                item {
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
                            when {
                                currentStep == 2 && loginMethod == "otp" -> {
                                    // OTP Verification Step
                                    Text(
                                        text = "Enter Verification Code",
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

                                    // Resend code
                                    TextButton(
                                        onClick = {
                                            startOTPVerification()
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
                                                text = "Verify & Login",
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
                                            verificationCode = ""
                                            errorMessage = ""
                                            phoneAuth.clearVerificationData()
                                        }
                                    ) {
                                        Text(
                                            text = "← Back to Phone Number",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                else -> {
                                    // Main Login Step
                                    Text(
                                        text = "Welcome Back",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Please enter your phone number to continue.",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // Phone number field
                                    Text(
                                        text = "Phone Number",
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    )

                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = {
                                            phoneNumber = it
                                            if (loginMethod != "auto") {
                                                loginMethod = "auto"
                                                userAuthMethod = ""
                                                errorMessage = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        placeholder = { Text("+60123456789") },
                                        isError = errorMessage.isNotEmpty(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Check account button (when method is auto)
                                    if (loginMethod == "auto") {
                                        Button(
                                            onClick = { checkUserAuthMethod() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFD700),
                                                contentColor = Color.Black
                                            ),
                                            enabled = !isLoading && phoneNumber.isNotBlank()
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(
                                                    color = Color.Black,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "Continue",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Password login (when method is password)
                                    if (loginMethod == "password") {
                                        Text(
                                            text = "Password",
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp)
                                        )

                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it },
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
                                            isError = errorMessage.isNotEmpty(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        // Forgot password link
                                        Text(
                                            text = "Forgot Password?",
                                            fontSize = 12.sp,
                                            color = Color(0xFF007AFF),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .clickable {
                                                    navController.navigate(Screen.ForgotPassword.name)
                                                }
                                                .padding(top = 8.dp, end = 4.dp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Remember me checkbox
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { rememberMe = !rememberMe }
                                        ) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { rememberMe = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFFFD700)
                                                )
                                            )
                                            Text(
                                                text = "Remember me",
                                                fontSize = 14.sp,
                                                color = Color(0xFF616161),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Login with password button
                                        Button(
                                            onClick = { loginWithPassword() },
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
                                                    text = "Login with Password",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // OTP login (when method is otp)
                                    if (loginMethod == "otp") {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF0F8FF)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "This account uses SMS verification. We'll send you a code to login.",
                                                fontSize = 13.sp,
                                                color = Color(0xFF0066CC),
                                                modifier = Modifier.padding(12.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Button(
                                            onClick = { startOTPVerification() },
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
                                                    text = "Send Verification Code",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
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

                                    // Back to phone number (when not auto)
                                    if (loginMethod != "auto") {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TextButton(
                                            onClick = {
                                                loginMethod = "auto"
                                                userAuthMethod = ""
                                                password = ""
                                                errorMessage = ""
                                            }
                                        ) {
                                            Text(
                                                text = "← Use Different Number",
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Footer text
                                    Text(
                                        text = "Welcome to Taiwanese House!\nEnjoy your meals with our app.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (currentStep == 1) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sign up prompt section
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Sign Up",
                                fontSize = 14.sp,
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.Signup.name)
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}