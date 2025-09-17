package com.example.taiwanesehouse

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.viewmodel.FoodItemViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    foodItemViewModel: FoodItemViewModel = viewModel(),
    cartManager: FirebaseCartManager = remember { FirebaseCartManager() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // Collect state from Firebase
    val cartItems by cartManager.cartItems.collectAsState()
    val memberCoins by cartManager.memberCoins.collectAsState()
    val coinsToUse by cartManager.coinsToUse.collectAsState()

    var isLoading by remember { mutableStateOf(false) }

    // Calculate totals
    val subtotal = cartItems.sumOf { it.getTotalPrice() }
    val finalTotal = subtotal

    // Check authentication
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to view your cart", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Menu.name) {
                popUpTo(Screen.Cart.name) { inclusive = true }
            }
        }
    }

    // Debug logging
    LaunchedEffect(cartItems) {
        Log.d("CartScreen", "Cart items count: ${cartItems.size}")
        cartItems.forEachIndexed { index, item ->
            Log.d("CartScreen", "Item $index: ${item.foodName} - ${item.getTotalPrice()}")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "🛒 My Cart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { padding ->
        if (cartItems.isEmpty()) {
            // Empty cart state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛒",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add some delicious items to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Menu.name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107)
                        )
                    ) {
                        Text(
                            text = "🍽️ Browse Menu",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Cart with items
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { item ->
                    CartItemCard(
                        cartItem = item,
                        onQuantityChange = { newQuantity ->
                            scope.launch {
                                if (newQuantity > 0) {
                                    val success = cartManager.updateCartItemQuantityById(
                                        item.documentId,
                                        newQuantity
                                    )
                                    if (!success) {
                                        Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val success = cartManager.removeFromCartById(item.documentId)
                                    if (!success) {
                                        Toast.makeText(context, "Failed to remove item", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onEdit = { newAddOns, newRemovals ->
                            scope.launch {
                                val success = cartManager.updateCartItemConfigById(
                                    documentId = item.documentId,
                                    newAddOns = newAddOns,
                                    newRemovals = newRemovals
                                )
                                if (!success) {
                                    Toast.makeText(context, "Failed to update item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRemove = {
                            scope.launch {
                                val success = cartManager.removeFromCartById(item.documentId)
                                if (success) {
                                    Toast.makeText(context, "${item.foodName} removed from cart", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to remove item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Summary section
                item {
                    CartSummaryCard(
                        subtotal = subtotal,
                        coinDiscount = 0.0,
                        finalTotal = finalTotal
                    )
                }

                // Checkout button
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (cartItems.isNotEmpty()) {
                                isLoading = true

                                // Move items to OrderDataManager (locked) and clear Firebase cart
                                com.example.taiwanesehouse.order.OrderDataManager.appendItems(
                                    newItems = cartItems,
                                    coinsUsedNow = coinsToUse
                                )
                                // Reset coin usage slider back to 0 after transfer
                                scope.launch { cartManager.updateCoinsToUse(0) }
                                // Clear live cart after transfer
                                scope.launch { cartManager.clearCart() }

                                // Navigate to order details screen
                                navController.navigate(Screen.Order.name)
                                isLoading = false
                            }
                        },
                        enabled = !isLoading && cartItems.isNotEmpty() && finalTotal > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFDD835)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Confirm Order.",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Add some bottom padding
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: (newAddOns: List<String>, newRemovals: List<String>) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Food image
            Image(
                painter = painterResource(id = cartItem.imagesRes),
                contentDescription = cartItem.foodName,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 12.dp)
            )

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (cartItem.foodAddOns.isNotEmpty()) {
                    Text(
                        text = "➕ Add: ${cartItem.foodAddOns.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (cartItem.foodRemovals.isNotEmpty()) {
                    Text(
                        text = "➖ Remove: ${cartItem.foodRemovals.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰 RM %.2f".format(cartItem.getTotalPrice()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit button
                        TextButton(
                            onClick = { 
                                // Simple quick-edit: toggle Egg/Vegetable add-on for demo; replace with a full dialog if needed
                                val currentAddOns = cartItem.foodAddOns.toMutableList()
                                val currentRemovals = cartItem.foodRemovals.toMutableList()
                                val options = listOf("Egg", "Vegetable")
                                // For now, just flip Egg
                                if (currentAddOns.contains("Egg")) currentAddOns.remove("Egg") else currentAddOns.add("Egg")
                                onEdit(currentAddOns, currentRemovals)
                            },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "✏️",
                                fontSize = 16.sp
                            )
                        }

                        // Decrease quantity button with emoji
                        TextButton(
                            onClick = { onQuantityChange(cartItem.foodQuantity - 1) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "➖",
                                fontSize = 16.sp,
                                color = Color(0xFFFFC107)
                            )
                        }

                        Text(
                            text = "${cartItem.foodQuantity}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Increase quantity button with emoji
                        TextButton(
                            onClick = { onQuantityChange(cartItem.foodQuantity + 1) },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "➕",
                                fontSize = 16.sp,
                                color = Color(0xFFFFC107)
                            )
                        }

                        // Remove item button with emoji
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "🗑️",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSummaryCard(
    subtotal: Double,
    coinDiscount: Double,
    finalTotal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 Order Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:")
                Text("RM %.2f".format(subtotal))
            }

            if (coinDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🪙 Coin Discount:")
                    Text("-RM %.2f".format(coinDiscount), color = Color.Red)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = Color.Gray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "💰 Total:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "RM %.2f".format(finalTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFC107)
                )
            }
        }
    }
}