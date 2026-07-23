package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.CustomerTransactionEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReminderType(val label: String) {
    POLITE("Polite Reminder"),
    URGENT("Urgent Reminder"),
    STATEMENT("Send Statement Summary")
}

object WhatsAppReminderUtils {

    fun buildReminderMessage(
        customerName: String,
        businessName: String,
        pendingAmount: Double,
        lastTransactionTimestamp: Long,
        reminderType: ReminderType,
        transactions: List<CustomerTransactionEntity> = emptyList()
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val dateStr = if (lastTransactionTimestamp > 0) dateFormat.format(Date(lastTransactionTimestamp)) else "Recent"
        val formattedAmount = String.format(Locale.US, "%.2f", pendingAmount)
        val bizName = if (businessName.isNotBlank()) businessName else "Kirana Store"

        return when (reminderType) {
            ReminderType.POLITE -> {
                "Dear $customerName,\n" +
                "This is a polite reminder from $bizName regarding your pending udhar balance of ₹$formattedAmount.\n\n" +
                "Last Transaction Date: $dateStr\n\n" +
                "Kindly clear your balance at your earliest convenience. You can pay via Cash, UPI, or QR.\n" +
                "Thank you for your business!"
            }

            ReminderType.URGENT -> {
                "⚠️ URGENT PAYMENT REMINDER\n\n" +
                "Dear $customerName,\n" +
                "Your pending udhar balance at $bizName is ₹$formattedAmount, which is overdue.\n\n" +
                "Last Transaction Date: $dateStr\n\n" +
                "Please clear your balance as soon as possible via Cash, UPI, or QR code.\n" +
                "Thank you!"
            }

            ReminderType.STATEMENT -> {
                val sb = StringBuilder()
                sb.append("📄 CREDIT LEDGER STATEMENT\n\n")
                sb.append("Store: $bizName\n")
                sb.append("Customer: $customerName\n")
                sb.append("Total Pending Balance: ₹$formattedAmount\n")
                sb.append("Last Active: $dateStr\n\n")

                if (transactions.isNotEmpty()) {
                    sb.append("--- RECENT TRANSACTIONS ---\n")
                    transactions.take(5).forEach { tx ->
                        val txDate = dateFormat.format(Date(tx.timestamp))
                        val sign = if (tx.type == "CREDIT") "Jama (-)" else "Udhar (+)"
                        val amt = String.format(Locale.US, "%.2f", tx.amount)
                        sb.append("• $txDate: $sign ₹$amt (${tx.paymentMode})\n")
                    }
                    sb.append("\n")
                }

                sb.append("Payment Accepted: Cash, UPI, PhonePe, GooglePay\n")
                sb.append("Thank you for shopping with us!")
                sb.toString()
            }
        }
    }

    fun sendWhatsAppReminder(context: Context, mobile: String, message: String) {
        val cleanMobile = mobile.replace("+", "").replace(" ", "").replace("-", "")
        if (cleanMobile.isBlank()) {
            Toast.makeText(context, "Invalid customer mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        val fullMobile = if (cleanMobile.length == 10) "91$cleanMobile" else cleanMobile
        val encodedMessage = Uri.encode(message)

        try {
            // 1. Primary Attempt: Direct WhatsApp Intent
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$fullMobile&text=$encodedMessage")
            val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                setPackage("com.whatsapp")
            }
            context.startActivity(whatsappIntent)
        } catch (e1: Exception) {
            try {
                // 2. Secondary Attempt: WhatsApp Business
                val whatsappBusinessUri = Uri.parse("https://api.whatsapp.com/send?phone=$fullMobile&text=$encodedMessage")
                val waBusinessIntent = Intent(Intent.ACTION_VIEW, whatsappBusinessUri).apply {
                    setPackage("com.whatsapp.w4b")
                }
                context.startActivity(waBusinessIntent)
            } catch (e2: Exception) {
                try {
                    // 3. Fallback: Browser / wa.me link without locked package name
                    val genericWaUri = Uri.parse("https://wa.me/$fullMobile?text=$encodedMessage")
                    val genericIntent = Intent(Intent.ACTION_VIEW, genericWaUri)
                    context.startActivity(genericIntent)
                } catch (e3: Exception) {
                    try {
                        // 4. Fallback: Standard Share Sheet / SMS
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send Payment Reminder via"))
                    } catch (e4: Exception) {
                        Toast.makeText(context, "Unable to open messaging app", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun shareTextViaStandardChooser(context: Context, message: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Payment Reminder"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share text", Toast.LENGTH_SHORT).show()
        }
    }
}
