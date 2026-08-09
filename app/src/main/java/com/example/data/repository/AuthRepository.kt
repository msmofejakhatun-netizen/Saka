package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.data.auth.Fast2SMSHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * Gets the currently authenticated Firebase user.
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Initiates SMS OTP sending via Fast2SMS API.
     */
    suspend fun sendFast2SmsOtp(
        context: Context,
        phoneNumber: String
    ): Result<String> {
        return Fast2SMSHelper.sendOtp(context, phoneNumber)
    }

    /**
     * Verifies the 6-digit Fast2SMS OTP code and manages Firestore user profile & subscription session.
     */
    suspend fun verifyFast2SmsOtp(
        context: Context,
        phoneNumber: String,
        userEnteredOtp: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val verifyResult = Fast2SMSHelper.verifyOtp(context, phoneNumber, userEnteredOtp)
        if (verifyResult.isFailure) {
            return@withContext Result.failure(verifyResult.exceptionOrNull() ?: Exception("OTP Verification Failed."))
        }

        try {
            val cleanNumber = Fast2SMSHelper.sanitizePhoneNumber(phoneNumber)
            val formattedPhone = "+91$cleanNumber"

            // Query Firestore `users` collection by phone number
            val usersQuery = firestore.collection("users")
                .whereEqualTo("phoneNumber", formattedPhone)
                .get()
                .await()

            val userId: String
            if (!usersQuery.isEmpty) {
                // User exists
                val docSnap = usersQuery.documents.first()
                userId = docSnap.id
                val updateData = hashMapOf<String, Any>(
                    "lastLoginAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(userId)
                    .set(updateData, SetOptions.merge()).await()
                Log.d(TAG, "Existing user logged in with phone: $userId")
            } else {
                // New user - create profile at users/{phoneUserId}
                userId = "phone_$cleanNumber"
                val profileData = hashMapOf(
                    "uid" to userId,
                    "phoneNumber" to formattedPhone,
                    "displayName" to "User $cleanNumber",
                    "createdAt" to System.currentTimeMillis(),
                    "lastLoginAt" to System.currentTimeMillis(),
                    "authProvider" to "phone",
                    "role" to "user"
                )
                firestore.collection("users").document(userId)
                    .set(profileData, SetOptions.merge()).await()
                Log.d(TAG, "Created new user profile at users/$userId")
            }

            // Sync FCM Token
            com.example.service.MyFirebaseMessagingService.syncFcmTokenToFirestore(userId)

            // Setup default subscription state under users/{phoneUserId}/subscription/current
            val subRef = firestore.collection("users")
                .document(userId)
                .collection("subscription")
                .document("current")

            val subSnap = subRef.get().await()
            if (!subSnap.exists()) {
                val initialSubData = hashMapOf(
                    "status" to "TRIAL_ACTIVE",
                    "plan" to "trial",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "isTrial" to true,
                    "isProUser" to true
                )
                subRef.set(initialSubData, SetOptions.merge()).await()
                Log.d(TAG, "Initialized default subscription state under users/$userId/subscription/current")
            }

            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Fast2SMS post-verification session creation error: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Firebase using a Google ID Token.
     */

    suspend fun signInWithGoogle(idToken: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                syncUserProfileAndSession(user, provider = "google")
            }
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Gets a configured GoogleSignInClient for legacy fallback sign-in.
     */
    fun getLegacyGoogleSignInClient(context: Context, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Authenticates with Firebase using a legacy GoogleSignInAccount.
     */
    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): Result<AuthResult> {
        val idToken = account.idToken
        if (idToken.isNullOrEmpty()) {
            return Result.failure(Exception("Google ID Token is null or empty from GoogleSignInAccount."))
        }
        return signInWithGoogle(idToken)
    }

    /**
     * Syncs user details to Firestore under `users/{userId}` and initializes subscription session.
     */
    suspend fun syncUserProfileAndSession(
        user: FirebaseUser,
        provider: String
    ) = withContext(Dispatchers.IO) {
        try {
            val userRef = firestore.collection("users").document(user.uid)
            val docSnap = userRef.get().await()

            if (!docSnap.exists()) {
                val profileData = hashMapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "phoneNumber" to (user.phoneNumber ?: ""),
                    "displayName" to (user.displayName ?: ""),
                    "createdAt" to System.currentTimeMillis(),
                    "authProvider" to provider,
                    "role" to "user"
                )
                userRef.set(profileData, SetOptions.merge()).await()
                Log.d(TAG, "Created new user profile document for ${user.uid}")
            } else {
                val updateData = hashMapOf<String, Any>(
                    "lastLoginAt" to System.currentTimeMillis()
                )
                if (!user.email.isNullOrEmpty()) updateData["email"] = user.email!!
                if (!user.phoneNumber.isNullOrEmpty()) updateData["phoneNumber"] = user.phoneNumber!!
                if (!user.displayName.isNullOrEmpty()) updateData["displayName"] = user.displayName!!
                userRef.set(updateData, SetOptions.merge()).await()
            }

            // Sync FCM Token to Firestore under users/{userId} as fcmToken
            com.example.service.MyFirebaseMessagingService.syncFcmTokenToFirestore(user.uid)

            // Check / Initialize users/{userId}/subscription/current path for subscription gating
            val subRef = firestore.collection("users")
                .document(user.uid)
                .collection("subscription")
                .document("current")
            
            val subSnap = subRef.get().await()
            if (!subSnap.exists()) {
                val initialSubData = hashMapOf(
                    "status" to "active",
                    "plan" to "trial",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "isTrial" to true
                )
                subRef.set(initialSubData, SetOptions.merge()).await()
                Log.d(TAG, "Initialized subscription session for user ${user.uid}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile and subscription in Firestore: ${e.localizedMessage}")
        }
    }

    /**
     * Signs out the current user, clears offline Firestore persistence cache,
     * purges local Room DB data, and resets session state.
     */
    suspend fun signOut(
        context: Context? = null,
        billingRepository: BillingRepository? = null
    ) = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            Log.d(TAG, "FirebaseAuth signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth signOut error: ${e.localizedMessage}")
        }

        try {
            firestore.clearPersistence().await()
            Log.d(TAG, "Firestore persistence cache cleared successfully")
        } catch (e: Exception) {
            Log.d(TAG, "Firestore clearPersistence skipped or already closed: ${e.localizedMessage}")
        }

        try {
            billingRepository?.clearLocalCache()
        } catch (e: Exception) {
            Log.e(TAG, "Clear local database cache error: ${e.localizedMessage}")
        }

        if (context != null) {
            try {
                com.example.data.subscription.AppSessionManager.clearSession(context)
            } catch (e: Exception) {
                Log.e(TAG, "Clear session preferences error: ${e.localizedMessage}")
            }
        }
    }
}
