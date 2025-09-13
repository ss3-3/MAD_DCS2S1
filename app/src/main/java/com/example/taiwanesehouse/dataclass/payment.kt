package com.example.taiwanesehouse.dataclass
import com.example.taiwanesehouse.database.entities.PaymentEntity
import com.example.taiwanesehouse.enumclass.PaymentMethod
import java.util.*

data class PaymentRequest(
    val paymentMethod: PaymentMethod,
    val amount: Double,
    val coinsUsed: Int = 0,
    val coinDiscount: Double = 0.0,
    val subtotal: Double,
    val cartItems: List<CartItem>
)

data class PaymentHistoryState(
    val payments: List<PaymentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PaymentResult(
    val success: Boolean,
    val orderId: String? = null,
    val transactionId: String? = null,
    val errorMessage: String? = null,
    val timestamp: String? = null
)

// PaymentState data class - THIS WAS MISSING
data class PaymentState(
    val paymentMethod: PaymentMethod? = null,
    val totalAmount: Double = 0.0,
    val cartItems: List<CartItem> = emptyList(),
    val isProcessing: Boolean = false
)

// Card details data class
data class CardDetails(
    val cardHolderName: String = "",
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = ""
)

// E-wallet details data class
data class EWalletDetails(
    val phoneNumber: String = "",
    val otp: String = "",
    val isOtpSent: Boolean = false
)