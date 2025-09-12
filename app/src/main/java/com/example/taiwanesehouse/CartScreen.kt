package com.example.taiwanesehouse

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taiwanesehouse.database.FoodItemEntity
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.viewmodel.FoodItemViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreenWithDatabase(
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
    val coinDiscount = coinsToUse * 0.01
    val finalTotal = (subtotal - coinDiscount).coerceAtLeast(0.0)
    val maxCoinsUsable = minOf(memberCoins, (subtotal * 100).toInt())

    // Check authentication
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to view your cart", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Menu.name) {
                popUpTo(Screen.Cart.name) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App bar
        CenterAlignedTopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFC107))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Order summary header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Summary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Total: %d product(s)".format(cartItems.sumOf { it.foodQuantity }),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (cartItems.isEmpty()) {
                // EMPTY CART content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Empty Cart",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your cart is empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add some delicious items to get started!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.navigate(Screen.Menu.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                    ) {
                        Text(
                            text = "Browse Menu",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // CART ITEMS
                cartItems.forEachIndexed { index, item ->
                    CartItemDisplayWithDatabase(
                        item = item,
                        foodItemViewModel = foodItemViewModel,
                        onRemove = {
                            scope.launch {
                                isLoading = true
                                val success = cartManager.removeFromCart(index)
                                isLoading = false
                                if (!success) {
                                    Toast.makeText(context, "Failed to remove item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onQuantityChange = { newQuantity ->
                            scope.launch {
                                isLoading = true
                                val success = cartManager.updateCartItemQuantity(index, newQuantity)
                                isLoading = false
                                if (!success) {
                                    Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ADD MORE ITEMS button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Gray.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { navController.navigate(Screen.Menu.name) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Add more items",
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MEMBER COINS section
                if (subtotal > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFFDD835).copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Member Coins",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Available: %d coins".format(memberCoins),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Use coins (1 coin = RM 0.01 discount)",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // COIN USAGE controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // MINUS coin button
                                    IconButton(
                                        onClick = {
                                            if (coinsToUse > 0) {
                                                cartManager.updateCoinsToUse(coinsToUse - 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFDD835), CircleShape)
                                    ) {
                                        Text(
                                            text = "-",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // COINS TO USE display
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = coinsToUse.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            modifier = Modifier.width(44.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // PLUS coin button
                                    IconButton(
                                        onClick = {
                                            if (coinsToUse < maxCoinsUsable) {
                                                cartManager.updateCoinsToUse(coinsToUse + 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFDD835), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add coin",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // USE MAX button
                                Button(
                                    onClick = { cartManager.updateCoinsToUse(maxCoinsUsable) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFDD835)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Use Max",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (coinsToUse > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Discount: -RM %.2f".format(coinDiscount),
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (subtotal > 0) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // PRICE BREAKDOWN
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.Gray.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal:",
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "RM %.2f".format(subtotal),
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        if (coinsToUse > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Coin Discount (%d coins):".format(coinsToUse),
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "-RM %.2f".format(coinDiscount),
                                    fontSize = 16.sp,
                                    color = Color.Red
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "RM %.2f".format(finalTotal),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // CHECKOUT button
                    Button(
                        onClick = {
                            navController.navigate(Screen.Payment.name)
                        },
                        enabled = !isLoading,
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
                                text = "Proceed to Checkout",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CartItemDisplayWithDatabase(
    item: CartItem,
    foodItemViewModel: FoodItemViewModel,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    var foodItem by remember { mutableStateOf<FoodItemEntity?>(null) }

    // Try to get food item details from database
    LaunchedEffect(item.foodName) {
        try {
            // Since we don't have the original ID, we'll need to search by name
            // This is a limitation - ideally CartItem should store the food ID
            foodItem = foodItemViewModel.getFoodItemByName(item.foodName)
        } catch (e: Exception) {
            // Handle error - food item not found in database
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Gray.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Food Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagesRes != 0) {
                    Image(
                        painter = painterResource(id = item.imagesRes),
                        contentDescription = item.foodName,
                        modifier = Modifier
                            .size(80.dp),
//                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback if no image
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = item.foodName,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            // Item Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.foodName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Base: RM %.2f".format(item.basePrice),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (item.foodAddOns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add-ons: ${item.foodAddOns.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.Blue
                    )
                }

                if (item.foodRemovals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remove: ${item.foodRemovals.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                            .clickable {
                                if (item.foodQuantity > 1) {
                                    onQuantityChange(item.foodQuantity - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = item.foodQuantity.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                            .clickable {
                                onQuantityChange(item.foodQuantity + 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Price and Remove
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "RM %.2f".format(item.getTotalPrice()),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Remove",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.clickable { onRemove() },
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}