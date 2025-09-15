package com.example.taiwanesehouse.order

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.taiwanesehouse.FirebaseCartManager
import com.example.taiwanesehouse.admin.FirebaseOrderManager
import com.example.taiwanesehouse.cart.CartDataManager
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.payment.FirebasePaymentManager
import com.example.taiwanesehouse.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
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
    val coinsUsed: Int = OrderDataManager.getCoinsUsed()
    val coinDiscount: Double = OrderDataManager.getCoinDiscount()
    val finalTotal: Double = OrderDataManager.getFinalTotal()

    // Order and payment state
    val isLoading by orderManager.isLoading.collectAsState()
    val orderCreated by orderViewModel.orderCreated.collectAsState()
    var isProcessingPayment by remember { mutableStateOf(false) }

    // Only payment selection; no customer info for dine-in/takeaway app
    var customerPhone by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("cash") }

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
                                "phoneNumber" to customerPhone
                            )
                            "online" -> mapOf(
                                "gateway" to "stripe",
                                "redirectUrl" to "taiwanesehouse://payment/success"
                            )
                            else -> emptyMap()
                        }

                        val paymentResult = paymentManager.processPayment(payment.copy(paymentMethod = normalizedMethod), paymentDetails)

                        if (paymentResult.isSuccess) {
                            // Award coins: 1 coin = RM 1 spent (use floor)
                            val coinsEarned = kotlin.math.floor(finalTotal).toInt()
                            if (coinsEarned > 0) {
                                try {
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        com.example.taiwanesehouse.repository.UserRepository(
                                            com.example.taiwanesehouse.database.AppDatabase.getDatabase(context).userDao()
                                        ).addCoinsToUser(userId, coinsEarned)
                                    }
                                } catch (_: Exception) {}
                            }
                            Toast.makeText(context, "Order placed successfully! Order ID: $orderId", Toast.LENGTH_LONG).show()

                            // Clear cart data after successful order
                            CartDataManager.clear()
                            cartManager.clearCart()

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

    // Show empty cart message
    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛒 Your cart is empty",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Screen.Menu.name) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                ) {
                    Text("Continue Shopping", color = Color.White)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == "online",
                            onClick = { selectedPaymentMethod = "online" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🌐 Online Payment", fontSize = 16.sp)
                    }
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