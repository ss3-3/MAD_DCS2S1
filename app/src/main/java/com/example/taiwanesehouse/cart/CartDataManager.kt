package com.example.taiwanesehouse.cart

import com.example.taiwanesehouse.dataclass.CartItem

object CartDataManager {
    private var cartItems: List<CartItem> = emptyList()
    private var subtotal: Double = 0.0
    private var coinDiscount: Double = 0.0
    private var finalTotal: Double = 0.0
    private var coinsUsed: Int = 0

    fun clear() {
        cartItems = emptyList()
        subtotal = 0.0
        coinDiscount = 0.0
        finalTotal = 0.0
        coinsUsed = 0
    }
}