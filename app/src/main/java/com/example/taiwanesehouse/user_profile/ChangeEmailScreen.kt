package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Change Email") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFFFC107))
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("New Email") }, singleLine = true)
            OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Current Password") }, singleLine = true)

            Button(onClick = {
                scope.launch {
                    try {
                        isProcessing = true
                        message = ""
                        val user = auth.currentUser
                        if (user == null) {
                            message = "Not logged in"
                            return@launch
                        }

                        // Re-authenticate
                        val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                        user.reauthenticate(credential).await()

                        val oldEmail = user.email ?: ""

                        // Update email in Auth
                        user.updateEmail(newEmail).await()

                        // Send alert to original email (best-effort)
                        try { auth.sendPasswordResetEmail(oldEmail).await() } catch (_: Exception) {}

                        // Send verification to new email
                        try { user.sendEmailVerification().await() } catch (_: Exception) {}

                        // Update Firestore document
                        firestore.collection("users").document(user.uid)
                            .update(mapOf("email" to newEmail, "emailVerified" to false)).await()

                        message = "Email updated. Verification sent to new address."
                    } catch (e: Exception) {
                        message = e.message ?: "Failed to change email"
                    } finally {
                        isProcessing = false
                    }
                }
            }, enabled = !isProcessing) {
                if (isProcessing) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp)) else Text("Update Email")
            }

            if (message.isNotEmpty()) {
                Text(message, color = Color.Gray)
            }
        }
    }
}


