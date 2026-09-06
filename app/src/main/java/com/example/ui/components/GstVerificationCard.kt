package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GstVerificationCard(
    isGstRegistered: Boolean,
    onGstRegisteredChange: (Boolean) -> Unit,
    gstin: String,
    onGstinChange: (String) -> Unit,
    isGstVerified: Boolean,
    legalBusinessName: String,
    isVerifying: Boolean,
    verificationError: String?,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGstVerified) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isGstVerified) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile_gst_verification_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (isGstRegistered) Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "GST Icon",
                            tint = if (isGstRegistered) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GST Registered Business?",
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable to issue compliant GST tax invoices",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isGstRegistered,
                    onCheckedChange = onGstRegisteredChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF16A34A),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.testTag("profile_gst_registered_switch")
                )
            }

            // Expandable GSTIN Input & Verification Section
            AnimatedVisibility(
                visible = isGstRegistered,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 14.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE2E8F0)
                    )

                    // GSTIN Input Field
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = {
                            if (it.length <= 15) {
                                onGstinChange(it.uppercase())
                            }
                        },
                        label = {
                            Text(
                                "GSTIN / GST Number",
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        placeholder = {
                            Text("e.g. 27ABCDE1234F1Z5", color = Color(0xFF94A3B8))
                        },
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = "GSTIN Pin",
                                tint = if (isGstVerified) Color(0xFF16A34A) else Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            if (isGstVerified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Icon",
                                    tint = Color(0xFF16A34A)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isGstVerified) Color(0xFF16A34A) else Color(0xFF2563EB),
                            unfocusedBorderColor = if (isGstVerified) Color(0xFFA7F3D0) else Color(0xFFCBD5E1),
                            focusedLabelColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF334155),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color(0xFF16A34A)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_gstin_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verify Button
                    Button(
                        onClick = onVerifyClick,
                        enabled = !isVerifying && gstin.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGstVerified) Color(0xFF16A34A) else Color(0xFF1E293B),
                            disabledContainerColor = Color(0xFFE2E8F0),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("profile_verify_gstin_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VERIFYING WITH GST PORTAL...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (isGstVerified) Icons.Default.Verified else Icons.Default.CheckCircle,
                                contentDescription = "Verify",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGstVerified) "GSTIN VERIFIED ✓" else "VERIFY GSTIN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Success Badge with Registered Trade Name / Legal Business Name
                    AnimatedVisibility(
                        visible = isGstVerified,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("profile_gst_verified_badge")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Badge",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (legalBusinessName.isNotBlank()) "Verified: $legalBusinessName" else "Verified Business Record",
                                        color = Color(0xFF065F46),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Registered Trade Name under GST Portal",
                                        color = Color(0xFF047857),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = verificationError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("profile_gst_error_badge")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = verificationError ?: "Invalid GSTIN or Business Record Not Found",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
