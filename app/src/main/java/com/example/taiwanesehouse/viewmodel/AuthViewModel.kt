package com.example.taiwanesehouse.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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

    // SharedPreferences for Remember Me
    private val sharedPreferences: SharedPreferences by lazy {
        application.getSharedPreferences("taiwanese_house_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Check email availability (debounced from UI)
     */
    fun checkEmailAvailability(email: String, callback: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim().lowercase()

        if (trimmedEmail.isEmpty()) {
            callback(false, "")
            return
        }

        if (!authManager.isValidEmail(trimmedEmail)) {
            callback(false, "Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            try {
                val exists = authManager.checkEmailExists(trimmedEmail)
                if (exists) {
                    callback(false, "This email is already registered")
                } else {
                    callback(true, "Email is available")
                }
            } catch (e: Exception) {
                callback(false, "Unable to check email availability")
            }
        }
    }

    /**
     * Check phone availability (debounced from UI)
     */
    fun checkPhoneAvailability(phoneNumber: String, callback: (Boolean, String) -> Unit) {
        val trimmedPhone = phoneNumber.trim()

        if (trimmedPhone.isEmpty()) {
            callback(false, "")
            return
        }

        if (!authManager.isValidPhoneNumber(trimmedPhone)) {
            callback(false, "Invalid phone number format")
            return
        }

        viewModelScope.launch {
            try {
                val exists = authManager.checkPhoneExists(trimmedPhone)
                if (exists) {
                    callback(false, "This phone number is already registered")
                } else {
                    callback(true, "Phone number is available")
                }
            } catch (e: Exception) {
                callback(false, "Unable to check phone availability")
            }
        }
    }

    init {
        // Load saved credentials and check for auto-login on app start
        loadSavedCredentials()
        checkAutoLogin()
    }

    private fun loadSavedCredentials() {
        val savedEmail = sharedPreferences.getString("saved_email", "") ?: ""
        val savedPhone = sharedPreferences.getString("saved_phone", "") ?: ""
        val rememberMe = sharedPreferences.getBoolean("remember_me", false)

        if (rememberMe && (savedEmail.isNotEmpty() || savedPhone.isNotEmpty())) {
            val savedCredential = if (savedEmail.isNotEmpty()) savedEmail else savedPhone

            _authState.value = _authState.value.copy(
                savedEmail = savedEmail,
                savedPhone = savedPhone,
                savedCredential = savedCredential, // Add this field to populate the input
                rememberMe = rememberMe
            )
        }
    }

    fun getSavedCredential(): String {
        val authState = _authState.value
        return when {
            authState.savedEmail.isNotEmpty() -> authState.savedEmail
            authState.savedPhone.isNotEmpty() -> authState.savedPhone
            else -> ""
        }
    }

    fun getSavedRememberMe(): Boolean {
        return _authState.value.rememberMe
    }

    private fun saveCredentials(emailOrPhone: String, rememberMe: Boolean) {
        with(sharedPreferences.edit()) {
            if (rememberMe) {
                val inputType = detectInputType(emailOrPhone)
                if (inputType == "email") {
                    putString("saved_email", emailOrPhone)
                    remove("saved_phone")
                } else if (inputType == "phone") {
                    putString("saved_phone", emailOrPhone)
                    remove("saved_email")
                }
                putBoolean("remember_me", true)
            } else {
                remove("saved_email")
                remove("saved_phone")
                putBoolean("remember_me", false)
            }
            apply()
        }

        // Update state immediately
        loadSavedCredentials()
    }

    fun clearSavedCredentials() {
        with(sharedPreferences.edit()) {
            remove("saved_email")
            remove("saved_phone")
            putBoolean("remember_me", false)
            apply()
        }

        // Update state immediately
        _authState.value = _authState.value.copy(
            savedEmail = "",
            savedPhone = "",
            savedCredential = "",
            rememberMe = false
        )
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
     * Login user - Fixed to properly save credentials
     */
    fun loginUser(
        emailOrPhone: String,
        password: String,
        rememberMe: Boolean
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
                    // Save credentials if login is successful
                    saveCredentials(emailOrPhone.trim(), rememberMe)

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
     * Logout user - Clear saved credentials if needed
     */
    fun logout(clearRememberedCredentials: Boolean = false) {
        viewModelScope.launch {
            if (clearRememberedCredentials) {
                clearSavedCredentials()
            }

            authManager.logout()
            _authState.value = AuthState().copy(
                // Preserve saved credentials if not explicitly cleared
                savedEmail = if (clearRememberedCredentials) "" else _authState.value.savedEmail,
                savedPhone = if (clearRememberedCredentials) "" else _authState.value.savedPhone,
                savedCredential = if (clearRememberedCredentials) "" else _authState.value.savedCredential,
                rememberMe = if (clearRememberedCredentials) false else _authState.value.rememberMe
            )
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