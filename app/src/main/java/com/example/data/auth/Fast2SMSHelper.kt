package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object Fast2SMSHelper {
    private const val TAG = "Fast2SMSHelper"
    private const val FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2"
    private const val OTP_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes expiry

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class OtpSession(
        val phoneNumber: String,
        val otpCode: String,
        val expiryTimestamp: Long
    )

    // Thread-safe in-memory cache for OTP sessions indexed by 10-digit mobile number
    private val otpCache = ConcurrentHashMap<String, OtpSession>()

    /**
     * Cleans input to a 10-digit mobile number string.
     */
    fun sanitizePhoneNumber(rawPhone: String): String {
        val cleanPhoneNumber = rawPhone.replace("\\D".toRegex(), "")
        return if (cleanPhoneNumber.length >= 10) {
            cleanPhoneNumber.takeLast(10)
        } else {
            cleanPhoneNumber
        }
    }

    /**
     * Generates a random 6-digit numeric OTP code.
     */
    private fun generateOtpCode(): String {
        val random = SecureRandom()
        val code = random.nextInt(900000) + 100000
        return code.toString()
    }

    /**
     * Sends a 6-digit SMS OTP via Fast2SMS Quick SMS API endpoint using POST.
     */
    suspend fun sendOtp(context: Context, rawPhoneNumber: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanNumber = sanitizePhoneNumber(rawPhoneNumber)
        if (cleanNumber.length != 10) {
            return@withContext Result.failure(Exception("Please enter a valid 10-digit mobile number."))
        }

        val otpCode = generateOtpCode()
        val expiryTime = System.currentTimeMillis() + OTP_EXPIRY_MS
        val session = OtpSession(cleanNumber, otpCode, expiryTime)

        // Store OTP session in-memory
        otpCache[cleanNumber] = session

        // Also persist in SharedPreferences as fallback
        try {
            val prefs = context.getSharedPreferences("fast2sms_otp_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("otp_$cleanNumber", otpCode)
                .putLong("expiry_$cleanNumber", expiryTime)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "SharedPreferences save error: ${e.localizedMessage}")
        }

        val apiKey = try {
            BuildConfig.FAST2SMS_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "YOUR_FAST2SMS_API_KEY") {
            Log.w(TAG, "Fast2SMS API Key is missing or placeholder. Generated OTP for dev: $otpCode")
            return@withContext Result.success("OTP sent successfully to +91 $cleanNumber")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("route", "otp")
                put("variables_values", otpCode)
                put("numbers", cleanNumber)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(FAST2SMS_URL)
                .addHeader("authorization", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "Fast2SMS API Response Code: ${response.code}, Body: $responseBody")

            if (response.isSuccessful) {
                var isSuccess = true
                var message = "SMS sent successfully."
                try {
                    val json = JSONObject(responseBody)
                    if (json.has("return")) {
                        isSuccess = json.optBoolean("return", true)
                    }
                    if (json.has("message")) {
                        val msgArr = json.optJSONArray("message")
                        if (msgArr != null && msgArr.length() > 0) {
                            message = msgArr.optString(0, message)
                        } else {
                            message = json.optString("message", message)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing Fast2SMS JSON: ${e.localizedMessage}")
                }

                if (isSuccess) {
                    Result.success("OTP sent successfully to +91 $cleanNumber")
                } else {
                    Log.e(TAG, "Fast2SMS returned error message: $message")
                    Result.failure(Exception("Fast2SMS Error: $message"))
                }
            } else {
                Log.e(TAG, "Fast2SMS HTTP Error ${response.code}: $responseBody")
                Result.failure(Exception("Failed to send SMS via Fast2SMS (HTTP ${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fast2SMS Network Exception: ${e.localizedMessage}")
            Result.failure(Exception("Network error while sending OTP: ${e.localizedMessage}"))
        }
    }

    /**
     * Validates the user-entered OTP against stored OTP session.
     */
    fun verifyOtp(context: Context, rawPhoneNumber: String, userEnteredOtp: String): Result<Boolean> {
        val cleanNumber = sanitizePhoneNumber(rawPhoneNumber)
        val trimmedOtp = userEnteredOtp.trim()

        if (trimmedOtp.length != 6) {
            return Result.failure(Exception("Please enter a valid 6-digit OTP."))
        }

        var session = otpCache[cleanNumber]

        // Fallback check in SharedPreferences if in-memory cache missed
        if (session == null) {
            try {
                val prefs = context.getSharedPreferences("fast2sms_otp_prefs", Context.MODE_PRIVATE)
                val savedOtp = prefs.getString("otp_$cleanNumber", null)
                val expiryTime = prefs.getLong("expiry_$cleanNumber", 0L)
                if (!savedOtp.isNullOrEmpty() && expiryTime > 0) {
                    session = OtpSession(cleanNumber, savedOtp, expiryTime)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SharedPreferences read error: ${e.localizedMessage}")
            }
        }

        if (session == null) {
            return Result.failure(Exception("No active OTP session found. Please request a new OTP."))
        }

        if (System.currentTimeMillis() > session.expiryTimestamp) {
            clearOtpSession(context, cleanNumber)
            return Result.failure(Exception("OTP has expired (5-minute limit). Please request a new OTP."))
        }

        if (session.otpCode == trimmedOtp) {
            clearOtpSession(context, cleanNumber)
            return Result.success(true)
        } else {
            return Result.failure(Exception("Invalid OTP code. Please check and try again."))
        }
    }

    /**
     * Clears expired or used OTP session.
     */
    fun clearOtpSession(context: Context, rawPhoneNumber: String) {
        val cleanNumber = sanitizePhoneNumber(rawPhoneNumber)
        otpCache.remove(cleanNumber)
        try {
            val prefs = context.getSharedPreferences("fast2sms_otp_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("otp_$cleanNumber")
                .remove("expiry_$cleanNumber")
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "SharedPreferences clear error: ${e.localizedMessage}")
        }
    }
}
