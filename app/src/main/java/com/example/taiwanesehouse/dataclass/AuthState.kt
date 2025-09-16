package com.example.taiwanesehouse.dataclass

import com.example.taiwanesehouse.database.entities.UserEntity

data class AuthState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val isLoggedIn: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = "",
    val savedEmail: String = "",
    val savedPhone: String = "",
    val savedCredential: String = "", // Combined field for UI convenience
    val rememberMe: Boolean = false
)