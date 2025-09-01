package com.example.taiwanesehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taiwanesehouse.ui.theme.TaiwaneseHouseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaiwaneseHouseTheme {
                PaymentScreen()
            }
        }
    }
}

enum class Screen {
    Signup,
    Menu,
    Order,
    Cart,
    Payment
}

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Payment.name) {
        composable(Screen.Signup.name) {
            SignUpScreen(
                navController = navController,
                name = "Taiwanese House",
                message = "Please enter your details to log in your account."
            )
        }
        composable(Screen.Menu.name) { Text("Menu Screen") }
        composable(Screen.Order.name) { Text("Order Screen") }
        composable(Screen.Cart.name) { Text("Cart Screen") }
        composable(Screen.Payment.name) { PaymentScreen() }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TaiwaneseHouseTheme {
        PaymentScreen()
    }
}
