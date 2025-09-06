package com.example.taiwanesehouse

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class CartItem(
    val foodName : String,
    val basePrice : Double,
    val foodQuantity : Int,
    val foodAddOns : List<String>,
    val foodRemovals : List<String>,
    val imagesRes: Int
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    navController : NavController,
    foodName: String,
    foodDescription: String,
    basePrice: Double,
    imagesRes: Int,
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                    ),
                        color = Color.Black
                    )
                }
            },
            // BACK button
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black // modify the icon image colour
                    )
                }
            },
            // CART button
            actions = {
                IconButton(onClick = { navController.navigate(Screen.Cart.name) }) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Order cart",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFFFC107) // the signature yellow
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
                    .height(220.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.05f), // alpha = modify transparency of the colour (80% transparent), f = float
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // image
                Image(
                    painter = painterResource(id = imagesRes),
                    contentDescription = foodName,
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
                Column(modifier = Modifier.weight(1f)) { // weight = takes all the remaining spaces (fixed)
                    // Food name
                    Text(
                        text = foodName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, // fontWeight = controls how thick and thin the text is
                        color = Color.Black
                    )
                }

                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // spacedBy = add equal spacing between items
                ) {
                    // MINUS button
                    IconButton(
                        onClick = { if (foodQuantity > 1) onFoodQuantityChange(foodQuantity - 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                    ) {
                        // MINUS icon
                        Text(
                            text = "-",
                            fontSize = 20.sp,
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
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = foodQuantity.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(40.dp),
                            color = Color.Black,
                            textAlign = TextAlign.Center // num will be in the middle of the shape
                        )
                    }
                    // PLUS button
                    IconButton(
                        onClick = { onFoodQuantityChange(foodQuantity + 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFDD835), CircleShape)
                    ) {
                        // PLUS icon
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Food Description
            Text(
                text = foodDescription,
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
                            foodName = foodName,
                            basePrice = basePrice,
                            foodQuantity = foodQuantity,
                            foodAddOns = addOns,
                            foodRemovals = removals,
                            imagesRes = imagesRes
                        )
                        onAddToCart(cartItem)

                        // RESET form after each order
                        onFoodQuantityChange(1)
                        onEggAddOnChange(false)
                        onVegetableAddOnChange(false)
                        onRemoveSpringOnionChange(false)
                        onRemoveVegetableChange(false)

                        navController.navigate(Screen.Cart.name)
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
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}