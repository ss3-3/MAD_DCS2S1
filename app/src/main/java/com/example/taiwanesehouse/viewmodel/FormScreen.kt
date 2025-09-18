package com.example.taiwanesehouse.viewmodel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun FormScreen(
    innerPadding: PaddingValues,
    uiState: com.example.taiwanesehouse.viewmodel.FeedbackUIState,
    formState: com.example.taiwanesehouse.viewmodel.FeedbackFormState,
    feedbackTypes: List<Pair<String, String>>,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onUpdateRating: (Int) -> Unit,
    onUpdateFeedbackType: (String) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateMessage: (String) -> Unit,
    onUpdateContactEmail: (String) -> Unit,
    onSubmitFeedback: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header
                    Text(
                        text = "We Value Your Feedback",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Help us improve by sharing your experience with Taiwanese House",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Status warnings
                    if (uiState.connectionState == ConnectionState.OFFLINE) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "💾 Offline mode. Your feedback will be saved locally and synced when you're back online.",
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (uiState.connectionState == ConnectionState.ONLINE && uiState.remainingSubmissions <= 1 && uiState.canSubmitFeedback) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF8E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "⚠️ You have ${uiState.remainingSubmissions} feedback submission remaining this week.",
                                fontSize = 12.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else if (!uiState.canSubmitFeedback && uiState.connectionState == ConnectionState.ONLINE) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF0F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "❌ You've reached the weekly feedback limit (3 submissions). Please try again next week.",
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Rating section
                    Text(
                        text = "Overall Rating *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        repeat(5) { index ->
                            IconButton(
                                onClick = { onUpdateRating(index + 1) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (index < formState.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Rate ${index + 1} star${if (index != 0) "s" else ""}",
                                    tint = if (index < formState.rating) Color(0xFFFFC107) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        if (formState.rating > 0) {
                            Text(
                                text = "(${formState.rating}/5)",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // Feedback type selection
                    Text(
                        text = "Feedback Category",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(feedbackTypes.size) { index ->
                            val (type, label) = feedbackTypes[index]
                            FilterChip(
                                onClick = { onUpdateFeedbackType(type) },
                                label = { Text(label, fontSize = 12.sp) },
                                selected = formState.feedbackType == type,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFC107),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Feedback title
                    OutlinedTextField(
                        value = formState.title,
                        onValueChange = onUpdateTitle,
                        label = { Text("Feedback Title *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !uiState.isSubmitting,
                        placeholder = { Text("Brief summary of your feedback") },
                        isError = uiState.errorMessage.contains("title", ignoreCase = true)
                    )

                    // Feedback message
                    OutlinedTextField(
                        value = formState.message,
                        onValueChange = onUpdateMessage,
                        label = { Text("Your Feedback *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !uiState.isSubmitting,
                        placeholder = { Text("Please share your detailed feedback, suggestions, or concerns...") },
                        isError = uiState.errorMessage.contains("message", ignoreCase = true)
                    )

                    // Character count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${formState.message.length}/500",
                            fontSize = 12.sp,
                            color = if (formState.message.length > 500) Color.Red else Color.Gray
                        )
                    }

                    // Contact email for non-authenticated users
                    if (currentUser == null) {
                        OutlinedTextField(
                            value = formState.contactEmail,
                            onValueChange = onUpdateContactEmail,
                            label = { Text("Contact Email (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            enabled = !uiState.isSubmitting,
                            placeholder = { Text("your.email@example.com") }
                        )
                    }

                    // Error message
                    if (uiState.errorMessage.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF0F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Submit button
                    Button(
                        onClick = onSubmitFeedback,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        ),
                        enabled = !uiState.isSubmitting &&
                                formState.rating > 0 &&
                                formState.title.trim().isNotEmpty() &&
                                formState.message.trim().isNotEmpty() &&
                                formState.message.length <= 500 &&
                                currentUser != null &&
                                (uiState.connectionState == ConnectionState.OFFLINE || uiState.canSubmitFeedback)
                    ) {
                        if (uiState.isSubmitting) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submitting...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = when {
                                    currentUser == null -> "Please Log In"
                                    !uiState.canSubmitFeedback && uiState.connectionState == ConnectionState.ONLINE -> "Weekly Limit Reached"
                                    uiState.connectionState == ConnectionState.OFFLINE -> "Save Locally"
                                    else -> "Submit Feedback"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel button
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isSubmitting
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            // Privacy notice
            Text(
                text = "Your feedback helps us improve our service. We respect your privacy and will only use this information to enhance your experience at Taiwanese House.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}