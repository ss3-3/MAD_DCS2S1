package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isSending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Verify Email") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFFFC107))
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("We can resend a verification email to your registered address.")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            isSending = true
                            message = ""
                            val user = auth.currentUser
                            if (user == null) {
                                message = "Not logged in"
                            } else {
                                user.sendEmailVerification().await()
                                message = "Verification email sent. Please check your inbox."
                            }
                        } catch (e: Exception) {
                            message = e.message ?: "Failed to send verification email"
                        } finally {
                            isSending = false
                        }
                    }
                },
                enabled = !isSending
            ) {
                if (isSending) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp)) else Text("Resend Verification Email")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val user = auth.currentUser
                            if (user != null) {
                                user.reload().await()
                                if (user.isEmailVerified) {
                                    firestore.collection("users").document(user.uid)
                                        .update("emailVerified", true).await()
                                    message = "Email verified. Profile updated."
                                } else {
                                    message = "Email not verified yet."
                                }
                            }
                        } catch (e: Exception) {
                            message = e.message ?: "Failed to check verification"
                        }
                    }
                }
            ) { Text("I have verified") }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color.Gray)
            }
        }
    }
}


