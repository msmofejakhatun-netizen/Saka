package com.example.ui.screens.paywall

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.subscription.PaymentGatewayConfig
import com.example.data.subscription.PaymentGatewayHandler
import com.example.data.subscription.SubscriptionManager
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: BillingViewModel,
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
        if (isProUser) {
            viewModel.closePaywall()
            onNavigateToDashboard()
        } else if (!isMandatory) {
            viewModel.closePaywall()
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
        PaywallScreenContent(
            viewModel = viewModel,
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
        if (isProUser) {
            viewModel.closePaywall()
            onNavigateToDashboard()
        } else if (!isMandatory) {
            viewModel.closePaywall()
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
            PaywallScreenContent(
                viewModel = viewModel,
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
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isProUser = subscriptionState.isProUser ||
            subscriptionState.autoPayMandateStatus == "ACTIVE" ||
            subscriptionState.autoPayMandateStatus == "TRIAL_ACTIVE"

    var selectedPlan by remember { mutableStateOf(PaymentGatewayHandler.SubscriptionPlan.TRIAL_3_DAYS_1_INR) }
    var showPaymentBottomSheet by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    var isSuccessOverlayVisible by remember { mutableStateOf(false) }
    var hasProcessedSuccess by remember { mutableStateOf(false) }

    val userId = currentUser?.mobileNumber.orEmpty().ifBlank {
        com.example.data.firebase.FirebaseManager.auth?.currentUser?.uid.orEmpty()
    }

    // Real-time snapshot listener on users/{userId}/subscription/current
    DisposableEffect(userId) {
        if (userId.isBlank() || !com.example.data.firebase.FirebaseManager.isFirebaseAvailable) {
            return@DisposableEffect onDispose { }
        }

        val firestore = com.example.data.firebase.FirebaseManager.firestore
            ?: return@DisposableEffect onDispose { }

        val registration = firestore.collection("users")
            .document(userId)
            .collection("subscription")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("PaywallScreen", "Subscription listener error: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status").orEmpty()
                    val isPro = snapshot.getBoolean("isProUser") ?: false

                    if ((status == "ACTIVE" || status == "TRIAL_ACTIVE" || isPro) && !hasProcessedSuccess) {
                        hasProcessedSuccess = true
                        isSuccessOverlayVisible = true
                        playSuccessChime(context)
                    }
                }
            }

        onDispose {
            registration.remove()
        }
    }

    // Fallback: Monitor local subscriptionState flow directly for instantaneous response
    LaunchedEffect(subscriptionState) {
        val status = subscriptionState.autoPayMandateStatus
        val isPro = subscriptionState.isProUser
        if ((status == "ACTIVE" || status == "TRIAL_ACTIVE" || isPro) && !hasProcessedSuccess) {
            hasProcessedSuccess = true
            isSuccessOverlayVisible = true
            playSuccessChime(context)
        }
    }

    // Auto-dismiss and navigate to dashboard after 1.5 second green checkmark confirmation
    LaunchedEffect(isSuccessOverlayVisible) {
        if (isSuccessOverlayVisible) {
            kotlinx.coroutines.delay(1500)
            viewModel.closePaywall()
            onNavigateToDashboard()
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1333),
                        Color(0xFF0F172A),
                        Color(0xFF1E1035)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMandatory || isProUser) {
                    IconButton(
                        onClick = {
                            viewModel.closePaywall()
                            if (isProUser) {
                                onNavigateToDashboard()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .background(Color(0x22FFFFFF), CircleShape)
                            .testTag("paywall_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x33EF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFEF4444))
                    }
                }

                Surface(
                    color = Color(0x33F59E0B),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (subscriptionState.isProUser) "VIP PRO ACTIVE" else "MANDATORY PRO ACCESS",
                            color = GoldYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isMandatory) {
                Surface(
                    color = Color(0x33EF4444),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = lockReason ?: "Subscription Expired. Upgrade to Pro to continue using SmartPOS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Pro Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart POS Pro",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "Razorpay & PhonePe Auto-Pay Integration",
                                color = Color(0xFFF1F5F9),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Start 3-Day Free Trial @ ₹1 Mandate. Direct settlement into Merchant Bank Account (${PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED}). Billed via Razorpay Subscriptions / PhonePe UPI Autopay.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Subscription Status if Pro
            if (subscriptionState.isProUser) {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Subscription (${subscriptionState.gatewayProvider})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Plan Tier: ${subscriptionState.subscriptionTier}",
                            color = EmeraldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (subscriptionState.subscriptionExpiryDate > 0) {
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val expiryStr = sdf.format(Date(subscriptionState.subscriptionExpiryDate))
                            Text(
                                text = "Current Cycle Valid Until: $expiryStr",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        if (subscriptionState.autoPayMandateId.isNotBlank()) {
                            Text(
                                text = "Gateway Mandate Ref: ${subscriptionState.autoPayMandateId}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Direct Settlement: ${subscriptionState.settlementAccount}",
                            color = GoldYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (subscriptionState.autoPayMandateStatus == "ACTIVE") {
                            OutlinedButton(
                                onClick = {
                                    SubscriptionManager.cancelSubscription(
                                        context = context,
                                        userUid = currentUser?.mobileNumber ?: "",
                                        onComplete = { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPink),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Auto-Pay Mandate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Plan Selection Selector
            Text(
                text = "Choose Your Subscription Plan",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Plan 1: 3-Day Trial @ ₹1 Mandate
            PlanCardOption(
                title = "3-Day Trial @ ₹1",
                badgeText = "🔥 RAZORPAY / PHONEPE MANDATE",
                priceText = "₹1 Setup Fee",
                subtext = "₹1 Mandate Authorization → 3 Days Free Trial → Then ₹79/Month Auto-Debit",
                isSelected = selectedPlan == PaymentGatewayHandler.SubscriptionPlan.TRIAL_3_DAYS_1_INR,
                onClick = { selectedPlan = PaymentGatewayHandler.SubscriptionPlan.TRIAL_3_DAYS_1_INR }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Plan 2: Monthly Plan
            PlanCardOption(
                title = "Monthly Pro Plan",
                badgeText = "REGULAR RECURRING",
                priceText = "₹79 / month",
                subtext = "Instant Bank Settlement. Recurring monthly debit via Razorpay/PhonePe. Cancel anytime.",
                isSelected = selectedPlan == PaymentGatewayHandler.SubscriptionPlan.MONTHLY_79_INR,
                onClick = { selectedPlan = PaymentGatewayHandler.SubscriptionPlan.MONTHLY_79_INR }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Plan 3: Annual Pass
            PlanCardOption(
                title = "Annual Pro Pass",
                badgeText = "BEST VALUE (SAVE 15%)",
                priceText = "₹799 / year",
                subtext = "Equivalent to ₹66/month. Direct settlement into linked merchant bank account.",
                isSelected = selectedPlan == PaymentGatewayHandler.SubscriptionPlan.ANNUAL_799_INR,
                onClick = { selectedPlan = PaymentGatewayHandler.SubscriptionPlan.ANNUAL_799_INR }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features Checklist Grid
            Text(
                text = "What You Get with Pro Access",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProFeatureRow(
                    icon = Icons.Default.ReceiptLong,
                    title = "Unlimited POS Invoices & Bluetooth Printing",
                    desc = "Print thermal receipts, invoices & bills without daily limits."
                )
                ProFeatureRow(
                    icon = Icons.Default.AccountBalance,
                    title = "Direct Merchant Settlement",
                    desc = "Instant bank payouts straight to ${PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED}."
                )
                ProFeatureRow(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Backup & Multi-Device Udhar Khata",
                    desc = "Real-time sync to Firebase Cloud, restore data on any device safely."
                )
                ProFeatureRow(
                    icon = Icons.Default.Send,
                    title = "Automated WhatsApp Udhar Reminders",
                    desc = "Send instant payment links and debt receipts to customers in 1 click."
                )
                ProFeatureRow(
                    icon = Icons.Default.Security,
                    title = "Razorpay & PhonePe UPI Autopay Security",
                    desc = "Protected by RBI e-mandate rules with 24h prior notification before debit."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Primary CTA Button
            Button(
                onClick = {
                    if (activity != null) {
                        PaymentGatewayHandler.launchRazorpayCheckout(
                            activity = activity,
                            plan = selectedPlan,
                            userEmail = "${currentUser?.mobileNumber ?: "9999999999"}@smartpos.com",
                            userPhone = currentUser?.mobileNumber ?: ""
                        )
                    } else {
                        showPaymentBottomSheet = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("paywall_start_trial_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EmeraldGreen, Color(0xFF059669))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedPlan) {
                                PaymentGatewayHandler.SubscriptionPlan.TRIAL_3_DAYS_1_INR -> "Start 3-Day Trial @ ₹1 Mandate"
                                PaymentGatewayHandler.SubscriptionPlan.MONTHLY_79_INR -> "Subscribe Monthly @ ₹79"
                                PaymentGatewayHandler.SubscriptionPlan.ANNUAL_799_INR -> "Get Annual Pass @ ₹799"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compliance & RBI Autopay Guarantee
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Direct Razorpay / PhonePe Payment Gateway. RBI e-Mandate Compliant.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Payment / Mandate Bottom Sheet Modal
        if (showPaymentBottomSheet) {
            PaymentGatewayBottomSheet(
                plan = selectedPlan,
                isProcessing = isProcessingPayment,
                onConfirmMandate = { provider, selectedApp, userVpa ->
                    if (provider == PaymentGatewayConfig.GatewayProvider.RAZORPAY && activity != null) {
                        showPaymentBottomSheet = false
                        PaymentGatewayHandler.launchRazorpayCheckout(
                            activity = activity,
                            plan = selectedPlan,
                            userEmail = "${currentUser?.mobileNumber ?: "9999999999"}@smartpos.com",
                            userPhone = currentUser?.mobileNumber ?: ""
                        )
                    } else {
                        isProcessingPayment = true
                        PaymentGatewayHandler.initiateSubscriptionMandate(
                            context = context,
                            plan = selectedPlan,
                            provider = provider,
                            selectedApp = selectedApp,
                            userVpa = userVpa,
                            userMobile = currentUser?.mobileNumber ?: "",
                            userUid = currentUser?.mobileNumber ?: "",
                            onResult = { result ->
                                isProcessingPayment = false
                                showPaymentBottomSheet = false
                                if (result.isSuccess) {
                                    val mandateRef = if (result.phonePeMandateId.isNotBlank()) result.phonePeMandateId else result.razorpaySubscriptionId
                                    Toast.makeText(
                                        context,
                                        "🎉 Mandate Approved! Pro Activated via ${result.provider}. Ref: $mandateRef",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isSuccessOverlayVisible = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error: ${result.errorMessage ?: "Payment authorization failed"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                },
                onDismiss = { if (!isProcessingPayment) showPaymentBottomSheet = false }
            )
        }

        // Green checkmark success overlay (1.5-second transition with instant click-to-proceed)
        if (isSuccessOverlayVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable {
                        viewModel.closePaywall()
                        onNavigateToDashboard()
                    }
                    .testTag("paywall_success_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(EmeraldGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Subscription Activated! 🎉",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlocking Pro POS Features...",
                        color = EmeraldLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCardOption(
    title: String,
    badgeText: String,
    priceText: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0x3310B981) else Color(0x221E293B)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) EmeraldGreen else Color(0x33FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    color = if (isSelected) EmeraldGreen else Color(0xFF334155),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = priceText,
                    color = EmeraldLight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtext,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0x3310B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentGatewayBottomSheet(
    plan: PaymentGatewayHandler.SubscriptionPlan,
    isProcessing: Boolean,
    onConfirmMandate: (
        provider: PaymentGatewayConfig.GatewayProvider,
        selectedApp: String,
        userVpa: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(PaymentGatewayConfig.GatewayProvider.RAZORPAY) }
    var selectedApp by remember { mutableStateOf("PhonePe") }
    var userVpa by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Surface(
                color = Color(0x3310B981),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RAZORPAY / PHONEPE RECURRING GATEWAY",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Authorize ₹1 Mandate Setup",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Middle Selection Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mandate Authorization Fee:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("₹1.00", color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Selected Plan:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(plan.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recurring Auto-Debit:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = if (plan == PaymentGatewayHandler.SubscriptionPlan.TRIAL_3_DAYS_1_INR) "₹79/mo (After 3-Day Trial)" else "${plan.recurringPrice}/${plan.billingCycle}",
                                color = GoldYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Direct Merchant Payout:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED, color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. Select Merchant Payment Gateway",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedProvider == PaymentGatewayConfig.GatewayProvider.RAZORPAY,
                        onClick = { selectedProvider = PaymentGatewayConfig.GatewayProvider.RAZORPAY },
                        label = { Text("Razorpay Subscriptions") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedProvider == PaymentGatewayConfig.GatewayProvider.PHONEPE,
                        onClick = { selectedProvider = PaymentGatewayConfig.GatewayProvider.PHONEPE },
                        label = { Text("PhonePe Autopay") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. Select UPI App / One-Tap App",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                val upiApps = listOf("PhonePe", "Google Pay", "Paytm", "BHIM", "Custom VPA Input")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upiApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedApp == app) Color(0x3310B981) else Color(0x11FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedApp = app }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedApp == app,
                                onClick = { selectedApp = app },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (selectedApp == "Custom VPA Input") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = userVpa,
                        onValueChange = { userVpa = it },
                        label = { Text("Enter UPI ID (e.g. mobile@ybl / merchant@okaxis)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Primary Action Button Area - Sits completely above navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (isProcessing) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = EmeraldGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Connecting to ${PaymentGatewayConfig.getActiveGatewayName(selectedProvider)}...",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { onConfirmMandate(selectedProvider, selectedApp, userVpa) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_mandate_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text(
                            "Authorize ₹1 Mandate Setup via ${if (selectedProvider == PaymentGatewayConfig.GatewayProvider.RAZORPAY) "Razorpay" else "PhonePe"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun playSuccessChime(context: android.content.Context) {
    try {
        val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
        ringtone?.play()
    } catch (e: Exception) {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 300)
        } catch (_: Exception) {}
    }
}
