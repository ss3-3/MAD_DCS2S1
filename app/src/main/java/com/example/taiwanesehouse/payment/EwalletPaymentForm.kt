package com.example.taiwanesehouse.payment

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
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
fun EWalletPaymentFormWithViewModel(
    viewModel: PaymentViewModel,
    totalAmount: Double,
    onPaymentSubmit: () -> Unit,
    isProcessing: Boolean
) {
    val context = LocalContext.current
    val ewalletDetails by viewModel.ewalletDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 600.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "E-Wallet Payment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = ewalletDetails.phoneNumber,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(11)
                    viewModel.updatePhoneNumber(digits)
                },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                placeholder = { Text("0123456789") },
                prefix = { Text("+60 ") },
                singleLine = true,
                enabled = !ewalletDetails.isOtpSent
            )

            if (!ewalletDetails.isOtpSent) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val (isValid, errorMsg) = viewModel.validateEWalletDetails()
                        if (isValid) {
                            viewModel.sendOtp()
                            Toast.makeText(context, "OTP sent to +60${ewalletDetails.phoneNumber}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Send OTP", color = Color.White)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ewalletDetails.otp,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(6)
                        viewModel.updateOtp(digits)
                    },
                    label = { Text("Enter OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("123456") },
                    singleLine = true,
                    supportingText = {
                        Text("OTP sent to +60${ewalletDetails.phoneNumber}")
                    }
                )

                TextButton(
                    onClick = viewModel::resetOtp
                ) {
                    Text("Change Phone Number", color = Color(0xFFFFC107))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!ewalletDetails.isOtpSent) {
                    val (isValid, errorMsg) = viewModel.validateEWalletDetails()
                    if (isValid) {
                        viewModel.sendOtp()
                        Toast.makeText(context, "OTP sent to +60${ewalletDetails.phoneNumber}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val (isValid, errorMsg) = viewModel.validateEWalletDetails()
                    if (isValid) {
                        onPaymentSubmit()
                    } else {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
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
                    if (ewalletDetails.isOtpSent) "Pay RM %.2f".format(totalAmount) else "Send OTP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}