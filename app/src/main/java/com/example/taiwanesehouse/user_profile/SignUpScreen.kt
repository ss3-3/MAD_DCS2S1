// Updated SignUpScreen.kt
package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.ui.components.*
import com.example.taiwanesehouse.viewmodel.AuthViewModel
import com.example.taiwanesehouse.utils.UsernameValidator  // Import the shared utility

@Composable
fun SignUpScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
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

    // Collect state from ViewModel
    val authState by viewModel.authState.collectAsState()

    // Form states
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var selectedQuestion by rememberSaveable { mutableStateOf("") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var agreeToTerms by rememberSaveable { mutableStateOf(false) }

    // Username validation state
    var usernameValidationMessage by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Real-time validation
    val isEmailValid = viewModel.validateEmail(email)
    val isPhoneValid = phoneNumber.isBlank() || viewModel.validatePhone(phoneNumber)
    val isPasswordValid = viewModel.validatePassword(password)
    val passwordsMatch = viewModel.validatePasswordMatch(password, confirmPassword)

    // Username validation using shared utility
    LaunchedEffect(username) {
        if (username.trim().isNotEmpty()) {
            val validation = UsernameValidator.validateUsername(username)
            usernameValidationMessage = if (validation.isValid) {
                "Username is available"
            } else {
                validation.errorMessage
            }
        } else {
            usernameValidationMessage = ""
        }
    }

    // Handle navigation on successful registration
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(Screen.Menu.name) {
                popUpTo(Screen.Signup.name) { inclusive = true }
            }
        }
    }

    // Show success message
    LaunchedEffect(authState.successMessage) {
        if (authState.successMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(authState.successMessage)
            viewModel.clearSuccess()
        }
    }

    // Form validation with username validation
    fun validateForm(): Boolean {
        val usernameValidation = UsernameValidator.validateUsername(username)
        return when {
            username.isBlank() || username.length < 2 -> false
            !usernameValidation.isValid -> false
            email.isBlank() -> false
            !isEmailValid -> false
            phoneNumber.isNotEmpty() && !isPhoneValid -> false
            !isPasswordValid -> false
            password != confirmPassword -> false
            selectedQuestion.isBlank() -> false
            securityAnswer.isBlank() -> false
            !agreeToTerms -> false
            else -> true
        }
    }

    // Register function with final username validation
    fun createAccount() {
        // Final validation before submission
        val usernameValidation = UsernameValidator.validateUsername(username.trim())
        if (!usernameValidation.isValid) {
            usernameValidationMessage = usernameValidation.errorMessage
            return
        }

        if (validateForm()) {
            viewModel.registerUser(
                username = username.trim(),
                email = email,
                phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                password = password,
                securityQuestion = selectedQuestion,
                securityAnswer = securityAnswer
            )
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
                    AuthHeader(
                        logoImage = logoImage,
                        title = "Taiwanese House",
                        subtitle = "Join Us Today"
                    )
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

                            // Username with validation (using shared utility)
                            OutlinedTextField(
                                value = username,
                                onValueChange = { input ->
                                    // Filter input while typing
                                    val filtered = input.take(30).filter { char ->
                                        char.isLetterOrDigit() || char.isWhitespace() || char in ".-_"
                                    }
                                    username = filtered
                                    viewModel.clearError()
                                },
                                label = { Text("Username *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                enabled = !authState.isLoading,
                                isError = usernameValidationMessage.isNotEmpty() &&
                                        !usernameValidationMessage.contains("available"),
                                placeholder = { Text("Enter your username") },
                                supportingText = {
                                    if (usernameValidationMessage.isNotEmpty()) {
                                        Text(
                                            text = usernameValidationMessage,
                                            color = if (usernameValidationMessage.contains("available"))
                                                Color(0xFF4CAF50) else Color.Red,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Email field (your existing component)
                            EmailField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    viewModel.clearError()
                                },
                                isValid = isEmailValid,
                                isRequired = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Phone Number (Optional) - your existing component
                            PhoneField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    viewModel.clearError()
                                },
                                isValid = isPhoneValid,
                                isOptional = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password - your existing component
                            PasswordField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    viewModel.clearError()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confirm Password - your existing component
                            ConfirmPasswordField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    viewModel.clearError()
                                },
                                originalPassword = password,
                                passwordsMatch = passwordsMatch,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Security Question Dropdown - your existing component
                            SecurityQuestionDropdown(
                                selectedQuestion = selectedQuestion,
                                onQuestionSelected = {
                                    selectedQuestion = it
                                    viewModel.clearError()
                                },
                                questions = securityQuestions,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Security Answer - your existing component
                            SecurityAnswerField(
                                value = securityAnswer,
                                onValueChange = {
                                    securityAnswer = it
                                    viewModel.clearError()
                                },
                                isEnabled = selectedQuestion.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Error message - your existing component
                            ErrorCard(
                                message = authState.errorMessage
                            )

                            if (authState.errorMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Terms and conditions - your existing component
                            TermsCheckbox(
                                checked = agreeToTerms,
                                onCheckedChange = { agreeToTerms = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Create Account button - your existing component
                            AuthButton(
                                text = "Create Account",
                                onClick = { createAccount() },
                                isLoading = authState.isLoading,
                                enabled = validateForm()
                            )
                        }
                    }
                }

                // Back to login prompt
                item { Spacer(modifier = Modifier.height(30.dp)) }

                item {
                    AuthNavigationLink(
                        prefixText = "Already have an account? ",
                        linkText = "Login",
                        onClick = { navController.navigate(Screen.Login.name) }
                    )
                }
            }
        }
    }
}