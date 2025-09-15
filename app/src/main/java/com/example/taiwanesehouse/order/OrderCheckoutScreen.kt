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

    // Get cart data from CartDataManager
    val cartItems = CartDataManager.getCartItems()
    val subtotal = CartDataManager.getSubtotal()
    val coinDiscount = CartDataManager.getCoinDiscount()
    val finalTotal = CartDataManager.getFinalTotal()
    val coinsUsed = CartDataManager.getCoinsUsed()

    // Order and payment state
    val isLoading by orderManager.isLoading.collectAsState()
    val orderCreated by orderViewModel.orderCreated.collectAsState()
    var isProcessingPayment by remember { mutableStateOf(false) }

    // Form state
    var customerName by remember { mutableStateOf(auth.currentUser?.displayName ?: "") }
    var customerEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
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

            // Customer Information (same as before)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "👤 Customer Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerEmail,
                        onValueChange = { customerEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Delivery Address (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Special Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
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
                        Text("💵 Cash on Delivery", fontSize = 16.sp)
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
                            if (customerName.isBlank() || customerEmail.isBlank() || customerPhone.isBlank()) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

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
                                    imageRes = cartItem.imageRes
                                )
                            }

                            orderViewModel.createOrderWithCartData(
                                cartItems = cartItems,
                                orderItems = orderItems,
                                customerName = customerName,
                                customerEmail = customerEmail,
                                customerPhone = customerPhone,
                                deliveryAddress = deliveryAddress.ifBlank { null },
                                notes = notes.ifBlank { null },
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