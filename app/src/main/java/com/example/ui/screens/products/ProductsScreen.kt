package com.example.ui.screens.products

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ProductEntity
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.PremiumGradientBackground
import com.example.ui.components.PremiumLoadingState
import androidx.compose.material.icons.filled.QrCodeScanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val activeCategories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Expiry Tracker, 2: Low Stock

    val expiredProducts = remember(filteredProducts) {
        filteredProducts.filter {
            com.example.util.PharmacyUtils.getExpiryStatus(it.expiryDate) is com.example.util.ExpiryStatus.Expired
        }
    }

    val nearExpiryProducts = remember(filteredProducts) {
        filteredProducts.filter {
            com.example.util.PharmacyUtils.getExpiryStatus(it.expiryDate) is com.example.util.ExpiryStatus.NearExpiry
        }
    }

    val lowStockProducts = remember(filteredProducts) {
        filteredProducts.filter { it.stockQuantity <= 5.0 }
    }

    val activeDisplayList = remember(selectedTab, filteredProducts, expiredProducts, nearExpiryProducts, lowStockProducts) {
        when (selectedTab) {
            1 -> (expiredProducts + nearExpiryProducts).distinctBy { it.id }
            2 -> lowStockProducts
            else -> filteredProducts
        }
    }

    // Form inputs state
    var itemNameInput by remember { mutableStateOf("") }
    var salePriceInput by remember { mutableStateOf("") }
    var purchasePriceInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Pcs") }
    var selectedCategory by remember { mutableStateOf("General") }

    // Pharmacy specific form inputs
    var batchNumberInput by remember { mutableStateOf("") }
    var expiryDateInput by remember { mutableStateOf("") }
    var manufacturerInput by remember { mutableStateOf("") }
    var saltCompositionInput by remember { mutableStateOf("") }
    var packConfigInput by remember { mutableStateOf("") }
    var isRxRequiredInput by remember { mutableStateOf(false) }

    var showScannerInDialog by remember { mutableStateOf(false) }

    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val units = remember { listOf("Pcs", "Strip", "Bottle", "Box", "Tablet", "Capsule", "Kg", "Gm", "Ltr", "Ml", "Pack") }

    fun openAddDialog() {
        selectedProductForEdit = null
        itemNameInput = ""
        salePriceInput = ""
        purchasePriceInput = ""
        stockInput = "10"
        barcodeInput = ""
        selectedUnit = "Strip"
        selectedCategory = activeCategories.firstOrNull { it.name.contains("Pharmacy", ignoreCase = true) }?.name ?: "Pharmacy / Medical"
        batchNumberInput = ""
        expiryDateInput = ""
        manufacturerInput = ""
        saltCompositionInput = ""
        packConfigInput = "1 Strip = 10 Tablets"
        isRxRequiredInput = false
        viewModel.productFormError = null
        showAddEditDialog = true
    }

    fun openEditDialog(product: ProductEntity) {
        selectedProductForEdit = product
        itemNameInput = product.name
        salePriceInput = product.salePrice.toString()
        purchasePriceInput = if (product.purchasePrice > 0) product.purchasePrice.toString() else ""
        stockInput = product.stockQuantity.toString()
        barcodeInput = product.barcode
        selectedUnit = product.unit
        selectedCategory = product.category
        batchNumberInput = product.batchNumber
        expiryDateInput = product.expiryDate
        manufacturerInput = product.manufacturer
        saltCompositionInput = product.saltComposition
        packConfigInput = product.packUnitConfig
        isRxRequiredInput = product.isRxRequired
        viewModel.productFormError = null
        showAddEditDialog = true
    }

    PremiumGradientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Inventory & Products",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.testTag("products_title")
                            )
                            Text(
                                text = "${filteredProducts.size} Items Available",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("products_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { openAddDialog() },
                            modifier = Modifier.testTag("products_add_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Product",
                                tint = EmeraldGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0x99090D22)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { openAddDialog() },
                    containerColor = EmeraldGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .testTag("products_add_fab")
                        .padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Product",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.productSearchQuery.value = it },
                    placeholder = { Text("Search products or categories...", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = EmeraldGreen
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.productSearchQuery.value = "" },
                                modifier = Modifier.testTag("products_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x0DFFFFFF),
                        unfocusedContainerColor = Color(0x05FFFFFF)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("products_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category & Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = EmeraldLight,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_all_products")
                    ) {
                        Surface(
                            color = if (selectedTab == 0) EmeraldGreen else Color(0x1F1E295D),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = "All Products (${filteredProducts.size})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_expiry_tracker")
                    ) {
                        Surface(
                            color = if (selectedTab == 1) Color(0xFFEF4444) else Color(0x1F1E295D),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚡ Expiry Tracker (${expiredProducts.size + nearExpiryProducts.size})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("tab_low_stock")
                    ) {
                        Surface(
                            color = if (selectedTab == 2) GoldYellow else Color(0x1F1E295D),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚠️ Low Stock (${lowStockProducts.size})",
                                color = if (selectedTab == 2) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Summary Stats Banner for Expiry Tracker Tab
                if (selectedTab == 1) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x22131B3E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x33EF4444), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Expired Items", color = Color(0xFFF87171), fontSize = 11.sp)
                                Text("${expiredProducts.size}", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0x22FFFFFF)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Near Expiry (60 Days)", color = Color(0xFFFBBF24), fontSize = 11.sp)
                                Text("${nearExpiryProducts.size}", color = Color(0xFFF59E0B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Product List
                if (activeDisplayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Empty Inventory",
                                tint = Color(0x44FFFFFF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedTab) {
                                    1 -> "Great! No expired or near-expiry medicines found."
                                    2 -> "All products are well stocked!"
                                    else -> if (searchQuery.isEmpty()) "No products in inventory yet" else "No matching products found"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Add Product' to populate your item list",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("products_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = activeDisplayList,
                            key = { it.firestoreId.ifEmpty { it.id.toString() } }
                        ) { product ->
                            ProductItemCard(
                                product = product,
                                onEdit = { openEditDialog(product) },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }
        }

        // --- Add / Edit Product Dialog ---
        if (showAddEditDialog) {
            AlertDialog(
                onDismissRequest = { showAddEditDialog = false },
                title = {
                    Text(
                        text = if (selectedProductForEdit == null) "Add New Product" else "Edit Product",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("product_dialog_title")
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Error message
                        AnimatedVisibility(
                            visible = viewModel.productFormError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            viewModel.productFormError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("product_form_error")
                                )
                            }
                        }

                        // Product Name Field
                        OutlinedTextField(
                            value = itemNameInput,
                            onValueChange = { itemNameInput = it },
                            label = { Text("Item / Product Name *", color = Color(0xFF94A3B8)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_name_input")
                        )

                        // Sale & Purchase Price
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = salePriceInput,
                                onValueChange = { salePriceInput = it },
                                label = { Text("Sale Price ($) *", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_sale_price_input")
                            )

                            OutlinedTextField(
                                value = purchasePriceInput,
                                onValueChange = { purchasePriceInput = it },
                                label = { Text("Purchase Price ($)", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_purchase_price_input")
                            )
                        }

                        // Stock Quantity & Unit Dropdown
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = stockInput,
                                onValueChange = { stockInput = it },
                                label = { Text("Stock Quantity *", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_stock_input")
                            )

                            // Unit Dropdown Box
                            ExposedDropdownMenuBox(
                                expanded = unitDropdownExpanded,
                                onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unit", color = Color(0xFF94A3B8)) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = EmeraldLight,
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("product_unit_select")
                                )

                                ExposedDropdownMenu(
                                    expanded = unitDropdownExpanded,
                                    onDismissRequest = { unitDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF0F172A))
                                ) {
                                    units.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit, color = Color.White) },
                                            onClick = {
                                                selectedUnit = unit
                                                unitDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Category Dropdown Box
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category", color = Color(0xFF94A3B8)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Category Dropdown",
                                        tint = EmeraldLight,
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("product_category_select")
                            )

                            ExposedDropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                if (activeCategories.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("General", color = Color.White) },
                                        onClick = {
                                            selectedCategory = "General"
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                } else {
                                    activeCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name, color = Color.White) },
                                            onClick = {
                                                selectedCategory = cat.name
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Barcode Field with Scan Button
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            label = { Text("Barcode / SKU (Optional)", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. 8901234567890") },
                            leadingIcon = {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showScannerInDialog = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = EmeraldGreen)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_barcode_input")
                        )

                        // --- Pharmacy / Medical Specific Fields ---
                        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Pharmacy & Medicine Details",
                            color = EmeraldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Batch Number
                            OutlinedTextField(
                                value = batchNumberInput,
                                onValueChange = { batchNumberInput = it },
                                label = { Text("Batch No.", color = Color(0xFF94A3B8)) },
                                placeholder = { Text("e.g. BATCH-1049") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_batch_input")
                            )

                            // Expiry Date (MM/YYYY)
                            OutlinedTextField(
                                value = expiryDateInput,
                                onValueChange = { expiryDateInput = it },
                                label = { Text("Expiry (MM/YYYY)", color = Color(0xFF94A3B8)) },
                                placeholder = { Text("e.g. 11/2027") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = Color(0x22FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("product_expiry_input")
                            )
                        }

                        // Manufacturer & Salt Composition
                        OutlinedTextField(
                            value = manufacturerInput,
                            onValueChange = { manufacturerInput = it },
                            label = { Text("Manufacturer / Brand", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. Micro Labs / Sun Pharma") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_manufacturer_input")
                        )

                        OutlinedTextField(
                            value = saltCompositionInput,
                            onValueChange = { saltCompositionInput = it },
                            label = { Text("Salt / Composition Name", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. Paracetamol 650mg") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_salt_input")
                        )

                        OutlinedTextField(
                            value = packConfigInput,
                            onValueChange = { packConfigInput = it },
                            label = { Text("Pack Config", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("e.g. 1 Strip = 10 Tablets") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x22FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_pack_input")
                        )

                        // Rx Required Checkbox Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRxRequiredInput = !isRxRequiredInput }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isRxRequiredInput,
                                onCheckedChange = { isRxRequiredInput = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldGreen,
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rx Prescription Required for Sale",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val saleP = salePriceInput.toDoubleOrNull() ?: -1.0
                            val purP = purchasePriceInput.toDoubleOrNull() ?: 0.0
                            val stk = stockInput.toDoubleOrNull() ?: -1.0

                            viewModel.saveProduct(
                                id = selectedProductForEdit?.id ?: 0,
                                firestoreId = selectedProductForEdit?.firestoreId ?: "",
                                name = itemNameInput,
                                salePrice = saleP,
                                purchasePrice = purP,
                                stockQuantity = stk,
                                unit = selectedUnit,
                                category = selectedCategory,
                                barcode = barcodeInput,
                                batchNumber = batchNumberInput,
                                expiryDate = expiryDateInput,
                                manufacturer = manufacturerInput,
                                saltComposition = saltCompositionInput,
                                packUnitConfig = packConfigInput,
                                isRxRequired = isRxRequiredInput,
                                onSuccess = {
                                    showAddEditDialog = false
                                }
                            )
                        },
                        enabled = !viewModel.isSavingProduct,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("product_dialog_save")
                    ) {
                        if (viewModel.isSavingProduct) {
                            PremiumLoadingState(text = "Saving...")
                        } else {
                            Text(
                                text = if (selectedProductForEdit == null) "Add Product" else "Update",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddEditDialog = false },
                        modifier = Modifier.testTag("product_dialog_cancel")
                    ) {
                        Text("Cancel", color = AccentPink)
                    }
                },
                containerColor = Color(0xFF131B3E),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            )
        }

        // --- Confirm Delete Dialog ---
        productToDelete?.let { product ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Delete Product", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to remove '${product.name}' from your inventory?",
                        color = Color(0xFF94A3B8)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(product)
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("product_delete_confirm")
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF131B3E),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            )
        }

        // --- Barcode Scanner Dialog inside Add/Edit Product form ---
        if (showScannerInDialog) {
            BarcodeScannerDialog(
                onBarcodeScanned = { scannedCode ->
                    barcodeInput = scannedCode
                    showScannerInDialog = false
                },
                onDismiss = { showScannerInDialog = false }
            )
        }
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stockQuantity <= 5.0
    val expiryStatus = com.example.util.PharmacyUtils.getExpiryStatus(product.expiryDate)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name and Category Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (product.saltComposition.isNotBlank()) {
                        Text(
                            text = "Salt: ${product.saltComposition}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (product.manufacturer.isNotBlank()) {
                        Text(
                            text = "Mfg: ${product.manufacturer}",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x228B5CF6), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.category,
                                color = ElectricVioletLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (product.batchNumber.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x223B82F6), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Batch: ${product.batchNumber}",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (product.isRxRequired) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x33EC4899), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Rx Required",
                                    color = Color(0xFFF472B6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isLowStock) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0x22EF4444), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LOW STOCK",
                                    color = Color(0xFFF87171),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Expiry status indicator badge
                    if (product.expiryDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val (bgColor, textColor, label) = when (expiryStatus) {
                            is com.example.util.ExpiryStatus.Expired -> Triple(Color(0x33EF4444), Color(0xFFEF4444), "EXPIRED (${product.expiryDate})")
                            is com.example.util.ExpiryStatus.NearExpiry -> Triple(Color(0x33F59E0B), Color(0xFFF59E0B), "EXPIRING SOON (${product.expiryDate})")
                            else -> Triple(Color(0x2210B981), Color(0xFF34D399), "Exp: ${product.expiryDate}")
                        }
                        Box(
                            modifier = Modifier
                                .background(bgColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit & Delete Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("product_edit_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("product_delete_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0x11FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Stock Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sale Price
                Column {
                    Text("Sale Price", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", product.salePrice)} / ${product.unit}",
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Purchase Price (if exists)
                if (product.purchasePrice > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cost Price", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", product.purchasePrice)}",
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                // Stock Quantity
                Column(horizontalAlignment = Alignment.End) {
                    Text("In Stock", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = com.example.util.KiranaUnitUtils.formatQuantityWithUnit(product.stockQuantity, product.unit),
                        color = if (isLowStock) Color(0xFFF87171) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
