package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Item data model for Central WhatsApp invoice dispatch payload.
 */
@JsonClass(generateAdapter = true)
data class ItemPayload(
    @Json(name = "name") val name: String,
    @Json(name = "quantity") val quantity: Double,
    @Json(name = "price") val price: Double,
    @Json(name = "unit") val unit: String = "Pcs",
    @Json(name = "total") val total: Double = quantity * price
)

/**
 * Request payload for central server-side WhatsApp invoice generation and dispatch.
 */
@JsonClass(generateAdapter = true)
data class InvoiceRequestPayload(
    @Json(name = "customerPhone") val customerPhone: String,
    @Json(name = "storeName") val storeName: String,
    @Json(name = "invoiceNumber") val invoiceNumber: String,
    @Json(name = "totalAmount") val totalAmount: String,
    @Json(name = "date") val date: String,
    @Json(name = "items") val items: List<ItemPayload> = emptyList(),
    @Json(name = "paymentMode") val paymentMode: String = "Cash",
    @Json(name = "customerName") val customerName: String = "",
    @Json(name = "subtotal") val subtotal: Double = 0.0,
    @Json(name = "discountAmount") val discountAmount: Double = 0.0,
    @Json(name = "taxAmount") val taxAmount: Double = 0.0
)

/**
 * Server response model for WhatsApp dispatch endpoint.
 */
@JsonClass(generateAdapter = true)
data class ApiResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "messageId") val messageId: String? = null,
    @Json(name = "status") val status: String? = null
)

/**
 * Retrofit API interface for automated central WhatsApp invoice messaging.
 */
interface WhatsAppApiService {

    @POST("api/send-central-invoice")
    suspend fun sendInvoice(
        @Body invoiceData: InvoiceRequestPayload
    ): Response<ApiResponse>

    @POST("api/send-central-invoice")
    suspend fun sendCentralInvoice(
        @Body request: InvoiceRequestPayload
    ): Response<ApiResponse> = sendInvoice(request)

    companion object {
        fun getInstance(): WhatsAppApiService = RetrofitClient.whatsAppApiService
    }
}
