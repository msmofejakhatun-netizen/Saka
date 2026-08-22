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

    private val _navigationChannel = Channel<SubscriptionNavEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<SubscriptionNavEvent> = _navigationChannel.receiveAsFlow()

    private var hasEmittedActiveNav = false

    init {
        viewModelScope.launch {
            subscriptionState.collect { info ->
                val isActive = info.isProUser ||
                        info.autoPayMandateStatus == "ACTIVE" ||
                        info.autoPayMandateStatus == "TRIAL_ACTIVE"
                if (isActive && !hasEmittedActiveNav) {
                    hasEmittedActiveNav = true
                    _navigationChannel.send(SubscriptionNavEvent.NavigateToDashboard)
                } else if (!isActive) {
                    hasEmittedActiveNav = false
                }
            }
        }
    }

    fun onSubscriptionSuccess(context: Context, userUid: String, paymentId: String) {
        viewModelScope.launch {
            PaymentGatewayConfig.handlePaymentSuccess(
                context = context,
                userUid = userUid,
                razorpayPaymentId = paymentId,
                onComplete = {
                    viewModelScope.launch {
                        _navigationChannel.send(SubscriptionNavEvent.NavigateToDashboard)
                    }
                }
            )
        }
    }

    fun refreshSubscription(context: Context, userUid: String) {
        AppSessionManager.verifyAndEnforceSubscriptionLock(context, userUid)
    }

    fun triggerDashboardNavigation() {
        viewModelScope.launch {
            _navigationChannel.send(SubscriptionNavEvent.NavigateToDashboard)
        }
    }
}

