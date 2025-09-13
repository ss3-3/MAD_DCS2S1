package com.example.taiwanesehouse.dataclass

import com.example.taiwanesehouse.database.entities.UserEntity

data class AuthState(
    val isLoading: Boolean = false,
    val user: UserEntity? = null,
    val isLoggedIn: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = ""
)