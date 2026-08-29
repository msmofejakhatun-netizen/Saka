package com.example.data.subscription

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

object SubscriptionRepository {
    private const val TAG = "SubscriptionRepository"

    suspend fun saveSubscription(
        userId: String,
        subscription: SubscriptionModel
    ): Boolean {
        if (userId.isBlank() || !FirebaseManager.isFirebaseAvailable) return false
        val firestore = FirebaseManager.firestore ?: return false

        return try {
            val startDateTimestamp = Timestamp(Date(subscription.startDate))
            val expiryDateTimestamp = Timestamp(Date(subscription.expiryDate))

            val subscriptionData = hashMapOf<String, Any>(
                "planType" to subscription.planType,
                "planName" to subscription.planName,
                "status" to subscription.status,
                "isProUser" to subscription.isProUser,
                "hasUsedTrial" to subscription.hasUsedTrial,
                "amountPaid" to subscription.amountPaid,
                "startDate" to startDateTimestamp,
                "expiryDate" to expiryDateTimestamp,
                "startTimestamp" to subscription.startDate,
                "expiryTimestamp" to subscription.expiryDate,
                "subscriptionExpiryDate" to subscription.expiryDate,
                "subscriptionTier" to if (subscription.planType == "ANNUAL") "ANNUAL_799_INR" else if (subscription.planType == "MONTHLY") "MONTHLY_79_INR" else "TRIAL_1_INR",
                "mandateId" to subscription.mandateId,
                "gatewayProvider" to subscription.gatewayProvider,
                "paymentMethod" to subscription.paymentMethod,
                "lastUpdated" to System.currentTimeMillis()
            )

            // Write to users/{userId}/subscription/current
            firestore.collection("users").document(userId)
                .collection("subscription").document("current")
                .set(subscriptionData, SetOptions.merge())
                .await()

            // Update top-level user document
            val userData = hashMapOf<String, Any>(
                "isProUser" to subscription.isProUser,
                "subscriptionTier" to if (subscription.planType == "ANNUAL") "ANNUAL_799_INR" else if (subscription.planType == "MONTHLY") "MONTHLY_79_INR" else "TRIAL_1_INR",
                "planType" to subscription.planType,
                "planName" to subscription.planName,
                "subscriptionStatus" to subscription.status,
                "hasUsedTrial" to true,
                "expiryTimestamp" to subscription.expiryDate,
                "updatedAt" to System.currentTimeMillis()
            )
            if (subscription.mandateId.isNotBlank()) {
                userData["mandateId"] = subscription.mandateId
            }

            firestore.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced subscription (${subscription.planType}) to Firestore for $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving subscription to Firestore: ${e.localizedMessage}")
            false
        }
    }

    suspend fun fetchSubscription(userId: String): SubscriptionModel? {
        if (userId.isBlank() || !FirebaseManager.isFirebaseAvailable) return null
        val firestore = FirebaseManager.firestore ?: return null

        return try {
            val doc = firestore.collection("users").document(userId)
                .collection("subscription").document("current").get().await()
            val userDoc = firestore.collection("users").document(userId).get().await()

            if (!doc.exists() && !userDoc.exists()) return null

            val isPro = doc.getBoolean("isProUser") ?: userDoc.getBoolean("isProUser") ?: false
            val tier = doc.getString("planType") ?: doc.getString("subscriptionTier") ?: userDoc.getString("planType") ?: userDoc.getString("subscriptionTier") ?: "FREE"
            val planName = doc.getString("planName") ?: userDoc.getString("planName") ?: when (tier) {
                "MONTHLY", "MONTHLY_79_INR" -> "Monthly Pro Plan (₹79/mo)"
                "ANNUAL", "ANNUAL_799_INR" -> "Annual Pro Plan (₹799/yr)"
                "TRIAL", "TRIAL_1_INR" -> "Free Trial (3 Days)"
                else -> "Free Plan"
            }
            val status = doc.getString("status") ?: doc.getString("autoPayMandateStatus") ?: userDoc.getString("subscriptionStatus") ?: "FREE"
            val amountPaid = doc.getDouble("amountPaid") ?: 0.0

            val startMillis = extractTimestampMillis(doc.get("startDate"))
                ?: doc.getLong("startTimestamp")
                ?: doc.getLong("trialStartDate")
                ?: 0L

            val expiryMillis = extractTimestampMillis(doc.get("expiryDate"))
                ?: doc.getLong("expiryTimestamp")
                ?: doc.getLong("subscriptionExpiryDate")
                ?: doc.getLong("trialExpiryDate")
                ?: 0L

            val hasUsedTrial = doc.getBoolean("hasUsedTrial")
                ?: userDoc.getBoolean("hasUsedTrial")
                ?: (startMillis > 0L || tier.contains("TRIAL") || tier.contains("MONTHLY") || tier.contains("ANNUAL"))

            val mandateId = doc.getString("mandateId") ?: doc.getString("autoPayMandateId") ?: userDoc.getString("mandateId") ?: ""
            val gatewayProvider = doc.getString("gatewayProvider") ?: "RAZORPAY"
            val paymentMethod = doc.getString("paymentMethod") ?: ""

            // Standardize planType
            val standardizedPlanType = when {
                tier.contains("MONTHLY", ignoreCase = true) -> "MONTHLY"
                tier.contains("ANNUAL", ignoreCase = true) -> "ANNUAL"
                tier.contains("TRIAL", ignoreCase = true) -> "TRIAL"
                else -> "FREE"
            }

            SubscriptionModel(
                planType = standardizedPlanType,
                planName = planName,
                status = status,
                isProUser = isPro,
                amountPaid = amountPaid,
                startDate = startMillis,
                expiryDate = expiryMillis,
                hasUsedTrial = hasUsedTrial,
                mandateId = mandateId,
                gatewayProvider = gatewayProvider,
                paymentMethod = paymentMethod
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subscription from Firestore: ${e.localizedMessage}")
            null
        }
    }

    private fun extractTimestampMillis(field: Any?): Long? {
        return when (field) {
            is Timestamp -> field.toDate().time
            is Date -> field.time
            is Long -> field
            is Number -> field.toLong()
            else -> null
        }
    }
}
