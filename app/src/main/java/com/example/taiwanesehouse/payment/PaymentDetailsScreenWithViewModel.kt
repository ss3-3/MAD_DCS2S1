package com.example.taiwanesehouse.payment

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taiwanesehouse.*
import com.example.taiwanesehouse.dataclass.PaymentResult
import com.example.taiwanesehouse.enumclass.*
import com.example.taiwanesehouse.manager.PaymentDataManager
import com.example.taiwanesehouse.viewmodel.*
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreenWithViewModel(
    viewModel: PaymentViewModel,
    navController: NavController,
    onBackClick: () -> Unit
) {
    val paymentState by viewModel.paymentState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (paymentState.paymentMethod) {
                            PaymentMethod.CARD -> "Card Details"
                            PaymentMethod.EWALLET -> "E-Wallet Details"
                            PaymentMethod.COUNTER -> "Counter Payment"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                when (paymentState.paymentMethod) {
                    PaymentMethod.CARD -> {
                        CardPaymentFormWithViewModel(
                            viewModel = viewModel,
                            totalAmount = paymentState.totalAmount,
                            onPaymentSubmit = {
                                // Process payment and handle navigation
                                viewModel.processPayment { success ->
                                    if (success) {
                                        // Navigate to success screen - make sure route matches exactly
                                        navController.navigate(Payment.PaymentSuccess.name) {
                                            // Optional: Remove the payment screen from back stack
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        // Navigate to error screen - make sure route matches exactly
                                        navController.navigate(Payment.PaymentError.name) {
                                            // Optional: Remove the payment screen from back stack
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            },
                            isProcessing = paymentState.isProcessing
                        )
                    }
                    PaymentMethod.EWALLET -> {
                        EWalletPaymentFormWithViewModel(
                            viewModel = viewModel,
                            totalAmount = paymentState.totalAmount,
                            onPaymentSubmit = {
                                // Process payment and handle navigation
                                viewModel.processPayment { success ->
                                    if (success) {
                                        // Navigate to success screen - make sure route matches exactly
                                        navController.navigate(Payment.PaymentSuccess.name) {
                                            // Optional: Remove the payment screen from back stack
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        // Navigate to error screen - make sure route matches exactly
                                        navController.navigate(Payment.PaymentError.name) {
                                            // Optional: Remove the payment screen from back stack
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            },
                            isProcessing = paymentState.isProcessing
                        )
                    }
                    PaymentMethod.COUNTER -> {
                        // Counter payment - simple implementation
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Counter Payment",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                "Please proceed to the counter to complete your payment of RM ${paymentState.totalAmount}",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    // For counter payment, assume success and navigate
                                    PaymentDataManager.setPaymentResult(
                                        PaymentResult(
                                            success = true,
                                            orderId = "ORD${System.currentTimeMillis()}",
                                            transactionId = "COUNTER${System.currentTimeMillis()}",
                                            errorMessage = null,
                                            timestamp = SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm:ss",
                                                Locale.getDefault()
                                            ).format(Date())
                                        )
                                    )
                                    navController.navigate(Payment.PaymentSuccess.name)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107)
                                )
                            ) {
                                Text(
                                    "Confirm Counter Payment",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}