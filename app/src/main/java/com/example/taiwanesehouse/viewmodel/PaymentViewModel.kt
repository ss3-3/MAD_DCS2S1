package com.example.taiwanesehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.taiwanesehouse.dataclass.PaymentResult
import com.example.taiwanesehouse.dataclass.PaymentState
import com.example.taiwanesehouse.dataclass.CardDetails
import com.example.taiwanesehouse.dataclass.EWalletDetails
import com.example.taiwanesehouse.enumclass.PaymentMethod
import com.example.taiwanesehouse.manager.PaymentDataManager
import java.text.SimpleDateFormat
import java.util.*

class PaymentViewModel : ViewModel() {

    // Main Payment State
    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    // Card Details
    private val _cardDetails = MutableStateFlow(CardDetails())
    val cardDetails: StateFlow<CardDetails> = _cardDetails.asStateFlow()

    // E-Wallet Details
    private val _ewalletDetails = MutableStateFlow(EWalletDetails())
    val ewalletDetails: StateFlow<EWalletDetails> = _ewalletDetails.asStateFlow()

    // Convenience properties for backward compatibility
    val paymentResult: PaymentResult?
        get() = _paymentState.value.paymentResult

    val errorMessage: String
        get() = _paymentState.value.errorMessage

    val isProcessing: Boolean
        get() = _paymentState.value.isProcessing

    // Initialize payment
    fun initializePayment(paymentMethod: PaymentMethod, totalAmount: Double) {
        _paymentState.value = _paymentState.value.copy(
            paymentMethod = paymentMethod,
            totalAmount = totalAmount,
            isProcessing = false,
            isSuccess = false,
            isError = false,
            paymentResult = null,
            errorMessage = ""
        )
    }

    // Card Details Updates
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

    // E-Wallet Details Updates
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

    // Validation
    fun validateCardDetails(): Pair<Boolean, String> {
        val card = _cardDetails.value

        return when {
            card.cardHolderName.isBlank() -> false to "Please enter card holder name"
            card.cardNumber.replace(" ", "").length != 16 -> false to "Please enter valid card number"
            card.expiryDate.length != 5 -> false to "Please enter valid expiry date (MM/YY)"
            card.cvv.length != 3 -> false to "Please enter valid CVV"
            else -> true to ""
        }
    }

    fun validateEWalletDetails(): Pair<Boolean, String> {
        val ewallet = _ewalletDetails.value

        return when {
            !ewallet.isOtpSent && ewallet.phoneNumber.length < 9 -> false to "Please enter valid phone number"
            ewallet.isOtpSent && ewallet.otp.length != 6 -> false to "Please enter 6-digit OTP"
            else -> true to ""
        }
    }

    // Process Payment
    fun processPayment(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Set processing state
            _paymentState.value = _paymentState.value.copy(
                isProcessing = true,
                isSuccess = false,
                isError = false
            )

            try {
                // Simulate payment processing delay
                delay(2000)

                // For demo purposes - 85% success rate
                val success = (0..100).random() > 15

                if (success) {
                    // Create successful payment result
                    val successResult = PaymentResult(
                        success = true,
                        orderId = "ORD${System.currentTimeMillis()}",
                        transactionId = "TXN${(1000000..9999999).random()}",
                        errorMessage = null,
                        timestamp = SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    )

                    _paymentState.value = _paymentState.value.copy(
                        isProcessing = false,
                        isSuccess = true,
                        isError = false,
                        paymentResult = successResult,
                        errorMessage = ""
                    )

                    // Store in PaymentDataManager for other screens
                    PaymentDataManager.setPaymentResult(successResult)

                } else {
                    // Create failed payment result
                    val errorMsg = when (_paymentState.value.paymentMethod) {
                        PaymentMethod.CARD -> "Card payment failed. Please check your card details and try again."
                        PaymentMethod.EWALLET -> "E-Wallet payment failed. Please verify your OTP and try again."
                        PaymentMethod.COUNTER -> "Counter payment failed. Please try again."
                    }

                    val failedResult = PaymentResult(
                        success = false,
                        orderId = null,
                        transactionId = null,
                        errorMessage = errorMsg,
                        timestamp = SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    )

                    _paymentState.value = _paymentState.value.copy(
                        isProcessing = false,
                        isSuccess = false,
                        isError = true,
                        paymentResult = failedResult,
                        errorMessage = errorMsg
                    )

                    // Store error in PaymentDataManager
                    PaymentDataManager.setErrorMessage(errorMsg)
                }

                onResult(success)

            } catch (e: Exception) {
                val networkErrorMsg = "Network error occurred. Please check your connection and try again."

                val errorResult = PaymentResult(
                    success = false,
                    orderId = null,
                    transactionId = null,
                    errorMessage = networkErrorMsg,
                    timestamp = SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                )

                _paymentState.value = _paymentState.value.copy(
                    isProcessing = false,
                    isSuccess = false,
                    isError = true,
                    paymentResult = errorResult,
                    errorMessage = networkErrorMsg
                )

                // Store network error in PaymentDataManager
                PaymentDataManager.setErrorMessage(networkErrorMsg)
                onResult(false)
            }
        }
    }

    // Reset payment state
    fun resetPaymentState() {
        _paymentState.value = PaymentState()
        _cardDetails.value = CardDetails()
        _ewalletDetails.value = EWalletDetails()
    }

    // Clear error
    fun clearError() {
        _paymentState.value = _paymentState.value.copy(
            isError = false,
            errorMessage = ""
        )
    }
}