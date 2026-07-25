package com.example.util

import com.example.data.db.ProductEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class ExpiryStatus {
    object Expired : ExpiryStatus()
    data class NearExpiry(val daysRemaining: Int) : ExpiryStatus()
    object Valid : ExpiryStatus()
    object NotSpecified : ExpiryStatus()
}

object PharmacyUtils {

    /**
     * Parses expiry date strings like "11/2026", "11/26", "2026-11", "11-2026", "15/11/2026".
     * Returns timestamp of end of month or date, or null if invalid.
     */
    fun parseExpiryDate(expiryStr: String): Long? {
        if (expiryStr.isBlank()) return null
        val clean = expiryStr.trim()
        val formats = listOf("MM/yyyy", "MM/yy", "yyyy-MM", "MM-yyyy", "dd/MM/yyyy")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    // If standard month/year, set to end of month
                    if (!format.startsWith("dd")) {
                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun getExpiryStatus(expiryStr: String, nearExpiryDaysThreshold: Int = 90): ExpiryStatus {
        val expiryTime = parseExpiryDate(expiryStr) ?: return ExpiryStatus.NotSpecified
        val now = System.currentTimeMillis()
        if (expiryTime < now) {
            return ExpiryStatus.Expired
        }
        val diffMillis = expiryTime - now
        val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return if (daysRemaining <= nearExpiryDaysThreshold) {
            ExpiryStatus.NearExpiry(daysRemaining)
        } else {
            ExpiryStatus.Valid
        }
    }

    fun isPharmacyProduct(product: ProductEntity): Boolean {
        val cat = product.category.lowercase(Locale.ROOT)
        return cat.contains("pharmacy") || cat.contains("medical") ||
                product.batchNumber.isNotBlank() || product.saltComposition.isNotBlank()
    }

    fun matchesPharmacySearch(product: ProductEntity, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase(Locale.ROOT)
        return product.name.lowercase(Locale.ROOT).contains(q) ||
                product.saltComposition.lowercase(Locale.ROOT).contains(q) ||
                product.batchNumber.lowercase(Locale.ROOT).contains(q) ||
                product.manufacturer.lowercase(Locale.ROOT).contains(q) ||
                product.barcode.lowercase(Locale.ROOT).contains(q) ||
                product.category.lowercase(Locale.ROOT).contains(q)
    }
}
