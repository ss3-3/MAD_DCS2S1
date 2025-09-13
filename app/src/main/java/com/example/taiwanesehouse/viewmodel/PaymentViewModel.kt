// Updated PaymentViewModel to include cart items
package com.example.taiwanesehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.dataclass.CardDetails
import com.example.taiwanesehouse.dataclass.EWalletDetails
import com.example.taiwanesehouse.dataclass.PaymentState
import com.example.taiwanesehouse.enumclass.PaymentMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {
    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _cardDetails = MutableStateFlow(CardDetails())
    val cardDetails: StateFlow<CardDetails> = _cardDetails.asStateFlow()

    private val _ewalletDetails = MutableStateFlow(EWalletDetails())
    val ewalletDetails: StateFlow<EWalletDetails> = _ewalletDetails.asStateFlow()

    fun initializePayment(paymentMethod: PaymentMethod, totalAmount: Double) {
        _paymentState.value = _paymentState.value.copy(
            paymentMethod = paymentMethod,
            totalAmount = totalAmount
        )
    }

    // Card details update methods
    fun updateCardHolderName(name: String) {
        _cardDetails.value = _cardDetails.value.copy(cardHolderName = name)
    }

    fun updateCardNumber(number: String) {
        _cardDetails.value = _cardDetails.value.copy(cardNumber = number)
    }

    fun updateExpiryDate(date: String) {
        _cardDetails.value = _cardDetails.value.copy(expiryDate = date)
    }

    fun updateCvv(cvv: String) {
        _cardDetails.value = _cardDetails.value.copy(cvv = cvv)
    }

    // E-wallet details update methods
    fun updatePhoneNumber(phone: String) {
        _ewalletDetails.value = _ewalletDetails.value.copy(phoneNumber = phone)
    }

    fun updateOtp(otp: String) {
        _ewalletDetails.value = _ewalletDetails.value.copy(otp = otp)
    }

    fun sendOtp() {
        _ewalletDetails.value = _ewalletDetails.value.copy(isOtpSent = true)
    }

    fun resetOtp() {
        _ewalletDetails.value = _ewalletDetails.value.copy(
            isOtpSent = false,
            otp = ""
        )
    }

    // Validation methods
    fun validateCardDetails(): Pair<Boolean, String> {
        val details = _cardDetails.value
        return when {
            details.cardHolderName.isBlank() -> false to "Card holder name is required"
            details.cardNumber.replace(" ", "").length != 16 -> false to "Invalid card number"
            details.expiryDate.length != 5 -> false to "Invalid expiry date"
            details.cvv.length != 3 -> false to "Invalid CVV"
            else -> true to ""
        }
    }

    fun validateEWalletDetails(): Pair<Boolean, String> {
        val details = _ewalletDetails.value
        return when {
            details.phoneNumber.length < 9 -> false to "Invalid phone number"
            details.isOtpSent && details.otp.length != 6 -> false to "Invalid OTP"
            else -> true to ""
        }
    }

    // Process payment method
    fun processPayment(onResult: (Boolean) -> Unit) {
        _paymentState.value = _paymentState.value.copy(isProcessing = true)

        viewModelScope.launch {
            delay(2000) // Simulate network call
            val success = kotlin.random.Random.nextBoolean() // Random success for demo

            _paymentState.value = _paymentState.value.copy(isProcessing = false)
            onResult(success)
        }
    }
}