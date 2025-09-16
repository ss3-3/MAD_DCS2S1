package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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

@Composable
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // Collect state from ViewModel
    val authState by viewModel.authState.collectAsState()
    val validationState by viewModel.validationState.collectAsState()

    // Form states - Initialize with saved credentials
    var emailOrPhone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    // Initialize form with saved credentials
    LaunchedEffect(Unit) {
        val savedCredential = viewModel.getSavedCredential()
        val savedRememberMe = viewModel.getSavedRememberMe()

        if (savedCredential.isNotEmpty()) {
            emailOrPhone = savedCredential
            rememberMe = savedRememberMe
        }
    }

    // Update form when auth state changes (in case of auto-load)
    LaunchedEffect(authState.savedCredential) {
        if (authState.savedCredential.isNotEmpty() && emailOrPhone.isEmpty()) {
            emailOrPhone = authState.savedCredential
            rememberMe = authState.rememberMe
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Detect input type
    val inputType = viewModel.detectInputType(emailOrPhone)

    // Real-time validation
    val isEmailValid = if (inputType == "email") {
        viewModel.validateEmail(emailOrPhone)
    } else false

    val isPhoneValid = if (inputType == "phone") {
        viewModel.validatePhone(emailOrPhone)
    } else false

    // Handle navigation on successful login
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(Screen.Menu.name) {
                popUpTo(Screen.Login.name) { inclusive = true }
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

    // Show error message
    LaunchedEffect(authState.errorMessage) {
        if (authState.errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(authState.errorMessage)
        }
    }

    // Login function
    fun performLogin() {
        viewModel.loginUser(
            emailOrPhone = emailOrPhone,
            password = password,
            rememberMe = rememberMe
        )
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
                    AuthHeader(
                        logoImage = logoImage,
                        title = "Taiwanese House",
                        subtitle = "Authentic Taiwanese Cuisine",
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
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

                            // Show welcome message for remembered users
                            if (rememberMe && emailOrPhone.isNotEmpty()) {
                                Text(
                                    text = "Welcome back! Your credentials have been remembered.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                Text(
                                    text = "Please enter your credentials to continue.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                            }

                            // Email or Phone field
                            EmailOrPhoneField(
                                value = emailOrPhone,
                                onValueChange = {
                                    emailOrPhone = it
                                    viewModel.clearError()
                                },
                                inputType = inputType,
                                isEmailValid = isEmailValid,
                                isPhoneValid = isPhoneValid,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password field
                            PasswordField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    viewModel.clearError()
                                },
                                label = "Password",
                                modifier = Modifier.fillMaxWidth()
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

                            // Remember me checkbox with clear option
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RememberMeCheckbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it }
                                )

                                // Clear saved credentials button
                                if (authState.savedCredential.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            viewModel.clearSavedCredentials()
                                            emailOrPhone = ""
                                            rememberMe = false
                                        }
                                    ) {
                                        Text(
                                            text = "Clear Saved",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF6B6B)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Error message
                            ErrorCard(
                                message = authState.errorMessage,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Login button
                            AuthButton(
                                text = "Login",
                                onClick = { performLogin() },
                                isLoading = authState.isLoading,
                                enabled = emailOrPhone.isNotBlank() && password.isNotBlank() &&
                                        ((inputType == "email" && isEmailValid) ||
                                                (inputType == "phone" && isPhoneValid) ||
                                                (inputType == "unknown"))
                            )

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

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Sign up prompt section
                item {
                    AuthNavigationLink(
                        prefixText = "Don't have an account? ",
                        linkText = "Sign Up",
                        onClick = { navController.navigate(Screen.Signup.name) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}