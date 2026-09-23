package com.example

import com.example.util.GstVerificationService
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GstVerificationTest {

    @Test
    fun testParseLgnmAndTradeNam() {
        val jsonString = """{"lgnm": "TRIGEM HOSPITALITY", "tradeNam": ""}"""
        val json = JSONObject(jsonString)
        val extractedName = GstVerificationService.extractNameFromJson(json)
        assertEquals("TRIGEM HOSPITALITY", extractedName)
    }

    @Test
    fun testParseTradeNamPriorityOverLgnm() {
        val jsonString = """{"lgnm": "TRIGEM HOSPITALITY PRIVATE LIMITED", "tradeNam": "TRIGEM CAFE"}"""
        val json = JSONObject(jsonString)
        val extractedName = GstVerificationService.extractNameFromJson(json)
        assertEquals("TRIGEM CAFE", extractedName)
    }

    @Test
    fun testTrigemHospitalityGstinVerification() = runBlocking {
        val result = GstVerificationService.verifyGst(
            rawGstin = "27AARFT7394K1Z4",
            merchantBusinessName = "Ali kirana"
        )
        assertTrue(result.isValid)
        assertEquals("TRIGEM HOSPITALITY", result.legalBusinessName)
        assertEquals("TRIGEM HOSPITALITY", result.tradeName)
        assertFalse(result.legalBusinessName.contains("Ali kirana"))
        assertFalse(result.legalBusinessName.startsWith("Verified:"))
    }

    @Test
    fun testInvalidGstin() = runBlocking {
        val result = GstVerificationService.verifyGst(rawGstin = "INVALID_GSTIN")
        assertFalse(result.isValid)
        assertEquals("Invalid GSTIN or Business Record Not Found", result.errorMessage)
    }
}
