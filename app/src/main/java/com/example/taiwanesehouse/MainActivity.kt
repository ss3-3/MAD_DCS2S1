// MainActivity with proper AndroidViewModel usage
package com.example.taiwanesehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable  
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taiwanesehouse.database.DatabaseInitializer
import com.example.taiwanesehouse.enumclass.*
import com.example.taiwanesehouse.payment.*
import com.example.taiwanesehouse.order.OrderCheckoutScreen
import com.example.taiwanesehouse.order.OrderScreenWithDatabase
import com.example.taiwanesehouse.ui.theme.TaiwaneseHouseTheme
import com.example.taiwanesehouse.user_profile.*
import com.example.taiwanesehouse.viewmodel.*
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Initialize database with food items synchronously to ensure data is ready
        DatabaseInitializer.initializeDatabaseSync(this)
        enableEdgeToEdge()
        setContent {
            TaiwaneseHouseTheme {
                NavigationApp()
            }
        }
    }
}

@Composable
fun NavigationApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.name) {
        composable(Screen.Signup.name) { SignUpScreen(navController = navController) }

        composable(Screen.Login.name) { LoginScreen(navController = navController) }

        composable(Screen.ForgotPassword.name) { ForgotPasswordScreen(navController = navController) }

        // No need to pass ViewModel - it will be created automatically
        composable(Screen.Menu.name) { MenuScreenWithDatabase(navController) }

        composable(
            route = "order/{foodId}",
            arguments = listOf(
                navArgument("foodId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
            OrderScreenWithDatabase(
                navController = navController,
                foodId = foodId
                // Remove the foodItemViewModel parameter - it will be created automatically
            )
        }

        composable(Screen.Cart.name) { CartScreen(navController) }

        composable(Screen.Order.name) { OrderCheckoutScreen(navController) }

        composable(Screen.Payment.name) { Payment(navController = navController) }

        // Card/Ewallet payment pages with args: orderId and amount
        composable(
            route = "CardPayment/{orderId}/{amount}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val amount = (backStackEntry.arguments?.getFloat("amount") ?: 0f).toDouble()
            com.example.taiwanesehouse.payment.CardPaymentPage(
                navController = navController,
                orderId = orderId,
                amount = amount
            )
        }

        composable(
            route = "EwalletPayment/{orderId}/{amount}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val amount = (backStackEntry.arguments?.getFloat("amount") ?: 0f).toDouble()
            com.example.taiwanesehouse.payment.EwalletPaymentPage(
                navController = navController,
                orderId = orderId,
                amount = amount
            )
        }

        composable(Screen.Profile.name) { UserProfileScreen(navController = navController) }

        composable(UserProfile.EditName.name) { EditNameScreen(navController = navController) }

        composable(UserProfile.PasswordUpdate.name) { PasswordUpdateScreen(navController = navController) }

        composable(UserProfile.PaymentHistory.name) { PaymentHistoryScreen(navController = navController) }

        composable(UserProfile.Feedback.name) { FeedbackScreen(navController = navController) }

        composable(UserProfile.Logout.name) { LogoutScreen(navController = navController) }

        composable("VerifyEmail") { VerifyEmailScreen(navController = navController) }
        composable("ChangeEmail") { ChangeEmailScreen(navController = navController) }
    }
}