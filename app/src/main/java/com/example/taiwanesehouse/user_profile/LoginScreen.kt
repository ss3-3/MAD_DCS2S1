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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

@Composable
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Login states
    var emailOrPhone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var inputType by rememberSaveable { mutableStateOf("unknown") } // "email", "phone", "unknown"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Helper functions
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(" ", "").replace("-", "").trim()
    }

    fun isValidEmail(email: String): Boolean {
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

    fun isValidPhoneNumber(phone: String): Boolean {
        val cleanPhone = cleanPhoneNumber(phone).replace("+", "")
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() }
    }

    fun detectInputType(input: String) {
        inputType = when {
            input.contains("@") -> "email"
            input.startsWith("+") || input.replace(" ", "").replace("-", "").all { it.isDigit() } -> "phone"
            else -> "unknown"
        }
    }

    // Login function
    fun performLogin() {
        if (emailOrPhone.isBlank()) {
            errorMessage = "Please enter your email or phone number"
            return
        }

        if (password.isBlank()) {
            errorMessage = "Please enter your password"
            return
        }

        isLoading = true
        errorMessage = ""

        scope.launch {
            try {
                when (inputType) {
                    "email" -> {
                        if (!isValidEmail(emailOrPhone)) {
                            errorMessage = "Please enter a valid email address"
                            return@launch
                        }

                        // Login with email directly
                        auth.signInWithEmailAndPassword(emailOrPhone.trim().lowercase(), password.trim()).await()

                        // Update last login time in Firestore
                        val user = auth.currentUser
                        if (user != null) {
                            firestore.collection("users")
                                .document(user.uid)
                                .update("lastLoginAt", System.currentTimeMillis())
                                .await()
                        }

                        snackbarHostState.showSnackbar("Login successful!")
                        navController.navigate(Screen.Menu.name) {
                            popUpTo(Screen.Login.name) { inclusive = true }
                        }
                    }

                    "phone" -> {
                        if (!isValidPhoneNumber(emailOrPhone)) {
                            errorMessage = "Please enter a valid phone number"
                            return@launch
                        }

                        val cleanPhone = cleanPhoneNumber(emailOrPhone)

                        // For phone number login, we need to find the user's email first
                        val querySnapshot = firestore.collection("users")
                            .whereEqualTo("phoneNumber", cleanPhone)
                            .limit(1)
                            .get()
                            .await()

                        if (querySnapshot.isEmpty) {
                            errorMessage = "No account found with this phone number"
                            return@launch
                        }

                        val userDoc = querySnapshot.documents[0]
                        val userEmail = userDoc.getString("email")
                        val authMethod = userDoc.getString("authMethod") ?: ""

                        when (authMethod) {
                            "email_password" -> {
                                // User registered with email+password but also has phone number
                                if (userEmail != null) {
                                    auth.signInWithEmailAndPassword(userEmail, password.trim()).await()

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
                                    errorMessage = "Account configuration error. Please contact support."
                                }
                            }

                            "phone_verification" -> {
                                // For phone-only accounts, show message about password reset
                                errorMessage = "This phone number was registered with SMS verification. Password login is not available for this account type."
                            }

                            else -> {
                                errorMessage = "Unknown authentication method for this account"
                            }
                        }
                    }

                    else -> {
                        errorMessage = "Please enter a valid email address or phone number"
                    }
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "Invalid email or password. Please try again."
            } catch (e: FirebaseAuthInvalidUserException) {
                when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> errorMessage = "No account found with this email address"
                    "ERROR_USER_DISABLED" -> errorMessage = "This account has been disabled"
                    else -> errorMessage = "Account not found or has been disabled"
                }
            } catch (e: Exception) {
                errorMessage = "Login failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
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
                            // Title
                            Text(
                                text = "Welcome Back",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Please enter your credentials to continue.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Email or Phone field
                            Text(
                                text = "Email or Phone Number",
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = emailOrPhone,
                                onValueChange = {
                                    emailOrPhone = it
                                    detectInputType(it)
                                    errorMessage = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                placeholder = {
                                    Text(
                                        when (inputType) {
                                            "email" -> "example@email.com"
                                            "phone" -> "+60123456789"
                                            else -> "Email or phone number"
                                        }
                                    )
                                },
                                isError = errorMessage.isNotEmpty() && errorMessage.contains("email") || errorMessage.contains("phone"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    if (emailOrPhone.isNotEmpty()) {
                                        Text(
                                            text = when (inputType) {
                                                "email" -> if (isValidEmail(emailOrPhone)) "📧" else "❌"
                                                "phone" -> if (isValidPhoneNumber(emailOrPhone)) "📱" else "❌"
                                                else -> "❓"
                                            },
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            )

                            // Show input type hint
                            if (emailOrPhone.isNotEmpty()) {
                                Text(
                                    text = when (inputType) {
                                        "email" -> if (isValidEmail(emailOrPhone)) "✓ Email format detected" else "Please enter a valid email address"
                                        "phone" -> if (isValidPhoneNumber(emailOrPhone)) "✓ Phone number detected" else "Please enter a valid phone number"
                                        else -> "Enter an email address or phone number"
                                    },
                                    color = when (inputType) {
                                        "email" -> if (isValidEmail(emailOrPhone)) Color.Green else Color.Red
                                        "phone" -> if (isValidPhoneNumber(emailOrPhone)) Color.Green else Color.Red
                                        else -> Color.Gray
                                    },
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password field
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
                                onValueChange = {
                                    password = it
                                    errorMessage = ""
                                },
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
                                isError = errorMessage.isNotEmpty() && errorMessage.contains("password"),
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Error message
                            if (errorMessage.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
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

                            // Login button
                            Button(
                                onClick = { performLogin() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color.Black
                                ),
                                enabled = !isLoading && emailOrPhone.isNotBlank() && password.isNotBlank() &&
                                        ((inputType == "email" && isValidEmail(emailOrPhone)) ||
                                                (inputType == "phone" && isValidPhoneNumber(emailOrPhone)))
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Login",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Additional info for phone users
                            if (inputType == "phone") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Blue.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Note: Phone numbers registered with SMS verification cannot use password login. Please use your email address instead.",
                                        color = Color.Blue,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

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

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}