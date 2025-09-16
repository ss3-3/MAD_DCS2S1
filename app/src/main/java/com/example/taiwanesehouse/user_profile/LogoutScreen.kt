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
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.Screen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LogoutScreen(navController: NavController) {
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    // State variables
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(true) }
    var logoutComplete by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Firebase instances
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Logout function
    fun performLogout() {
        isLoading = true
        scope.launch {
            try {
                auth.signOut()
                logoutComplete = true
                snackbarHostState.showSnackbar("Logged out successfully!")

                // Small delay for better UX
                kotlinx.coroutines.delay(1000)

                // Navigate to login and clear back stack
                navController.navigate(Screen.Login.name) {
                    popUpTo(0) { inclusive = true }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Logout failed: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-logout if no user is signed in
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screen.Login.name) {
                popUpTo(0) { inclusive = true }
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
                                            text = "You have been successfully logged out.\nThank you for using Taiwanese House!",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 20.dp)
                                        )

                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color(0xFFFFD700),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
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

                                    // Logout button
                                    Button(
                                        onClick = {
                                            showConfirmDialog = false
                                            performLogout()
                                        },
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
                                                text = "Yes, Logout",
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