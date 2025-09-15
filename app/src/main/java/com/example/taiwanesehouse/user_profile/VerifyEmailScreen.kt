package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val snackbarHostState = remember { SnackbarHostState() }

    // States
    var isSendingVerification by rememberSaveable { mutableStateOf(false) }
    var isCheckingVerification by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var messageType by rememberSaveable { mutableStateOf(MessageType.INFO) }
    var userEmail by rememberSaveable { mutableStateOf("") }

    // Get current user email
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userEmail = currentUser.email ?: ""
        }
    }

    // Success dialog state
    var successDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Verify Email",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        // Success dialog
        if (successDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    successDialogVisible = false
                    navController.popBackStack()
                },
                title = {
                    Text(
                        text = "Email Verified!",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Text(
                        text = "Your email has been successfully verified. Your profile has been updated.",
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            successDialogVisible = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Continue")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // Main card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Email icon
                        Card(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(40.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFC107).copy(alpha = 0.2f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Verify Your Email",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "We need to verify your email address to secure your account and enable all features.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email display
                        if (userEmail.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Email Address",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = userEmail,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Send verification email button
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isSendingVerification = true
                                        message = ""
                                        val user = auth.currentUser
                                        if (user == null) {
                                            message = "Not logged in"
                                            messageType = MessageType.ERROR
                                        } else {
                                            user.sendEmailVerification().await()
                                            message = "Verification email sent successfully!"
                                            messageType = MessageType.SUCCESS
                                        }
                                    } catch (e: Exception) {
                                        message = "Failed to send verification email: ${e.message}"
                                        messageType = MessageType.ERROR
                                    } finally {
                                        isSendingVerification = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107),
                                contentColor = Color.Black
                            ),
                            enabled = !isSendingVerification && !isCheckingVerification
                        ) {
                            if (isSendingVerification) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sending...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Send Email",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Verification Email",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Check verification button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        isCheckingVerification = true
                                        message = ""
                                        val user = auth.currentUser
                                        if (user != null) {
                                            user.reload().await()
                                            if (user.isEmailVerified) {
                                                firestore.collection("users")
                                                    .document(user.uid)
                                                    .update("emailVerified", true)
                                                    .await()

                                                successDialogVisible = true
                                            } else {
                                                message = "Email not verified yet. Please check your inbox and click the verification link."
                                                messageType = MessageType.WARNING
                                            }
                                        } else {
                                            message = "Not logged in"
                                            messageType = MessageType.ERROR
                                        }
                                    } catch (e: Exception) {
                                        message = "Failed to check verification: ${e.message}"
                                        messageType = MessageType.ERROR
                                    } finally {
                                        isCheckingVerification = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFFC107)
                            ),
                            enabled = !isSendingVerification && !isCheckingVerification
                        ) {
                            if (isCheckingVerification) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Checking...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check Verification",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I've Verified My Email",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Message display
                        if (message.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (messageType) {
                                        MessageType.SUCCESS -> Color(0xFFE8F5E8)
                                        MessageType.ERROR -> Color(0xFFFDE8E8)
                                        MessageType.WARNING -> Color(0xFFFFF8E1)
                                        MessageType.INFO -> Color(0xFFE3F2FD)
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = message,
                                    color = when (messageType) {
                                        MessageType.SUCCESS -> Color(0xFF2E7D32)
                                        MessageType.ERROR -> Color(0xFFD32F2F)
                                        MessageType.WARNING -> Color(0xFFEF6C00)
                                        MessageType.INFO -> Color(0xFF1976D2)
                                    },
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Instructions card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F9FA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Instructions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        InstructionItem(
                            number = "1",
                            text = "Click 'Send Verification Email' to receive a verification link"
                        )

                        InstructionItem(
                            number = "2",
                            text = "Check your email inbox (and spam folder)"
                        )

                        InstructionItem(
                            number = "3",
                            text = "Click the verification link in the email"
                        )

                        InstructionItem(
                            number = "4",
                            text = "Return here and click 'I've Verified My Email'"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Didn't receive the email? Check your spam folder or try sending again.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun InstructionItem(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFC107)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

enum class MessageType {
    SUCCESS, ERROR, WARNING, INFO
}