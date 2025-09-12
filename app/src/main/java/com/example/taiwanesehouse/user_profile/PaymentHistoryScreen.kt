package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taiwanesehouse.BottomNavigationBar
import com.example.taiwanesehouse.enumclass.PaymentMethod
import com.example.taiwanesehouse.enumclass.PaymentStatus
import java.text.SimpleDateFormat
import java.util.*

// Data class for payment history
data class PaymentHistoryItem(
    val id: String,
    val orderId: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val timestamp: Date,
    val items: List<String>,
    val transactionId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(navController: NavController) {
    // Mock payment history data
    val paymentHistory = remember {
        listOf(
            PaymentHistoryItem(
                id = "1",
                orderId = "ORD001",
                amount = 45.90,
                paymentMethod = PaymentMethod.CARD,
                status = PaymentStatus.COMPLETED,
                timestamp = Date(System.currentTimeMillis() - 86400000), // 1 day ago
                items = listOf("Beef Noodles", "Milk Tea", "Spring Rolls"),
                transactionId = "TXN123456"
            ),
            PaymentHistoryItem(
                id = "2",
                orderId = "ORD002",
                amount = 28.50,
                paymentMethod = PaymentMethod.EWALLET,
                status = PaymentStatus.COMPLETED,
                timestamp = Date(System.currentTimeMillis() - 172800000), // 2 days ago
                items = listOf("Pork Bun", "Taiwanese Tea"),
                transactionId = "TXN789012"
            ),
            PaymentHistoryItem(
                id = "3",
                orderId = "ORD003",
                amount = 67.20,
                paymentMethod = PaymentMethod.CARD,
                status = PaymentStatus.FAILED,
                timestamp = Date(System.currentTimeMillis() - 259200000), // 3 days ago
                items = listOf("Hot Pot Set", "Bubble Tea", "Dumplings"),
                transactionId = null
            ),
            PaymentHistoryItem(
                id = "4",
                orderId = "ORD004",
                amount = 32.80,
                paymentMethod = PaymentMethod.COUNTER,
                status = PaymentStatus.COMPLETED,
                timestamp = Date(System.currentTimeMillis() - 345600000), // 4 days ago
                items = listOf("Chicken Rice", "Soup", "Iced Tea"),
                transactionId = "COUNTER001"
            ),
            PaymentHistoryItem(
                id = "5",
                orderId = "ORD005",
                amount = 55.40,
                paymentMethod = PaymentMethod.EWALLET,
                status = PaymentStatus.PENDING,
                timestamp = Date(System.currentTimeMillis() - 3600000), // 1 hour ago
                items = listOf("Taiwanese Beef Stew", "Pearl Milk Tea", "Egg Roll"),
                transactionId = "TXN345678"
            )
        ).sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Payment History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { padding ->
        if (paymentHistory.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🧾",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Payment History",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                    Text(
                        "Your payment history will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Payment history list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paymentHistory) { payment ->
                    PaymentHistoryCard(payment = payment)
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(payment: PaymentHistoryItem) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with order ID and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payment.orderId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )

                PaymentStatusChip(status = payment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount and payment method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RM ${String.format("%.2f", payment.amount)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFFC107)
                )

                PaymentMethodChip(method = payment.paymentMethod)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items ordered - Using LazyRow for horizontal scrolling
            Text(
                text = "Items:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(payment.items) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🍽️ $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction ID (if available)
            payment.transactionId?.let { txnId ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔗 ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Transaction: $txnId",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Date and time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = dateFormat.format(payment.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        PaymentStatus.COMPLETED -> Triple(Color(0xFF4CAF50), Color.White, "Completed")
        PaymentStatus.PENDING -> Triple(Color(0xFFFF9800), Color.White, "Pending")
        PaymentStatus.FAILED -> Triple(Color(0xFFF44336), Color.White, "Failed")
        PaymentStatus.CANCELLED -> Triple(Color(0xFF9E9E9E), Color.White, "Cancelled")
    }

    val emoji = when (status) {
        PaymentStatus.COMPLETED -> "✅"
        PaymentStatus.PENDING -> "⏳"
        PaymentStatus.FAILED -> "❌"
        PaymentStatus.CANCELLED -> "🚫"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}

@Composable
fun PaymentMethodChip(method: PaymentMethod) {
    val (emoji, text, color) = when (method) {
        PaymentMethod.CARD -> Triple("💳", "Card", Color(0xFF2196F3))
        PaymentMethod.EWALLET -> Triple("📱", "E-Wallet", Color(0xFF9C27B0))
        PaymentMethod.COUNTER -> Triple("🏪", "Counter", Color(0xFF795548))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = color
        )
    }
}