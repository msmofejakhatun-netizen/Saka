package com.example.util

import com.example.data.subscription.SubscriptionInfo

/**
 * Centralized Strict Subscription & Authentication Guard.
 * Enforces subscription validation across app start, foreground resume, and navigation routes.
 */
object AuthGuard {

    /**
     * Strict Subscription Verification Check:
     * - isProUserActive: True if subscribed to a paid tier (Monthly ₹79 / Annual ₹799) with non-expired timestamp & valid mandate.
     * - isTrialActive: True only if trial is active AND current time is strictly less than trial expiry time (3 days).
     * - isTrialExpired: When trial has ended, requires a completed, successful paid recurring cycle.
     */
    fun isSubscriptionValid(
        info: SubscriptionInfo,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        val isTrial = info.autoPayMandateStatus == "TRIAL_ACTIVE" || info.subscriptionTier == "TRIAL_1_INR"
        val trialExpiryTime = if (info.subscriptionExpiryDate > 0L) {
            info.subscriptionExpiryDate
        } else if (info.trialStartDate > 0L) {
            info.trialStartDate + (3 * 24 * 60 * 60 * 1000L)
        } else {
            0L
        }

        val isProUserActive = info.isProUser &&
                (info.subscriptionTier == "MONTHLY_79_INR" || info.subscriptionTier == "ANNUAL_799_INR") &&
                (info.subscriptionExpiryDate == 0L || currentTime < info.subscriptionExpiryDate) &&
                info.autoPayMandateStatus != "FAILED" &&
                info.autoPayMandateStatus != "EXPIRED" &&
                info.autoPayMandateStatus != "CANCELLED"

        val isTrialActive = isTrial &&
                info.isProUser &&
                info.autoPayMandateStatus != "FAILED" &&
                info.autoPayMandateStatus != "EXPIRED" &&
                info.autoPayMandateStatus != "CANCELLED"

        val isTrialExpired = isTrial && (trialExpiryTime > 0L && currentTime >= trialExpiryTime)

        val hasCompletedSuccessfulPayment = info.isProUser &&
                (info.subscriptionTier in listOf("MONTHLY_79_INR", "ANNUAL_799_INR")) &&
                (info.subscriptionExpiryDate == 0L || currentTime < info.subscriptionExpiryDate) &&
                info.autoPayMandateStatus == "ACTIVE"

        return when {
            isProUserActive -> true
            isTrialActive && (currentTime < trialExpiryTime) -> true
            isTrialExpired && hasCompletedSuccessfulPayment -> true
            else -> false
        }
    }
}
