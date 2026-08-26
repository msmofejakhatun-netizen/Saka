package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.subscription.AppSessionManager
import com.example.data.subscription.PaymentGatewayConfig
import com.example.data.subscription.SubscriptionInfo
import com.example.data.subscription.SubscriptionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SubscriptionNavEvent {
    object NavigateToDashboard : SubscriptionNavEvent()
    data class ShowToast(val message: String) : SubscriptionNavEvent()
}

data class SubscriptionUiState(
    val showTrialPlan: Boolean = true,
    val hasUsedTrial: Boolean = false,
    val isProUser: Boolean = false,
    val subscriptionStatus: String = "FREE",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SubscriptionViewModel : ViewModel() {

    companion object {
        private const val TAG = "SubscriptionViewModel"
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val subscriptionState: StateFlow<SubscriptionInfo> = SubscriptionManager.subscriptionState

    private val _uiState = MutableStateFlow(
        SubscriptionUiState(
            showTrialPlan = !SubscriptionManager.subscriptionState.value.hasUsedTrial,
            hasUsedTrial = SubscriptionManager.subscriptionState.value.hasUsedTrial,
            isProUser = SubscriptionManager.subscriptionState.value.isProUser,
            subscriptionStatus = SubscriptionManager.subscriptionState.value.autoPayMandateStatus
        )
    )
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    val isProUser: StateFlow<Boolean> = subscriptionState
        .map { it.isProUser }
        .distinctUntilChanged()
        .let { flow ->
            val initial = subscriptionState.value.isProUser
            val stateFlow = MutableStateFlow(initial)
            viewModelScope.launch {
                flow.collect { stateFlow.value = it }
            }
            stateFlow
        }

    val subscriptionTier: StateFlow<String> = subscriptionState
        .map { it.subscriptionTier }
        .distinctUntilChanged()
        .let { flow ->
            val initial = subscriptionState.value.subscriptionTier
            val stateFlow = MutableStateFlow(initial)
            viewModelScope.launch {
                flow.collect { stateFlow.value = it }
            }
            stateFlow
        }

    val hasUsedTrial: StateFlow<Boolean> = subscriptionState
        .map { it.hasUsedTrial }
        .distinctUntilChanged()
        .let { flow ->
            val initial = subscriptionState.value.hasUsedTrial
            val stateFlow = MutableStateFlow(initial)
            viewModelScope.launch {
                flow.collect { stateFlow.value = it }
            }
            stateFlow
        }

    private val _isSuccessDialogVisible = MutableStateFlow(false)
    val isSuccessDialogVisible: StateFlow<Boolean> = _isSuccessDialogVisible.asStateFlow()

    private val _navigationChannel = Channel<SubscriptionNavEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<SubscriptionNavEvent> = _navigationChannel.receiveAsFlow()

    init {
        val currentAuthUser = FirebaseManager.auth?.currentUser
        val uid = currentAuthUser?.uid ?: ""
        if (uid.isNotBlank()) {
            checkTrialEligibility(uid)
        }

        viewModelScope.launch {
            subscriptionState.collect { info ->
                val usedTrial = info.hasUsedTrial || info.trialStartDate > 0L || info.subscriptionTier == "TRIAL_1_INR"
                _uiState.update {
                    it.copy(
                        showTrialPlan = !usedTrial,
                        hasUsedTrial = usedTrial,
                        isProUser = info.isProUser,
                        subscriptionStatus = info.autoPayMandateStatus
                    )
                }
            }
        }
    }

    /**
     * Verifies trial eligibility directly against the server-side Firestore records:
     * - users/{userId} (hasUsedTrial, trialStartDate, mandateId, subscriptionStatus)
     * - users/{userId}/subscription/current
     */
    fun checkTrialEligibility(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val subDoc = firestore.collection("users").document(userId)
                    .collection("subscription").document("current").get().await()

                val hasAlreadyUsedTrial = (userDoc.getBoolean("hasUsedTrial") ?: false) ||
                        (subDoc.getBoolean("hasUsedTrial") ?: false) ||
                        ((userDoc.getLong("trialStartDate") ?: 0L) > 0L) ||
                        ((subDoc.getLong("trialStartDate") ?: 0L) > 0L) ||
                        (subDoc.getString("subscriptionTier") == "TRIAL_1_INR")

                val status = subDoc.getString("status")
                    ?: userDoc.getString("subscriptionStatus")
                    ?: userDoc.getString("status")
                    ?: "FREE"
                val isPro = subDoc.getBoolean("isProUser") ?: userDoc.getBoolean("isProUser") ?: false

                if (hasAlreadyUsedTrial) {
                    // Hide the 3-Day Trial option completely
                    // Show only Regular Monthly (₹79) and Annual (₹799) plans
                    _uiState.update {
                        it.copy(
                            showTrialPlan = false,
                            hasUsedTrial = true,
                            subscriptionStatus = status,
                            isProUser = isPro,
                            isLoading = false
                        )
                    }
                } else {
                    // Allow trial only if never used before
                    _uiState.update {
                        it.copy(
                            showTrialPlan = true,
                            hasUsedTrial = false,
                            subscriptionStatus = status,
                            isProUser = isPro,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking trial eligibility: ${e.localizedMessage}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun showSuccessDialog() {
        _isSuccessDialogVisible.value = true
    }

    fun dismissSuccessDialog() {
        _isSuccessDialogVisible.value = false
    }

    fun onSubscriptionSuccess(context: Context, userUid: String, paymentId: String) {
        viewModelScope.launch {
            _isSuccessDialogVisible.value = true
            PaymentGatewayConfig.handlePaymentSuccess(
                context = context,
                userUid = userUid,
                razorpayPaymentId = paymentId,
                onComplete = {
                    checkTrialEligibility(userUid)
                }
            )
        }
    }

    fun cancelSubscription(
        context: Context,
        userUid: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        SubscriptionManager.cancelSubscription(
            context = context,
            userUid = userUid,
            onComplete = onComplete
        )
    }

    fun refreshSubscription(context: Context, userUid: String) {
        SubscriptionManager.init(context, userUid)
        AppSessionManager.verifyAndEnforceSubscriptionLock(context, userUid)
        checkTrialEligibility(userUid)
    }

    fun triggerDashboardNavigation() {
        viewModelScope.launch {
            _navigationChannel.send(SubscriptionNavEvent.NavigateToDashboard)
        }
    }
}
