package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.Fast2SMSHelper
import com.example.data.repository.AuthRepository
import com.example.data.repository.BillingRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    var isSendingOtp by mutableStateOf(false)
        private set

    var isOtpSent by mutableStateOf(false)
        internal set

    var isVerifyingOtp by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        internal set

    var timerSeconds by mutableIntStateOf(0)
        private set

    var authMobile by mutableStateOf("")
        private set

    private var timerJob: Job? = null

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    /**
     * Initiates Fast2SMS OTP sending to phone number.
     */
    fun sendPhoneOtp(mobileNumber: String, context: Context) {
        val cleanNumber = Fast2SMSHelper.sanitizePhoneNumber(mobileNumber)
        if (cleanNumber.length < 10) {
            authError = "Please enter a valid 10-digit mobile number."
            return
        }

        val formattedPhone = "+91$cleanNumber"
        authMobile = formattedPhone
        authError = null
        isSendingOtp = true

        viewModelScope.launch {
            val result = authRepository.sendFast2SmsOtp(context, cleanNumber)
            isSendingOtp = false
            result.onSuccess { msg ->
                isOtpSent = true
                startResendTimer()
                _toastMessage.emit(msg)
            }.onFailure { error ->
                authError = error.localizedMessage ?: "Failed to send OTP via Fast2SMS."
            }
        }
    }

    /**
     * Verifies the 6-digit Fast2SMS OTP code.
     */
    fun verifyPhoneOtp(context: Context, userEnteredOtp: String, onSuccess: (String) -> Unit) {
        if (userEnteredOtp.length != 6) {
            authError = "Please enter a valid 6-digit OTP."
            return
        }

        authError = null
        isVerifyingOtp = true

        viewModelScope.launch {
            val result = authRepository.verifyFast2SmsOtp(context, authMobile, userEnteredOtp)
            isVerifyingOtp = false
            result.onSuccess { userId ->
                _toastMessage.emit("Authentication successful!")
                resetAuthState()
                onSuccess(userId)
            }.onFailure { error ->
                authError = error.localizedMessage ?: "OTP verification failed."
            }
        }
    }

    /**
     * Sign in with Google ID Token via Firebase GoogleAuthProvider.
     */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        authError = null
        isVerifyingOtp = true

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            isVerifyingOtp = false
            result.onSuccess {
                _toastMessage.emit("Google Sign-In successful!")
                resetAuthState()
                onSuccess()
            }.onFailure { error ->
                authError = "Google Sign-In failed: ${error.localizedMessage}"
            }
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerSeconds = 30
        timerJob = viewModelScope.launch {
            while (timerSeconds > 0) {
                delay(1000)
                timerSeconds--
            }
        }
    }

    fun resetAuthState() {
        authMobile = ""
        isOtpSent = false
        isVerifyingOtp = false
        isSendingOtp = false
        authError = null
        timerJob?.cancel()
        timerSeconds = 0
    }

    fun signOut(context: Context? = null, billingRepository: BillingRepository? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context, billingRepository)
            resetAuthState()
            onSuccess()
        }
    }
}

