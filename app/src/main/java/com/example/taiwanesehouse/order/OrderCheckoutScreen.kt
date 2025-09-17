package com.example.taiwanesehouse.order

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taiwanesehouse.BottomNavigationBar
import com.example.taiwanesehouse.FirebaseCartManager
import com.example.taiwanesehouse.admin.FirebaseOrderManager
import com.example.taiwanesehouse.cart.CartDataManager
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.payment.FirebasePaymentManager
import com.example.taiwanesehouse.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCheckoutScreen(
    navController: NavController,
    cartManager: FirebaseCartManager = remember { FirebaseCartManager() },
    orderManager: FirebaseOrderManager = remember { FirebaseOrderManager() },
    paymentManager: FirebasePaymentManager = run {
        val ctx = LocalContext.current
        remember(ctx) { FirebasePaymentManager(ctx) }
    }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val orderViewModel: OrderViewModel = viewModel()

    // Get cart data: prefer CartDataManager if set, else fall back to live Firebase cart
    // Use locked items from OrderDataManager
    val cartItems: List<CartItem> = OrderDataManager.getItems()
    val subtotal: Double = OrderDataManager.getSubtotal()
    var coinsUsed by remember { mutableStateOf(OrderDataManager.getCoinsUsed()) }
    var coinDiscount by remember { mutableStateOf(OrderDataManager.getCoinDiscount()) }
    var finalTotal by remember { mutableStateOf(OrderDataManager.getFinalTotal()) }
    var voucherDiscount by remember { mutableStateOf(OrderDataManager.getVoucherDiscount()) }
    var selectedVoucher by remember { mutableStateOf(OrderDataManager.getAppliedVoucher()) }
    var isVoucherDialogOpen by remember { mutableStateOf(false) }
    var showOrderConfirmDialog by remember { mutableStateOf(false) }

    val availableVouchers = remember {
        listOf(
            OrderDataManager.Voucher.Flat(amount = 5.0, minimumSpend = 30.0, label = "RM5 OFF >= RM30"),
            OrderDataManager.Voucher.Flat(amount = 15.0, minimumSpend = 100.0, label = "RM15 OFF >= RM100"),
            OrderDataManager.Voucher.Percent(percent = 0.20, cap = 15.0, minimumSpend = 50.0, label = "20% OFF, cap RM15, >= RM50")
        )
    }

    // Voucher picker dialog with better UI
    if (isVoucherDialogOpen) {
        AlertDialog(
            onDismissRequest = { isVoucherDialogOpen = false },
            confirmButton = {
                TextButton(onClick = { isVoucherDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = "🎟️ Choose Voucher",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select a voucher to apply discount:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    availableVouchers.forEach { v ->
                        val isEligible = subtotal >= v.minSpend

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isEligible) {
                                    if (isEligible) {
                                        selectedVoucher = v
                                        OrderDataManager.setVoucher(selectedVoucher)
                                        voucherDiscount = OrderDataManager.getVoucherDiscount()
                                        coinDiscount = OrderDataManager.getCoinDiscount()
                                        finalTotal = OrderDataManager.getFinalTotal()
                                        isVoucherDialogOpen = false
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEligible) Color.White else Color.Gray.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isEligible) 2.dp else 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = v.code,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEligible) Color.Black else Color.Gray
                                )
                                Text(
                                    text = if (isEligible) "✅ Eligible" else "❌ Minimum spend: RM${v.minSpend}",
                                    fontSize = 12.sp,
                                    color = if (isEligible) Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    // Order and payment state
    val isLoading by orderManager.isLoading.collectAsState()
    val orderCreated by orderViewModel.orderCreated.collectAsState()
    var isProcessingPayment by remember { mutableStateOf(false) }
    val memberCoins by cartManager.memberCoins.collectAsState()

    // Only payment selection; no customer info for dine-in/takeaway app
    var customerPhone by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("cash") }
    var phoneInput by remember { mutableStateOf("") }

    // Order confirmation dialog
    if (showOrderConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOrderConfirmDialog = false },
            title = {
                Text(
                    text = "🛒 Confirm Order",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to place this order?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: RM %.2f".format(finalTotal),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107)
                    )
                    if (selectedPaymentMethod == "cash") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💵 You will pay at the counter",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOrderConfirmDialog = false
                        scope.launch {
                            try {
                                // Create order with cart data integration
                                val orderItems = cartItems.map { cartItem ->
                                    val addOnCost = cartItem.foodAddOns.sumOf { addOn ->
                                        when (addOn.lowercase()) {
                                            "egg" -> 1.0
                                            "vegetable" -> 2.0
                                            else -> 0.0
                                        }
                                    }

                                    OrderItemEntity(
                                        orderId = "", // Will be set when order is created
                                        foodName = cartItem.foodName,
                                        basePrice = cartItem.basePrice,
                                        quantity = cartItem.foodQuantity,
                                        addOns = cartItem.foodAddOns,
                                        removals = cartItem.foodRemovals,
                                        itemTotalPrice = (cartItem.basePrice + addOnCost) * cartItem.foodQuantity,
                                        imageRes = cartItem.imagesRes
                                    )
                                }

                                orderViewModel.createOrderWithCartData(
                                    cartItems = cartItems,
                                    orderItems = orderItems,
                                    customerName = "",
                                    customerEmail = "",
                                    customerPhone = customerPhone,
                                    deliveryAddress = null,
                                    notes = null,
                                    paymentMethod = selectedPaymentMethod,
                                    subtotal = subtotal,
                                    coinDiscount = coinDiscount,
                                    finalTotal = finalTotal,
                                    coinsUsed = coinsUsed
                                )

                                // If cash, assume immediate success and finalize here
                                if (selectedPaymentMethod == "cash") {
                                    val userId = auth.currentUser?.uid
                                    // Deduct coins used
                                    if (userId != null && coinsUsed > 0) {
                                        try {
                                            com.example.taiwanesehouse.repository.UserRepository(
                                                com.example.taiwanesehouse.database.AppDatabase.getDatabase(context).userDao()
                                            ).deductCoinsFromUser(userId, coinsUsed)
                                        } catch (_: Exception) {}
                                    }
                                    // Award coins based on subtotal before discounts
                                    val coinsEarned = kotlin.math.floor(subtotal).toInt()
                                    if (userId != null && coinsEarned > 0) {
                                        try {
                                            com.example.taiwanesehouse.repository.UserRepository(
                                                com.example.taiwanesehouse.database.AppDatabase.getDatabase(context).userDao()
                                            ).addCoinsToUser(userId, coinsEarned)
                                        } catch (_: Exception) {}
                                    }
                                    // Refresh local coins from Firestore
                                    try {
                                        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId ?: "").get().await()
                                        val latestCoins = doc.getLong("coins")?.toInt() ?: 0
                                        com.example.taiwanesehouse.database.AppDatabase
                                            .getDatabase(context)
                                            .userDao()
                                            .updateUserCoins(userId ?: "", latestCoins)
                                    } catch (_: Exception) {}

                                    Toast.makeText(context, "Order placed successfully! 🎉", Toast.LENGTH_LONG).show()
                                    // Clear cart and order state
                                    CartDataManager.clear()
                                    cartManager.clearCart()
                                    OrderDataManager.clear()
                                    navController.navigate(com.example.taiwanesehouse.enumclass.Screen.Menu.name)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Confirm Order", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore last pending order if app restarted and manager is empty
    LaunchedEffect(Unit) {
        if (OrderDataManager.isEmpty()) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val snapshot = FirebaseFirestore.getInstance().collection("orders")
                        .whereEqualTo("userId", uid)
                        .whereEqualTo("paymentStatus", "pending")
                        .orderBy("orderDate", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()

                    val doc = snapshot.documents.firstOrNull()
                    val data = doc?.data
                    if (data != null) {
                        val orderItems = (data["orderItems"] as? List<Map<String, Any>>).orEmpty()
                        val restoredItems = orderItems.map { item ->
                            CartItem(
                                foodName = item["foodName"] as? String ?: "",
                                basePrice = (item["basePrice"] as? Number)?.toDouble() ?: 0.0,
                                foodQuantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                foodAddOns = (item["addOns"] as? List<String>) ?: emptyList(),
                                foodRemovals = (item["removals"] as? List<String>) ?: emptyList(),
                                imagesRes = (item["imageRes"] as? Number)?.toInt() ?: 0
                            )
                        }
                        val restoredCoinsUsed = (data["coinsUsed"] as? Number)?.toInt() ?: 0
                        val restoredSubtotal = (data["subtotalAmount"] as? Number)?.toDouble()
                        val restoredFinalTotal = (data["totalAmount"] as? Number)?.toDouble()
                        val restoredCoinDiscount = (data["coinDiscount"] as? Number)?.toDouble()

                        OrderDataManager.restoreFromPersistedOrder(
                            restoredItems,
                            restoredCoinsUsed,
                            restoredSubtotal,
                            restoredCoinDiscount,
                            restoredFinalTotal
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Handle order creation success - now proceed to payment
    LaunchedEffect(orderCreated) {
        orderCreated?.let { orderId ->
            scope.launch {
                try {
                    isProcessingPayment = true

                    val paymentCreateResult = paymentManager.createPayment(
                        orderId = orderId,
                        amount = finalTotal, // Use final total with discounts
                        paymentMethod = selectedPaymentMethod
                    )

                    if (paymentCreateResult.isSuccess) {
                        val payment = paymentCreateResult.getOrNull()!!

                        val normalizedMethod = when (selectedPaymentMethod) {
                            "cash" -> "counter"
                            else -> selectedPaymentMethod
                        }

                        val paymentDetails = when (normalizedMethod) {
                            "card" -> mapOf(
                                "cardNumber" to "****",
                                "expiryDate" to "**/**",
                                "cvv" to "***"
                            )
                            "ewallet" -> mapOf(
                                "walletType" to "TNG",
                                "phoneNumber" to (if (phoneInput.isNotBlank()) phoneInput else customerPhone)
                            )
                            "online" -> mapOf(
                                "gateway" to "stripe",
                                "redirectUrl" to "taiwanesehouse://payment/success"
                            )
                            else -> emptyMap()
                        }

                        // Navigate to dedicated forms for card/ewallet; process inline for cash/online
                        if (normalizedMethod == "card") {
                            isProcessingPayment = false
                            navController.navigate("CardPayment/${orderId}/${finalTotal.toFloat()}")
                            orderViewModel.clearOrderCreated()
                            return@launch
                        } else if (normalizedMethod == "ewallet") {
                            isProcessingPayment = false
                            navController.navigate("EwalletPayment/${orderId}/${finalTotal.toFloat()}")
                            orderViewModel.clearOrderCreated()
                            return@launch
                        }

                        val paymentResult = paymentManager.processPayment(payment.copy(paymentMethod = normalizedMethod), paymentDetails)

                        if (paymentResult.isSuccess) {
                            // Deduct coins after success
                            val userId = auth.currentUser?.uid
                            if (userId != null && coinsUsed > 0) {
                                try {
                                    com.example.taiwanesehouse.repository.UserRepository(
                                        com.example.taiwanesehouse.database.AppDatabase.getDatabase(context).userDao()
                                    ).deductCoinsFromUser(userId, coinsUsed)
                                } catch (_: Exception) {}
                            }

                            // Award coins: 1 coin = RM 1 spent (use floor of subtotal before discount)
                            val coinsEarned = kotlin.math.floor(subtotal).toInt()
                            if (userId != null && coinsEarned > 0) {
                                try {
                                    com.example.taiwanesehouse.repository.UserRepository(
                                        com.example.taiwanesehouse.database.AppDatabase.getDatabase(context).userDao()
                                    ).addCoinsToUser(userId, coinsEarned)
                                } catch (_: Exception) {}
                            }

                            // Force-refresh coins into Room from Firestore so UI updates immediately
                            try {
                                val doc = FirebaseFirestore.getInstance().collection("users").document(userId ?: "").get().await()
                                val latestCoins = doc.getLong("coins")?.toInt() ?: 0
                                com.example.taiwanesehouse.database.AppDatabase
                                    .getDatabase(context)
                                    .userDao()
                                    .updateUserCoins(userId ?: "", latestCoins)
                            } catch (_: Exception) {}
                            Toast.makeText(context, "Order placed successfully! Order ID: $orderId", Toast.LENGTH_LONG).show()

                            // Clear cart data after successful order
                            CartDataManager.clear()
                            cartManager.clearCart()
                            // Clear locked order data
                            OrderDataManager.clear()

                            // Navigate to order confirmation
                            navController.navigate(Screen.Menu.name)
                            orderViewModel.clearOrderCreated()
                        } else {
                            Toast.makeText(context, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create payment record", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Payment error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingPayment = false
                }
            }
        }
    }

    // Show empty cart message with BottomNavigationBar
    if (cartItems.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🧾 Checkout",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.Black
                            )
                        }
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFFC107)
                    )
                )
            },
            bottomBar = { BottomNavigationBar(navController = navController) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🧺 Your cart is empty",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add some delicious items to get started!",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Menu.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "🍽️ Explore Menu",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧾 Checkout",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.Black
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(text = "⬅️", fontSize = 20.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFFFC107)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Summary with enhanced UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Order Summary",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Surface(
                            color = Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "${cartItems.size} items",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display cart items with better spacing
                    cartItems.forEachIndexed { index, cartItem ->
                        OrderSummaryItem(cartItem = cartItem)
                        if (index < cartItems.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced pricing breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 16.sp, color = Color.Black)
                            Text("RM %.2f".format(subtotal), fontSize = 16.sp, color = Color.Black)
                        }

                        if (voucherDiscount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🎟️ Voucher Discount:", fontSize = 16.sp, color = Color(0xFF1976D2))
                                Text("-RM %.2f".format(voucherDiscount), fontSize = 16.sp, color = Color(0xFF1976D2))
                            }
                        }

                        if (coinDiscount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🪙 Coin Discount ($coinsUsed coins):", fontSize = 16.sp, color = Color(0xFF4CAF50))
                                Text("-RM %.2f".format(coinDiscount), fontSize = 16.sp, color = Color(0xFF4CAF50))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Amount:",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "RM %.2f".format(finalTotal),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
                            )
                        }
                    }

                    // Show applied voucher info
                    if (selectedVoucher != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🎟️ ${selectedVoucher?.code}",
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Enhanced Coin Usage Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙 Use Your Coins",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Surface(
                            color = Color(0xFFFFE082),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "1 coin = RM0.10",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = Color(0xFF6D4C41),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Available coins display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Available Balance:",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "🪙 $memberCoins coins (≈ RM %.2f)".format(memberCoins * 0.10),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Coin selector with enhanced controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coins to use:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Decrease button
                            Button(
                                onClick = {
                                    coinsUsed = (coinsUsed - 1).coerceAtLeast(0)
                                    OrderDataManager.setCoinsUsed(coinsUsed)
                                    coinDiscount = OrderDataManager.getCoinDiscount()
                                    voucherDiscount = OrderDataManager.getVoucherDiscount()
                                    finalTotal = OrderDataManager.getFinalTotal()
                                },
                                enabled = coinsUsed > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("➖", fontSize = 16.sp, color = Color.Black)
                            }

                            // Current coins display
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.widthIn(min = 60.dp)
                            ) {
                                Text(
                                    text = "$coinsUsed",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Increase button
                            Button(
                                onClick = {
                                    val maxBySubtotal = OrderDataManager.getMaxCoinsBySubtotal()
                                    val maxAllowed = minOf(maxBySubtotal, memberCoins)
                                    coinsUsed = (coinsUsed + 1).coerceAtMost(maxAllowed)
                                    OrderDataManager.setCoinsUsed(coinsUsed)
                                    coinDiscount = OrderDataManager.getCoinDiscount()
                                    voucherDiscount = OrderDataManager.getVoucherDiscount()
                                    finalTotal = OrderDataManager.getFinalTotal()
                                },
                                enabled = coinsUsed < minOf(OrderDataManager.getMaxCoinsBySubtotal(), memberCoins),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("➕", fontSize = 16.sp, color = Color.Black)
                            }

                            // Max button - NEW FEATURE
                            Button(
                                onClick = {
                                    val maxBySubtotal = OrderDataManager.getMaxCoinsBySubtotal()
                                    val maxAllowed = minOf(maxBySubtotal, memberCoins)
                                    coinsUsed = maxAllowed
                                    OrderDataManager.setCoinsUsed(coinsUsed)
                                    coinDiscount = OrderDataManager.getCoinDiscount()
                                    voucherDiscount = OrderDataManager.getVoucherDiscount()
                                    finalTotal = OrderDataManager.getFinalTotal()
                                },
                                enabled = coinsUsed < minOf(OrderDataManager.getMaxCoinsBySubtotal(), memberCoins),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "MAX",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Show discount amount and max limit info
                    if (coinsUsed > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFE8F5E8),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "💰 You'll save RM %.2f with $coinsUsed coins".format(coinDiscount),
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Show max coins info
                    val maxCoins = minOf(OrderDataManager.getMaxCoinsBySubtotal(), memberCoins)
                    if (maxCoins > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 You can use up to $maxCoins coins for this order",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Enhanced Voucher Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🎟️ Vouchers & Discounts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedVoucher != null) {
                                Text(
                                    text = selectedVoucher!!.code,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                                if (voucherDiscount > 0) {
                                    Text(
                                        text = "✅ Discount: -RM %.2f".format(voucherDiscount),
                                        color = Color(0xFF1976D2),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "No voucher selected",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Tap 'Choose' to save more!",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isVoucherDialogOpen = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF1976D2)
                                )
                            ) {
                                Text("Choose", fontWeight = FontWeight.Medium)
                            }

                            if (selectedVoucher != null) {
                                OutlinedButton(
                                    onClick = {
                                        selectedVoucher = null
                                        OrderDataManager.setVoucher(null)
                                        voucherDiscount = OrderDataManager.getVoucherDiscount()
                                        coinDiscount = OrderDataManager.getCoinDiscount()
                                        finalTotal = OrderDataManager.getFinalTotal()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.Gray
                                    )
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }

            // Status Banner with Better Design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏳", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Order Status: Pending",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8D6E63)
                        )
                        Text(
                            text = "Complete payment to confirm your order",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Enhanced Payment Method Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "💳 Choose Payment Method",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment options with better styling
                    val paymentOptions = listOf(
                        Triple("cash", "💵", "Pay at Counter"),
                        Triple("card", "💳", "Card Payment"),
                        Triple("ewallet", "📱", "E-Wallet (TNG/GrabPay)")
                    )

                    paymentOptions.forEach { (method, icon, label) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == method)
                                    Color(0xFFE3F2FD) else Color(0xFFF8F9FA)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selectedPaymentMethod == method) 4.dp else 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == method,
                                    onClick = { selectedPaymentMethod = method },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF1976D2)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    when (method) {
                                        "cash" -> Text(
                                            text = "Quick and easy - pay when you collect",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        "card" -> Text(
                                            text = "Secure card payment processing",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        "ewallet" -> Text(
                                            text = "Touch 'n Go or GrabPay",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // E-wallet phone input
                    if (selectedPaymentMethod == "ewallet") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { new ->
                                phoneInput = new.filter { it.isDigit() }.take(11)
                            },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            placeholder = { Text("0123456789") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("🇲🇾 +60", fontSize = 14.sp) },
                            supportingText = {
                                Text(
                                    text = "Enter your registered e-wallet phone number",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            isError = selectedPaymentMethod == "ewallet" && phoneInput.length < 10
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced Place Order Button
            Button(
                onClick = {
                    // Validation for e-wallet
                    if (selectedPaymentMethod == "ewallet" && phoneInput.length < 10) {
                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    showOrderConfirmDialog = true
                },
                enabled = !isLoading && !isProcessingPayment && cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Creating Order...", fontSize = 16.sp, color = Color.White)
                    }
                    isProcessingPayment -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing Payment...", fontSize = 16.sp, color = Color.White)
                    }
                    else -> {
                        val buttonText = when (selectedPaymentMethod) {
                            "card" -> "💳 Pay with Card - RM %.2f".format(finalTotal)
                            "ewallet" -> "📱 Pay with E-wallet - RM %.2f".format(finalTotal)
                            "online" -> "🌐 Pay Online - RM %.2f".format(finalTotal)
                            "cash" -> "💵 Order & Pay at Counter - RM %.2f".format(finalTotal)
                            else -> "🛒 Place Order - RM %.2f".format(finalTotal)
                        }

                        Text(
                            text = buttonText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Add some bottom padding for better scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OrderSummaryItem(cartItem: CartItem) {
    val addOnCost = cartItem.foodAddOns.sumOf { addOn ->
        when (addOn.lowercase()) {
            "egg" -> 1.0
            "vegetable" -> 2.0
            else -> 0.0
        }
    }
    val itemTotal = (cartItem.basePrice + addOnCost) * cartItem.foodQuantity

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cartItem.foodName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            if (cartItem.foodAddOns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFE8F5E8),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "➕ ${cartItem.foodAddOns.joinToString(", ")}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            if (cartItem.foodRemovals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "➖ ${cartItem.foodRemovals.joinToString(", ")}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color(0xFFE57373)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Qty: ${cartItem.foodQuantity}",
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "RM %.2f".format(itemTotal),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (addOnCost > 0) {
                Text(
                    text = "(+RM %.2f add-ons)".format(addOnCost * cartItem.foodQuantity),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}