package com.example.taiwanesehouse

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

class PhoneAuth(private val activity: Activity) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isVerificationInProgress = false

    companion object {
        private const val TAG = "PhoneAuth"
        private const val TIMEOUT_SECONDS = 60L
    }

    interface PhoneAuthListener {
        fun onVerificationCompleted(credential: PhoneAuthCredential)
        fun onVerificationFailed(exception: FirebaseException)
        fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken)
        fun onCodeAutoRetrievalTimeOut(verificationId: String)
        fun onSignInSuccess(user: FirebaseUser?)
        fun onSignInFailure(exception: Exception)
    }

    private var phoneAuthListener: PhoneAuthListener? = null

    fun setPhoneAuthListener(listener: PhoneAuthListener) {
        this.phoneAuthListener = listener
    }

    /**
     * Start phone number verification process
     * @param phoneNumber The phone number to verify (with country code, e.g., "+60123456789")
     * @param forceResend Whether to force resend the SMS (optional)
     */
    fun verifyPhoneNumber(phoneNumber: String, forceResend: Boolean = false) {
        Log.d(TAG, "Starting phone number verification for: $phoneNumber")

        // Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            phoneAuthListener?.onVerificationFailed(
                FirebaseAuthInvalidCredentialsException("INVALID_PHONE_NUMBER", "Invalid phone number format")
            )
            return
        }

        isVerificationInProgress = true

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        // Add resend token if force resend is requested and token exists
        if (forceResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        val options = optionsBuilder.build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Verify the SMS code entered by user
     * @param code The 6-digit verification code from SMS
     */
    fun verifyCode(code: String) {
        Log.d(TAG, "Verifying code: $code")

        if (storedVerificationId == null) {
            Log.e(TAG, "No verification ID stored")
            phoneAuthListener?.onVerificationFailed(
                FirebaseAuthInvalidCredentialsException("NO_VERIFICATION_ID", "No verification ID available")
            )
            return
        }

        if (!isValidVerificationCode(code)) {
            phoneAuthListener?.onVerificationFailed(
                FirebaseAuthInvalidCredentialsException("INVALID_CODE", "Invalid verification code format")
            )
            return
        }

        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    /**
     * Sign in with phone auth credential
     */
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "Signing in with phone auth credential")

        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                isVerificationInProgress = false

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = task.result?.user
                    phoneAuthListener?.onSignInSuccess(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    when (val exception = task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Log.e(TAG, "The verification code entered was invalid")
                        }
                        else -> {
                            Log.e(TAG, "Sign in failed: ${exception?.message}")
                        }
                    }
                    phoneAuthListener?.onSignInFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            isVerificationInProgress = false
            phoneAuthListener?.onVerificationCompleted(credential)
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "onVerificationFailed", e)
            isVerificationInProgress = false

            // Simple string-based error handling
            val errorMessage = when {
                e.message?.contains("invalid", ignoreCase = true) == true -> "Invalid phone number format"
                e.message?.contains("quota", ignoreCase = true) == true -> "SMS quota exceeded"
                e.message?.contains("activity", ignoreCase = true) == true -> "Activity context required for verification"
                else -> "Verification failed: ${e.message}"
            }

            Log.e(TAG, errorMessage)
            phoneAuthListener?.onVerificationFailed(e)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token

            phoneAuthListener?.onCodeSent(verificationId, token)
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            // Called when the auto-sms-retrieval has timed out, based on the given
            // timeout duration specified to PhoneAuthProvider#verifyPhoneNumber.
            Log.d(TAG, "onCodeAutoRetrievalTimeOut:$verificationId")
            phoneAuthListener?.onCodeAutoRetrievalTimeOut(verificationId)
        }
    }

    /**
     * Set language for SMS localization
     * @param languageCode Language code (e.g., "en", "zh", "ms")
     */
    fun setLanguageCode(languageCode: String) {
        auth.setLanguageCode(languageCode)
    }

    /**
     * Use app's default language
     */
    fun useAppLanguage() {
        auth.useAppLanguage()
    }

    /**
     * Check if verification is currently in progress
     */
    fun isVerificationInProgress(): Boolean {
        return isVerificationInProgress
    }

    /**
     * Get current stored verification ID
     */
    fun getStoredVerificationId(): String? {
        return storedVerificationId
    }

    /**
     * Clear verification data (call this when verification is complete or failed)
     */
    fun clearVerificationData() {
        storedVerificationId = null
        resendToken = null
        isVerificationInProgress = false
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        clearVerificationData()
        Log.d(TAG, "User signed out")
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Helper methods
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic validation - should start with + and contain only digits and spaces/dashes
        return phoneNumber.matches(Regex("^\\+[1-9]\\d{1,14}$")) ||
                phoneNumber.matches(Regex("^\\+[1-9][\\d\\s-]{7,17}$"))
    }

    private fun isValidVerificationCode(code: String): Boolean {
        // Verification codes are typically 6 digits
        return code.matches(Regex("^\\d{6}$"))
    }
}