package com.example.data.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class SubscriptionInfo(
    val isProUser: Boolean = false,
    val subscriptionTier: String = "FREE", // "FREE", "TRIAL_1_INR", "MONTHLY_79_INR", "ANNUAL_799_INR"
    val subscriptionExpiryDate: Long = 0L,
    val autoPayMandateStatus: String = "NONE", // "NONE", "ACTIVE", "CANCELLED", "FAILED"
    val autoPayMandateId: String = "",
    val gatewayProvider: String = "RAZORPAY", // "RAZORPAY", "PHONEPE", "DIRECT_UPI_MANDATE"
    val gatewaySubscriptionId: String = "",
    val settlementAccount: String = PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED,
    val trialStartDate: Long = 0L,
    val paymentMethod: String = ""
)

object SubscriptionManager {
    private const val TAG = "SubscriptionManager"
    private const val PREFS_NAME = "kirana_subscription_prefs"

    private const val KEY_IS_PRO = "is_pro_user"
    private const val KEY_TIER = "subscription_tier"
    private const val KEY_EXPIRY = "subscription_expiry"
    private const val KEY_MANDATE_STATUS = "auto_pay_mandate_status"
    private const val KEY_MANDATE_ID = "auto_pay_mandate_id"
    private const val KEY_GATEWAY_PROVIDER = "gateway_provider"
    private const val KEY_GATEWAY_SUB_ID = "gateway_subscription_id"
    private const val KEY_SETTLEMENT_ACCT = "settlement_account"
    private const val KEY_TRIAL_START = "trial_start_date"
    private const val KEY_PAYMENT_METHOD = "payment_method"

    private val _subscriptionState = MutableStateFlow(SubscriptionInfo())
    val subscriptionState: StateFlow<SubscriptionInfo> = _subscriptionState.asStateFlow()

    fun init(context: Context, userUid: String = "") {
        val effectiveUid = userUid.ifBlank { FirebaseManager.auth?.currentUser?.uid ?: "" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSavedUid = prefs.getString("last_user_uid", "") ?: ""

        // Clear local state if a different user logged in
        if (effectiveUid.isNotBlank() && lastSavedUid.isNotBlank() && effectiveUid != lastSavedUid) {
            prefs.edit().clear().apply()
            _subscriptionState.value = SubscriptionInfo()
        }

        if (effectiveUid.isNotBlank()) {
            prefs.edit().putString("last_user_uid", effectiveUid).apply()
        }

        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        val tier = prefs.getString(KEY_TIER, "FREE") ?: "FREE"
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        val mandateStatus = prefs.getString(KEY_MANDATE_STATUS, "NONE") ?: "NONE"
        val mandateId = prefs.getString(KEY_MANDATE_ID, "") ?: ""
        val provider = prefs.getString(KEY_GATEWAY_PROVIDER, "RAZORPAY") ?: "RAZORPAY"
        val subId = prefs.getString(KEY_GATEWAY_SUB_ID, "") ?: ""
        val settlement = prefs.getString(KEY_SETTLEMENT_ACCT, PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED) ?: PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        val method = prefs.getString(KEY_PAYMENT_METHOD, "") ?: ""

        val now = System.currentTimeMillis()
        var validPro = isPro
        if (expiry > 0L && now > expiry) {
            if (mandateStatus == "ACTIVE") {
                val newExpiry = now + TimeUnit.DAYS.toMillis(30)
                prefs.edit().putLong(KEY_EXPIRY, newExpiry).apply()
                validPro = true
            } else {
                validPro = false
                prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
            }
        }

        val info = SubscriptionInfo(
            isProUser = validPro,
            subscriptionTier = tier,
            subscriptionExpiryDate = expiry,
            autoPayMandateStatus = mandateStatus,
            autoPayMandateId = mandateId,
            gatewayProvider = provider,
            gatewaySubscriptionId = subId,
            settlementAccount = settlement,
            trialStartDate = trialStart,
            paymentMethod = method
        )
        _subscriptionState.value = info

        if (effectiveUid.isNotBlank()) {
            fetchRemoteSubscription(effectiveUid, context)
        }
    }

    fun clearLocalSubscriptionState(context: Context? = null) {
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing prefs: ${e.localizedMessage}")
            }
        }
        _subscriptionState.value = SubscriptionInfo()
        Log.d(TAG, "Cleared local subscription session state on logout")
    }

    fun activateTrialMandate(
        context: Context,
        paymentMethod: String,
        userUid: String,
        gatewayProvider: String = "RAZORPAY",
        subscriptionId: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val trialExpiry = now + TimeUnit.DAYS.toMillis(3) // 3-day trial
        val mandateId = if (subscriptionId.isNotBlank()) subscriptionId else "MND-RZP-" + (100000..999999).random()

        val info = SubscriptionInfo(
            isProUser = true,
            subscriptionTier = "TRIAL_1_INR",
            subscriptionExpiryDate = trialExpiry,
            autoPayMandateStatus = "TRIAL_ACTIVE",
            autoPayMandateId = mandateId,
            gatewayProvider = gatewayProvider,
            gatewaySubscriptionId = mandateId,
            settlementAccount = PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED,
            trialStartDate = now,
            paymentMethod = paymentMethod
        )

        saveLocal(context, info)
        _subscriptionState.value = info

        syncToFirebase(userUid, info)
        onComplete(true, "3-Day Trial @ ₹1 Activated Successfully via $gatewayProvider! Mandate Ref: $mandateId")
    }

    /**
     * Immediate client-side success callback handler for Razorpay / PhonePe SDK payment completion.
     * Performs instant optimistic state transition locally and updates Firestore users/{userId}/subscription/current.
     */
    fun onPaymentSuccess(
        context: Context,
        userUid: String,
        razorpayPaymentId: String,
        paymentData: Any? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val effectiveUid = resolveUserUid(userUid)
        val now = System.currentTimeMillis()
        val trialExpiry = now + (3 * 24 * 60 * 60 * 1000L) // 3-day trial period
        val mandateId = razorpayPaymentId.ifBlank { "MND-RZP-" + (100000..999999).random() }

        val info = SubscriptionInfo(
            isProUser = true,
            subscriptionTier = "TRIAL_1_INR",
            subscriptionExpiryDate = trialExpiry,
            autoPayMandateStatus = "TRIAL_ACTIVE",
            autoPayMandateId = mandateId,
            gatewayProvider = "RAZORPAY",
            gatewaySubscriptionId = mandateId,
            settlementAccount = PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED,
            trialStartDate = now,
            paymentMethod = "Razorpay Checkout (UPI Autopay)"
        )

        saveLocal(context, info)
        _subscriptionState.value = info

        if (effectiveUid.isNotBlank() && FirebaseManager.isFirebaseAvailable) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestore = FirebaseManager.firestore
                    if (firestore != null) {
                        val subMap = hashMapOf(
                            "status" to "TRIAL_ACTIVE",
                            "isProUser" to true,
                            "mandateId" to mandateId,
                            "expiryTimestamp" to trialExpiry,
                            "subscriptionTier" to "TRIAL_1_INR",
                            "lastUpdated" to System.currentTimeMillis(),
                            "autoPayMandateStatus" to "TRIAL_ACTIVE",
                            "gatewayProvider" to "RAZORPAY",
                            "gatewaySubscriptionId" to mandateId,
                            "settlementAccount" to PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED,
                            "trialStartDate" to now,
                            "paymentMethod" to "Razorpay Checkout (UPI Autopay)"
                        )
                        firestore.collection("users").document(effectiveUid)
                            .collection("subscription").document("current")
                            .set(subMap, com.google.firebase.firestore.SetOptions.merge()).await()

                        firestore.collection("users").document(effectiveUid)
                            .set(
                                hashMapOf(
                                    "isProUser" to true,
                                    "subscriptionTier" to "TRIAL_1_INR",
                                    "subscriptionStatus" to "TRIAL_ACTIVE",
                                    "updatedAt" to System.currentTimeMillis()
                                ),
                                com.google.firebase.firestore.SetOptions.merge()
                            ).await()
                        Log.d(TAG, "Successfully updated Firestore users/$effectiveUid/subscription/current on payment success")
                    }

                    // Update OneSignal CRM Tagging
                    try {
                        com.onesignal.OneSignal.User.addTag("subscription_status", "PRO_ACTIVE")
                        com.onesignal.OneSignal.User.addTag("is_pro_user", "true")
                    } catch (e: Exception) {
                        Log.d(TAG, "OneSignal tag update error: ${e.localizedMessage}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating Firestore in onPaymentSuccess: ${e.localizedMessage}")
                }
            }
        }

        com.example.data.subscription.AppSessionManager.verifyAndEnforceSubscriptionLock(context, effectiveUid)
        onComplete?.invoke()
    }

    fun activateSubscriptionPlan(
        context: Context,
        tier: String, // "MONTHLY_79_INR" or "ANNUAL_799_INR"
        paymentMethod: String,
        userUid: String,
        gatewayProvider: String = "RAZORPAY",
        subscriptionId: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val durationDays = if (tier == "ANNUAL_799_INR") 365L else 30L
        val expiry = now + TimeUnit.DAYS.toMillis(durationDays)
        val mandateId = if (subscriptionId.isNotBlank()) subscriptionId else "SUB-GATEWAY-" + (100000..999999).random()

        val info = SubscriptionInfo(
            isProUser = true,
            subscriptionTier = tier,
            subscriptionExpiryDate = expiry,
            autoPayMandateStatus = "ACTIVE",
            autoPayMandateId = mandateId,
            gatewayProvider = gatewayProvider,
            gatewaySubscriptionId = mandateId,
            settlementAccount = PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED,
            trialStartDate = if (_subscriptionState.value.trialStartDate > 0) _subscriptionState.value.trialStartDate else now,
            paymentMethod = paymentMethod
        )

        saveLocal(context, info)
        _subscriptionState.value = info

        syncToFirebase(userUid, info)
        val planName = if (tier == "ANNUAL_799_INR") "Annual Plan (₹799/yr)" else "Monthly Plan (₹79/mo)"
        onComplete(true, "Successfully subscribed to $planName via $gatewayProvider! Mandate Ref: $mandateId")
    }

    fun cancelSubscription(
        context: Context,
        userUid: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val current = _subscriptionState.value
        val now = System.currentTimeMillis()
        val updated = current.copy(
            autoPayMandateStatus = "CANCELLED",
            isProUser = current.subscriptionExpiryDate > now
        )

        saveLocal(context, updated)
        _subscriptionState.value = updated
        syncToFirebase(userUid, updated)

        onComplete(true, "Auto-pay mandate cancelled on ${current.gatewayProvider}. You retain Pro benefits until ${current.subscriptionExpiryDate}.")
    }

    /**
     * Handles background Webhooks from Razorpay or PhonePe (e.g. Mandate Authenticated, Monthly Auto-Debit Success/Failure).
     */
    fun handleWebhookUpdate(
        context: Context,
        userUid: String,
        eventType: PaymentGatewayHandler.WebhookEventType,
        mandateRef: String
    ) {
        val current = _subscriptionState.value
        val now = System.currentTimeMillis()

        val updated = when (eventType) {
            PaymentGatewayHandler.WebhookEventType.SUBSCRIPTION_AUTHENTICATED -> {
                current.copy(
                    isProUser = true,
                    autoPayMandateStatus = "ACTIVE",
                    autoPayMandateId = mandateRef.ifBlank { current.autoPayMandateId }
                )
            }
            PaymentGatewayHandler.WebhookEventType.RECURRING_DEBIT_SUCCESS -> {
                val newExpiry = (if (current.subscriptionExpiryDate > now) current.subscriptionExpiryDate else now) + TimeUnit.DAYS.toMillis(30)
                current.copy(
                    isProUser = true,
                    autoPayMandateStatus = "ACTIVE",
                    subscriptionExpiryDate = newExpiry
                )
            }
            PaymentGatewayHandler.WebhookEventType.RECURRING_DEBIT_FAILED -> {
                current.copy(
                    autoPayMandateStatus = "FAILED"
                )
            }
            PaymentGatewayHandler.WebhookEventType.SUBSCRIPTION_CANCELLED -> {
                current.copy(
                    autoPayMandateStatus = "CANCELLED"
                )
            }
            PaymentGatewayHandler.WebhookEventType.SUBSCRIPTION_HALTED -> {
                current.copy(
                    autoPayMandateStatus = "FAILED",
                    isProUser = false
                )
            }
        }

        saveLocal(context, updated)
        _subscriptionState.value = updated
        syncToFirebase(userUid, updated)
    }

    private fun saveLocal(context: Context, info: SubscriptionInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_PRO, info.isProUser)
            .putString(KEY_TIER, info.subscriptionTier)
            .putLong(KEY_EXPIRY, info.subscriptionExpiryDate)
            .putString(KEY_MANDATE_STATUS, info.autoPayMandateStatus)
            .putString(KEY_MANDATE_ID, info.autoPayMandateId)
            .putString(KEY_GATEWAY_PROVIDER, info.gatewayProvider)
            .putString(KEY_GATEWAY_SUB_ID, info.gatewaySubscriptionId)
            .putString(KEY_SETTLEMENT_ACCT, info.settlementAccount)
            .putLong(KEY_TRIAL_START, info.trialStartDate)
            .putString(KEY_PAYMENT_METHOD, info.paymentMethod)
            .apply()
    }

    private fun resolveUserUid(providedUid: String): String {
        if (providedUid.isNotBlank()) return providedUid
        return FirebaseManager.auth?.currentUser?.uid ?: ""
    }

    private fun syncToFirebase(userUid: String, info: SubscriptionInfo) {
        val targetUid = resolveUserUid(userUid)
        if (targetUid.isBlank() || !FirebaseManager.isFirebaseAvailable) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val statusStr = if (info.isProUser) {
                        if (info.autoPayMandateStatus == "TRIAL_ACTIVE" || info.subscriptionTier == "TRIAL_1_INR") "TRIAL_ACTIVE"
                        else if (info.autoPayMandateStatus == "CANCELLED") "CANCELLED"
                        else "ACTIVE"
                    } else {
                        "EXPIRED"
                    }
                    val subMap = hashMapOf(
                        "isProUser" to info.isProUser,
                        "subscriptionTier" to info.subscriptionTier,
                        "status" to statusStr,
                        "mandateId" to info.autoPayMandateId,
                        "expiryTimestamp" to info.subscriptionExpiryDate,
                        "lastUpdated" to System.currentTimeMillis(),
                        "autoPayMandateStatus" to info.autoPayMandateStatus,
                        "gatewayProvider" to info.gatewayProvider,
                        "gatewaySubscriptionId" to info.gatewaySubscriptionId,
                        "settlementAccount" to info.settlementAccount,
                        "trialStartDate" to info.trialStartDate,
                        "paymentMethod" to info.paymentMethod
                    )
                    firestore.collection("users").document(targetUid)
                        .collection("subscription").document("current")
                        .set(subMap).await()
                    firestore.collection("users").document(targetUid)
                        .set(
                            hashMapOf(
                                "isProUser" to info.isProUser,
                                "subscriptionTier" to info.subscriptionTier,
                                "subscriptionStatus" to statusStr,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        ).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing subscription to Firebase: ${e.localizedMessage}")
            }
        }
    }

    private fun fetchRemoteSubscription(userUid: String, context: Context) {
        val targetUid = resolveUserUid(userUid)
        if (targetUid.isBlank() || !FirebaseManager.isFirebaseAvailable) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val doc = firestore.collection("users").document(targetUid)
                        .collection("subscription").document("current").get().await()
                    if (doc.exists()) {
                        val isPro = doc.getBoolean("isProUser") ?: false
                        val tier = doc.getString("subscriptionTier") ?: "FREE"
                        val expiry = doc.getLong("expiryTimestamp") ?: doc.getLong("subscriptionExpiryDate") ?: 0L
                        val mandateStatus = doc.getString("autoPayMandateStatus") ?: "NONE"
                        val mandateId = doc.getString("mandateId") ?: doc.getString("autoPayMandateId") ?: ""
                        val provider = doc.getString("gatewayProvider") ?: "RAZORPAY"
                        val subId = doc.getString("gatewaySubscriptionId") ?: ""
                        val settlement = doc.getString("settlementAccount") ?: PaymentGatewayConfig.SETTLEMENT_ACCOUNT_MASKED
                        val trialStart = doc.getLong("trialStartDate") ?: 0L
                        val method = doc.getString("paymentMethod") ?: ""

                        val now = System.currentTimeMillis()
                        val validPro = isPro && (expiry == 0L || expiry > now)

                        val remoteInfo = SubscriptionInfo(
                            isProUser = validPro,
                            subscriptionTier = tier,
                            subscriptionExpiryDate = expiry,
                            autoPayMandateStatus = mandateStatus,
                            autoPayMandateId = mandateId,
                            gatewayProvider = provider,
                            gatewaySubscriptionId = subId,
                            settlementAccount = settlement,
                            trialStartDate = trialStart,
                            paymentMethod = method
                        )

                        saveLocal(context, remoteInfo)
                        _subscriptionState.value = remoteInfo
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote subscription: ${e.localizedMessage}")
            }
        }
    }
}
