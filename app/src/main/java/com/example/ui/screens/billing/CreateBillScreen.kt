package com.example.ui.screens.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.InvoiceEntity
import com.example.data.db.ProductEntity
import com.example.ui.components.LooseQuantityDialog
import com.example.ui.theme.AccentPink
import com.example.ui.theme.DarkGray
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldYellow
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.POSCartItem
import com.example.util.KiranaUnitUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBillScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showSuccessReceiptDialog by remember { mutableStateOf(false) }
    var generatedInvoiceForReceipt by remember { mutableStateOf<InvoiceEntity?>(null) }

    var selectedProductForLooseQty by remember { mutableStateOf<ProductEntity?>(null) }
    var editingCartItemQuantity by remember { mutableStateOf<POSCartItem?>(null) }

    val categoriesList = remember(products) {
        val set = products.map { it.category }.filter { it.isNotBlank() }.toSet()
        listOf("All") + set.toList()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        products.filter { prod ->
            val matchesCategory = selectedCategoryFilter == "All" || prod.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "POS Billing & Invoice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pos_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (viewModel.posCartItems.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearPOSCart() },
                            modifier = Modifier.testTag("pos_clear_cart_button")
                        ) {
                            Text("Clear Cart", color = AccentPink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0x99090D22)
                ),
                modifier = Modifier.testTag("pos_top_bar")
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0F28),
                            DarkGray,
                            Color(0xFF090C1E)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // 1. Customer Details Section
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .testTag("pos_customer_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Customer",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Customer Information",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2210B981))
                                        .clickable { viewModel.posCustomerName = "Walk-in Customer" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Cash Sale", color = EmeraldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = viewModel.posCustomerName,
                                    onValueChange = { viewModel.posCustomerName = it },
                                    label = { Text("Customer Name", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("pos_customer_name_input")
                                )

                                OutlinedTextField(
                                    value = viewModel.posCustomerMobile,
                                    onValueChange = { viewModel.posCustomerMobile = it },
                                    label = { Text("Mobile (Opt)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pos_customer_mobile_input")
                                )
                            }
                        }
                    }
                }

                // 2. Product Picker & Inventory Search Engine
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x151E295D)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Products",
                                    tint = GoldYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Select Products from Inventory",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${filteredProducts.size} items available",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            // Search Field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search product name or category...", color = Color(0xFF64748B), fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldYellow, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldYellow,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pos_product_search_input")
                            )

                            // Category Filter Chips
                            if (categoriesList.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(categoriesList) { cat ->
                                        val isSelected = selectedCategoryFilter == cat
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedCategoryFilter = cat },
                                            label = { Text(cat, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldYellow,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Color(0x22FFFFFF),
                                                labelColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                    }
                                }
                            }

                            // Product Cards List
                            if (filteredProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No products found matching '$searchQuery'", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredProducts.take(15).forEach { product ->
                                        ProductPOSRow(
                                            product = product,
                                            onAddToCart = {
                                                if (KiranaUnitUtils.isLooseUnit(product.unit)) {
                                                    selectedProductForLooseQty = product
                                                } else {
                                                    viewModel.addToPOSCart(product)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Itemized Bill / Cart Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x2210B981), RoundedCornerShape(16.dp))
                            .testTag("pos_cart_section")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Cart",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Itemized Bill (${viewModel.posCartItems.size} items)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = "Subtotal: ₹${String.format(Locale.US, "%.2f", viewModel.posSubtotal)}",
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            if (viewModel.posCartItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color(0x44FFFFFF),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Cart is currently empty", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                        Text("Tap products above to add items to bill", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    viewModel.posCartItems.forEach { cartItem ->
                                        CartItemRow(
                                            cartItem = cartItem,
                                            onEditQuantity = {
                                                editingCartItemQuantity = cartItem
                                            },
                                            onIncrease = {
                                                val step = if (KiranaUnitUtils.isLooseUnit(cartItem.product.unit)) 0.25 else 1.0
                                                viewModel.updatePOSCartQuantity(cartItem.product, cartItem.quantity + step)
                                            },
                                            onDecrease = {
                                                val step = if (KiranaUnitUtils.isLooseUnit(cartItem.product.unit)) 0.25 else 1.0
                                                viewModel.updatePOSCartQuantity(cartItem.product, cartItem.quantity - step)
                                            },
                                            onRemove = {
                                                viewModel.removeFromPOSCart(cartItem.product)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Summary Calculation Breakdown (Discount, Tax, Payment Mode, Final Total)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .testTag("pos_summary_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Invoice Summary & Payment",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            // Discount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Discount, contentDescription = "Discount", tint = ElectricVioletLight, modifier = Modifier.size(18.dp))
                                Text("Discount", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.width(64.dp))

                                // Toggle Fixed ($) or Percentage (%)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x22FFFFFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(if (viewModel.posDiscountType == "Fixed") ElectricViolet else Color.Transparent)
                                            .clickable { viewModel.posDiscountType = "Fixed" }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("₹", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(if (viewModel.posDiscountType == "Percentage") ElectricViolet else Color.Transparent)
                                            .clickable { viewModel.posDiscountType = "Percentage" }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = viewModel.posDiscountInput,
                                    onValueChange = { viewModel.posDiscountInput = it },
                                    placeholder = { Text("0", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricVioletLight,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pos_discount_input")
                                )

                                Text(
                                    text = "-₹${String.format(Locale.US, "%.2f", viewModel.posDiscountAmount)}",
                                    color = AccentPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Tax / GST Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Tax", tint = GoldYellow, modifier = Modifier.size(18.dp))
                                Text("Tax / GST", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.width(64.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    listOf("0", "5", "12", "18").forEach { taxRate ->
                                        val isSelected = viewModel.posTaxPercentageInput == taxRate
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) GoldYellow else Color(0x22FFFFFF))
                                                .clickable { viewModel.posTaxPercentageInput = taxRate }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "$taxRate%",
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "+₹${String.format(Locale.US, "%.2f", viewModel.posTaxAmount)}",
                                    color = GoldYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Payment Mode Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Select Payment Mode", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Cash", "UPI / QR", "Online", "Credit (Udhar)").forEach { mode ->
                                        val isSelected = viewModel.posPaymentMode == mode
                                        Card(
                                            onClick = { viewModel.posPaymentMode = mode },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) EmeraldGreen else Color(0x11FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) EmeraldLight else Color(0x22FFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .testTag("pos_payment_mode_${mode.lowercase().replace(" ", "_").replace("/", "")}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = mode,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Divider
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))

                            // Total Breakdown Table
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SummaryLineItem("Subtotal", "₹${String.format(Locale.US, "%.2f", viewModel.posSubtotal)}")
                                if (viewModel.posDiscountAmount > 0) {
                                    SummaryLineItem("Discount Deducted", "-₹${String.format(Locale.US, "%.2f", viewModel.posDiscountAmount)}", isNegative = true)
                                }
                                if (viewModel.posTaxAmount > 0) {
                                    SummaryLineItem("Tax (${viewModel.posTaxPercentageInput}%)", "+₹${String.format(Locale.US, "%.2f", viewModel.posTaxAmount)}")
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Grand Total Amount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", viewModel.posFinalTotal)}",
                                        color = EmeraldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.testTag("pos_final_total_value")
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Banner
                item {
                    AnimatedVisibility(
                        visible = viewModel.posInvoiceError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        viewModel.posInvoiceError?.let { err ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0x55EF4444), RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = err,
                                    color = Color(0xFFF87171),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Bar: Generate Bill Action
            Surface(
                color = Color(0xFF0D122B),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL BILL", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", viewModel.posFinalTotal)}",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.generatePOSInvoice { generatedInvoice ->
                                generatedInvoiceForReceipt = generatedInvoice
                                showSuccessReceiptDialog = true
                            }
                        },
                        enabled = viewModel.posCartItems.isNotEmpty() && !viewModel.isGeneratingPOSInvoice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            disabledContainerColor = Color(0x3310B981)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("pos_generate_bill_button")
                    ) {
                        if (viewModel.isGeneratingPOSInvoice) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...")
                        } else {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERATE BILL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Success Digital Receipt Dialog Modal
    if (showSuccessReceiptDialog && generatedInvoiceForReceipt != null) {
        val invoice = generatedInvoiceForReceipt!!
        val formattedDate = remember(invoice.timestamp) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
        }

        AlertDialog(
            onDismissRequest = {
                showSuccessReceiptDialog = false
                onNavigateBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Invoice Generated!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Receipt #${invoice.firestoreId.take(8).ifBlank { invoice.id }}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Details
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(currentUser?.businessName ?: "Billing Store", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Customer: ${invoice.customerName} ${if (invoice.customerMobile.isNotBlank()) "(${invoice.customerMobile})" else ""}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("Date: $formattedDate", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("Payment Mode: ${invoice.paymentMode}", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Purchased Items Summary
                    Text("Purchased Items Summary:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = invoice.itemsSummary.ifBlank { "${invoice.itemsCount} items billed" },
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color(0x11FFFFFF), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .fillMaxWidth()
                    )

                    // Financial Summary Breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SummaryLineItem("Subtotal", "₹${String.format(Locale.US, "%.2f", invoice.subtotal)}")
                        if (invoice.discountAmount > 0) {
                            SummaryLineItem("Discount", "-₹${String.format(Locale.US, "%.2f", invoice.discountAmount)}", isNegative = true)
                        }
                        if (invoice.taxAmount > 0) {
                            SummaryLineItem("Tax", "+₹${String.format(Locale.US, "%.2f", invoice.taxAmount)}")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Paid Amount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", invoice.amount)}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PDF Action Engine (Print, WhatsApp Share, Share PDF)
                    val localContext = androidx.compose.ui.platform.LocalContext.current
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val pdf = com.example.util.InvoicePdfHelper.generateInvoicePdf(
                                    context = localContext,
                                    invoice = invoice,
                                    businessName = currentUser?.businessName,
                                    merchantMobile = currentUser?.mobileNumber
                                )
                                com.example.util.InvoicePdfHelper.printInvoicePdf(localContext, pdf)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("receipt_print_pdf_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Print Invoice PDF", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val pdf = com.example.util.InvoicePdfHelper.generateInvoicePdf(
                                    context = localContext,
                                    invoice = invoice,
                                    businessName = currentUser?.businessName,
                                    merchantMobile = currentUser?.mobileNumber
                                )
                                com.example.util.InvoicePdfHelper.shareInvoicePdfWhatsApp(
                                    context = localContext,
                                    pdfFile = pdf,
                                    invoice = invoice,
                                    businessName = currentUser?.businessName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("receipt_whatsapp_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share via WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessReceiptDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("receipt_start_new_sale_button")
                ) {
                    Text("Start New Sale", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessReceiptDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Dashboard", color = ElectricVioletLight)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color(0x3310B981), RoundedCornerShape(20.dp))
        )
    }

    // Modal Loose Quantity Dialog when adding a loose item (+ Add clicked on Kg, Gm, Ltr, Ml)
    selectedProductForLooseQty?.let { product ->
        LooseQuantityDialog(
            product = product,
            initialQuantity = 1.0,
            onDismiss = { selectedProductForLooseQty = null },
            onConfirm = { qty ->
                viewModel.addToPOSCart(product, qty)
                selectedProductForLooseQty = null
            }
        )
    }

    // Modal Loose Quantity Dialog when editing quantity of an existing item in cart
    editingCartItemQuantity?.let { cartItem ->
        LooseQuantityDialog(
            product = cartItem.product,
            initialQuantity = cartItem.quantity,
            onDismiss = { editingCartItemQuantity = null },
            onConfirm = { qty ->
                viewModel.updatePOSCartQuantity(cartItem.product, qty)
                editingCartItemQuantity = null
            }
        )
    }
}

@Composable
private fun ProductPOSRow(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0x0C1E295D) else Color(0x1F1E295D)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isOutOfStock) Color(0x22EF4444) else Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .then(
                if (!isOutOfStock) {
                    Modifier.clickable { onAddToCart() }
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", product.salePrice)} / ${product.unit}",
                        color = GoldYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("  ·  ", color = Color(0xFF64748B), fontSize = 11.sp)
                    Text(
                        text = if (isOutOfStock) "Out of stock" else "Stock: ${KiranaUnitUtils.formatQuantityWithUnit(product.stockQuantity, product.unit)}",
                        color = if (isOutOfStock) Color(0xFFF87171) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onAddToCart,
                enabled = !isOutOfStock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldYellow,
                    disabledContainerColor = Color(0x22FFFFFF)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("pos_add_product_${product.name.lowercase().replace(" ", "_")}")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (isOutOfStock) Color.Gray else Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isOutOfStock) "Empty" else "Add",
                    color = if (isOutOfStock) Color.Gray else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: POSCartItem,
    onEditQuantity: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val maxStock = cartItem.product.stockQuantity
    val isLoose = KiranaUnitUtils.isLooseUnit(cartItem.product.unit)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x2A1E295D)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2210B981), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .clickable { onEditQuantity() }
            ) {
                Text(
                    text = cartItem.product.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", cartItem.customPrice)} / ${cartItem.product.unit}  =  ₹${String.format(Locale.US, "%.2f", cartItem.totalAmount)}",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quantity selector with Loose Qty formatting: - [ Qty ] +
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { onDecrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                Surface(
                    onClick = onEditQuantity,
                    color = Color(0x3310B981),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = KiranaUnitUtils.formatQuantityWithUnit(cartItem.quantity, cartItem.product.unit),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (cartItem.quantity < maxStock) EmeraldGreen else Color(0x22FFFFFF))
                        .clickable(enabled = cartItem.quantity < maxStock) { onIncrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = if (cartItem.quantity < maxStock) Color.White else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AccentPink, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryLineItem(label: String, value: String, isNegative: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(
            value,
            color = if (isNegative) AccentPink else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}
