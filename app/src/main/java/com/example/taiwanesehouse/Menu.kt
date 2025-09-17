// Updated MenuScreen with proper AndroidViewModel usage
package com.example.taiwanesehouse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import com.example.taiwanesehouse.FirebaseCartManager
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.viewmodel.FoodItemViewModel

@Composable
fun CategoryMenu(category: String, navController: NavController, viewModel: FoodItemViewModel) {
    val items by viewModel.getFoodItemsByCategory(category).collectAsState(initial = emptyList())

    // Sort by ID using natural order (prefix + numeric part), e.g., R1, R2, R10
    val sortedItems = remember(items) {
        items.sortedWith(
            compareBy<com.example.taiwanesehouse.database.entities.FoodItemEntity>(
                { it.id.takeWhile { ch -> ch.isLetter() }.lowercase() },
                { it.id.dropWhile { ch -> ch.isLetter() }.toIntOrNull() ?: Int.MAX_VALUE },
                { it.id }
            )
        )
    }

    LazyColumn {
        items(sortedItems) { item ->
            DatabaseFoodCard(item = item, onAddClick = {}, navController = navController)
        }
    }
}

@Composable
fun DatabaseFoodCard(item: FoodItemEntity, onAddClick: () -> Unit, navController: NavController) {
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
                        text = "RM %.2f".format(item.price),
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
                            .background(Color(0xFFFFC107), CircleShape)
                            .clickable {
                                navController.navigate("order/${item.id}")
                                onAddClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenWithDatabase(navController: NavController) {
    // Get the AndroidViewModel using viewModel() composable
    // This will automatically handle the Application context
    val viewModel: FoodItemViewModel = viewModel()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rice", "Noodles", "Not Too Full", "Snacks", "Drinks")
    var searchText by rememberSaveable { mutableStateOf("") }
    val cartManager = remember { FirebaseCartManager() }
    val cartItems by cartManager.cartItems.collectAsState()
    val cartItemCount = cartItems.sumOf { it.foodQuantity }

    // Collect search results - only search when text is not empty
    val searchResults by if (searchText.isNotEmpty()) {
        viewModel.searchFoodItems(searchText).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<FoodItemEntity>()) }
    }

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
                actions = {
                    Box {
                        IconButton(onClick = { navController.navigate(Screen.Cart.name)}) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        if (cartItemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cartItemCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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

            if (searchText.isEmpty()) {
                // Category tabs when not searching
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp),
                    color = Color.Black
                )

                // Content from database by category
                CategoryMenu(
                    category = tabs[selectedTabIndex],
                    navController = navController,
                    viewModel = viewModel
                )
            } else {
                // Display search results
                Text(
                    text = "Search Results for \"$searchText\"",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 2.dp),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (searchResults.isEmpty()) {
                    // Show "No results found" message
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items found matching \"$searchText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn {
                        items(searchResults) { item ->
                            DatabaseFoodCard(item = item, onAddClick = {}, navController = navController)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}