package com.example.taiwanesehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taiwanesehouse.ui.theme.TaiwaneseHouseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaiwaneseHouseTheme {
                NavigationApp()
            }
        }
    }
}

enum class Screen {
    Signup,
    Login,
    ForgotPassword,
    Menu,
    Cart,
    Profile,
}

@Composable
fun NavigationApp() {
    val navController = rememberNavController()

    // FOOD data
    var foodQuantity by rememberSaveable { mutableIntStateOf(1) }
    var eggAddOn by rememberSaveable { mutableStateOf(false) }
    var vegetableAddOn by rememberSaveable { mutableStateOf(false) }
    var removeSpringOnion by rememberSaveable { mutableStateOf(false) }
    var removeVegetable by rememberSaveable { mutableStateOf(false) }

    // CART data
    var cartItems by rememberSaveable { mutableStateOf(listOf<CartItem>()) }

    // MEMBER COIN data
    val memberCoins by rememberSaveable { mutableIntStateOf(150) } // example coin = 150
    var coinsToUse by rememberSaveable { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = Screen.Login.name) {
        composable(Screen.Signup.name) {
            SignUpScreen(
                navController = navController,
                name = "Taiwanese House",
                message = "Please enter your details to log in your account."
            )
        }
        composable(Screen.Login.name) { LoginScreen(navController = navController) }
        composable(Screen.Menu.name) { MenuScreen(navController) }
        composable(
            route = "order/{foodId}",
            arguments = listOf(
                navArgument("foodId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
            val foodItem = getFoodItemById(foodId)

            OrderScreen(
                navController = navController,
                foodName = foodItem.name,
                foodDescription = foodItem.description,
                basePrice = foodItem.price,
                imagesRes = foodItem.imageRes,
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
                onAddToCart = { item ->
                    cartItems += item
                }
            )
        }
        composable(Screen.Cart.name) {
            CartScreen(
                navController,
                cartItems = cartItems,
                memberCoins = memberCoins,
                coinsToUse = coinsToUse,
                onCoinToUseChange = { coinsToUse = it },
                onRemoveItems = { index -> // function for onRemove()
                    cartItems = cartItems.toMutableList()
                        .apply { removeAt(index) } // toMutableList = create a new copy of the immutable list (List<CartItem>) to mutable, apply = performs operations & returns an object
                }
            )
        }
        composable(
            route = "payment/{totalAmount}",
            arguments = listOf(
                navArgument("totalAmount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val totalAmount = backStackEntry.arguments?.getFloat("totalAmount")?.toDouble() ?: 0.0
            PaymentScreen(navController = navController, totalAmount = totalAmount)
        }
        composable(Screen.Profile.name) { UserProfileScreen(navController = navController) }
    }
}

private fun getFoodItemById(foodId: String): FoodItem {
    val allItems = getAllFoodItems()
    return allItems.find { it.id == foodId } ?: allItems.first()
}

private fun getAllFoodItems(): List<FoodItem> {
    return listOf(
        FoodItem("R1", "Signature Braised Pork Rice", "Japanese Pearl Rice - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili", 15.90, R.drawable.signature_braised_pork_rice),
        FoodItem("R2", "High CP Salted Chicken Rice", "Japanese Pearl Rice - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.signature_braised_pork_rice),
        FoodItem("R3", "Meatball & Sausage Minced Pork Rice", "Japanese Pearl Rice - Minced Pork - Taiwan Sausage - Pork Meatball - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.meatball_and_sausage_minced_pork_rice),
        FoodItem("R4", "House Crispy Chicken Chop Rice", "Japanese Pearl Rice - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.house_crispy_chicken_chop_rice),
        FoodItem("R5", "Taiwanese Pork Chop Rice", "Jasmine Pearl Rice - Taiwanese Pork Chop - First Egg - Taiwanese Pickled Vegetable - Sour Chili", 21.90, R.drawable.taiwanese_belly_pork_chop_rice),
        FoodItem("R6", "Khong Bah Peng", "Jasmine Pearl Rice - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili", 21.90, R.drawable.khong_bah_peng),
        FoodItem("R7", "Three Cup Chicken Rice", "Japanese Pearl Rice - 3 Cup Chicken - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili", 25.90, R.drawable.three_cup_chicken_rice),
        FoodItem("N1", "Signature Braised Pork QQ Noodle", "Handmade Noodle - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili", 15.90, R.drawable.signature_braised_pork_qq_noodle),
        FoodItem("N2", "High CP Salted Chicken QQ Noodle", "Handmade Noodle - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.high_cp_salted_chicken_qq_noodle),
        FoodItem("N3", "Meatball & Sausage Minced Pork QQ Noodle", "Handmade Noodle - Minced Pork - Taiwan Sausage - Pork Meatball - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili", 17.90, R.drawable.meatball_and_sausage_minced_pork_qq_noodle),
        FoodItem("N4", "House Crispy Chicken Chop QQ Noodle", "Handmade Noodle - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 19.90, R.drawable.house_chicken_chop_qq_noodle),
        FoodItem("N5", "Taiwanese Belly Pork Chop QQ Noodle", "Handmade Noodle - Pork Chop - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.taiwanese_belly_pork_chop_qq_noodle),
        FoodItem("N6", "Gozhabi Stewed Belly QQ Noodle", "Handmade Noodle - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili", 20.90, R.drawable.gozhabi_stewed_belly_qq_noodle),
        FoodItem("N7", "Twice Egg Scallion Noodle", "Handmade Noodle - Twice Wallet Egg - Side Dish (Daily) - Sour Chili", 11.90, R.drawable.twice_egg_scallion_noodle),
        FoodItem("E1", "Yam Floss Egg Crepe", "Yam Paste - Chicken Floss - Egg - Crepe", 8.90, R.drawable.yam_floss_egg_crepe),
        FoodItem("E2", "Cheese Floss Egg Crepe", "Cheese - Chicken Floss - Egg - Crepe - Mayonnaise - Sweet Chili Sauce", 8.90, R.drawable.cheese_floss_egg_crepe),
        FoodItem("E3", "Cheese Ham Egg Crepe", "Chicken Ham, Cheese - Egg - Crepe - Mayonnaise - Sweet Chili Sauce", 8.90, R.drawable.cheese_ham_egg_crepe),
        FoodItem("E4", "Double Cheese Egg Scallion Sandwich", "Double Cheese - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.double_cheese_egg_scallion_sandwich),
        FoodItem("E5", "Floss Egg Scallion Sandwich", "Chicken Floss - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.floss_egg_scallion_sandwich),
        FoodItem("E6", "Ham Egg Scallion Sandwich", "Chicken Ham - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce", 12.90, R.drawable.ham_egg_scallion_sandwich),
        FoodItem("S1", "Garlic Slice Taiwanese Sausage", "Taiwan Sausage 2 Pcs", 8.90, R.drawable.garlic_slice_taiwanese_sausage),
        FoodItem("S2", "Tempura Oyster Mushroom", "Fried Oyster Mushroom (Spicy / Original)", 9.90, R.drawable.tempura_oyster_mushroom),
        FoodItem("S3", "Sweet Plum Potato Fries", "Fired Sweet Orange Potato", 9.90, R.drawable.sweet_plum_potato_fried),
        FoodItem("S4", "High CP Salted Chicken", "Fried Salted Chicken (Spicy / Original)", 12.90, R.drawable.high_cp_salted_chicken),
        FoodItem("S5", "Taiwanese Belly Pork Chop", "Fried Juicy Pork Chop (Spicy / Original)", 14.90, R.drawable.taiwanese_belly_pork_chop),
        FoodItem("S6", "House Crispy Chicken Chop", "Fried Juicy Chicken Chop (Spicy / Original)", 13.90, R.drawable.house_crispy_chicken_chop),
        FoodItem("S7", "Sweet Not Spicy", "Tempura (No Spicy)", 12.90, R.drawable.sweet_not_spicy),
        FoodItem("D1", "Aloe Yakult Tea", "", 8.90, R.drawable.aloe_yakult_tea),
        FoodItem("D2", "TW Aiyu Jelly", "", 7.90, R.drawable.tw_aiyu_tea),
        FoodItem("D3", "Dark Aroma Lemon Tea", "", 5.90, R.drawable.dark_aroma_lemon_tea),
        FoodItem("D4", "Original Lemon Tea", "", 5.90, R.drawable.original_lemon_tea),
        FoodItem("D5", "Earl Grey Milk Tea", "", 7.90, R.drawable.earl_grey_milk_tea),
        FoodItem("D6", "Pearl Earl Milk Tea", "", 8.90, R.drawable.pearl_earl_milk_tea),
        FoodItem("D7", "White Peach Milk Tea", "", 7.90, R.drawable.white_peach_milk_tea),
        FoodItem("D8", "Jasmine Milk Tea", "", 7.90, R.drawable.jasmine_milk_tea)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TaiwaneseHouseTheme {
        NavigationApp()
    }
}