package com.example.taiwanesehouse.payment

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.taiwanesehouse.viewmodel.PaymentViewModel

@Composable
fun CardPaymentFormWithViewModel(
    viewModel: PaymentViewModel,
    totalAmount: Double,
    onPaymentSubmit: () -> Unit,
    isProcessing: Boolean
) {
    val context = LocalContext.current
    val cardDetails by viewModel.cardDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 600.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Enter your card details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = cardDetails.cardHolderName,
                onValueChange = viewModel::updateCardHolderName,
                label = { Text("Card Holder Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cardDetails.cardNumber,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(16)
                    val formatted = digits.chunked(4).joinToString(" ")
                    viewModel.updateCardNumber(formatted)
                },
                label = { Text("Card Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("1234 5678 9012 3456") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cardDetails.expiryDate,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        val formatted = when {
                            digits.length <= 2 -> digits
                            else -> "${digits.take(2)}/${digits.drop(2)}"
                        }
                        viewModel.updateExpiryDate(formatted)
                    },
                    label = { Text("Expiry") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("MM/YY") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardDetails.cvv,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(3)
                        viewModel.updateCvv(digits)
                    },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("123") },
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val (isValid, errorMsg) = viewModel.validateCardDetails()
                if (isValid) {
                    onPaymentSubmit()
                } else {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFC107)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Pay RM %.2f".format(totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}