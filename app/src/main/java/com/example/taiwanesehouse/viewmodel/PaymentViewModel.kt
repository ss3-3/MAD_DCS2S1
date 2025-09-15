package com.example.taiwanesehouse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.database.entities.PaymentEntity
import com.example.taiwanesehouse.payment.FirebasePaymentManager
import com.example.taiwanesehouse.payment.PaymentResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val paymentDao = AppDatabase.getDatabase(application).paymentDao()
    private val firebasePaymentManager = FirebasePaymentManager(application)
    private val auth = FirebaseAuth.getInstance()

    // Payment processing state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    // Card payment details
    private val _cardDetails = MutableStateFlow(CardDetails())
    val cardDetails: StateFlow<CardDetails> = _cardDetails.asStateFlow()

    // E-wallet payment details
    private val _ewalletDetails = MutableStateFlow(EWalletDetails())
    val ewalletDetails: StateFlow<EWalletDetails> = _ewalletDetails.asStateFlow()

    // User's payment history
    private val _paymentHistory = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val paymentHistory: StateFlow<List<PaymentEntity>> = _paymentHistory.asStateFlow()

    companion object {
        private const val TAG = "PaymentViewModel"
    }

    init {
        loadPaymentHistory()
    }

    // Card Details Management
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

    // E-Wallet Details Management
    fun updatePhoneNumber(phone: String) {
        _ewalletDetails.value = _ewalletDetails.value.copy(phoneNumber = phone)
    }

    fun updateOtp(otp: String) {
        _ewalletDetails.value = _ewalletDetails.value.copy(otp = otp)
    }

    fun sendOtp() {
        _ewalletDetails.value = _ewalletDetails.value.copy(
            isOtpSent = true,
            otp = ""
        )
    }

    fun resetOtp() {
        _ewalletDetails.value = _ewalletDetails.value.copy(
            isOtpSent = false,
            otp = ""
        )
    }

    // Validation Methods
    fun validateCardDetails(): Pair<Boolean, String> {
        val details = _cardDetails.value

        return when {
            details.cardHolderName.isBlank() -> false to "Please enter card holder name"
            details.cardNumber.replace(" ", "").length != 16 -> false to "Please enter a valid 16-digit card number"
            !isValidCardNumber(details.cardNumber.replace(" ", "")) -> false to "Please enter a valid card number"
            details.expiryDate.length != 5 || !details.expiryDate.contains("/") -> false to "Please enter expiry date in MM/YY format"
            !isValidExpiryDate(details.expiryDate) -> false to "Card has expired or invalid expiry date"
            details.cvv.length != 3 -> false to "Please enter a valid 3-digit CVV"
            else -> true to ""
        }
    }

    fun validateEWalletDetails(): Pair<Boolean, String> {
        val details = _ewalletDetails.value

        return when {
            details.phoneNumber.length !in 10..11 -> false to "Please enter a valid phone number"
            !details.phoneNumber.startsWith("01") && details.phoneNumber.length == 10 -> false to "Phone number must start with 01"
            details.isOtpSent && details.otp.length != 6 -> false to "Please enter the 6-digit OTP"
            else -> true to ""
        }
    }

    // Payment Processing
    fun processPayment(
        orderId: String,
        amount: Double,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _paymentError.value = null

                // Create payment record
                val createResult = firebasePaymentManager.createPayment(
                    orderId = orderId,
                    amount = amount,
                    paymentMethod = paymentMethod
                )

                if (createResult.isFailure) {
                    _paymentError.value = "Failed to create payment record"
                    return@launch
                }

                val payment = createResult.getOrNull()!!

                // Prepare payment details based on method
                val paymentDetails = when (paymentMethod) {
                    "card" -> {
                        val cardDetails = _cardDetails.value
                        mapOf(
                            "cardNumber" to cardDetails.cardNumber.replace(" ", ""),
                            "expiryDate" to cardDetails.expiryDate,
                            "cvv" to cardDetails.cvv,
                            "cardHolderName" to cardDetails.cardHolderName
                        )
                    }
                    "ewallet" -> {
                        val ewalletDetails = _ewalletDetails.value
                        mapOf(
                            "phoneNumber" to ewalletDetails.phoneNumber,
                            "otp" to ewalletDetails.otp,
                            "walletType" to "TNG"
                        )
                    }
                    "counter" -> emptyMap()
                    else -> emptyMap()
                }

                // Process the payment
                val processResult = firebasePaymentManager.processPayment(payment, paymentDetails)

                if (processResult.isSuccess) {
                    val result = processResult.getOrNull()!!
                    _paymentResult.value = result

                    // Save to local database
                    savePaymentToLocalDB(payment.copy(
                        paymentStatus = result.status,
                        transactionId = result.transactionId,
                        paymentDate = if (result.success) Date() else null
                    ))

                    // Refresh payment history
                    loadPaymentHistory()

                    // Clear sensitive details after successful payment
                    if (result.success) {
                        clearPaymentDetails()
                    }
                } else {
                    _paymentError.value = processResult.exceptionOrNull()?.message
                        ?: "Payment processing failed"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Payment processing error", e)
                _paymentError.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Save payment to local database
    private suspend fun savePaymentToLocalDB(payment: PaymentEntity) {
        try {
            paymentDao.insertPayment(payment)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving payment to local DB", e)
        }
    }

    // Load user's payment history
    private fun loadPaymentHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _paymentHistory.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                paymentDao.getPaymentsByUserId(currentUser.uid).collect { payments ->
                    _paymentHistory.value = payments
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading payment history", e)
                _paymentHistory.value = emptyList()
            }
        }
    }

    // Get payment by ID
    fun getPaymentById(paymentId: String) {
        viewModelScope.launch {
            try {
                val payment = paymentDao.getPaymentById(paymentId)
                // Handle payment retrieval if needed
            } catch (e: Exception) {
                Log.e(TAG, "Error getting payment by ID", e)
            }
        }
    }

    // Update payment status
    fun updatePaymentStatus(paymentId: String, status: String) {
        viewModelScope.launch {
            try {
                paymentDao.updatePaymentStatus(
                    paymentId = paymentId,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                loadPaymentHistory() // Refresh history
            } catch (e: Exception) {
                Log.e(TAG, "Error updating payment status", e)
            }
        }
    }

    // Clear payment details for security
    private fun clearPaymentDetails() {
        _cardDetails.value = CardDetails()
        _ewalletDetails.value = EWalletDetails()
    }

    // Clear payment result
    fun clearPaymentResult() {
        _paymentResult.value = null
        _paymentError.value = null
    }

    // Clear all payment data
    fun clearAllPaymentData() {
        clearPaymentDetails()
        clearPaymentResult()
    }

    // Validation helper methods
    private fun isValidCardNumber(cardNumber: String): Boolean {
        // Luhn algorithm for basic card validation
        if (cardNumber.length != 16 || !cardNumber.all { it.isDigit() }) {
            return false
        }

        var sum = 0
        var isEven = false

        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].digitToInt()

            if (isEven) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            isEven = !isEven
        }

        return sum % 10 == 0
    }

    private fun isValidExpiryDate(expiryDate: String): Boolean {
        try {
            val parts = expiryDate.split("/")
            if (parts.size != 2) return false

            val month = parts[0].toInt()
            val year = parts[1].toInt() + 2000 // Convert YY to YYYY

            if (month !in 1..12) return false

            // Check if not expired
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

            return when {
                year > currentYear -> true
                year == currentYear && month >= currentMonth -> true
                else -> false
            }
        } catch (e: Exception) {
            return false
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clear sensitive data when ViewModel is destroyed
        clearPaymentDetails()
    }
}

// Data classes for payment details
data class CardDetails(
    val cardHolderName: String = "",
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = ""
)

data class EWalletDetails(
    val phoneNumber: String = "",
    val otp: String = "",
    val isOtpSent: Boolean = false
)