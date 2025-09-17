package com.example.taiwanesehouse.order

import com.example.taiwanesehouse.dataclass.CartItem

object OrderDataManager {
    private val confirmedItems: MutableList<CartItem> = mutableListOf()
    private var subtotal: Double = 0.0
    private var coinDiscount: Double = 0.0
    private var finalTotal: Double = 0.0
    private var coinsUsed: Int = 0
    private var voucher: Voucher? = null
    private var voucherDiscount: Double = 0.0

    // Constants for coin system
    private const val COIN_VALUE = 0.10 // 1 coin = RM 0.10 discount
    private const val COINS_PER_RM_SPENT = 1.0 // 1 coin earned per RM 1 spent

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

    fun getVoucherDiscount(): Double = voucherDiscount
    fun getAppliedVoucher(): Voucher? = voucher

    fun getCoinValue(): Double = COIN_VALUE

    fun getCoinsPerRMSpent(): Double = COINS_PER_RM_SPENT

    fun clear() {
        confirmedItems.clear()
        subtotal = 0.0
        coinDiscount = 0.0
        finalTotal = 0.0
        coinsUsed = 0
        voucher = null
        voucherDiscount = 0.0
    }

    fun setCoinsUsed(newCoinsUsed: Int) {
        coinsUsed = if (newCoinsUsed < 0) 0 else newCoinsUsed
        recalcTotals()
    }

    /**
     * Get maximum coins that can be used for the current order
     * based on subtotal (1 coin = RM 0.10 discount)
     */
    fun getMaxCoinsBySubtotal(): Int {
        val maxAfterVoucher = (subtotal - voucherDiscount).coerceAtLeast(0.0)
        return kotlin.math.floor(maxAfterVoucher / COIN_VALUE).toInt()
    }

    /**
     * Get maximum coins that can be used considering both subtotal and user balance
     */
    fun getMaxCoinsUsable(userCoinBalance: Int): Int {
        return minOf(userCoinBalance, getMaxCoinsBySubtotal())
    }

    /**
     * Calculate coins that will be earned from this order
     * Based on original subtotal (before coin discount)
     */
    fun getCoinsToEarn(): Int {
        return kotlin.math.floor(subtotal * COINS_PER_RM_SPENT).toInt()
    }

    /**
     * Validate if the given number of coins can be used
     */
    fun canUseCoins(coinsToUse: Int, userCoinBalance: Int): Boolean {
        return coinsToUse >= 0 &&
                coinsToUse <= userCoinBalance &&
                coinsToUse <= getMaxCoinsBySubtotal()
    }

    /**
     * Set coins used with validation
     */
    fun setCoinsUsedWithValidation(newCoinsUsed: Int, userCoinBalance: Int): Boolean {
        if (canUseCoins(newCoinsUsed, userCoinBalance)) {
            setCoinsUsed(newCoinsUsed)
            return true
        }
        return false
    }

    private fun recalcTotals() {
        subtotal = confirmedItems.sumOf { it.getTotalPrice() }
        // Calculate voucher discount first
        voucherDiscount = calculateVoucherDiscount(subtotal, voucher)
        // Then apply coins, clamped so total never goes below zero
        val maxCoinsBySubtotal = getMaxCoinsBySubtotal()
        if (coinsUsed > maxCoinsBySubtotal) {
            coinsUsed = maxCoinsBySubtotal
        }
        coinDiscount = (coinsUsed * COIN_VALUE)
        val totalBeforeClamp = subtotal - voucherDiscount - coinDiscount
        finalTotal = totalBeforeClamp.coerceAtLeast(0.0)
    }

    fun isEmpty(): Boolean = confirmedItems.isEmpty()

    /**
     * Restore state from a persisted order (e.g., Firebase order document)
     */
    fun restoreFromPersistedOrder(
        items: List<CartItem>,
        restoredCoinsUsed: Int,
        restoredSubtotal: Double?,
        restoredCoinDiscount: Double?,
        restoredFinalTotal: Double?
    ) {
        confirmedItems.clear()
        confirmedItems.addAll(items)
        coinsUsed = restoredCoinsUsed

        if (restoredSubtotal != null && restoredCoinDiscount != null && restoredFinalTotal != null) {
            subtotal = restoredSubtotal
            coinDiscount = restoredCoinDiscount
            finalTotal = restoredFinalTotal
        } else {
            recalcTotals()
        }
    }

    // Voucher support
    sealed class Voucher(val code: String) {
        data class Flat(val amount: Double, val minSpend: Double, val label: String): Voucher(label)
        data class Percent(val percent: Double, val cap: Double, val minSpend: Double, val label: String): Voucher(label)
    }

    fun setVoucher(newVoucher: Voucher?) {
        voucher = newVoucher
        recalcTotals()
    }

    private fun calculateVoucherDiscount(currentSubtotal: Double, voucher: Voucher?): Double {
        if (voucher == null) return 0.0
        return when (voucher) {
            is Voucher.Flat -> {
                if (currentSubtotal >= voucher.minSpend) voucher.amount.coerceAtMost(currentSubtotal) else 0.0
            }
            is Voucher.Percent -> {
                if (currentSubtotal >= voucher.minSpend) {
                    val raw = currentSubtotal * voucher.percent
                    raw.coerceAtMost(voucher.cap).coerceAtMost(currentSubtotal)
                } else 0.0
            }
        }
    }
}