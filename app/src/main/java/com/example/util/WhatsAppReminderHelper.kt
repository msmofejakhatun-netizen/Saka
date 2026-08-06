package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object WhatsAppReminderHelper {

    /**
     * Builds a standard deep-linked UPI Payment URL.
     * Example: upi://pay?pa=merchant@upi&pn=Kirana%20Store&am=150.00&cu=INR&tn=Udhar%20Payment
     */
    fun buildUpiPaymentUrl(
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String = "Udhar Payment Clearance"
    ): String {
        val cleanUpi = if (upiId.isNotBlank()) upiId.trim() else "merchant@upi"
        val cleanName = Uri.encode(if (merchantName.isNotBlank()) merchantName.trim() else "Kirana & Retail Store")
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val cleanNote = Uri.encode(if (note.isNotBlank()) note.trim() else "Udhar Payment Clearance")

        return "upi://pay?pa=$cleanUpi&pn=$cleanName&am=$formattedAmount&cu=INR&tn=$cleanNote"
    }

    /**
     * Appends an interactive UPI deep link into WhatsApp reminder text.
     */
    fun appendInteractiveUpiPaymentLink(
        originalMessage: String,
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String = "Udhar Payment"
    ): String {
        val upiUrl = buildUpiPaymentUrl(upiId, merchantName, amount, note)
        val cleanUpi = if (upiId.isNotBlank()) upiId.trim() else "merchant@upi"

        val paymentBlock = "\n\n" +
            "📲 CLICK TO PAY INSTANTLY VIA UPI / GPAY / PHONEPE:\n" +
            "$upiUrl\n\n" +
            "• Merchant UPI ID: $cleanUpi\n" +
            "(Tap the link above directly in WhatsApp to settle your balance using GPay, PhonePe, or Paytm!)"

        return originalMessage.trim() + paymentBlock
    }

    /**
     * Directly launches the native device UPI app picker for a given amount.
     */
    fun launchUpiPaymentIntent(
        context: Context,
        upiId: String,
        merchantName: String,
        amount: Double,
        note: String = "Udhar Clearance"
    ) {
        val upiUrl = buildUpiPaymentUrl(upiId, merchantName, amount, note)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl))
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI Payment App found on device", Toast.LENGTH_SHORT).show()
        }
    }
}
