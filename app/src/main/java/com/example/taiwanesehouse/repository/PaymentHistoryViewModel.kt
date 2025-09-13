// Payment History ViewModel
package com.example.taiwanesehouse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.dataclass.PaymentHistoryState
import com.example.taiwanesehouse.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentHistoryViewModel(
    private val paymentRepository: PaymentRepository,
    private val userId: String
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentHistoryState())
    val state: StateFlow<PaymentHistoryState> = _state.asStateFlow()

    init {
        loadPaymentHistory()
    }

    private fun loadPaymentHistory() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                paymentRepository.getPaymentHistory(userId).collect { payments ->
                    _state.value = _state.value.copy(
                        payments = payments,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load payment history"
                )
            }
        }
    }

    fun retry() {
        loadPaymentHistory()
    }
}

class PaymentHistoryViewModelFactory(
    private val paymentRepository: PaymentRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentHistoryViewModel(paymentRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}