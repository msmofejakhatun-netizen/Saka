package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
     * Initiates Firebase Phone Number verification via official PhoneAuthProvider.
     */
    fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        val cleanDigits = phoneNumber.replace("\\D".toRegex(), "")
        val formattedPhone = when {
            phoneNumber.startsWith("+") -> phoneNumber
            cleanDigits.length == 10 -> "+91$cleanDigits"
            else -> "+$cleanDigits"
        }

        Log.d(TAG, "Starting Firebase Phone Auth verification for: $formattedPhone")

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    /**
     * Signs in with a PhoneAuthCredential generated automatically or via verification code.
     */
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                syncUserProfileAndSession(user, provider = "phone")
            }
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithPhoneCredential error: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Verifies the 6-digit OTP code using verification ID and PhoneAuthProvider credential.
     */
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<AuthResult> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
            signInWithPhoneCredential(credential)
        } catch (e: Exception) {
            Log.e(TAG, "verifyOtp error: ${e.localizedMessage}")
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
                    "displayName" to (user.displayName ?: "User ${user.uid.take(6)}"),
                    "createdAt" to System.currentTimeMillis(),
                    "lastLoginAt" to System.currentTimeMillis(),
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
                    "status" to "TRIAL_ACTIVE",
                    "plan" to "trial",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "isTrial" to true,
                    "isProUser" to true
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
            Log.e(TAG, "Firestore clearPersistence skipped or already closed: ${e.localizedMessage}")
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
