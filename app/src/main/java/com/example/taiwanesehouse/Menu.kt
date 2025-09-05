package com.example.taiwanesehouse;

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview



// Data class for food items
data class FoodItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageRes: Int
)

@Composable
fun SimpleRiceMenu() {
    // Use system drawable to avoid resource issues
    val riceItems = listOf(
        FoodItem("R1", "Signature Braised Pork Rice", "Japanese Pearl Rice - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili", 15.90, R.drawable.signature_braised_pork_rice),
        FoodItem("R2", "High CP Salted Chicken Rice", "Japanese Pearl Rice - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.signature_braised_pork_rice),
        FoodItem("R3", "Meatball & Sausage Minced Pork Rice", "Japanese Pearl Rice - Minced Pork - Taiwan Sausage - Pork Meatball - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.meatball_and_sausage_minced_pork_rice),
        FoodItem("R4", "House Crispy Chicken Chop Rice", "Japanese Pearl Rice - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.house_crispy_chicken_chop_rice),
        FoodItem("R5", "Taiwanese Pork Chop Rice", "Jasmine Pearl Rice - Taiwanese Pork Chop - First Egg - Taiwanese Pickled Vegetable - Sour Chili", 21.90, R.drawable.taiwanese_belly_pork_chop_rice),
        FoodItem("R6", "Khong Bah Peng", "Jasmine Pearl Rice - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili", 21.90, R.drawable.khong_bah_peng),
        FoodItem("R7", "Three Cup Chicken Rice", "Japanese Pearl Rice - 3 Cup Chicken - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili", 25.90, R.drawable.three_cup_chicken_rice)
    )

    LazyColumn {
        items(riceItems) { item ->
            FoodCard(item = item, onAddClick = {
                // TODO: Handle add to cart action
            })
        }
    }
}

@Composable
fun NoodlesMenu() {
    val noodleItems = listOf(
        FoodItem("N1", "Signature Braised Pork QQ Noodle", "Handmade Noodle - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili", 15.90, R.drawable.signature_braised_pork_qq_noodle),
        FoodItem("N2", "High CP Salted Chicken QQ Noodle", "Handmade Noodle - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.high_cp_salted_chicken_qq_noodle),
        FoodItem("N3", "Meatball & Sausage Minced Pork QQ Noodle", "Handmade Noodle - Minced Pork - Taiwan Sausage - Pork Meatball - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.meatball_and_sausage_minced_pork_qq_noodle),
        FoodItem("N4", "House Crispy Chicken Chop QQ Noodle", "Handmade Noodle - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 19.90, R.drawable.house_chicken_chop_qq_noodle),
        FoodItem("N5", "Taiwanese Belly Pork Chop QQ Noodle", "Handmade Noodle - Pork Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.taiwanese_belly_pork_chop_qq_noodle),
        FoodItem("N6", "Gozhabi Stewed Belly QQ Noodle", "Handmade Noodle - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.gozhabi_stewed_belly_qq_noodle),
        FoodItem("N7", "Twice Egg Scallion Noodle", "Handmade Noodle - Twice Wallet Egg - Side Dish (Daily) - Sour Chili", 11.90, R.drawable.twice_egg_scallion_noodle)
    )

    LazyColumn {
        items(noodleItems) { item ->
            FoodCard(item = item, onAddClick = {
                    // TODO: Handle add to cart action
            })
        }
    }
}

@Composable
fun NotTooFullMenu() {
    val lightItems = listOf(
        FoodItem("E1", "Yam Floss Egg Crepe", "Yam Paste - Chicken Floss - Egg - Crepe", 8.90, R.drawable.yam_floss_egg_crepe),
        FoodItem("E2", "Cheese Floss Egg Crepe", "Cheese - Chicken Floss - Egg - Crepe - Mayonnaise - Sweet Chili Sauce", 8.90, R.drawable.cheese_floss_egg_crepe),
        FoodItem("E3", "Cheese Ham Egg Crepe", "Chicken Ham, Cheese - Egg - Crepe - Mayonnaise - Sweet Chili Sauce", 8.90, R.drawable.cheese_ham_egg_crepe),
        FoodItem("E4", "Double Cheese Egg Scallion Sandwich", "Double Cheese - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.double_cheese_egg_scallion_sandwich),
        FoodItem("E5", "Floss Egg Scallion Sandwich", "Chicken Floss - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.floss_egg_scallion_sandwich),
        FoodItem("E6", "Ham Egg Scallion Sandwich", "Chicken Ham - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.ham_egg_scallion_sandwich)
    )

    LazyColumn {
        items(lightItems) { item ->
            FoodCard(item = item, onAddClick = {
                // TODO: Handle add to cart action
            })
        }
    }
}

@Composable
fun SnackMenu() {
    val snackItems = listOf(
        FoodItem("S1", "Garlic Slice Taiwanese Sausage", "Taiwan Sausage 2 Pcs", 8.90, R.drawable.garlic_slice_taiwanese_sausage),
        FoodItem("S2", "Tempura Oyster Mushroom", "Fried Oyster Mushroom (Spicy / Original)", 9.90, R.drawable.tempura_oyster_mushroom),
        FoodItem("S3", "Sweet Plum Potato Fries", "Fired Sweet Orange Potato", 9.90, R.drawable.sweet_plum_potato_fried),
        FoodItem("S4", "High CP Salted Chicken", "Fried Salted Chicken (Spicy / Original)", 12.90, R.drawable.high_cp_salted_chicken),
        FoodItem("S5", "Taiwanese Belly Pork Chop", "Fried Juicy Pork Chop (Spicy / Original)", 14.90, R.drawable.taiwanese_belly_pork_chop),
        FoodItem("S6", "House Crispy Chicken Chop", "Fried Juicy Chicken Chop (Spicy / Original)", 13.90, R.drawable.house_crispy_chicken_chop),
        FoodItem("S7", "Sweet Not Spicy", "Tempura (No Spicy)", 12.90, R.drawable.sweet_not_spicy)
    )

    LazyColumn {
        items(snackItems) { item ->
            FoodCard(item = item, onAddClick = {
                // TODO: Handle add to cart action
            })
        }
    }
}

@Composable
fun DrinkMenu() {
    val drinkItems = listOf(
        FoodItem("D1", "Aloe Yakult Tea", "", 8.90, R.drawable.aloe_yakult_tea),
        FoodItem("D2", "TW Aiyu Jelly", "", 7.90, R.drawable.tw_aiyu_tea),
        FoodItem("D3", "Dark Aroma Lemon Tea", "", 5.90, R.drawable.dark_aroma_lemon_tea),
        FoodItem("D4", "Original Lemon Tea", "", 5.90, R.drawable.original_lemon_tea),
        FoodItem("D5", "Earl Grey Milk Tea", "", 7.90, R.drawable.earl_grey_milk_tea),
        FoodItem("D6", "Pearl Earl Milk Tea", "", 8.90, R.drawable.pearl_earl_milk_tea),
        FoodItem("D7", "White Peach Milk Tea", "", 7.90, R.drawable.white_peach_milk_tea),
        FoodItem("D8", "Jasmine Milk Tea", "", 7.90, R.drawable.jasmine_milk_tea)
    )

    LazyColumn {
        items(drinkItems) { item ->
            FoodCard(item = item, onAddClick = {
                // TODO: Handle add to cart action
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCard(item: FoodItem, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 18.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            // Food Image
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${item.id} ${item.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF777777),
                        fontSize = 10.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = "%.2f".format(item.price),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Add Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFFC107), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp) // 单独调 icon 大小
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rice", "Noodles", "Not Too Full", "Snacks", "Drinks")
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { /* TODO: Handle cart click */ }) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Red, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        bottomBar = { BottomNavigationBar() },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search in menu", color = Color(0xFF555555), fontSize = 16.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp).size(28.dp)
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 28.dp, vertical = 4.dp)
                    .defaultMinSize(minHeight = 58.dp),
                shape = RoundedCornerShape(40.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    focusedTextColor = Color.Gray,
                    unfocusedTextColor = Color.Gray,
                    cursorColor = Color.Gray,
                    focusedIndicatorColor = Color(0xFFF0F0F0),
                    unfocusedIndicatorColor = Color(0xFFF0F0F0)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFFFFC107)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.height(40.dp),
                        text = {
                            Text(
                                text = title,
                                color = if (isSelected) Color(0xFFFFC107) else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 18.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (selectedTabIndex) {
                    0 -> "Premium Pearl Rice"
                    1 -> "Classic Noodles"
                    2 -> "Not Too Full Menu"
                    3 -> "Snacks & Treats"
                    4 -> "Drinks"
                    else -> ""
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp),
                color = Color.Black
            )

            // Content
            when (selectedTabIndex) {
                0 -> SimpleRiceMenu()
                1 -> NoodlesMenu()
                2 -> NotTooFullMenu()
                3 -> SnackMenu()
                4 -> DrinkMenu()
            }

            Spacer(modifier = Modifier.height(8.dp))
            BottomNavigationBar()
        }
    }
}

fun onBackClick() {
    TODO("Not yet implemented")
}

@Composable
fun BottomNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", fontSize = 28.sp)
            Text("Menu", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛒", fontSize = 28.sp)
            Text("Cart", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👤", fontSize = 28.sp)
            Text("Me", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, showSystemUi = true)
@Composable
fun SimpleMenuPreview() {
    MaterialTheme {
        MenuScreen()
    }
}