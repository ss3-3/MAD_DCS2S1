package com.example.taiwanesehouse

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen() {
    var showSuccess by remember { mutableStateOf(false) }
    var showCardSheet by remember { mutableStateOf(false) }
    var showTngSheet by remember { mutableStateOf(false) }
    var showCounter by remember { mutableStateOf(false) }
    var paymentTime by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }

    if (showSuccess) {
        PaymentSuccessScreen(
            transactionId = transactionId,
            paymentTime = paymentTime,
            onBackClick = { showSuccess = false },
            onHomeClick = { showSuccess = false }
        )
    } else {
        PaymentMethodScreen(
            onBackClick = {},
            onCardPay = { showCardSheet = true },
            onTngPay = { showTngSheet = true },
            onCounterPay = { showCounter = true }
        )
    }

    if (showCardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCardSheet = false },
            containerColor = Color.White
        ) {
            CardPaymentFormSheet(
                onConfirm = { time ->
                    showCardSheet = false
                    paymentTime = time
                    transactionId = "TXN-${(100000..999999).random()}"
                    showSuccess = true
                }
            )
        }
    }

    if (showTngSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTngSheet = false },
            containerColor = Color.White
        ) {
            TngPaymentFormSheet(
                onConfirm = { time ->
                    showTngSheet = false
                    paymentTime = time
                    transactionId = "TXN-${(100000..999999).random()}"
                    showSuccess = true
                }
            )
        }
    }

    if (showCounter) {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        CounterPaymentScreen(
            paymentTime = time,
            onBackClick = { showCounter = false },
            onHomeClick = { showCounter = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    onBackClick: () -> Unit = {},
    onCardPay: () -> Unit,
    onTngPay: () -> Unit,
    onCounterPay: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Card") }

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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            PaymentMethod(
                "💳",
                "Credit or Debit Card",
                selectedMethod == "Card") {
                selectedMethod = "Card"
            }

            PaymentMethod(
                "📱",
                "E-Wallet",
                selectedMethod == "E-Wallet") {
                selectedMethod = "E-Wallet"
            }

            PaymentMethod(
                "🏪",
                "Pay at Counter",
                selectedMethod == "Counter") {
                selectedMethod = "Counter"
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    when (selectedMethod) {
                        "Card" -> onCardPay()
                        "E-Wallet" -> onTngPay()
                        "Counter" -> onCounterPay()
                    }
                },
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
fun CardPaymentFormSheet(onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var cardField by remember { mutableStateOf(TextFieldValue("")) }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Enter Card Details", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cardField,
            onValueChange = { newValue ->
                if (newValue.composition != null) {
                    cardField = newValue
                    return@OutlinedTextField
                }

                val digits = newValue.text.filter { it.isDigit() }.take(16)
                val formatted = digits.chunked(4).joinToString(" ")
                val digitsBeforeCursor = newValue.text.take(newValue.selection.start).count { it.isDigit() }
                val spacesBefore = digitsBeforeCursor / 4
                val newCursorPos = (digitsBeforeCursor + spacesBefore).coerceAtMost(formatted.length)

                cardField = TextFieldValue(
                    text = formatted,
                    selection = TextRange(newCursorPos)
                )
            },
            label = { Text("Card Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = expiryDate,
            onValueChange = {},
            label = { Text("Expiry Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, y, m, _ -> expiryDate = String.format(
                            Locale.getDefault(), "%02d/%02d", m + 1, y % 100
                        ) },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
            enabled = false,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cvv,
            onValueChange = { input -> cvv = input.filter { ch -> ch.isDigit() }.take(4) },
            label = { Text("CVV") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val cleanCardLen = cardField.text.replace(" ", "").length
                if (cleanCardLen == 16 && cvv.length in 3..4 && expiryDate.isNotBlank()) {
                    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    onConfirm(time)
                } else {
                    Toast.makeText(context, "Invalid card details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm Payment",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun TngPaymentFormSheet(onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf("Malaysia") }
    var countryCode by remember { mutableStateOf("+60") }
    var expanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("TNG eWallet", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(countryCode)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Malaysia (+60)") },
                        onClick = {
                            selectedCountry = "Malaysia"
                            countryCode = "+60"
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Singapore (+65)") },
                        onClick = {
                            selectedCountry = "Singapore"
                            countryCode = "+65"
                            expanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { ch -> ch.isDigit() } },
                label = { Text("Phone Number") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("Enter OTP") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val validPhone = when (countryCode) {
                    "+60" -> phone.length in 9..10
                    "+65" -> phone.length == 8
                    else -> false
                }
                if (validPhone && otp.length == 6) {
                    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    onConfirm(time)
                } else {
                    Toast.makeText(context, "Invalid Phone or OTP", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Confirm Payment",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSuccessScreen(
    transactionId: String,
    paymentTime: String,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Payment Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Payment\nSuccessful",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Transaction ID: $transactionId",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text(
                "Order Confirmed",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                "Today at $paymentTime",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onHomeClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Back to Home",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            BottomNavigationBar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterPaymentScreen(
    paymentTime: String,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Payment Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Order Placed",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Please proceed to cashier to complete your order.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Order Confirmed",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                "Today at $paymentTime",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onHomeClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Back to Home",
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
fun PaymentMethod(
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
