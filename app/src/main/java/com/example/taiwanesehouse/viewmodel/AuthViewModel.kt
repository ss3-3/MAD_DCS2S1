package com.example.taiwanesehouse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.auth.HybridAuthManager
import com.example.taiwanesehouse.dataclass.AuthState
import com.example.taiwanesehouse.dataclass.FormValidationState
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = HybridAuthManager.getInstance(application)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _validationState = MutableStateFlow(FormValidationState())
    val validationState: StateFlow<FormValidationState> = _validationState.asStateFlow()

    // Current user flow from Room database
    val currentUser = authManager.getCurrentUserFlow()

    init {
        // Check for auto-login on app start
        checkAutoLogin()
    }

    /**
     * Auto-login check for remembered users
     */
    private fun checkAutoLogin() {
        viewModelScope.launch {
            val result = authManager.autoLogin()
            if (result is com.example.taiwanesehouse.auth.AuthResult.Success) {
                _authState.value = _authState.value.copy(
                    user = result.user,
                    isLoggedIn = true
                )
            }
        }
    }

    /**
     * Register new user
     */
    fun registerUser(
        username: String,
        email: String,
        phoneNumber: String?,
        password: String,
        securityQuestion: String,
        securityAnswer: String
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = "")

            val result = authManager.registerUser(
                username = username.trim(),
                email = email.trim(),
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                password = password.trim(),
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer.trim()
            )

            when (result) {
                is com.example.taiwanesehouse.auth.AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = result.user,
                        isLoggedIn = true,
                        successMessage = "Account created successfully!"
                    )
                }
                is com.example.taiwanesehouse.auth.AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Login user
     */
    fun loginUser(
        emailOrPhone: String,
        password: String,
        rememberMe: Boolean = false
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = "")

            val result = authManager.loginUser(
                emailOrPhone = emailOrPhone.trim(),
                password = password.trim(),
                rememberMe = rememberMe
            )

            when (result) {
                is com.example.taiwanesehouse.auth.AuthResult.Success -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = result.user,
                        isLoggedIn = true,
                        successMessage = "Login successful!"
                    )
                }
                is com.example.taiwanesehouse.auth.AuthResult.Error -> {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _authState.value = AuthState() // Reset to initial state
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = "")
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _authState.value = _authState.value.copy(successMessage = "")
    }

    /**
     * Email validation
     */
    fun validateEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        val isValid = emailPattern.matcher(email).matches()
        updateValidationState(isEmailValid = isValid)
        return isValid
    }

    /**
     * Phone validation
     */
    fun validatePhone(phone: String): Boolean {
        val cleanPhone = authManager.cleanPhoneNumber(phone)
        val isValid = authManager.isValidPhoneNumber(phone)
        updateValidationState(isPhoneValid = isValid)
        return isValid
    }

    /**
     * Password validation
     */
    fun validatePassword(password: String): Boolean {
        // Strong password: min 8 chars, at least 1 upper, 1 lower, 1 digit
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}")
        val isValid = regex.matches(password)
        updateValidationState(isPasswordValid = isValid)
        return isValid
    }

    /**
     * Confirm password validation
     */
    fun validatePasswordMatch(password: String, confirmPassword: String): Boolean {
        val match = password == confirmPassword
        updateValidationState(passwordsMatch = match)
        return match
    }

    /**
     * Update validation state
     */
    private fun updateValidationState(
        isEmailValid: Boolean? = null,
        isPhoneValid: Boolean? = null,
        isPasswordValid: Boolean? = null,
        passwordsMatch: Boolean? = null
    ) {
        val current = _validationState.value
        val updated = current.copy(
            isEmailValid = isEmailValid ?: current.isEmailValid,
            isPhoneValid = isPhoneValid ?: current.isPhoneValid,
            isPasswordValid = isPasswordValid ?: current.isPasswordValid,
            passwordsMatch = passwordsMatch ?: current.passwordsMatch
        )

        // Calculate overall form validity
        val isFormValid = updated.isEmailValid && updated.isPasswordValid &&
                (updated.passwordsMatch || passwordsMatch == null)

        _validationState.value = updated.copy(isFormValid = isFormValid)
    }

    /**
     * Detect input type (email or phone)
     */
    fun detectInputType(input: String): String {
        return when {
            input.contains("@") -> "email"
            input.replace(Regex("[\\s\\-\\+]"), "").all { it.isDigit() } -> "phone"
            else -> "unknown"
        }
    }

    /**
     * Sync pending users (for offline-to-online sync)
     */
    fun syncPendingUsers() {
        viewModelScope.launch {
            authManager.syncPendingUsers()
        }
    }
}