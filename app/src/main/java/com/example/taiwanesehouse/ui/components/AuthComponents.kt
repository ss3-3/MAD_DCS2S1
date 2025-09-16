// AuthComponents.kt
package com.example.taiwanesehouse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared header component for auth screens
 */
@Composable
fun AuthHeader(
    logoImage: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = logoImage,
            contentDescription = "Taiwanese House Logo",
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF666666),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Email input field with validation
 */
@Composable
fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    isRequired: Boolean = true,
    modifier: Modifier = Modifier,
    label: String = if (isRequired) "Email Address *" else "Email Address"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            placeholder = { Text("example@email.com") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = value.isNotEmpty() && !isValid,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    Text(
                        text = if (isValid) "✅" else "❌",
                        fontSize = 16.sp
                    )
                }
            }
        )

        if (value.isNotEmpty()) {
            Text(
                text = if (isValid) "✓ Valid email address" else "Please enter a valid email address",
                color = if (isValid) Color.Green else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Phone input field with validation
 */
@Composable
fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    isOptional: Boolean = true,
    modifier: Modifier = Modifier,
    helperText: String = "Adding a phone number allows you to login using either email or phone"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (isOptional) "Phone Number (Optional)" else "Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            placeholder = { Text("+60123456789") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = value.isNotEmpty() && !isValid,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    Text(
                        text = if (isValid) "✅" else "❌",
                        fontSize = 16.sp
                    )
                }
            }
        )

        if (value.isNotEmpty()) {
            Text(
                text = if (isValid) "✓ Valid phone number" else "Please enter a valid phone number",
                color = if (isValid) Color.Green else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        if (isOptional) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

/**
 * Email or Phone input field (for login)
 */
@Composable
fun EmailOrPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    inputType: String,
    isEmailValid: Boolean,
    isPhoneValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Email or Phone Number",
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            placeholder = {
                Text(
                    when (inputType) {
                        "email" -> "example@email.com"
                        "phone" -> "+60123456789"
                        else -> "example@email.com or +60123456789"
                    }
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    Text(
                        text = when (inputType) {
                            "email" -> if (isEmailValid) "📧" else "❌"
                            "phone" -> if (isPhoneValid) "📱" else "❌"
                            else -> "❓"
                        },
                        fontSize = 16.sp
                    )
                }
            }
        )

        if (value.isNotEmpty()) {
            Text(
                text = when (inputType) {
                    "email" -> if (isEmailValid) "✓ Email format detected" else "Please enter a valid email address"
                    "phone" -> if (isPhoneValid) "✓ Phone number detected" else "Please enter a valid phone number"
                    else -> "Enter an email address or phone number"
                },
                color = when (inputType) {
                    "email" -> if (isEmailValid) Color.Green else Color.Red
                    "phone" -> if (isPhoneValid) Color.Green else Color.Red
                    else -> Color.Gray
                },
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        } else {
            // Helper text when field is empty
            Text(
                text = "You can login with either your email address or phone number",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Password field with show/hide toggle
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label.contains("Password") && !label.contains("Confirm")) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF)
                    )
                }
            },
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

/**
 * Confirm password field with matching validation
 */
@Composable
fun ConfirmPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    originalPassword: String,
    passwordsMatch: Boolean,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = value.isNotEmpty() && !passwordsMatch
        )

        if (value.isNotEmpty()) {
            Text(
                text = if (passwordsMatch) "✓ Passwords match" else "Passwords do not match",
                color = if (passwordsMatch) Color.Green else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Security question dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionDropdown(
    selectedQuestion: String,
    onQuestionSelected: (String) -> Unit,
    questions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedQuestion,
            onValueChange = { },
            readOnly = true,
            label = { Text("Security Question") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Select a security question") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            questions.forEach { question ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = question,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onQuestionSelected(question)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Security answer field
 */
@Composable
fun SecurityAnswerField(
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Security Answer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Enter your answer") },
            enabled = isEnabled
        )

        if (isEnabled) {
            Text(
                text = "This will help you recover your password if needed",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Error message card
 */
@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    if (message.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * Info card for additional information
 */
@Composable
fun InfoCard(
    message: String,
    backgroundColor: Color = Color.Blue.copy(alpha = 0.1f),
    textColor: Color = Color.Blue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Primary action button (Login/Register)
 */
@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700),
            contentColor = Color.Black
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Remember me checkbox
 */
@Composable
fun RememberMeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFFD700)
            )
        )
        Text(
            text = "Remember me",
            fontSize = 14.sp,
            color = Color(0xFF616161),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Terms agreement checkbox
 */
@Composable
fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFFD700)
            )
        )
        Text(
            text = "I agree to the Terms and Conditions",
            fontSize = 14.sp,
            color = Color(0xFF616161),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Auth screen navigation link
 */
@Composable
fun AuthNavigationLink(
    prefixText: String,
    linkText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = prefixText,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = linkText,
            fontSize = 14.sp,
            color = Color(0xFF007AFF),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onClick() }
        )
    }
}