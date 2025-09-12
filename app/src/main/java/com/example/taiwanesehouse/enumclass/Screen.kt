package com.example.taiwanesehouse.enumclass

enum class Screen {
    Signup,
    Login,
    ForgotPassword,
    Menu,
    Cart,
    Payment,
    Profile,
}

enum class UserProfile{
    EditName,
    PasswordUpdate,
    PaymentHistory,
    Feedback,
    Logout
}

enum class Payment {
    PaymentSuccess,
    PaymentError
}