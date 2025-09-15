package com.example.taiwanesehouse.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class for validation results
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

// Username validation utility
object UsernameValidator {
    // English offensive/inappropriate words
    private val englishOffensive = setOf(
        "fuck", "shit", "bitch", "asshole", "damn", "hell", "bastard", "slut", "whore",
        "nigger", "faggot", "retard", "cunt", "pussy", "dick", "cock", "penis", "vagina",
        "sex", "porn", "xxx", "nude", "naked", "horny", "sexy", "boobs", "breast",
        "kill", "die", "suicide", "murder", "rape", "terrorist", "bomb", "gun", "hate",
        "stupid", "idiot", "moron", "loser", "freak", "ugly", "fat", "skinny"
    )

    // Malay offensive/inappropriate words
    private val malayOffensive = setOf(
        "bodoh", "sial", "pukimak", "lancau", "cb", "knn", "ccb", "wtf", "knnbccb",
        "babi", "anjing", "monyet", "bengap", "bangang", "cilaka", "celaka",
        "pundek", "kimak", "pantat", "tetek", "konek", "pepek", "jubur",
        "gila", "setan", "iblis", "biadap", "kurang ajar", "haram", "laknat"
    )

    // Chinese offensive/inappropriate words (romanized)
    private val chineseOffensive = setOf(
        "cao", "sb", "tmb", "cnm", "wqnmlgb", "mlgb", "tmd", "nmd", "mmp",
        "bitch", "biaozi", "shabi", "zhinao", "baichi", "shagua", "baga",
        "chiba", "diao", "ji", "niubi", "niu", "sha", "ben", "zhu"
    )

    // Tamil/Indian offensive words
    private val tamilOffensive = setOf(
        "punda", "otha", "kasmala", "kirukku", "loosu", "mental", "waste",
        "thayoli", "koothi", "sunni", "oombu", "naaye", "panni"
    )

    // System reserved names
    private val reservedNames = setOf(
        "admin", "administrator", "moderator", "mod", "support", "help", "system", "root",
        "guest", "user", "default", "test", "demo", "null", "undefined", "anonymous",
        "taiwanese", "house", "restaurant", "food", "menu", "order", "booking",
        "reservation", "customer", "service", "staff", "manager", "chef", "waiter",
        "cashier", "kitchen", "delivery", "takeaway", "dine", "eat", "cook", "server"
    )

    // Spam/scam related words
    private val spamWords = setOf(
        "spam", "scam", "fake", "bot", "hack", "cheat", "virus", "phishing",
        "casino", "gambling", "lottery", "winner", "prize", "money", "cash",
        "free", "offer", "deal", "sale", "discount", "promo", "advertisement",
        "marketing", "business", "company", "official", "verified", "vip"
    )

    // Combine all sensitive words
    private val allSensitiveWords = englishOffensive + malayOffensive +
            chineseOffensive + tamilOffensive +
            reservedNames + spamWords

    // Suspicious patterns
    private val suspiciousPatterns = listOf(
        Regex("""(.)\1{4,}"""), // Repeated characters (aaaaa)
        Regex("""^\d+$"""), // Only numbers
        Regex("""^[^a-zA-Z\u4e00-\u9fff\u0590-\u05ff]*$"""), // No letters (including Chinese/Hebrew)
        Regex(""".*\d{6,}.*"""), // Long sequences of numbers (phone/ID numbers)
        Regex(""".*[!@#$%^&*()]{3,}.*"""), // Excessive special characters
        Regex("""^(test|admin|user)\d*$""", RegexOption.IGNORE_CASE) // Common test patterns
    )

    fun validateUsername(username: String): ValidationResult {
        val original = username.trim()
        val lowercase = original.lowercase()
        val normalized = normalizeText(lowercase)

        // Basic length and format checks
        if (original.isBlank()) {
            return ValidationResult(false, "Username cannot be empty")
        }

        if (original.length < 2) {
            return ValidationResult(false, "Username must be at least 2 characters")
        }

        if (original.length > 30) {
            return ValidationResult(false, "Username must be less than 30 characters")
        }

        // Check for only whitespace or special characters
        if (original.all { it.isWhitespace() || !it.isLetterOrDigit() }) {
            return ValidationResult(false, "Username must contain letters or numbers")
        }

        // Check for sensitive words (exact match and contains)
        for (word in allSensitiveWords) {
            if (normalized == word || normalized.contains(word)) {
                return ValidationResult(false, "This username is not available")
            }
        }

        // Check for creative spelling of banned words
        if (containsCreativeSpelling(normalized)) {
            return ValidationResult(false, "This username is not available")
        }

        // Check suspicious patterns
        for (pattern in suspiciousPatterns) {
            if (pattern.containsMatchIn(normalized)) {
                return ValidationResult(false, "Please choose a more appropriate username")
            }
        }

        // Check for URLs, emails, or phone numbers
        if (containsContactInfo(normalized)) {
            return ValidationResult(false, "Username cannot contain contact information")
        }

        // Check for excessive repetition
        if (hasExcessiveRepetition(normalized)) {
            return ValidationResult(false, "Username contains too much repetition")
        }

        // Check character variety (at least 50% should be letters)
        val letterCount = original.count { it.isLetter() }
        if (original.length > 3 && letterCount < original.length * 0.5) {
            return ValidationResult(false, "Username should contain more letters")
        }

        return ValidationResult(true)
    }

    // Normalize text for better matching (remove spaces, special chars, numbers used as letters)
    private fun normalizeText(text: String): String {
        return text
            .replace(Regex("""[^\w]"""), "")  // Remove non-word characters
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
            .replace("@", "a")
            .replace("!", "i")
            .lowercase()
    }

    // Check for creative spelling of banned words
    private fun containsCreativeSpelling(text: String): Boolean {
        val creativeMappings = mapOf(
            "fuck" to listOf("fuk", "f*ck", "f-u-c-k", "phuck", "fock", "fuxk"),
            "shit" to listOf("sht", "s*it", "sh1t", "shyt", "scheisse"),
            "bitch" to listOf("b*tch", "biatch", "bytch", "beatch", "b1tch"),
            "damn" to listOf("d*mn", "dam", "damm", "dayum"),
            "bodoh" to listOf("b0d0h", "bod0h", "b*doh", "bodot"),
            "sial" to listOf("s*al", "si4l", "sy4l", "siel")
        )

        for ((_, variants) in creativeMappings) {
            for (variant in variants) {
                if (text.contains(variant)) {
                    return true
                }
            }
        }
        return false
    }

    // Check for contact information patterns
    private fun containsContactInfo(text: String): Boolean {
        val contactPatterns = listOf(
            Regex(""".*www\..*"""),
            Regex(""".*http.*"""),
            Regex(""".*\.com.*"""),
            Regex(""".*@.*\."""),
            Regex(""".*\d{3,}-?\d{3,}.*"""), // Phone-like patterns
            Regex(""".*\+\d+.*""") // International phone format
        )

        return contactPatterns.any { it.containsMatchIn(text) }
    }

    // Check for excessive repetition
    private fun hasExcessiveRepetition(text: String): Boolean {
        if (text.length < 4) return false

        // Check for repeated substrings
        for (i in 2..text.length/2) {
            val substring = text.substring(0, i)
            val repeated = substring.repeat(text.length / i)
            if (repeated.length >= text.length * 0.7 && text.startsWith(repeated.substring(0, minOf(repeated.length, text.length)))) {
                return true
            }
        }

        return false
    }
}

// Extension function for easy validation in composables
fun String.isValidUsername(): ValidationResult = UsernameValidator.validateUsername(this)

// Composable utility function for consistent username input
@Composable
fun ValidatedUsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Username",
    placeholder: String = "Enter your username",
    enabled: Boolean = true,
    isRequired: Boolean = true
) {
    var validationMessage by remember { mutableStateOf("") }

    // Real-time validation
    LaunchedEffect(value) {
        if (value.trim().isNotEmpty()) {
            val validation = UsernameValidator.validateUsername(value)
            validationMessage = if (validation.isValid) {
                "Username is available"
            } else {
                validation.errorMessage
            }
        } else {
            validationMessage = ""
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Filter input while typing
            val filtered = input.take(30).filter { char ->
                char.isLetterOrDigit() || char.isWhitespace() || char in ".-_"
            }
            onValueChange(filtered)
        },
        label = {
            Text("$label${if (isRequired) " *" else ""}")
        },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        isError = validationMessage.isNotEmpty() && !validationMessage.contains("available"),
        placeholder = { Text(placeholder) },
        supportingText = {
            if (validationMessage.isNotEmpty()) {
                Text(
                    text = validationMessage,
                    color = if (validationMessage.contains("available"))
                        Color(0xFF4CAF50) else Color.Red,
                    fontSize = 12.sp
                )
            }
        }
    )
}