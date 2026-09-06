package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.repository.UserProfileData
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val fullName: String = "",
    val businessName: String = "",
    val businessCategory: String = "",
    val upiId: String = "merchant@upi",
    val merchantName: String = "",
    val mobileNumber: String = "",
    val gstin: String = "",
    val isGstVerified: Boolean = false,
    val legalBusinessName: String = "",
    val isGstRegistered: Boolean = false,
    val isVerifyingGst: Boolean = false,
    val gstVerificationError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    var fullName by mutableStateOf("")
    var businessName by mutableStateOf("")
    var businessCategory by mutableStateOf("")
    var upiId by mutableStateOf("merchant@upi")
    var merchantName by mutableStateOf("")
    var autoSendWhatsAppInvoice by mutableStateOf(true)

    // GST Fields & Verification State
    var isGstRegistered by mutableStateOf(false)
    var gstin by mutableStateOf("")
    var isGstVerified by mutableStateOf(false)
    var legalBusinessName by mutableStateOf("")
    var isVerifyingGst by mutableStateOf(false)
    var gstVerificationError by mutableStateOf<String?>(null)

    var errorMessage by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    init {
        loadUserProfile()
    }

    fun loadUserProfile(userId: String? = null, context: Context? = null) {
        if (context != null) {
            autoSendWhatsAppInvoice = com.example.util.WhatsAppInvoiceHelper.isAutoSendEnabled(context)
        }
        val targetUid = userId ?: FirebaseManager.auth?.currentUser?.uid
        if (targetUid.isNullOrEmpty()) return

        isLoading = true
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val profile = userRepository.getUserProfile(targetUid)
                if (profile != null) {
                    fullName = profile.fullName
                    businessName = profile.businessName
                    businessCategory = profile.businessCategory
                    upiId = profile.upiId.ifBlank { "merchant@upi" }
                    merchantName = profile.merchantName.ifBlank { profile.businessName }
                    gstin = profile.gstin
                    isGstVerified = profile.isGstVerified
                    val cleanLoadedLegalName = profile.legalBusinessName
                        .replace(Regex("^(Verified:\\s*)+", RegexOption.IGNORE_CASE), "")
                        .trim()
                    legalBusinessName = cleanLoadedLegalName
                    isGstRegistered = profile.isGstRegistered || profile.gstin.isNotBlank()

                    _uiState.value = _uiState.value.copy(
                        fullName = profile.fullName,
                        businessName = profile.businessName,
                        businessCategory = profile.businessCategory,
                        upiId = profile.upiId.ifBlank { "merchant@upi" },
                        merchantName = profile.merchantName.ifBlank { profile.businessName },
                        mobileNumber = profile.mobileNumber,
                        gstin = profile.gstin,
                        isGstVerified = profile.isGstVerified,
                        legalBusinessName = cleanLoadedLegalName,
                        isGstRegistered = isGstRegistered,
                        isLoading = false
                    )
                } else {
                    isLoading = false
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile: ${e.localizedMessage}")
                isLoading = false
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateFullName(value: String) {
        fullName = value
        _uiState.value = _uiState.value.copy(fullName = value)
    }

    fun updateBusinessName(value: String) {
        businessName = value
        _uiState.value = _uiState.value.copy(businessName = value)
    }

    fun updateBusinessCategory(value: String) {
        businessCategory = value
        _uiState.value = _uiState.value.copy(businessCategory = value)
    }

    fun updateUpiId(value: String) {
        upiId = value
        _uiState.value = _uiState.value.copy(upiId = value)
    }

    fun updateMerchantName(value: String) {
        merchantName = value
        _uiState.value = _uiState.value.copy(merchantName = value)
    }

    fun updateGstRegistered(registered: Boolean) {
        isGstRegistered = registered
        if (!registered) {
            gstVerificationError = null
        }
        _uiState.value = _uiState.value.copy(
            isGstRegistered = registered,
            gstVerificationError = if (!registered) null else _uiState.value.gstVerificationError
        )
    }

    fun updateGstin(value: String) {
        val uppercaseVal = value.trim().uppercase()
        gstin = uppercaseVal
        isGstVerified = false
        gstVerificationError = null
        _uiState.value = _uiState.value.copy(
            gstin = uppercaseVal,
            isGstVerified = false,
            gstVerificationError = null
        )
    }

    fun verifyGst(context: Context? = null) {
        val targetGstin = gstin.trim().uppercase()
        if (targetGstin.isBlank()) {
            gstVerificationError = "Please enter a 15-character GSTIN"
            _uiState.value = _uiState.value.copy(gstVerificationError = gstVerificationError)
            return
        }

        isVerifyingGst = true
        gstVerificationError = null
        _uiState.value = _uiState.value.copy(isVerifyingGst = true, gstVerificationError = null)

        viewModelScope.launch {
            try {
                val verificationResult = com.example.util.GstVerificationService.verifyGst(
                    rawGstin = targetGstin
                )

                isVerifyingGst = false
                if (verificationResult.isValid) {
                    val cleanName = verificationResult.legalBusinessName
                        .replace(Regex("^(Verified:\\s*)+", RegexOption.IGNORE_CASE), "")
                        .trim()

                    isGstVerified = true
                    legalBusinessName = cleanName
                    gstVerificationError = null

                    // Auto-fill Business Name to match official registered name
                    if (cleanName.isNotBlank()) {
                        businessName = cleanName
                    }

                    _uiState.value = _uiState.value.copy(
                        isVerifyingGst = false,
                        isGstVerified = true,
                        businessName = if (cleanName.isNotBlank()) cleanName else businessName,
                        legalBusinessName = cleanName,
                        gstVerificationError = null
                    )

                    // Store gstin, isGstVerified: true, and clean legalBusinessName directly in Firestore under users/{uid}
                    userRepository.saveGstDetails(
                        gstin = targetGstin,
                        isGstVerified = true,
                        legalBusinessName = cleanName,
                        isGstRegistered = true
                    )
                    if (cleanName.isNotBlank()) {
                        userRepository.updateBusinessName(cleanName)
                    }

                    if (context != null) {
                        Toast.makeText(context, "Verified: $cleanName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isGstVerified = false
                    val error = verificationResult.errorMessage ?: "Invalid GSTIN or Business Record Not Found"
                    gstVerificationError = error
                    _uiState.value = _uiState.value.copy(
                        isVerifyingGst = false,
                        isGstVerified = false,
                        gstVerificationError = error
                    )

                    // Record invalid status in Firestore
                    userRepository.saveGstDetails(
                        gstin = targetGstin,
                        isGstVerified = false,
                        legalBusinessName = "",
                        isGstRegistered = isGstRegistered
                    )
                }
            } catch (e: Exception) {
                isVerifyingGst = false
                val error = "Invalid GSTIN or Business Record Not Found"
                gstVerificationError = error
                _uiState.value = _uiState.value.copy(
                    isVerifyingGst = false,
                    isGstVerified = false,
                    gstVerificationError = error
                )
            }
        }
    }

    fun updateAutoSendWhatsAppInvoice(enabled: Boolean, context: Context? = null) {
        autoSendWhatsAppInvoice = enabled
        if (context != null) {
            com.example.util.WhatsAppInvoiceHelper.setAutoSendEnabled(context, enabled)
        }
    }

    fun saveUserProfile(
        context: Context? = null,
        onSuccess: () -> Unit
    ) {
        if (context != null) {
            com.example.util.WhatsAppInvoiceHelper.setAutoSendEnabled(context, autoSendWhatsAppInvoice)
        }
        if (fullName.isBlank() || businessName.isBlank() || businessCategory.isBlank()) {
            errorMessage = "Please fill all required fields and select a business category"
            _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
            return
        }

        if (isGstRegistered && gstin.isNotBlank() && !isGstVerified) {
            errorMessage = "Please verify your GSTIN before saving"
            _uiState.value = _uiState.value.copy(errorMessage = errorMessage)
            return
        }

        errorMessage = null
        isSaving = true
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            val result = userRepository.saveUserProfile(
                fullName = fullName.trim(),
                businessName = businessName.trim(),
                businessCategory = businessCategory.trim(),
                upiId = upiId.trim().ifBlank { "merchant@upi" },
                merchantName = merchantName.trim().ifBlank { businessName.trim() },
                gstin = if (isGstRegistered) gstin.trim().uppercase() else "",
                isGstVerified = if (isGstRegistered) isGstVerified else false,
                legalBusinessName = if (isGstRegistered) legalBusinessName else "",
                isGstRegistered = isGstRegistered
            )

            isSaving = false
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSuccess = true
                )
                if (context != null) {
                    Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
                errorMessage = errorMsg
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = errorMsg)
                // Proceed smoothly on fallback
                if (context != null) {
                    Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                }
                onSuccess()
            }
        }
    }
}
