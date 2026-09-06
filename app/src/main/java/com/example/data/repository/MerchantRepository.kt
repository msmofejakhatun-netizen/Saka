package com.example.data.repository

import android.util.Log
import com.example.data.db.UserDao
import com.example.data.db.UserEntity
import com.example.data.firebase.FirebaseManager
import com.example.util.GstVerificationResult
import com.example.util.GstVerificationService
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for managing Merchant Business Profile, GST Verification, and Tax Preferences.
 */
class MerchantRepository(
    private val userDao: UserDao? = null
) {
    companion object {
        private const val TAG = "MerchantRepository"
    }

    private val firestore get() = FirebaseManager.firestore
    private val auth get() = FirebaseManager.auth

    /**
     * Retrieves the merchant profile from Firestore or local Room cache.
     */
    suspend fun getMerchantProfile(userId: String? = null): UserProfileData? = withContext(Dispatchers.IO) {
        val targetUid = userId ?: auth?.currentUser?.uid
        if (targetUid.isNullOrEmpty()) return@withContext null

        try {
            if (FirebaseManager.isFirebaseAvailable && firestore != null) {
                val doc = firestore!!.collection("users").document(targetUid).get().await()
                if (doc != null && doc.exists()) {
                    val fullName = doc.getString("fullName")
                        ?: doc.getString("displayName")
                        ?: doc.getString("name")
                        ?: ""
                    val businessName = doc.getString("businessName")
                        ?: doc.getString("shopName")
                        ?: ""
                    val businessCategory = doc.getString("businessCategory")
                        ?: doc.getString("category")
                        ?: doc.getString("selectedCategory")
                        ?: ""
                    val upiId = doc.getString("upiId")
                        ?: doc.getString("merchantUpi")
                        ?: doc.getString("vpa")
                        ?: "merchant@upi"
                    val merchantName = doc.getString("merchantName") ?: businessName
                    val mobile = doc.getString("mobileNumber")
                        ?: doc.getString("phoneNumber")
                        ?: doc.getString("mobile")
                        ?: ""
                    val email = doc.getString("email") ?: ""
                    val gstin = doc.getString("gstin").orEmpty()
                    val isGstVerified = doc.getBoolean("isGstVerified") ?: false
                    val legalBusinessName = doc.getString("legalBusinessName").orEmpty()
                    val isGstRegistered = doc.getBoolean("isGstRegistered")
                        ?: doc.getBoolean("gstRegistered")
                        ?: (gstin.isNotBlank() && isGstVerified)

                    return@withContext UserProfileData(
                        uid = targetUid,
                        fullName = fullName,
                        businessName = businessName,
                        businessCategory = businessCategory,
                        upiId = upiId.ifBlank { "merchant@upi" },
                        merchantName = merchantName,
                        mobileNumber = mobile,
                        email = email,
                        role = doc.getString("role") ?: "user",
                        gstin = gstin,
                        isGstVerified = isGstVerified,
                        legalBusinessName = legalBusinessName,
                        isGstRegistered = isGstRegistered
                    )
                }
            }

            // Fallback to local Room database
            if (userDao != null) {
                val localUser = userDao.getUserById(targetUid.hashCode())
                    ?: userDao.getAllUsers().firstOrNull()
                if (localUser != null) {
                    return@withContext UserProfileData(
                        uid = targetUid,
                        fullName = localUser.fullName,
                        businessName = localUser.businessName,
                        businessCategory = localUser.category,
                        upiId = localUser.upiId.ifBlank { "merchant@upi" },
                        merchantName = localUser.merchantName,
                        mobileNumber = localUser.mobileNumber,
                        gstin = localUser.gstin,
                        isGstVerified = localUser.isGstVerified,
                        legalBusinessName = localUser.legalBusinessName,
                        isGstRegistered = localUser.isGstRegistered
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching merchant profile: ${e.localizedMessage}")
        }
        null
    }

    /**
     * Verifies GSTIN against the syntax regex and verification endpoint.
     */
    suspend fun verifyGst(gstin: String, businessName: String = ""): Result<GstVerificationResult> {
        val result = GstVerificationService.verifyGst(gstin, businessName)
        return if (result.isValid) {
            Result.success(result)
        } else {
            Result.failure(Exception(result.errorMessage ?: "Invalid GSTIN or Business Record Not Found"))
        }
    }

    /**
     * Stores GST details directly into Firestore under users/{uid} and updates local database.
     */
    suspend fun saveGstDetails(
        userId: String? = null,
        gstin: String,
        isGstVerified: Boolean,
        legalBusinessName: String,
        isGstRegistered: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetUid = userId ?: auth?.currentUser?.uid ?: ""
        if (targetUid.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Merchant is not authenticated"))
        }

        val gstData = hashMapOf<String, Any>(
            "gstin" to gstin.trim().uppercase(),
            "isGstVerified" to isGstVerified,
            "legalBusinessName" to legalBusinessName,
            "isGstRegistered" to isGstRegistered,
            "gstRegistered" to isGstRegistered,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            if (FirebaseManager.isFirebaseAvailable && firestore != null) {
                firestore!!.collection("users")
                    .document(targetUid)
                    .set(gstData, SetOptions.merge())
                    .await()
            }

            // Update local Room database
            if (userDao != null) {
                val existing = userDao.getUserById(targetUid.hashCode())
                if (existing != null) {
                    val updated = existing.copy(
                        gstin = gstin.trim().uppercase(),
                        isGstVerified = isGstVerified,
                        legalBusinessName = legalBusinessName,
                        isGstRegistered = isGstRegistered
                    )
                    userDao.updateUser(updated)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving GST details to Firestore: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Updates full merchant profile including GST attributes.
     */
    suspend fun saveMerchantProfile(
        userId: String? = null,
        fullName: String,
        businessName: String,
        businessCategory: String,
        upiId: String,
        merchantName: String = businessName,
        mobileNumber: String = "",
        gstin: String = "",
        isGstVerified: Boolean = false,
        legalBusinessName: String = "",
        isGstRegistered: Boolean = false
    ): Result<UserProfileData> = withContext(Dispatchers.IO) {
        val targetUid = userId ?: auth?.currentUser?.uid ?: ""
        if (targetUid.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Merchant is not authenticated"))
        }

        val profileData = hashMapOf<String, Any>(
            "uid" to targetUid,
            "fullName" to fullName,
            "displayName" to fullName,
            "businessName" to businessName,
            "businessCategory" to businessCategory,
            "category" to businessCategory,
            "upiId" to upiId.ifBlank { "merchant@upi" },
            "merchantName" to merchantName.ifBlank { businessName },
            "gstin" to gstin.trim().uppercase(),
            "isGstVerified" to isGstVerified,
            "legalBusinessName" to legalBusinessName,
            "isGstRegistered" to isGstRegistered,
            "gstRegistered" to isGstRegistered,
            "updatedAt" to System.currentTimeMillis()
        )
        if (mobileNumber.isNotBlank()) {
            profileData["mobileNumber"] = mobileNumber
            profileData["phoneNumber"] = mobileNumber
        }

        try {
            if (FirebaseManager.isFirebaseAvailable && firestore != null) {
                firestore!!.collection("users")
                    .document(targetUid)
                    .set(profileData, SetOptions.merge())
                    .await()
            }

            if (userDao != null) {
                val localUser = UserEntity(
                    id = targetUid.hashCode(),
                    fullName = fullName,
                    businessName = businessName,
                    mobileNumber = mobileNumber,
                    passwordHash = "",
                    category = businessCategory,
                    upiId = upiId.ifBlank { "merchant@upi" },
                    merchantName = merchantName.ifBlank { businessName },
                    gstin = gstin.trim().uppercase(),
                    isGstVerified = isGstVerified,
                    legalBusinessName = legalBusinessName,
                    isGstRegistered = isGstRegistered
                )
                userDao.insertUser(localUser)
            }

            val saved = UserProfileData(
                uid = targetUid,
                fullName = fullName,
                businessName = businessName,
                businessCategory = businessCategory,
                upiId = upiId.ifBlank { "merchant@upi" },
                merchantName = merchantName.ifBlank { businessName },
                mobileNumber = mobileNumber,
                gstin = gstin.trim().uppercase(),
                isGstVerified = isGstVerified,
                legalBusinessName = legalBusinessName,
                isGstRegistered = isGstRegistered
            )
            Result.success(saved)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving merchant profile: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}
