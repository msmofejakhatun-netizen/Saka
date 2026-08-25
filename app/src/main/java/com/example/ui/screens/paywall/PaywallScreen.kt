package com.example.ui.screens.paywall

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: BillingViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = onBack,
    isMandatory: Boolean = false,
    lockReason: String? = null
) {
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val isProUser = subscriptionState.isProUser ||
            subscriptionState.autoPayMandateStatus == "ACTIVE" ||
            subscriptionState.autoPayMandateStatus == "TRIAL_ACTIVE"

    BackHandler(enabled = true) {
        viewModel.closePaywall()
        if (isProUser) {
            onNavigateToDashboard()
        } else if (!isMandatory) {
            onBack()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF0F172A)
    ) {
        SubscriptionScreenContent(
            subscriptionViewModel = subscriptionViewModel,
            billingViewModel = viewModel,
            onClose = onBack,
            onNavigateToDashboard = onNavigateToDashboard,
            isMandatory = isMandatory,
            lockReason = lockReason
        )
    }
}

@Composable
fun PaywallModalDialog(
    viewModel: BillingViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onDismiss: () -> Unit,
    onNavigateToDashboard: () -> Unit = onDismiss,
    isMandatory: Boolean = false,
    lockReason: String? = null
) {
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val isProUser = subscriptionState.isProUser ||
            subscriptionState.autoPayMandateStatus == "ACTIVE" ||
            subscriptionState.autoPayMandateStatus == "TRIAL_ACTIVE"

    BackHandler(enabled = true) {
        viewModel.closePaywall()
        if (isProUser) {
            onNavigateToDashboard()
        } else if (!isMandatory) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (isProUser) {
                viewModel.closePaywall()
                onNavigateToDashboard()
            } else if (!isMandatory) {
                viewModel.closePaywall()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = isProUser || !isMandatory,
            dismissOnClickOutside = isProUser || !isMandatory
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 12.dp
        ) {
            SubscriptionScreenContent(
                subscriptionViewModel = subscriptionViewModel,
                billingViewModel = viewModel,
                onClose = onDismiss,
                onNavigateToDashboard = onNavigateToDashboard,
                isMandatory = isMandatory,
                lockReason = lockReason
            )
        }
    }
}

@Composable
fun PaywallScreenContent(
    viewModel: BillingViewModel,
    onClose: () -> Unit,
    onNavigateToDashboard: () -> Unit = onClose,
    isMandatory: Boolean = false,
    lockReason: String? = null
) {
    val subVm: SubscriptionViewModel = viewModel()
    SubscriptionScreenContent(
        subscriptionViewModel = subVm,
        billingViewModel = viewModel,
        onClose = onClose,
        onNavigateToDashboard = onNavigateToDashboard,
        isMandatory = isMandatory,
        lockReason = lockReason
    )
}
