package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
     * Initiates real Phone Number OTP verification via Firebase PhoneAuthProvider.
     */
    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResendingToken != null) {
            optionsBuilder.setForceResendingToken(forceResendingToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    /**
     * Verifies the 6-digit OTP code using PhoneAuthProvider credential.
     */
    suspend fun verifyPhoneOtp(
        verificationId: String,
        userEnteredOtp: String
    ): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, userEnteredOtp)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                syncUserProfileAndSession(user, provider = "phone")
            }
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "Phone OTP verification error: ${e.localizedMessage}")
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
     * Signs out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
}
