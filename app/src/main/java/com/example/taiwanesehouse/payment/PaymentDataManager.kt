package com.example.taiwanesehouse.manager

import com.example.taiwanesehouse.dataclass.PaymentResult

object PaymentDataManager {
    private var _paymentResult: PaymentResult? = null
    private var _errorMessage: String = ""

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
}