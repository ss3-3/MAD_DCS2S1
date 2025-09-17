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
import java.util.Date

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
    val availableVouchers = remember {
        listOf(
            OrderDataManager.Voucher.Flat(amount = 5.0, minSpend = 30.0, label = "RM5 OFF >= RM30"),
            OrderDataManager.Voucher.Flat(amount = 15.0, minSpend = 100.0, label = "RM15 OFF >= RM100"),
            OrderDataManager.Voucher.Percent(percent = 0.20, cap = 15.0, minSpend = 50.0, label = "20% OFF, cap RM15, >= RM50")
        )
    }

    // Voucher picker dialog
    if (isVoucherDialogOpen) {
        AlertDialog(
            onDismissRequest = { isVoucherDialogOpen = false },
            confirmButton = {},
            title = { Text("Choose Voucher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableVouchers.forEach { v ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoucher = v
                                    OrderDataManager.setVoucher(selectedVoucher)
                                    voucherDiscount = OrderDataManager.getVoucherDiscount()
                                    coinDiscount = OrderDataManager.getCoinDiscount()
                                    finalTotal = OrderDataManager.getFinalTotal()
                                    isVoucherDialogOpen = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = v.code,
                                modifier = Modifier.padding(12.dp),
                                color = Color.Black
                            )
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

    // For cash: assume success immediately (no pending) and finalize inline
    LaunchedEffect(selectedPaymentMethod) {
        // No-op here; handled on button click below
    }

    // Show empty cart message
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
                        text = "\uD83E\uDDFE Your order is empty",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Menu.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                    ) {
                        Text("Continue explore menu \uD83C\uDF7D\uFE0F", color = Color.White)
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
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
                .padding(16.dp)
        ) {
            // Order Summary with Cart Integration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📋 Order Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display cart items
                    cartItems.forEach { cartItem ->
                        OrderSummaryItem(cartItem = cartItem)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pricing breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", fontSize = 16.sp, color = Color.Black)
                        Text("RM %.2f".format(subtotal), fontSize = 16.sp, color = Color.Black)
                    }

                    if (voucherDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Voucher:", fontSize = 16.sp, color = Color(0xFF1976D2))
                            Text("-RM %.2f".format(voucherDiscount), fontSize = 16.sp, color = Color(0xFF1976D2))
                        }
                    }

                    if (coinDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Coin Discount ($coinsUsed coins):", fontSize = 16.sp, color = Color.Green)
                            Text("-RM %.2f".format(coinDiscount), fontSize = 16.sp, color = Color.Green)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Amount:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "💰 RM %.2f".format(finalTotal),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                    }

                    // Show applied voucher label under the total
                    if (selectedVoucher != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Voucher: ${selectedVoucher?.code}",
                            fontSize = 12.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Coin selector (1 coin = RM0.10)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎁 Use Coins",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Coins:", color = Color.Black)
                            Text(
                                text = "Available: $memberCoins (RM %.2f)".format(memberCoins * 0.10),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                coinsUsed = (coinsUsed - 1).coerceAtLeast(0)
                                OrderDataManager.setCoinsUsed(coinsUsed)
                                coinDiscount = OrderDataManager.getCoinDiscount()
                                voucherDiscount = OrderDataManager.getVoucherDiscount()
                                finalTotal = OrderDataManager.getFinalTotal()
                            }) { Text("➖") }
                            Text("$coinsUsed", color = Color.Black)
                            TextButton(onClick = {
                                // Max by (subtotal - voucher) and available coins
                                val maxBySubtotal = OrderDataManager.getMaxCoinsBySubtotal()
                                val maxAllowed = minOf(maxBySubtotal, memberCoins)
                                coinsUsed = (coinsUsed + 1).coerceAtMost(maxAllowed)
                                OrderDataManager.setCoinsUsed(coinsUsed)
                                coinDiscount = OrderDataManager.getCoinDiscount()
                                voucherDiscount = OrderDataManager.getVoucherDiscount()
                                finalTotal = OrderDataManager.getFinalTotal()
                            }) { Text("➕") }
                        }
                    }
                    Text("Discount: -RM %.2f".format(coinDiscount), color = Color(0xFF4CAF50))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voucher selector (separated section)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎟️ Voucher", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedVoucher?.code ?: "No voucher selected",
                                color = Color.Black
                            )
                            if (voucherDiscount > 0) {
                                Text(
                                    text = "Applied: -RM %.2f".format(voucherDiscount),
                                    color = Color(0xFF1976D2),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { isVoucherDialogOpen = true }) { Text("Choose") }
                            TextButton(onClick = {
                                selectedVoucher = null
                                OrderDataManager.setVoucher(null)
                                voucherDiscount = OrderDataManager.getVoucherDiscount()
                                coinDiscount = OrderDataManager.getCoinDiscount()
                                finalTotal = OrderDataManager.getFinalTotal()
                            }) { Text("Clear") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Text(
                    text = "Order status: pending • Please proceed to payment",
                    color = Color(0xFF8D6E63),
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Method (same as before)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💳 Payment Method",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == "cash",
                            onClick = { selectedPaymentMethod = "cash" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💵 Pay at Counter", fontSize = 16.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == "card",
                            onClick = { selectedPaymentMethod = "card" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💳 Card Payment", fontSize = 16.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == "ewallet",
                            onClick = { selectedPaymentMethod = "ewallet" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📱 E-Wallet (TNG/GrabPay)", fontSize = 16.sp)
                    }

                    if (selectedPaymentMethod == "ewallet") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { new ->
                                phoneInput = new.filter { it.isDigit() }.take(11)
                            },
                            label = { Text("Phone Number (+60)") },
                            singleLine = true,
                            placeholder = { Text("0123456789") }
                        )
                    }

//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        RadioButton(
//                            selected = selectedPaymentMethod == "online",
//                            onClick = { selectedPaymentMethod = "online" }
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("🌐 Online Payment", fontSize = 16.sp)
//                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Place Order Button
            Button(
                onClick = {
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

                                Toast.makeText(context, "Order placed successfully! (Cash)", Toast.LENGTH_LONG).show()
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
                },
                enabled = !isLoading && !isProcessingPayment && cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Order...", fontSize = 16.sp, color = Color.White)
                    }
                    isProcessingPayment -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Payment...", fontSize = 16.sp, color = Color.White)
                    }
                    else -> {
                        val buttonText = when (selectedPaymentMethod) {
                            "card" -> "💳 Pay with Card (RM %.2f)".format(finalTotal)
                            "ewallet" -> "📱 Pay with E-wallet (RM %.2f)".format(finalTotal)
                            "online" -> "🌐 Pay Online (RM %.2f)".format(finalTotal)
                            "cash" -> "💵 Order & Pay Cash (RM %.2f)".format(finalTotal)
                            else -> "🛒 Place Order (RM %.2f)".format(finalTotal)
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

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.foodName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                if (cartItem.foodAddOns.isNotEmpty()) {
                    Text(
                        text = "Add-ons: ${cartItem.foodAddOns.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (cartItem.foodRemovals.isNotEmpty()) {
                    Text(
                        text = "Remove: ${cartItem.foodRemovals.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }

                Text(
                    text = "Qty: ${cartItem.foodQuantity}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "RM %.2f".format(itemTotal),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}