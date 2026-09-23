package com.example.ui.screens.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.POSCartItem
import com.example.util.KiranaUnitUtils
import java.util.Locale

/**
 * Cart Review and Discounts Screen (Step 2/3 of Billing).
 * Features verified GST gating: locked GST Invoice Mode switch if GSTIN is unverified,
 * and displays verified GSTIN + state tax breakup (CGST / SGST intra-state or IGST inter-state) when verified.
 */
@Composable
fun CartScreen(
    viewModel: BillingViewModel,
    onProceedToPayment: () -> Unit,
    onChangeCustomerClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onEditQuantity: (POSCartItem) -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Surface(
                color = VyaparSurface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("cart_screen_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VyaparTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Step 2/3: Review Cart & Discounts",
                            color = VyaparTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${viewModel.posCartItems.size} items in cart",
                            color = VyaparTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        CartScreenContent(
            viewModel = viewModel,
            onProceedToPayment = onProceedToPayment,
            onChangeCustomerClick = onChangeCustomerClick,
            onDismiss = onNavigateBack,
            onEditQuantity = onEditQuantity,
            onNavigateToProfile = onNavigateToProfile,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Reusable Cart Content for both full-screen and modal sheet/dialog experiences.
 */
@Composable
fun CartScreenContent(
    viewModel: BillingViewModel,
    onProceedToPayment: () -> Unit,
    onChangeCustomerClick: () -> Unit,
    onDismiss: () -> Unit,
    onEditQuantity: (POSCartItem) -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isGstVerified = currentUser?.isGstVerified == true || (viewModel.isGstVerified && viewModel.profileGstin.isNotBlank())
    val merchantGstin = currentUser?.gstin?.ifBlank { viewModel.profileGstin } ?: ""
    val legalName = currentUser?.legalBusinessName?.ifBlank { viewModel.profileLegalBusinessName } ?: ""
    var showGstGateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = VyaparRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Step 2/3: Review Cart & Discounts",
                        color = VyaparTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${viewModel.posCartItems.size} products added to bill",
                        color = VyaparTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cart_review_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = VyaparTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Customer Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VyaparBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = VyaparRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = viewModel.posCustomerName.ifBlank { "Walk-in Customer" },
                                    color = VyaparTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (viewModel.posCustomerMobile.isNotBlank()) {
                                    Text(viewModel.posCustomerMobile, color = VyaparTextSecondary, fontSize = 11.sp)
                                }
                                if (viewModel.posDoctorName.isNotBlank() || viewModel.posPatientInfo.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            viewModel.posDoctorName.takeIf { it.isNotBlank() }?.let { "Dr. $it" },
                                            viewModel.posPatientInfo.takeIf { it.isNotBlank() }?.let { "Patient: $it" }
                                        ).joinToString(" · "),
                                        color = VyaparTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = onChangeCustomerClick,
                            modifier = Modifier.testTag("cart_review_change_customer_btn")
                        ) {
                            Text("Change", color = VyaparDeepBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Itemized Product List Section
            item {
                Text(
                    text = "ITEMIZED CART",
                    color = VyaparTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }

            items(viewModel.posCartItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = VyaparSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VyaparBorder, RoundedCornerShape(10.dp))
                        .testTag("cart_review_item_${item.product.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    color = VyaparTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@ ₹${String.format(Locale.US, "%.2f", item.customPrice)} / ${item.product.unit}",
                                    color = VyaparTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", item.totalAmount)}",
                                    color = VyaparTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.removeFromPOSCart(item.product) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quantity adjustment controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Qty: ${KiranaUnitUtils.formatQuantityWithUnit(item.quantity, item.product.unit, item.product)}",
                                color = VyaparTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val step = if (KiranaUnitUtils.isLooseUnit(item.product.unit, item.product)) 0.1 else 1.0

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            if (item.quantity > step) {
                                                viewModel.updatePOSCartQuantity(item.product, item.quantity - step)
                                            } else {
                                                viewModel.removeFromPOSCart(item.product)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = VyaparTextPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { onEditQuantity(item) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Exact Qty", tint = VyaparDeepBlue, modifier = Modifier.size(13.dp))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            viewModel.addToPOSCart(item.product, step)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = VyaparRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Gated GST Invoice Mode Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isGstInvoiceMode && isGstVerified) Color(0xFFF0FDF4) else VyaparSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (viewModel.isGstInvoiceMode && isGstVerified) Color(0xFFA7F3D0) else VyaparBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("pos_gst_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (viewModel.isGstInvoiceMode && isGstVerified) Color(0xFF16A34A) else VyaparDeepBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (viewModel.isGstInvoiceMode && isGstVerified) "GST Invoice Mode" else "Simple Estimate (Non-GST)",
                                        color = VyaparTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (!isGstVerified) {
                                        Text(
                                            text = "GSTIN Verification Required",
                                            color = Color(0xFFDC2626),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = "Compliant Tax Invoice with CGST/SGST/IGST",
                                            color = Color(0xFF047857),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Interactive or Clickable Disabled Switch
                            Box(
                                modifier = Modifier
                                    .testTag("pos_gst_toggle_container")
                                    .clickable(!isGstVerified) {
                                        showGstGateDialog = true
                                    }
                            ) {
                                Switch(
                                    checked = viewModel.isGstInvoiceMode && isGstVerified,
                                    onCheckedChange = { checked ->
                                        if (isGstVerified) {
                                            viewModel.isGstInvoiceMode = checked
                                            if (!checked) viewModel.posTaxPercentageInput = "0"
                                        } else {
                                            showGstGateDialog = true
                                        }
                                    },
                                    enabled = isGstVerified,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF16A34A),
                                        disabledCheckedTrackColor = Color(0xFFE2E8F0),
                                        disabledUncheckedTrackColor = Color(0xFFE2E8F0),
                                        disabledUncheckedThumbColor = Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier.testTag("pos_gst_toggle_switch")
                                )
                            }
                        }

                        // Unverified Warning Banner
                        if (!isGstVerified) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGstGateDialog = true }
                                    .testTag("pos_gst_unverified_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GSTIN not verified. Tap to verify in Profile to unlock GST Mode.",
                                        color = Color(0xFF991B1B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "VERIFY",
                                        color = Color(0xFFDC2626),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Verified State Details: Verified GSTIN Badge & State Tax Breakup
                        if (viewModel.isGstInvoiceMode && isGstVerified) {
                            // Verified GSTIN Display Badge
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pos_gst_verified_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Verified GSTIN: ${merchantGstin.ifBlank { "Registered Taxpayer" }}",
                                            color = Color(0xFF065F46),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (legalName.isNotBlank()) {
                                            Text(
                                                text = legalName,
                                                color = Color(0xFF047857),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // GST Rate Selectors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "GST Rate:",
                                    color = VyaparTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(60.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    listOf("0", "5", "12", "18").forEach { taxRate ->
                                        val isSelected = viewModel.posTaxPercentageInput == taxRate
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) Color(0xFF16A34A) else Color(0xFFF1F5F9))
                                                .clickable { viewModel.posTaxPercentageInput = taxRate }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "$taxRate%",
                                                color = if (isSelected) Color.White else VyaparTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "+₹${String.format(Locale.US, "%.2f", viewModel.posTaxAmount)}",
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // State Tax Breakup (Intra-state CGST+SGST vs Inter-state IGST)
                            if (viewModel.posTaxAmount > 0) {
                                val rateNum = viewModel.posTaxPercentageInput.toDoubleOrNull() ?: 0.0
                                val halfRate = rateNum / 2.0
                                val halfTax = viewModel.posTaxAmount / 2.0

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "STATE TAX BREAKUP (INTRA-STATE)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "CGST (${String.format(Locale.US, "%.1f", halfRate)}%):",
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", halfTax)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "SGST (${String.format(Locale.US, "%.1f", halfRate)}%):",
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", halfTax)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "⚡ Simple Estimate Mode active — Tax lines & GSTIN omitted on receipt.",
                                color = VyaparTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Order & Financial Breakdown Summary
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VyaparBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = VyaparTextSecondary, fontSize = 12.sp)
                            Text("₹${String.format(Locale.US, "%.2f", viewModel.posSubtotal)}", color = VyaparTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (viewModel.posDiscountAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount", color = Color(0xFF16A34A), fontSize = 12.sp)
                                Text("-₹${String.format(Locale.US, "%.2f", viewModel.posDiscountAmount)}", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (viewModel.isGstInvoiceMode && isGstVerified && viewModel.posTaxAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GST / Tax (${viewModel.posTaxPercentageInput}%)", color = Color(0xFF16A34A), fontSize = 12.sp)
                                Text("+₹${String.format(Locale.US, "%.2f", viewModel.posTaxAmount)}", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = VyaparBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Final Total", color = VyaparTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("₹${String.format(Locale.US, "%.2f", viewModel.posFinalTotal)}", color = VyaparRed, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Proceed to Payment Action Button
        Button(
            onClick = onProceedToPayment,
            colors = ButtonDefaults.buttonColors(containerColor = VyaparRed),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("cart_review_proceed_btn")
        ) {
            Text("Proceed to Payment →", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Helper Dialog when user attempts to enable GST Invoice Mode without verified GSTIN
    if (showGstGateDialog) {
        AlertDialog(
            onDismissRequest = { showGstGateDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEF2F2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "GST Verification Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "GST Invoice Mode requires a verified GSTIN. Please update your Business Profile first.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGstGateDialog = false
                        onDismiss()
                        onNavigateToProfile?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VyaparRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_go_to_profile_button")
                ) {
                    Text("Go to Profile", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGstGateDialog = false },
                    modifier = Modifier.testTag("dialog_dismiss_gst_gate")
                ) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("gst_verification_required_dialog")
        )
    }
}
