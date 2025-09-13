package com.example.taiwanesehouse.dataclass

data class FormValidationState(
    val isEmailValid: Boolean = false,
    val isPhoneValid: Boolean = false,
    val isPasswordValid: Boolean = false,
    val passwordsMatch: Boolean = false,
    val isFormValid: Boolean = false
)