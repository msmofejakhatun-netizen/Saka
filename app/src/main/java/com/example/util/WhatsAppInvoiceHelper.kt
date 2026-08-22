package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.db.InvoiceEntity
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helper for formatting invoice receipts and auto-dispatching WhatsApp messages to customers.
 */
object WhatsAppInvoiceHelper {

    private const val TAG = "WhatsAppInvoiceHelper"
    private const val PREFS_NAME = "smartpos_settings_prefs"
    private const val KEY_AUTO_SEND_WHATSAPP = "key_auto_send_whatsapp_invoice"

    /**
     * Checks if auto-send WhatsApp bill is enabled by merchant.
     * Defaults to true for Kirana & retail stores.
     */
    fun isAutoSendEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SEND_WHATSAPP, true)
    }

    /**
     * Updates the merchant's preference for auto-sending WhatsApp bills on checkout.
     */
    fun setAutoSendEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SEND_WHATSAPP, enabled).apply()
    }

    /**
     * Formats the standard invoice receipt message for WhatsApp.
     */
    fun formatInvoiceText(invoice: InvoiceEntity, businessName: String): String {
        val invoiceNumber = if (invoice.firestoreId.isNotBlank()) {
            invoice.firestoreId.take(8).uppercase()
        } else {
            "INV-${invoice.id}"
        }

        val dateStr = if (invoice.timestamp > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
        } else {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        }

        val storeName = if (businessName.isNotBlank()) businessName.trim() else "SmartPOS Retail Store"
        val formattedAmount = String.format(Locale.US, "%.2f", invoice.amount)
        val mode = if (invoice.paymentMode.isNotBlank()) invoice.paymentMode else "Cash"

        return """
        🧾 *INVOICE: $invoiceNumber*
        *Store:* $storeName
        *Date:* $dateStr
        ----------------------------
        *Total Amount:* ₹$formattedAmount
        *Payment Mode:* $mode

        Thank you for shopping with us!
        """.trimIndent()
    }

    /**
     * Launches WhatsApp with pre-filled phone number and invoice text.
     * Returns true if intent was launched successfully.
     */
    fun sendWhatsAppInvoice(
        context: Context,
        customerPhone: String,
        invoice: InvoiceEntity,
        businessName: String
    ): Boolean {
        val cleanPhone = customerPhone.replace("\\D".toRegex(), "").takeLast(10)
        if (cleanPhone.length < 10) {
            Log.w(TAG, "Customer phone number invalid or less than 10 digits: $customerPhone")
            return false
        }

        val invoiceText = formatInvoiceText(invoice, businessName)

        return try {
            val encodedText = URLEncoder.encode(invoiceText, "UTF-8")
            val fullPhoneWithCountryCode = "91$cleanPhone"

            // 1. Primary Attempt: Standard WhatsApp URL
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$fullPhoneWithCountryCode&text=$encodedText")
            val primaryIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(primaryIntent)
                Log.d(TAG, "Opened WhatsApp directly for customer: $fullPhoneWithCountryCode")
                true
            } catch (e1: Exception) {
                // 2. Secondary Attempt: WhatsApp Business
                try {
                    val waBusinessIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(waBusinessIntent)
                    Log.d(TAG, "Opened WhatsApp Business for customer: $fullPhoneWithCountryCode")
                    true
                } catch (e2: Exception) {
                    // 3. Fallback: Generic Browser or App Picker
                    try {
                        val genericIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(genericIntent)
                        true
                    } catch (e3: Exception) {
                        // 4. Fallback: Standard Chooser
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, invoiceText)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share Invoice Bill").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(chooser)
                        true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WhatsApp invoice: ${e.localizedMessage}")
            Toast.makeText(context, "Unable to launch WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
