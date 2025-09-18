package com.example.taiwanesehouse.viewmodel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.taiwanesehouse.user_profile.FeedbackItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EnhancedFeedbackHistoryCard(feedback: FeedbackItem) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val statusColor = when (feedback.status) {
        "pending" -> Color(0xFFFF9800)
        "reviewed" -> Color(0xFF2196F3)
        "resolved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with status and sync indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feedback.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync status indicator
                    if (!feedback.isSynced) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            )
                        ) {
                            Text(
                                text = "LOCAL",
                                fontSize = 10.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Status indicator
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = statusColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = feedback.status.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Rating and category
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < feedback.rating) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (index < feedback.rating) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = " • ${feedback.feedbackType.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Message preview
            Text(
                text = feedback.message,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                maxLines = 3,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Timestamp with sync info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = try {
                        dateFormat.format(Date(feedback.timestamp))
                    } catch (e: Exception) {
                        "Date unavailable"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (!feedback.isSynced) {
                    Text(
                        text = "Pending sync",
                        fontSize = 10.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}