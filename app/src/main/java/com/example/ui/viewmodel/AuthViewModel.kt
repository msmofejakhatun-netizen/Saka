package com.example.ui.viewmodel

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
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

    private var verificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var timerJob: Job? = null

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    /**
     * Initiates real SMS OTP sending to phone number.
     */
    fun sendPhoneOtp(mobileNumber: String, activity: Activity) {
        val cleanNumber = mobileNumber.filter { it.isDigit() }
        if (cleanNumber.length < 10) {
            authError = "Please enter a valid 10-digit mobile number."
            return
        }

        val formattedPhone = if (mobileNumber.startsWith("+")) mobileNumber else "+91$cleanNumber"
        authMobile = formattedPhone
        authError = null
        isSendingOtp = true

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification automatically completed")
                viewModelScope.launch {
                    isSendingOtp = false
                    try {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val result = auth.signInWithCredential(credential)
                        result.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = task.result?.user
                                if (user != null) {
                                    viewModelScope.launch {
                                        authRepository.syncUserProfileAndSession(user, "phone")
                                        _toastMessage.emit("Phone number verified automatically!")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto sign-in failed: ${e.localizedMessage}")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Verification failed: ${e.localizedMessage}")
                isSendingOtp = false
                authError = "SMS Verification Failed: ${e.localizedMessage}"
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "OTP code sent to $formattedPhone")
                verificationId = verId
                resendToken = token
                isSendingOtp = false
                isOtpSent = true
                startResendTimer()
                viewModelScope.launch {
                    _toastMessage.emit("Verification code sent to $formattedPhone")
                }
            }
        }

        try {
            authRepository.sendPhoneOtp(
                phoneNumber = formattedPhone,
                activity = activity,
                callbacks = callbacks,
                forceResendingToken = resendToken
            )
        } catch (e: Exception) {
            isSendingOtp = false
            authError = "Failed to send OTP: ${e.localizedMessage}"
        }
    }

    /**
     * Verifies the entered 6-digit OTP code against Firebase PhoneAuth.
     */
    fun verifyPhoneOtp(userEnteredOtp: String, onSuccess: () -> Unit) {
        if (userEnteredOtp.length != 6) {
            authError = "Please enter a valid 6-digit OTP."
            return
        }
        if (verificationId.isEmpty()) {
            authError = "Invalid verification session. Please resend OTP."
            return
        }

        authError = null
        isVerifyingOtp = true

        viewModelScope.launch {
            val result = authRepository.verifyPhoneOtp(verificationId, userEnteredOtp)
            isVerifyingOtp = false
            result.onSuccess {
                _toastMessage.emit("Authentication successful!")
                resetAuthState()
                onSuccess()
            }.onFailure { error ->
                authError = "OTP verification failed: ${error.localizedMessage}"
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
        verificationId = ""
        resendToken = null
        timerJob?.cancel()
        timerSeconds = 0
    }

    fun signOut(onSuccess: () -> Unit) {
        authRepository.signOut()
        resetAuthState()
        onSuccess()
    }
}
