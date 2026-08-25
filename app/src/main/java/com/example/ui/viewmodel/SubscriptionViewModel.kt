package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.subscription.AppSessionManager
import com.example.data.subscription.PaymentGatewayConfig
import com.example.data.subscription.SubscriptionInfo
import com.example.data.subscription.SubscriptionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class SubscriptionNavEvent {
    object NavigateToDashboard : SubscriptionNavEvent()
    data class ShowToast(val message: String) : SubscriptionNavEvent()
}

class SubscriptionViewModel : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionInfo> = SubscriptionManager.subscriptionState

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
                    // Success handled cleanly, dialog can be dismissed by user or transition
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
    }

    fun triggerDashboardNavigation() {
        viewModelScope.launch {
            _navigationChannel.send(SubscriptionNavEvent.NavigateToDashboard)
        }
    }
}
