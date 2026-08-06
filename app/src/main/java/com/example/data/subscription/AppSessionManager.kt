package com.example.data.subscription

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SessionAccessState {
    object Granted : SessionAccessState()
    data class Locked(val reason: String) : SessionAccessState()
}

object AppSessionManager {
    private const val TAG = "AppSessionManager"

    private val _accessState = MutableStateFlow<SessionAccessState>(SessionAccessState.Granted)
    val accessState: StateFlow<SessionAccessState> = _accessState.asStateFlow()

    /**
     * Checks subscriptionStatus and subscriptionExpiryDate against current time.
     * On launch / foreground resume:
     * Active States: TRIAL_ACTIVE (within 3 days of ₹1 setup) OR PRO_ACTIVE (recurring ₹79 paid). -> Grant full access to Dashboard.
     * Expired / Cancelled States: If Autopay is cancelled, revoked, or fails after 3 days, immediately block app access
     * and set lock reason to "Subscription Expired. Upgrade to Pro to continue using SmartPOS".
     */
    fun verifyAndEnforceSubscriptionLock(context: Context, userUid: String = ""): SessionAccessState {
        SubscriptionManager.init(context, userUid)
        val info = SubscriptionManager.subscriptionState.value
        val now = System.currentTimeMillis()

        val isPro = info.isProUser
        val expiry = info.subscriptionExpiryDate
        val mandateStatus = info.autoPayMandateStatus

        // Check active trial or pro plan
        val isExpired = expiry > 0L && now >= expiry
        val isMandateRevokedOrFailed = mandateStatus in listOf("CANCELLED", "REVOKED", "FAILED", "HALTED")

        // Active state: isPro is true, not expired, and mandate not failed/cancelled if expiry passed
        val isAccessGranted = isPro && !isExpired && !(isMandateRevokedOrFailed && isExpired)

        val result = if (isAccessGranted) {
            SessionAccessState.Granted
        } else {
            val message = if (isExpired || !isPro) {
                "Subscription Expired. Upgrade to Pro to continue using SmartPOS"
            } else {
                "Mandate Authorization Required. Upgrade to Pro to continue using SmartPOS"
            }
            SessionAccessState.Locked(reason = message)
        }

        _accessState.value = result
        Log.d(TAG, "Subscription lock check result: $result (isPro=$isPro, expiry=$expiry, now=$now, mandate=$mandateStatus)")
        return result
    }

    fun isAccessGranted(context: Context, userUid: String = ""): Boolean {
        return verifyAndEnforceSubscriptionLock(context, userUid) is SessionAccessState.Granted
    }

    /**
     * Clears all session data and shared preferences upon logout.
     */
    fun clearSession(context: Context) {
        try {
            val smartPosPrefs = context.getSharedPreferences("smart_pos_prefs", Context.MODE_PRIVATE)
            smartPosPrefs.edit().clear().apply()

            val subPrefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
            subPrefs.edit().clear().apply()

            SubscriptionManager.clearLocalSubscriptionState(context)
            _accessState.value = SessionAccessState.Granted
            Log.d(TAG, "AppSessionManager session and preferences cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session in AppSessionManager: ${e.localizedMessage}")
        }
    }
}
