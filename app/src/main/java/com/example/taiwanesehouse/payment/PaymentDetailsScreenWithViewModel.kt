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

    // Add logging to debug the payment state
    LaunchedEffect(paymentState) {
        android.util.Log.d("PaymentDebug", "Payment method: ${paymentState.paymentMethod}, Amount: ${paymentState.totalAmount}")
    }

    // Handle null payment method case BEFORE using it in the UI
    if (paymentState.paymentMethod == null) {
        // Show loading or error state instead of crashing
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFFFFC107))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading payment details...")
            }
        }
        return // Exit early if payment method is null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        // FIX: Remove TODO() and handle null case above
                        when (paymentState.paymentMethod!!) { // Safe to use !! here because we checked above
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
                // This is also safe now because we handled null above
                when (paymentState.paymentMethod!!) {
                    PaymentMethod.CARD -> {
                        CardPaymentFormWithViewModel(
                            viewModel = viewModel,
                            totalAmount = paymentState.totalAmount,
                            onPaymentSubmit = {
                                viewModel.processPayment { success ->
                                    if (success) {
                                        navController.navigate(Payment.PaymentSuccess.name) {
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(Payment.PaymentError.name) {
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
                                viewModel.processPayment { success ->
                                    if (success) {
                                        navController.navigate(Payment.PaymentSuccess.name) {
                                            popUpTo("${Screen.Payment.name}/{paymentMethod}/{totalAmount}") {
                                                inclusive = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(Payment.PaymentError.name) {
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

// ALTERNATIVE APPROACH - More robust null handling:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsScreenWithViewModelSafe(
    viewModel: PaymentViewModel,
    navController: NavController,
    onBackClick: () -> Unit
) {
    val paymentState by viewModel.paymentState.collectAsState()

    // Safe title handling
    val screenTitle = when (paymentState.paymentMethod) {
        PaymentMethod.CARD -> "Card Details"
        PaymentMethod.EWALLET -> "E-Wallet Details"
        PaymentMethod.COUNTER -> "Counter Payment"
        null -> "Payment Details" // Safe fallback
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        screenTitle,
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
                // Safe content handling
                when (paymentState.paymentMethod) {
                    PaymentMethod.CARD -> {
                        // Your card payment form
                    }
                    PaymentMethod.EWALLET -> {
                        // Your ewallet payment form
                    }
                    PaymentMethod.COUNTER -> {
                        // Your counter payment form
                    }
                    null -> {
                        // Show error or loading state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading payment method...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC107)
                                )
                            ) {
                                Text("Go Back", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}