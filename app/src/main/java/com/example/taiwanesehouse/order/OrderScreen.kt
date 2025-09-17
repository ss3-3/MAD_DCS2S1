package com.example.taiwanesehouse.order

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taiwanesehouse.FirebaseCartManager
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.viewmodel.FoodItemViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreenWithDatabase(
    navController: NavController,
    foodId: String,
    cartManager: FirebaseCartManager = remember { FirebaseCartManager() }
) {
    // Get the AndroidViewModel using viewModel() composable - no manual initialization needed
    val foodItemViewModel: FoodItemViewModel = viewModel()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // Food item from database
    var foodItem by remember { mutableStateOf<FoodItemEntity?>(null) }
    var isLoadingFood by remember { mutableStateOf(true) }

    // Load food item from database
    LaunchedEffect(foodId) {
        try {
            // Debug: Check database status
            val dbStatus = foodItemViewModel.getDatabaseStatus()
            android.util.Log.d("OrderScreen", "Database status: $dbStatus")
            
            foodItem = foodItemViewModel.getFoodItemById(foodId)
            isLoadingFood = false
            if (foodItem == null) {
                Toast.makeText(context, "Food item not found: $foodId. DB Status: $dbStatus", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            isLoadingFood = false
            Toast.makeText(context, "Error loading food item: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Local state for form
    var foodQuantity by remember { mutableIntStateOf(1) }
    var eggAddOn by remember { mutableStateOf(false) }
    var vegetableAddOn by remember { mutableStateOf(false) }
    var removeSpringOnion by remember { mutableStateOf(false) }
    var removeVegetable by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Cart items for displaying cart count
    val cartItems by cartManager.cartItems.collectAsState()

    if (isLoadingFood) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFC107)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading food item...",
                    color = Color.Gray
                )
            }
        }
        return
    }

    foodItem?.let { item ->
        val isNotTooFull = item.category.equals("Not Too Full", ignoreCase = true)
        val isSnack = item.category.equals("Snacks", ignoreCase = true)
        val isDrink = item.category.equals("Drinks", ignoreCase = true)
        val eggPrice = 1.0
        val vegetablePrice = 2.0
        val totalPrice = (item.price + (if (!isNotTooFull && !isSnack && !isDrink && eggAddOn) eggPrice else 0.0) +
                (if (!isNotTooFull && !isSnack && !isDrink && vegetableAddOn) vegetablePrice else 0.0)) * foodQuantity

        LaunchedEffect(item.id) {
            if (isNotTooFull || isSnack || isDrink) {
                eggAddOn = false
                vegetableAddOn = false
                removeSpringOnion = false
                removeVegetable = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Top App bar
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🍽️ Order",
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
                        Text(
                            text = "⬅️",
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    Box {
                        TextButton(
                            onClick = { navController.navigate(Screen.Cart.name) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(
                                text = "🛒",
                                fontSize = 20.sp
                            )
                        }
                        // Cart item count badge
                        if (cartItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cartItems.sumOf { it.foodQuantity }.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                // Food Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(220.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.name,
                        modifier = Modifier
                            .width(260.dp)
                            .height(220.dp),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Food Title and Quantity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // Quantity Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // MINUS button with emoji
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .background(Color(0xFFFDD835), CircleShape)
                                .clickable {
                                    if (foodQuantity > 1) foodQuantity--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "➖",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }

                        // Quantity NUM
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.Gray.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = foodQuantity.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(40.dp),
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        // PLUS button with emoji
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .background(Color(0xFFFDD835), CircleShape)
                                .clickable {
                                    foodQuantity++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "➕",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Food Description
                Text(
                    text = item.description.ifEmpty { "Delicious ${item.name} prepared with care." },
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Customization Box (hidden for Not Too Full Snacks and Drinks category)
                if (!isNotTooFull && !isSnack && !isDrink) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFFDD835),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            // ADD ON section
                            Text(
                                text = "➕ Add-On",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // EGG add-on
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { eggAddOn = !eggAddOn }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🥚 Egg x1",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "💰 RM 1.00",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Checkbox(
                                        checked = eggAddOn,
                                        onCheckedChange = { eggAddOn = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color.White,
                                            uncheckedColor = Color.White,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                }
                            }

                            // VEGETABLE add-on
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vegetableAddOn = !vegetableAddOn }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🥬 Vegetable",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "💰 RM 2.00",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Checkbox(
                                        checked = vegetableAddOn,
                                        onCheckedChange = { vegetableAddOn = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color.White,
                                            uncheckedColor = Color.White,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // REMOVE section
                            Text(
                                text = "➖ Remove",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // SPRING ONION removal
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { removeSpringOnion = !removeSpringOnion }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧅 Spring Onion",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Checkbox(
                                    checked = removeSpringOnion,
                                    onCheckedChange = { removeSpringOnion = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.White,
                                        uncheckedColor = Color.White,
                                        checkmarkColor = Color.Black
                                    )
                                )
                            }

                            // VEGETABLE removal
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { removeVegetable = !removeVegetable }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🥬 Vegetable",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Checkbox(
                                    checked = removeVegetable,
                                    onCheckedChange = { removeVegetable = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.White,
                                        uncheckedColor = Color.White,
                                        checkmarkColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Not Too Full/Snacks/Drinks: show note that customization is not available
                    Text(
                        text = "No customization available for this category.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // PRICE display and ADD TO CART button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price
                    Text(
                        text = "💰 RM %.2f".format(totalPrice),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Add to Cart Button
                    Button(
                        onClick = {
                            // Check if user is authenticated
                            if (auth.currentUser == null) {
                                Toast.makeText(context, "Please log in to add items to cart", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isLoading = true

                                try {
                                    val addOns = mutableListOf<String>()
                                    val removals = mutableListOf<String>()

                                    if (eggAddOn) addOns.add("Egg")
                                    if (vegetableAddOn) addOns.add("Vegetable")
                                    if (removeSpringOnion) removals.add("Spring Onion")
                                    if (removeVegetable) removals.add("Vegetable")

                                    val cartItem = CartItem(
                                        foodName = item.name,
                                        basePrice = item.price,
                                        foodQuantity = foodQuantity,
                                        foodAddOns = addOns,
                                        foodRemovals = removals,
                                        imagesRes = item.imageRes
                                    )

                                    val success = cartManager.addToCart(cartItem)

                                    if (success) {
                                        // Reset form after successful add
                                        foodQuantity = 1
                                        eggAddOn = false
                                        vegetableAddOn = false
                                        removeSpringOnion = false
                                        removeVegetable = false

                                        Toast.makeText(context, "Added to cart! 🛒", Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.Menu.name)
                                    } else {
                                        Toast.makeText(context, "Failed to add to cart ❌", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error adding to cart: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                            .padding(start = 16.dp),
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
                                text = "🛒 Add to Cart",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    } ?: run {
        // Error state - food item not found
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❌ Food item not found",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Item ID: $foodId",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107)
                    )
                ) {
                    Text(
                        text = "⬅️ Go Back",
                        color = Color.White
                    )
                }
            }
        }
    }
}