package com.example.taiwanesehouse.utils

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility object for handling email verification checks across the app
 */
object EmailVerificationUtils {

    /**
     * Data class to hold verification check results
     */
    data class VerificationResult(
        val isVerified: Boolean,
        val userEmail: String?,
        val errorMessage: String? = null
    )

    /**
     * Check if the current user's email is verified
     * @return VerificationResult with verification status and user info
     */
    suspend fun checkEmailVerificationStatus(): VerificationResult {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                return VerificationResult(
                    isVerified = false,
                    userEmail = null,
                    errorMessage = "User not logged in"
                )
            }

            // Reload user to get latest verification status
            currentUser.reload().await()

            val isVerified = currentUser.isEmailVerified
            val userEmail = currentUser.email

            // Update Firestore if email is verified but not recorded
            if (isVerified) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("emailVerified", true)
                        .await()
                } catch (e: Exception) {
                    // Silently handle Firestore update error - verification status is still valid
                    println("Failed to update Firestore verification status: ${e.message}")
                }
            }

            VerificationResult(
                isVerified = isVerified,
                userEmail = userEmail
            )

        } catch (e: Exception) {
            VerificationResult(
                isVerified = false,
                userEmail = null,
                errorMessage = "Failed to check verification status: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Send verification email to current user
     * @return Success message or error message
     */
    suspend fun sendVerificationEmail(): String {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                "User not logged in"
            } else {
                currentUser.sendEmailVerification().await()
                "Verification email sent successfully!"
            }

        } catch (e: Exception) {
            "Failed to send verification email: ${e.localizedMessage}"
        }
    }

    /**
     * Check if user is logged in and email is verified
     * @return Triple of (isLoggedIn, isEmailVerified, userEmail)
     */
    suspend fun getUserVerificationInfo(): Triple<Boolean, Boolean, String?> {
        val result = checkEmailVerificationStatus()
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        return Triple(isLoggedIn, result.isVerified, result.userEmail)
    }

    /**
     * Composable helper function to check email verification with loading state
     */
    @androidx.compose.runtime.Composable
    fun rememberEmailVerificationState(): EmailVerificationState {
        var state by remember { mutableStateOf(EmailVerificationState()) }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            state = state.copy(isLoading = true)
            val result = checkEmailVerificationStatus()
            state = EmailVerificationState(
                isVerified = result.isVerified,
                userEmail = result.userEmail,
                errorMessage = result.errorMessage,
                isLoading = false
            )
        }

        return state
    }
}

/**
 * Data class to hold email verification state for Compose
 */
data class EmailVerificationState(
    val isVerified: Boolean = false,
    val userEmail: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

/**
 * Composable function to show email verification requirement dialog
 */
@androidx.compose.runtime.Composable
fun EmailVerificationRequiredDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onNavigateToVerification: () -> Unit,
    title: String = "Email Verification Required",
    message: String = "Please verify your email address to use this feature."
) {
    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                androidx.compose.material3.Text(
                    text = title,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(
                        text = message,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.ui.Modifier.height(8.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "📧 Verify your email to continue using all features securely.",
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF1976D2)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        onDismiss()
                        onNavigateToVerification()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFFC107),
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    )
                ) {
                    androidx.compose.material3.Text("Verify Email")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = onDismiss
                ) {
                    androidx.compose.material3.Text(
                        "Later",
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}