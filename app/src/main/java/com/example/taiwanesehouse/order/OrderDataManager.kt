package com.example.taiwanesehouse.order

import com.example.taiwanesehouse.dataclass.CartItem

object OrderDataManager {
    private val confirmedItems: MutableList<CartItem> = mutableListOf()
    private var subtotal: Double = 0.0
    private var coinDiscount: Double = 0.0
    private var finalTotal: Double = 0.0
    private var coinsUsed: Int = 0

    fun appendItems(newItems: List<CartItem>, coinsUsedNow: Int = 0) {
        confirmedItems.addAll(newItems)
        coinsUsed += coinsUsedNow
        recalcTotals()
    }

    fun getItems(): List<CartItem> = confirmedItems.toList()

    fun getSubtotal(): Double = subtotal
    fun getCoinDiscount(): Double = coinDiscount
    fun getFinalTotal(): Double = finalTotal
    fun getCoinsUsed(): Int = coinsUsed

    fun clear() {
        confirmedItems.clear()
        subtotal = 0.0
        coinDiscount = 0.0
        finalTotal = 0.0
        coinsUsed = 0
    }

    fun setCoinsUsed(newCoinsUsed: Int) {
        coinsUsed = if (newCoinsUsed < 0) 0 else newCoinsUsed
        recalcTotals()
    }

    private fun recalcTotals() {
        subtotal = confirmedItems.sumOf { it.getTotalPrice() }
        coinDiscount = coinsUsed * 0.10
        finalTotal = (subtotal - coinDiscount).coerceAtLeast(0.0)
    }
}


