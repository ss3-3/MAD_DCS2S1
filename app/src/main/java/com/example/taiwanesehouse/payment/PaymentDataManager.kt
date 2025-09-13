package com.example.taiwanesehouse.manager

import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.dataclass.PaymentResult

object PaymentDataManager {
    private var _paymentResult: PaymentResult? = null
    private var _errorMessage: String = ""

    // New fields for cart data
    private var _cartItems: List<CartItem> = emptyList()
    private var _subtotal: Double = 0.0
    private var _coinDiscount: Double = 0.0
    private var _finalTotal: Double = 0.0
    private var _coinsUsed: Int = 0

    fun setPaymentResult(result: PaymentResult) {
        _paymentResult = result
    }

    fun setErrorMessage(message: String) {
        _errorMessage = message
    }

    fun getPaymentResult(): PaymentResult? = _paymentResult

    fun getErrorMessage(): String = _errorMessage

    fun clear() {
        _paymentResult = null
        _errorMessage = ""
    }

    fun setCartData(cartItems: List<CartItem>, subtotal: Double, coinDiscount: Double, finalTotal: Double, coinsUsed: Int) {
        _cartItems = cartItems
        _subtotal = subtotal
        _coinDiscount = coinDiscount
        _finalTotal = finalTotal
        _coinsUsed = coinsUsed
    }
}