package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taiwanesehouse.FirebaseCartManager
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.enumclass.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var payments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Order preview and reorder state
    var isOrderDialogOpen by remember { mutableStateOf(false) }
    var isOrderDialogLoading by remember { mutableStateOf(false) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var selectedOrderItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Cart manager for reordering
    val cartManager = remember { FirebaseCartManager() }

    // Check orientation changes
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(userId) {
        if (userId == null) {
            error = "Please log in to view payment history"
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                isLoading = true
                error = null
                val snapshot = firestore.collection("payments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // Build normalized list with required fields (ensure orderId/paymentId and sortable timestamp)
                val normalized = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val paymentId = (data["paymentId"] as? String) ?: doc.id
                    val orderId = (data["orderId"] as? String) ?: paymentId
                    val createdAtMs = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    val updatedAtMs = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: createdAtMs
                    data + mapOf(
                        "paymentId" to paymentId,
                        "orderId" to orderId,
                        "_sortTs" to updatedAtMs
                    )
                }

                // De-duplicate by orderId (fallback to paymentId) keeping the latest record
                val latestById = normalized
                    .groupBy { it["orderId"] as String }
                    .values
                    .map { group -> group.maxBy { (it["_sortTs"] as? Long) ?: 0L } }

                // Sort latest-first and limit
                payments = latestById
                    .sortedByDescending { (it["_sortTs"] as? Long) ?: 0L }
                    .take(50)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingScreen()
                }
                error != null -> {
                    ErrorScreen(error = error ?: "Unknown error")
                }
                payments.isEmpty() -> {
                    EmptyPaymentScreen()
                }
                else -> {
                    PaymentList(
                        payments = payments,
                        isLandscape = isLandscape,
                        onClickPayment = { orderId ->
                            scope.launch {
                                try {
                                    isOrderDialogLoading = true
                                    selectedOrderId = orderId
                                    isOrderDialogOpen = true

                                    // Fetch order details from Firestore
                                    val orderDoc = firestore.collection("orders").document(orderId).get().await()
                                    val data = orderDoc.data
                                    val items = (data?.get("orderItems") as? List<Map<String, Any>>).orEmpty()
                                    selectedOrderItems = items
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    isOrderDialogLoading = false
                                }
                            }
                        },
                        onOrderAgain = { orderId ->
                            scope.launch {
                                try {
                                    isOrderDialogLoading = true
                                    // Ensure we have items. If dialog not opened, fetch directly
                                    val items = if (selectedOrderId == orderId && selectedOrderItems.isNotEmpty()) {
                                        selectedOrderItems
                                    } else {
                                        val orderDoc = firestore.collection("orders").document(orderId).get().await()
                                        (orderDoc.data?.get("orderItems") as? List<Map<String, Any>>).orEmpty()
                                    }

                                    // Add each item to cart
                                    items.forEach { item ->
                                        val cartItem = CartItem(
                                            foodName = item["foodName"] as? String ?: "",
                                            basePrice = (item["basePrice"] as? Number)?.toDouble() ?: 0.0,
                                            foodQuantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                            foodAddOns = (item["addOns"] as? List<String>) ?: emptyList(),
                                            foodRemovals = (item["removals"] as? List<String>) ?: emptyList(),
                                            imagesRes = (item["imageRes"] as? Number)?.toInt() ?: 0
                                        )
                                        cartManager.addToCart(cartItem)
                                    }

                                    // Navigate to cart
                                    navController.navigate(Screen.Cart.name)
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    isOrderDialogLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Order review dialog
    if (isOrderDialogOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isOrderDialogOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedOrderId?.let { onId ->
                            // Trigger reorder
                            scope.launch {
                                isOrderDialogOpen = false
                                isOrderDialogLoading = true
                                try {
                                    val items = selectedOrderItems
                                    items.forEach { item ->
                                        val cartItem = CartItem(
                                            foodName = item["foodName"] as? String ?: "",
                                            basePrice = (item["basePrice"] as? Number)?.toDouble() ?: 0.0,
                                            foodQuantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                            foodAddOns = (item["addOns"] as? List<String>) ?: emptyList(),
                                            foodRemovals = (item["removals"] as? List<String>) ?: emptyList(),
                                            imagesRes = (item["imageRes"] as? Number)?.toInt() ?: 0
                                        )
                                        cartManager.addToCart(cartItem)
                                    }
                                    navController.navigate(Screen.Cart.name)
                                } finally {
                                    isOrderDialogLoading = false
                                }
                            }
                        }
                    }
                ) { Text("Order again") }
            },
            dismissButton = {
                TextButton(onClick = { isOrderDialogOpen = false }) { Text("Close") }
            },
            title = { Text(text = "Order ${selectedOrderId ?: ""}") },
            text = {
                if (isOrderDialogLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFFC107))
                        Spacer(Modifier.height(8.dp))
                        Text("Loading order...")
                    }
                } else if (selectedOrderItems.isEmpty()) {
                    Text("No items found for this order.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedOrderItems.forEach { item ->
                            val name = item["foodName"] as? String ?: "Item"
                            val qty = (item["quantity"] as? Number)?.toInt() ?: 1
                            val addOns = (item["addOns"] as? List<String>).orEmpty()
                            val removals = (item["removals"] as? List<String>).orEmpty()
                            Text("$name x$qty")
                            if (addOns.isNotEmpty()) Text("Add: ${addOns.joinToString(", ")}", color = Color.Gray)
                            if (removals.isNotEmpty()) Text("Remove: ${removals.joinToString(", ")}", color = Color.Gray)
                            Divider()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFFC107),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading payment history...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❌",
                    fontSize = 48.sp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun EmptyPaymentScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        Text(
                            text = "🧾",
                            fontSize = 40.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No Payment History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You haven't made any payments yet. Your payment history will appear here once you make your first order.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentList(
    payments: List<Map<String, Any>>,
    isLandscape: Boolean,
    onClickPayment: (orderId: String) -> Unit,
    onOrderAgain: (orderId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (isLandscape) 32.dp else 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header card with summary
        item {
            PaymentSummaryCard(payments = payments)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Payment items
        items(payments) { payment ->
            PaymentCard(
                payment = payment,
                isLandscape = isLandscape,
                onClick = { orderId -> onClickPayment(orderId) },
                onOrderAgain = { orderId -> onOrderAgain(orderId) }
            )
        }

        // Footer spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentSummaryCard(payments: List<Map<String, Any>>) {
    // Only sum amounts for completed payments
    val totalAmount = payments
        .filter { (it["paymentStatus"] as? String)?.lowercase() == "completed" }
        .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    val completedCount = payments.count { (it["paymentStatus"] as? String)?.lowercase() == "completed" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC107)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                title = "Total Spent",
                value = "RM %.2f".format(totalAmount),
                modifier = Modifier.weight(1f)
            )
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = Color.Black.copy(alpha = 0.2f)
            )
            SummaryItem(
                title = "Completed",
                value = "$completedCount/${payments.size}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.Black
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PaymentCard(
    payment: Map<String, Any>,
    isLandscape: Boolean,
    onClick: (orderId: String) -> Unit,
    onOrderAgain: (orderId: String) -> Unit
) {
    val amount = (payment["amount"] as? Number)?.toDouble() ?: 0.0
    val method = payment["paymentMethod"] as? String ?: "Unknown"
    val status = payment["paymentStatus"] as? String ?: "Unknown"
    val orderId = payment["orderId"] as? String ?: (payment["paymentId"] as? String ?: "N/A")
    val timestamp = payment["createdAt"] as? com.google.firebase.Timestamp

    val dateFormat = SimpleDateFormat(
        if (isLandscape) "MMM dd, yyyy HH:mm" else "MMM dd\nHH:mm",
        Locale.getDefault()
    )
    val formattedDate = timestamp?.toDate()?.let { dateFormat.format(it) } ?: "Unknown date"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = orderId != "N/A") { onClick(orderId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (isLandscape) {
            // Landscape layout - horizontal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentStatusIcon(status = status)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RM %.2f".format(amount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                    Text(
                        text = method,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    PaymentStatusChip(status = status)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.End
                    )
                }
            }
        } else {
            // Portrait layout - vertical
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PaymentStatusIcon(status = status)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "RM %.2f".format(amount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        PaymentStatusChip(status = status)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (orderId != "N/A") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Order: $orderId",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onOrderAgain(orderId) }) {
                            Text("Order again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusIcon(status: String) {
    val (emoji, color) = when (status.lowercase()) {
        "completed", "success" -> "✅" to Color(0xFF4CAF50)
        "pending" -> "⏳" to Color(0xFFFF9800)
        "failed", "error" -> "❌" to Color(0xFFE57373)
        else -> "⏳" to Color.Gray
    }

    Card(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun PaymentStatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "completed", "success" -> Color(0xFFE8F5E8) to Color(0xFF2E7D32)
        "pending" -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
        "failed", "error" -> Color(0xFFFDE8E8) to Color(0xFFD32F2F)
        else -> Color(0xFFF5F5F5) to Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}