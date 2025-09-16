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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LogoutScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // State variables
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(true) }
    var logoutComplete by rememberSaveable { mutableStateOf(false) }
    var clearSavedCredentials by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsState()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Auto-redirect if already logged out
    LaunchedEffect(currentUser) {
        if (currentUser == null && !logoutComplete) {
            // User is already logged out, navigate to login immediately
            navController.navigate(Screen.Login.name) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Logout function with better error handling
    fun performLogout() {
        scope.launch {
            try {
                isLoading = true
                showConfirmDialog = false

                // Use ViewModel's logout function to handle credentials
                viewModel.logout(clearRememberedCredentials = clearSavedCredentials)

                // Sign out from Firebase
                auth.signOut()

                // Mark logout as complete
                logoutComplete = true

                // Show success message
                val message = if (clearSavedCredentials) {
                    "Logged out successfully! Saved credentials cleared."
                } else {
                    "Logged out successfully!"
                }
                snackbarHostState.showSnackbar(message)

                // Wait for snackbar to show
                kotlinx.coroutines.delay(1500)

                // Navigate to login and clear entire back stack
                navController.navigate(Screen.Login.name) {
                    popUpTo(0) { inclusive = true }
                }
            } catch (e: Exception) {
                // Handle any logout errors
                snackbarHostState.showSnackbar("Logout failed: ${e.localizedMessage}")
                isLoading = false
                showConfirmDialog = true // Reset to allow retry
                logoutComplete = false
            }
        }
    }

    // If user is already null and logout is complete, show loading while navigating
    if (currentUser == null && logoutComplete) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFFFFC107))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Redirecting to login...", color = Color.Gray)
            }
        }
        return
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
                verticalArrangement = Arrangement.Center
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
                                text = "Thank You for Visiting",
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
                            when {
                                logoutComplete -> {
                                    // Logout Success
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 20.dp)
                                    ) {
                                        Text(
                                            text = "👋",
                                            fontSize = 48.sp,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Text(
                                            text = "Goodbye!",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        Text(
                                            text = if (clearSavedCredentials) {
                                                "You have been logged out and saved credentials cleared.\nRedirecting to login..."
                                            } else {
                                                "You have been successfully logged out.\nRedirecting to login..."
                                            },
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 20.dp)
                                        )

                                        CircularProgressIndicator(
                                            color = Color(0xFFFFD700),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                showConfirmDialog -> {
                                    // Logout Confirmation
                                    Text(
                                        text = "Logout Confirmation",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Are you sure you want to logout from your account?",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    // User info card (if available)
                                    currentUser?.let { user ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF5F5F5)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Currently logged in as:",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )

                                                Text(
                                                    text = user.displayName ?: "User",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Black
                                                )

                                                user.phoneNumber?.let { phone ->
                                                    Text(
                                                        text = phone,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF666666),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Remember Me options (if credentials are saved)
                                    if (authState.savedCredential.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFFF3CD)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "💡 Remember Me Settings",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF856404),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )

                                                Text(
                                                    text = "You have saved credentials for: ${authState.savedCredential}",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF856404),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = clearSavedCredentials,
                                                        onCheckedChange = { clearSavedCredentials = it },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFFFF6B6B)
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Also clear saved login credentials",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF856404)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Logout button
                                    Button(
                                        onClick = { performLogout() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF6B6B),
                                            contentColor = Color.White
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                text = if (clearSavedCredentials) "Yes, Logout & Clear Data" else "Yes, Logout",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Cancel button
                                    OutlinedButton(
                                        onClick = {
                                            navController.popBackStack()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.Black
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Helper text
                                    Text(
                                        text = "You can always login again anytime to enjoy our delicious meals!",
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
            }
        }
    }
}