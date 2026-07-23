package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val businessName: String,
    val mobileNumber: String,
    val passwordHash: String,
    val category: String,
    val upiId: String = "merchant@upi",
    val merchantName: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val iconName: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String = "",
    val customerName: String,
    val customerMobile: String = "",
    val amount: Double,
    val itemsCount: Int,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val paymentMode: String = "Cash", // Cash, UPI / QR, Online, Credit (Udhar)
    val itemsSummary: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Paid" // Paid, Unpaid, Pending
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String = "",
    val name: String,
    val salePrice: Double,
    val purchasePrice: Double = 0.0,
    val stockQuantity: Double,
    val unit: String = "Pcs", // Pcs, Kg, Ltr, Box, Meter
    val category: String = "General",
    val barcode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String = "",
    val name: String,
    val mobileNumber: String,
    val totalPendingBalance: Double = 0.0,
    val lastTransactionTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_transactions")
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String = "",
    val customerMobile: String,
    val customerName: String = "",
    val type: String, // DEBIT (Udhar) or CREDIT (Jama)
    val amount: Double,
    val paymentMode: String = "Cash", // Cash, UPI, Online
    val note: String = "",
    val invoiceId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
