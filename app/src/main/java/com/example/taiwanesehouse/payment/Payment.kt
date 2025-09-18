package com.example.taiwanesehouse.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taiwanesehouse.*
import com.example.taiwanesehouse.enumclass.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Payment(
    navController: NavController,
    cartManager: FirebaseCartManager = remember { FirebaseCartManager() }
) {
    val scope = rememberCoroutineScope()
    val cartItems by cartManager.cartItems.collectAsState()
    val coinsToUse by cartManager.coinsToUse.collectAsState()

    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Calculate totals
    val subtotal = cartItems.sumOf { it.getTotalPrice() }
    // Align discount: 1 coin = RM0.10
    val coinDiscount = coinsToUse * 0.10
    val finalTotal = (subtotal - coinDiscount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Choose Payment Method",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Gray.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Order Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Items (${cartItems.sumOf { it.foodQuantity }}):",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "RM %.2f".format(subtotal),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        if (coinsToUse > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Coin Discount:",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "-RM %.2f".format(coinDiscount),
                                    fontSize = 14.sp,
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "RM %.2f".format(finalTotal),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Payment Methods Title
            item {
                Text(
                    text = "Select Payment Method",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Credit/Debit Card Option
            item {
                PaymentMethodCard(
                    title = "Credit/Debit Card",
                    subtitle = "Visa, Mastercard, American Express",
                    emoji = "💳",
                    isSelected = selectedPaymentMethod == PaymentMethod.CARD,
                    onClick = { selectedPaymentMethod = PaymentMethod.CARD }
                )
            }

            // E-Wallet Option
            item {
                PaymentMethodCard(
                    title = "E-Wallet",
                    subtitle = "Touch 'n Go, GrabPay, Boost, ShopeePay",
                    emoji = "📱",
                    isSelected = selectedPaymentMethod == PaymentMethod.EWALLET,
                    onClick = { selectedPaymentMethod = PaymentMethod.EWALLET }
                )
            }

            // Counter Option
            item {
                PaymentMethodCard(
                    title = "Pay at Counter",
                    subtitle = "Dine-in, Take-out",
                    emoji = "📱",
                    isSelected = selectedPaymentMethod == PaymentMethod.COUNTER,
                    onClick = { selectedPaymentMethod = PaymentMethod.COUNTER }
                )
            }

            // Continue Button
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        selectedPaymentMethod?.let { method ->
                            isLoading = true
                            scope.launch {
                                try {
                                    // Navigate to payment details with selected method and total
                                    navController.navigate(
                                        "${Screen.Payment.name}/${method.name}/${finalTotal.toFloat()}"
                                    )
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = selectedPaymentMethod != null && !isLoading && finalTotal > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Continue to Payment",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Security Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Your payment information is secure and encrypted",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        Color(0xFFFFC107),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.border(
                        1.dp,
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFFFC107).copy(alpha = 0.1f)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Payment Method Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Payment Method Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Selection Indicator
            RadioButton(
                selected = isSelected,
                onClick = { onClick() },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFFFC107),
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}