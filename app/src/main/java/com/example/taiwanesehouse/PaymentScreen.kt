package com.example.taiwanesehouse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentScreen() {
    var currentScreen by remember { mutableStateOf("method") }
    var selectedMethod by remember { mutableStateOf("Card") }

    when (currentScreen) {
        "method" -> PaymentMethodScreen(
            selectedMethod = selectedMethod,
            onMethodSelected = { selectedMethod = it },
            onPayClick = {
                currentScreen = when (selectedMethod) {
                    "Card" -> "card"
                    "E-Wallet" -> "e-wallet"
                    "Counter" -> "counter"
                    else -> "method"
                }
            }
        )

        "card" -> CardPaymentScreen(onBackClick = { currentScreen = "method" })
        "e-wallet" -> EWalletPaymentScreen(onBackClick = { currentScreen = "method" })
        "counter" -> CounterPaymentScreen(onBackClick = { currentScreen = "method" })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onPayClick: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Cart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Payment Method",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            )

            PaymentOptionCard(
                icon = "💳",
                title = "Credit or Debit Card",
                selected = selectedMethod == "Card",
                onClick = { onMethodSelected("Card") }
            )

            PaymentOptionCard(
                icon = "📱",
                title = "E-Wallet",
                selected = selectedMethod == "E-Wallet",
                onClick = { onMethodSelected("E-Wallet") }
            )

            PaymentOptionCard(
                icon = "🏪",
                title = "Pay at Counter",
                selected = selectedMethod == "Counter",
                onClick = { onMethodSelected("Counter") }
            )

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onPayClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Pay RM37.00",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            BottomNavigationBar()
        }
    }
}

@Composable
fun PaymentOptionCard(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(icon, fontSize = 40.sp, modifier = Modifier.width(56.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = { onClick() })
        }
    }
}

@Composable
fun BottomNavigationBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", fontSize = 28.sp)
            Text("Menu", style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛒", fontSize = 28.sp)
            Text("Cart", style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👤", fontSize = 28.sp)
            Text("Me", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CardPaymentScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💳 Credit/Debit Card Payment Page", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onBackClick() }) {
            Text("Back")
        }
    }
}

@Composable
fun EWalletPaymentScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱 E-Wallet Payment Page", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onBackClick() }) {
            Text("Back")
        }
    }
}

@Composable
fun CounterPaymentScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏪 Pay at Counter Page", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onBackClick() }) {
            Text("Back")
        }
    }
}
