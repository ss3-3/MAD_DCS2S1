package com.example.taiwanesehouse.payment

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taiwanesehouse.cart.CartDataManager
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.enumclass.Screen
import com.example.taiwanesehouse.order.OrderDataManager
import com.example.taiwanesehouse.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EwalletPaymentPage(
    navController: NavController,
    orderId: String,
    amount: Double,
    subtotal: Double = amount, // Kept for compatibility; actual values taken from OrderDataManager
    coinsUsed: Int = 0, // Kept for compatibility; actual values taken from OrderDataManager
    paymentManager: FirebasePaymentManager = run {
        val ctx = LocalContext.current
        remember(ctx) { FirebasePaymentManager(ctx) }
    },
    viewModel: PaymentViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isProcessing by paymentManager.isProcessing.collectAsState()
    val auth = remember { FirebaseAuth.getInstance() }
    // Read latest locked order values
    val coinsUsedVal = remember { OrderDataManager.getCoinsUsed() }
    val subtotalVal = remember { OrderDataManager.getSubtotal() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Wallet Payment", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFC107))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            EWalletPaymentFormWithViewModel(
                viewModel = viewModel,
                totalAmount = amount,
                isProcessing = isProcessing,
                onPaymentSubmit = {
                    scope.launch {
                        try {
                            val createResult = paymentManager.createPayment(
                                orderId = orderId,
                                amount = amount,
                                paymentMethod = "ewallet"
                            )
                            if (createResult.isSuccess) {
                                val payment = createResult.getOrNull()!!

                                val details = mapOf(
                                    "walletType" to "TNG",
                                    "phoneNumber" to viewModel.ewalletDetails.value.phoneNumber,
                                    "otp" to viewModel.ewalletDetails.value.otp
                                )

                                val processResult = paymentManager.processPayment(payment, details)
                                if (processResult.isSuccess) {
                                    // Clear confirmed order
                                    OrderDataManager.clear()

                                    // Deduct coins used (only now, after success)
                                    val userId = auth.currentUser?.uid
                                    if (userId != null && coinsUsedVal > 0) {
                                        try {
                                            com.example.taiwanesehouse.repository.UserRepository(
                                                AppDatabase.getDatabase(context).userDao()
                                            ).deductCoinsFromUser(userId, coinsUsedVal)
                                        } catch (_: Exception) {}
                                    }

                                    // Award coins based on ORIGINAL subtotal (before coin discount)
                                    val coinsEarned = kotlin.math.floor(subtotalVal).toInt()

                                    if (userId != null && coinsEarned > 0) {
                                        try {
                                            com.example.taiwanesehouse.repository.UserRepository(
                                                AppDatabase.getDatabase(context).userDao()
                                            ).addCoinsToUser(userId, coinsEarned)

                                            val message = if (coinsUsedVal > 0) {
                                                "Payment successful! You earned $coinsEarned coins (after using $coinsUsedVal coins)."
                                            } else {
                                                "Payment successful! You earned $coinsEarned coins."
                                            }

                                            Toast.makeText(
                                                context,
                                                message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Payment successful but failed to award coins",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Payment successful", Toast.LENGTH_SHORT).show()
                                    }

                                    CartDataManager.clear()

                                    // Force-refresh coins into Room from Firestore so UI updates immediately
                                    try {
                                        val uid = auth.currentUser?.uid
                                        if (uid != null) {
                                            val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                                            val latestCoins = doc.getLong("coins")?.toInt() ?: 0
                                            AppDatabase.getDatabase(context).userDao().updateUserCoins(uid, latestCoins)
                                        }
                                    } catch (_: Exception) {}
                                    navController.navigate(Screen.Menu.name) {
                                        // Clear the entire back stack to prevent returning to payment
                                        popUpTo(Screen.Menu.name) { inclusive = false }
                                    }
                                } else {
                                    Toast.makeText(context, "Payment failed", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to create payment", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}