package com.example.madass


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.madass.ui.theme.MADassTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MADassTheme {
                App()
            }
        }
    }
}


enum class Screen {
    foodOrderScreen, OrderCartScreen
}


data class CartItem(
    val foodName : String,
    val basePrice : Double,
    val foodQuantity : Int,
    val foodAddOns : List<String>,
    val foodRemovals : List<String>
) {
    fun getTotalPrice() : Double {
        var updatedPrice = basePrice
        foodAddOns.forEach { foodAddOns ->
            when (foodAddOns) {
                "Egg" -> updatedPrice += 1.0
                "Vegetable" -> updatedPrice += 2.0
            }
        }
        return updatedPrice * foodQuantity
    }
}


@Composable
fun App() {
    val navController = rememberNavController()


    // FOOD data
    var foodQuantity by rememberSaveable { mutableStateOf(1) }
    var eggAddOn by rememberSaveable { mutableStateOf(false) }
    var vegetableAddOn by rememberSaveable { mutableStateOf(false) }
    var removeSpringOnion by rememberSaveable { mutableStateOf(false) }
    var removeVegetable by rememberSaveable { mutableStateOf(false) }


    // CART data
    var cartItems by rememberSaveable { mutableStateOf(listOf<CartItem>()) }


    // MEMBER COIN data
    var memberCoins by rememberSaveable { mutableStateOf(150) } // example coin = 150
    var coinsToUse by rememberSaveable { mutableStateOf(0) }


    NavHost (navController = navController, startDestination = Screen.foodOrderScreen.name) {
        composable (Screen.foodOrderScreen.name) {
            foodOrderScreen(
                navController,
                modifier = Modifier,
                foodQuantity = foodQuantity,
                onFoodQuantityChange = { foodQuantity = it },
                eggAddOn = eggAddOn,
                onEggAddOnChange = { eggAddOn = it },
                vegetableAddOn = vegetableAddOn,
                onVegetableAddOnChange = { vegetableAddOn = it },
                removeSpringOnion = removeSpringOnion,
                onRemoveSpringOnionChange = { removeSpringOnion = it },
                removeVegetable = removeVegetable,
                onRemoveVegetableChange = { removeVegetable = it },
                onAddToCart = { item -> // -> = lambda function, item is the parameter
                    cartItems += item
                }
            )
        }


        composable (Screen.OrderCartScreen.name) {
            OrderCartScreen(
                navController,
                cartItems = cartItems,
                memberCoins = memberCoins,
                coinsToUse = coinsToUse,
                onCoinToUseChange = { coinsToUse = it },
                onRemoveItems = {index -> // function for onRemove()
                    cartItems = cartItems.toMutableList().apply { removeAt(index) } // toMutableList = create a new copy of the immutable list (List<CartItem>) to mutable, apply = performs operations & returns an object
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun foodOrderScreen(
    navController : NavController,
    modifier : Modifier,
    foodQuantity: Int,
    onFoodQuantityChange: (Int) -> Unit,
    eggAddOn: Boolean,
    onEggAddOnChange: (Boolean) -> Unit,
    vegetableAddOn: Boolean,
    onVegetableAddOnChange: (Boolean) -> Unit,
    removeSpringOnion: Boolean,
    onRemoveSpringOnionChange: (Boolean) -> Unit,
    removeVegetable: Boolean,
    onRemoveVegetableChange: (Boolean) -> Unit,
    onAddToCart: (CartItem) -> Unit
) {
    val basePrice = 15.90
    val eggPrice = 1.0
    val vegetablePrice = 2.0


    val totalPrice = (basePrice + (if (eggAddOn) eggPrice else 0.0) +
            (if (vegetableAddOn) vegetablePrice else 0.0)) * foodQuantity


    Column(
        modifier = Modifier
            .fillMaxSize() // fill up available spaces
            .background(Color.White)
    ) {
        // Top App bar
        TopAppBar(
            // TITLE of screen
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Order",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            // BACK button
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black // modify the icon image colour
                    )
                }
            },
            // CART button
            actions = {
                IconButton(onClick = { navController.navigate(Screen.OrderCartScreen.name) }) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Order cart",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFFDD835) // the signature yellow
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
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.2f), // alpha = modify transparency of the colour (80% transparent), f = float
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                // image
                Image(
                    painter = painterResource(id = R.drawable.N1),
                    contentDescription = "N1 Signature Braised Pork QQ Noodle",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            Spacer(modifier = Modifier.height(16.dp))


            // Food Title and Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) { // weight = takes all the remaining spaces (fixed)
                    // Food name
                    Text(
                        text = "N1 Signature Braised Pork",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, // fontWeight = controls how thick and thin the text is
                        color = Color.Black
                    )
                    Text(
                        text = "QQ Noodle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }


                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // spacedBy = add equal spacing between items
                ) {
                    // MINUS button
                    IconButton(
                        onClick = { if (foodQuantity > 1) onFoodQuantityChange(foodQuantity - 1) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                    ) {
                        // MINUS icon
                        Text(
                            text = "-",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
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
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = foodQuantity.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(32.dp),
                            color = Color.Black,
                            textAlign = TextAlign.Center // num will be in the middle of the shape
                        )
                    }
                    // PLUS button
                    IconButton(
                        onClick = { onFoodQuantityChange(foodQuantity + 1) },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                    ) {
                        // PLUS icon
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))


            // Food Description
            Text(
                text = "Recipe from Taiwan TAIPEI! very soft & tasty Handmade Noodle - Signature Braised Pork - Fried Egg Side Dish (Daily) - Sour Chilli",
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )


            Spacer(modifier = Modifier.height(24.dp))


            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFFDD835),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp) // padding inside the box
            ) {
                Column {
                    // ADD ON section
                    Text(
                        text = "Add-On",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )


                    Spacer(modifier = Modifier.height(12.dp))


                    // EGG add-on
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEggAddOnChange(!eggAddOn) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Egg x1",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RM 1",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = eggAddOn,
                                onCheckedChange = { onEggAddOnChange(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.Black,
                                    uncheckedColor = Color.White
                                )
                            )
                        }
                    }


                    // VEGETABLE add-on
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVegetableAddOnChange(!vegetableAddOn) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vegetable",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RM 2",
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = vegetableAddOn,
                                onCheckedChange = { onVegetableAddOnChange(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.Black,
                                    uncheckedColor = Color.White
                                )
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(8.dp))


                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.White
                    )


                    Spacer(modifier = Modifier.height(8.dp))


                    // REMOVE section
                    Text(
                        text = "Remove",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )


                    Spacer(modifier = Modifier.height(12.dp))


                    // SPRING ONION removal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveSpringOnionChange(!removeSpringOnion) } // no spring onion or spring onion
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spring Onion",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Checkbox(
                            checked = removeSpringOnion,
                            onCheckedChange = { onRemoveSpringOnionChange(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Black,
                                uncheckedColor = Color.White
                            )
                        )
                    }


                    // VEGETABLE removal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveVegetableChange(!removeVegetable) } // no vegetable or vegetable
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vegetable",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Checkbox(
                            checked = removeVegetable,
                            onCheckedChange = { onRemoveVegetableChange(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Black,
                                uncheckedColor = Color.White
                            )
                        )
                    }
                }


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
                    text = "RM %.2f".format(totalPrice),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )


                // Button
                Button(
                    onClick = {
                        val addOns = mutableListOf<String>()
                        val removals = mutableListOf<String>()


                        if (eggAddOn) addOns.add("Egg")
                        if (vegetableAddOn) addOns.add("Vegetable")
                        if (removeSpringOnion) removals.add("Spring Onion")
                        if (removeVegetable) removals.add("Vegetable")


                        val cartItem = CartItem (
                            foodName = "N1 Signature Braised Pork QQ Noodle",
                            basePrice = 15.90,
                            foodQuantity = foodQuantity,
                            foodAddOns = addOns,
                            foodRemovals = removals
                        )
                        onAddToCart(cartItem)


                        // RESET form after each order
                        onFoodQuantityChange(1)
                        onEggAddOnChange(false)
                        onVegetableAddOnChange(false)
                        onRemoveSpringOnionChange(false)
                        onRemoveVegetableChange(false)


                        navController.navigate(Screen.OrderCartScreen.name)
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(start = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFDD835)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Add to Cart",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCartScreen(
    navController: NavController,
    cartItems : List<CartItem>,
    memberCoins : Int,
    coinsToUse : Int,
    onCoinToUseChange : (Int) -> Unit,
    onRemoveItems : (Int) -> Unit
) {
    val subtotal = cartItems.sumOf { it.getTotalPrice() }
    val coinDiscount = coinsToUse * 0.01
    val finalTotal = (subtotal - coinDiscount).coerceAtLeast(0.0)
    val maxCoinsUsable = minOf(memberCoins, (subtotal * 100).toInt())


    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App bar
        TopAppBar(
            // TITLE of screen
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cart",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            // BACK button
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black // modify the icon image colour
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDD835))
        )


        // order summary header
        Column (
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text (
                    text = "Order Summary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text (
                    text = "Total: %d product(s)".format(cartItems.sumOf { it.foodQuantity }),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }


            Spacer (modifier = Modifier.height(24.dp))


            if (cartItems.isEmpty()) {
                // EMPTY CART content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // EMPTY CART icon
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
                }
            } else {
                // CART ITEMS
                cartItems.forEachIndexed { index, item -> // forEachIndexed = for each loop with index number
                    // CART ITEMS display
                    CartItemDisplay (
                        item = item,
                        onRemove = { onRemoveItems(index) }
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
                        .clickable { navController.popBackStack() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ADD icon
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )


                    Spacer(modifier = Modifier.width(12.dp))


                    // Add more items text
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // MINUS coin button
                                IconButton(
                                    onClick = {
                                        if (coinsToUse > 0) onCoinToUseChange(coinsToUse - 1)
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFFDD835), CircleShape)
                                ) {
                                    Text(
                                        text = "-",
                                        fontSize = 14.sp,
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
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = coinsToUse.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black,
                                        modifier = Modifier.width(40.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }


                                // PLUS coin button
                                IconButton(
                                    onClick = {
                                        if (coinsToUse < maxCoinsUsable) onCoinToUseChange(coinsToUse + 1)
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFFDD835), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add coin",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }


                            // USE MAX button
                            Button(
                                onClick = { onCoinToUseChange(maxCoinsUsable) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFDD835)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Use Max",
                                    color = Color.White,
                                    fontSize = 12.sp,
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
                    onClick = { /* Handle checkout */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFDD835)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Proceed to Checkout",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

        }
    }
}


@Composable
fun CartItemDisplay(
    item: CartItem,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Gray.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // FOOD NAME display
                    Text(
                        text = item.foodName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // QUANTITY display
                    Text(
                        text = "Qty: %d".format(item.foodQuantity),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    // ADD ONS display
                    if (item.foodAddOns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add-ons: %s".format(item.foodAddOns.joinToString(", ")), // joinToString = join the strings together while separated by a coma (,)
                            fontSize = 12.sp,
                            color = Color.Blue
                        )
                    }
                    // REMOVALS display
                    if (item.foodRemovals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remove: %s".format(item.foodRemovals.joinToString(", ")),
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }
                }


                // TOTAL PRICE display
                Column(horizontalAlignment = Alignment.End) {
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
                        textDecoration = TextDecoration.Underline // underline the text with TextDecoration
                    )
                }
            }
        }
    }
}
