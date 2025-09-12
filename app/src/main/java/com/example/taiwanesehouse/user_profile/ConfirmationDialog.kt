package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Enum to identify the type of confirmation needed
enum class ConfirmationType {
    UPDATE_USERNAME,
    RESET_PASSWORD,
    DELETE_ACCOUNT,
    SEND_EMAIL
}

// Data class to hold confirmation dialog parameters
data class ConfirmationDialogState(
    val type: ConfirmationType,
    val title: String,
    val message: String,
    val isVisible: Boolean = false,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

// Reusable confirmation dialog component
@Composable
fun ConfirmationDialog(
    state: ConfirmationDialogState,
    onDismissRequest: () -> Unit
) {
    if (state.isVisible) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = state.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Message
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Cancel button
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Confirm button
                        Button(
                            onClick = {
                                state.onConfirm()
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50) // Green color
                            )
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

// Success message dialog
@Composable
fun SuccessDialog(
    isVisible: Boolean,
    message: String,
    onDismissRequest: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Saved",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Message
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Close button
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green color
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}