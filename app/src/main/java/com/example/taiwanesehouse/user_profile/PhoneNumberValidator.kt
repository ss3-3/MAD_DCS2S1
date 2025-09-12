package com.example.taiwanesehouse.user_profile

import java.util.regex.Pattern

class PhoneNumberValidator {

    companion object {
        // Malaysian phone number patterns
        private val MALAYSIA_MOBILE_PATTERNS = listOf(
            // Mobile numbers starting with +60
            "^\\+60(10|11|12|13|14|15|16|17|18|19)\\d{7,8}$",
            // Mobile numbers starting with 60 (without +)
            "^60(10|11|12|13|14|15|16|17|18|19)\\d{7,8}$",
            // Mobile numbers starting with 0 (local format)
            "^0(10|11|12|13|14|15|16|17|18|19)\\d{7,8}$"
        )

        // Common international mobile patterns (basic validation)
        private val INTERNATIONAL_PATTERNS = listOf(
            // Singapore
            "^\\+65[689]\\d{7}$",
            "^65[689]\\d{7}$",
            // Indonesia
            "^\\+62[8]\\d{8,11}$",
            "^62[8]\\d{8,11}$",
            // Thailand
            "^\\+66[689]\\d{8}$",
            "^66[689]\\d{8}$",
            // Philippines
            "^\\+63[9]\\d{9}$",
            "^63[9]\\d{9}$",
            // Vietnam
            "^\\+84[3579]\\d{8}$",
            "^84[3579]\\d{8}$"
        )

        // Invalid patterns to avoid
        private val INVALID_PATTERNS = listOf(
            "^\\+?\\d{0,5}$", // Too short
            "^\\+?\\d{16,}$", // Too long
            "^\\+?0{6,}$", // All zeros
            "^\\+?1{6,}$", // All ones
            "^\\+?2{6,}$", // All twos (etc.)
            "^\\+?1234567890$", // Sequential numbers
            "^\\+?0123456789$" // Sequential numbers starting with 0
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = "",
        val formattedNumber: String = "",
        val countryDetected: String = "",
        val isMalaysianNumber: Boolean = false
    )

    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        val cleanNumber = phoneNumber.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        // Basic checks
        if (cleanNumber.isEmpty()) {
            return ValidationResult(false, "Phone number cannot be empty")
        }

        if (cleanNumber.length < 8) {
            return ValidationResult(false, "Phone number is too short")
        }

        if (cleanNumber.length > 15) {
            return ValidationResult(false, "Phone number is too long")
        }

        // Check for invalid characters
        if (!cleanNumber.matches(Regex("^\\+?[0-9]+$"))) {
            return ValidationResult(false, "Phone number can only contain digits and + symbol")
        }

        // Check for invalid patterns
        for (pattern in INVALID_PATTERNS) {
            if (Pattern.matches(pattern, cleanNumber)) {
                return ValidationResult(false, "Please enter a valid phone number")
            }
        }

        // Check Malaysian patterns first (priority for local app)
        for (pattern in MALAYSIA_MOBILE_PATTERNS) {
            if (Pattern.matches(pattern, cleanNumber)) {
                val formatted = formatMalaysianNumber(cleanNumber)
                return ValidationResult(
                    isValid = true,
                    formattedNumber = formatted,
                    countryDetected = "Malaysia",
                    isMalaysianNumber = true
                )
            }
        }

        // Check international patterns
        for (pattern in INTERNATIONAL_PATTERNS) {
            if (Pattern.matches(pattern, cleanNumber)) {
                val country = detectCountry(cleanNumber)
                val formatted = formatInternationalNumber(cleanNumber)
                return ValidationResult(
                    isValid = true,
                    formattedNumber = formatted,
                    countryDetected = country,
                    isMalaysianNumber = false
                )
            }
        }

        // If no specific pattern matches, do basic validation
        return performBasicValidation(cleanNumber)
    }

    private fun formatMalaysianNumber(number: String): String {
        val clean = number.replace(Regex("^0+"), "").replace(Regex("^60"), "")
        return if (clean.startsWith("+60")) clean else "+60$clean"
    }

    private fun formatInternationalNumber(number: String): String {
        return if (number.startsWith("+")) number else "+$number"
    }

    private fun detectCountry(number: String): String {
        val cleanNum = number.replace("+", "")
        return when {
            cleanNum.startsWith("60") -> "Malaysia"
            cleanNum.startsWith("65") -> "Singapore"
            cleanNum.startsWith("62") -> "Indonesia"
            cleanNum.startsWith("66") -> "Thailand"
            cleanNum.startsWith("63") -> "Philippines"
            cleanNum.startsWith("84") -> "Vietnam"
            else -> "International"
        }
    }

    private fun performBasicValidation(number: String): ValidationResult {
        // Basic validation for other countries
        val cleanNum = number.replace("+", "")

        // Check if it looks like a valid international number
        if (cleanNum.length >= 8 && cleanNum.length <= 15) {
            // Additional checks for obviously fake numbers
            val uniqueDigits = cleanNum.toSet().size
            if (uniqueDigits < 3) {
                return ValidationResult(false, "Please enter a valid phone number")
            }

            // Check for sequential patterns
            var hasSequential = false
            for (i in 0..cleanNum.length - 4) {
                val segment = cleanNum.substring(i, i + 4)
                if (isSequential(segment)) {
                    hasSequential = true
                    break
                }
            }

            if (hasSequential) {
                return ValidationResult(false, "Please enter a valid phone number")
            }

            val formatted = if (number.startsWith("+")) number else "+$number"
            return ValidationResult(
                isValid = true,
                formattedNumber = formatted,
                countryDetected = "International",
                isMalaysianNumber = false
            )
        }

        return ValidationResult(false, "Please enter a valid phone number")
    }

    private fun isSequential(segment: String): Boolean {
        if (segment.length < 4) return false

        // Check ascending sequence
        var isAscending = true
        for (i in 1 until segment.length) {
            if (segment[i].digitToInt() != segment[i-1].digitToInt() + 1) {
                isAscending = false
                break
            }
        }

        // Check descending sequence
        var isDescending = true
        for (i in 1 until segment.length) {
            if (segment[i].digitToInt() != segment[i-1].digitToInt() - 1) {
                isDescending = false
                break
            }
        }

        return isAscending || isDescending
    }

    fun validateForSMS(phoneNumber: String): ValidationResult {
        val basicValidation = validatePhoneNumber(phoneNumber)

        if (!basicValidation.isValid) {
            return basicValidation
        }

        // Additional SMS-specific validation
        val cleanNumber = basicValidation.formattedNumber

        // Warn about potential SMS delivery issues for non-Malaysian numbers
        if (!basicValidation.isMalaysianNumber) {
            return ValidationResult(
                isValid = true,
                errorMessage = "SMS verification may not work for international numbers. Consider using phone + password method instead.",
                formattedNumber = cleanNumber,
                countryDetected = basicValidation.countryDetected,
                isMalaysianNumber = false
            )
        }

        return basicValidation
    }

    fun suggestCorrection(phoneNumber: String): String? {
        val clean = phoneNumber.trim().replace(Regex("[\\s\\-\\(\\)]"), "")

        // Common Malaysian number corrections
        return when {
            // Missing country code for Malaysian numbers
            clean.matches(Regex("^[01]\\d{9,10}$")) -> {
                val withoutZero = clean.removePrefix("0")
                "+60$withoutZero"
            }
            // Missing + for international format
            clean.matches(Regex("^60\\d{9,10}$")) -> "+$clean"
            clean.matches(Regex("^6[2-9]\\d{8,12}$")) -> "+$clean"
            else -> null
        }
    }
}