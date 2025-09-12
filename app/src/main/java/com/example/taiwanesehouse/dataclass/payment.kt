package com.example.taiwanesehouse.dataclass
import com.example.taiwanesehouse.CartItem
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

data class PaymentResult(
    val success: Boolean,
    val orderId: String? = null,
    val transactionId: String? = null,
    val errorMessage: String? = null,
    val timestamp: String? = null
)

data class PaymentState(
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val totalAmount: Double = 0.0,
    val isProcessing: Boolean = false,
    val paymentResult: PaymentResult? = null,
    val errorMessage: String = "",
    val isSuccess: Boolean = false,
    val isError: Boolean = false
)

data class CardDetails(
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val cardHolderName: String = ""
)

data class EWalletDetails(
    val phoneNumber: String = "",
    val otp: String = "",
    val isOtpSent: Boolean = false
)