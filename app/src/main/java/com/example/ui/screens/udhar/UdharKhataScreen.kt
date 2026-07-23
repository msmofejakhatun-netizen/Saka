package com.example.ui.screens.udhar

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CustomerEntity
import com.example.data.db.CustomerTransactionEntity
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.util.ReminderType
import com.example.util.WhatsAppReminderUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdharKhataScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    val customers by viewModel.filteredCustomers.collectAsState()
    val allCustomers by viewModel.customers.collectAsState()
    val searchQuery by viewModel.customerSearchQuery.collectAsState()

    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var showJamaDialog by remember { mutableStateOf(false) }
    var showAddUdharDialog by remember { mutableStateOf(false) }

    // WhatsApp Reminder Modal States
    var showWhatsAppReminderModal by remember { mutableStateOf(false) }
    var reminderTargetCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedReminderTypeForModal by remember { mutableStateOf(ReminderType.POLITE) }

    val totalPendingAmount = remember(allCustomers) {
        allCustomers.sumOf { it.totalPendingBalance }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Udhar Khata (Credit Ledger)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    if (onOpenDrawer != null) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("udhar_khata_drawer_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("udhar_khata_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddUdharDialog = true },
                        modifier = Modifier.testTag("udhar_khata_add_customer_action")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = EmeraldGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0x99090D22)),
                modifier = Modifier.testTag("udhar_khata_top_bar")
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddUdharDialog = true },
                containerColor = RoseRed,
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Udhar") },
                text = { Text("New Udhar Entry", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("udhar_khata_add_entry_fab")
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedCustomerForLedger != null) {
                CustomerLedgerDetailView(
                    customer = selectedCustomerForLedger!!,
                    viewModel = viewModel,
                    onBack = {
                        selectedCustomerForLedger = null
                        viewModel.activeCustomerMobileForLedger.value = ""
                    },
                    onOpenJamaDialog = { showJamaDialog = true },
                    onOpenAddUdharDialog = { showAddUdharDialog = true },
                    onOpenWhatsAppReminder = { cust, type ->
                        reminderTargetCustomer = cust
                        selectedReminderTypeForModal = type
                        showWhatsAppReminderModal = true
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary KPI Card
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("udhar_khata_summary_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL PENDING UDHARI",
                                    color = Color(0xFFF87171),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", totalPendingAmount)}",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("udhar_khata_total_amount")
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CREDIT CUSTOMERS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${allCustomers.size}",
                                    color = EmeraldLight,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("udhar_khata_customers_count")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.customerSearchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("udhar_khata_search_input"),
                        placeholder = { Text("Search by Customer Name or Mobile...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.customerSearchQuery.value = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x220D1333),
                            unfocusedContainerColor = Color(0x220D1333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Customer List
                    if (customers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Empty",
                                    tint = Color(0x33FFFFFF),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No customer Udhar records found" else "No matching customers found",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tap '+ New Udhar Entry' below to record credit given",
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
                                .testTag("udhar_khata_customer_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(customers) { customer ->
                                CustomerBalanceCard(
                                    customer = customer,
                                    onSelect = {
                                        selectedCustomerForLedger = customer
                                        viewModel.activeCustomerMobileForLedger.value = customer.mobileNumber
                                    },
                                    onOpenReminder = { cust ->
                                        reminderTargetCustomer = cust
                                        selectedReminderTypeForModal = ReminderType.POLITE
                                        showWhatsAppReminderModal = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Jama Payment Received Dialog Modal
            if (showJamaDialog) {
                val activeCustomer = selectedCustomerForLedger
                JamaPaymentDialog(
                    initialName = activeCustomer?.name ?: "",
                    initialMobile = activeCustomer?.mobileNumber ?: "",
                    onDismiss = { showJamaDialog = false },
                    onSave = { name, mobile, amount, mode, note ->
                        viewModel.recordJamaPayment(name, mobile, amount, mode, note) {
                            showJamaDialog = false
                            // Refresh selected customer state
                            val updatedCustomer = allCustomers.find { it.mobileNumber == mobile }
                            if (updatedCustomer != null) {
                                selectedCustomerForLedger = updatedCustomer
                            }
                        }
                    }
                )
            }

            // New Udhar Entry Dialog Modal
            if (showAddUdharDialog) {
                val activeCustomer = selectedCustomerForLedger
                AddUdharEntryDialog(
                    initialName = activeCustomer?.name ?: "",
                    initialMobile = activeCustomer?.mobileNumber ?: "",
                    onDismiss = { showAddUdharDialog = false },
                    onSave = { name, mobile, amount, note ->
                        viewModel.recordUdharEntry(name, mobile, amount, note) {
                            showAddUdharDialog = false
                            val updatedCustomer = allCustomers.find { it.mobileNumber == mobile }
                            if (updatedCustomer != null) {
                                selectedCustomerForLedger = updatedCustomer
                            }
                        }
                    }
                )
            }

            // WhatsApp Reminder Engine Modal Dialog
            if (showWhatsAppReminderModal && reminderTargetCustomer != null) {
                val currentUser by viewModel.currentUser.collectAsState()
                val transactions by viewModel.selectedCustomerTransactions.collectAsState()

                WhatsAppReminderModalDialog(
                    customer = reminderTargetCustomer!!,
                    businessName = currentUser?.businessName ?: "Kirana Store",
                    initialType = selectedReminderTypeForModal,
                    transactions = transactions,
                    onDismiss = {
                        showWhatsAppReminderModal = false
                        reminderTargetCustomer = null
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomerBalanceCard(
    customer: CustomerEntity,
    onSelect: () -> Unit,
    onOpenReminder: (CustomerEntity) -> Unit
) {
    val formattedDate = remember(customer.lastTransactionTimestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(customer.lastTransactionTimestamp))
    }

    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = Color(0x221E295D)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .testTag("udhar_customer_item_${customer.mobileNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x22EF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.take(1).uppercase(),
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = customer.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📱 ${customer.mobileNumber}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Last: $formattedDate",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", customer.totalPendingBalance)}",
                        color = if (customer.totalPendingBalance > 0) Color(0xFFF87171) else EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                if (customer.totalPendingBalance > 0) Color(0x33EF4444) else Color(0x3310B981),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (customer.totalPendingBalance > 0) "Pending Udhar" else "Settled",
                            color = if (customer.totalPendingBalance > 0) Color(0xFFF87171) else EmeraldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // WhatsApp Payment Reminder Action Bar
            if (customer.totalPendingBalance > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0x11FFFFFF))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onOpenReminder(customer) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("udhar_reminder_button_${customer.mobileNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "WhatsApp Reminder",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp Reminder",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerLedgerDetailView(
    customer: CustomerEntity,
    viewModel: BillingViewModel,
    onBack: () -> Unit,
    onOpenJamaDialog: () -> Unit,
    onOpenAddUdharDialog: () -> Unit,
    onOpenWhatsAppReminder: (CustomerEntity, ReminderType) -> Unit
) {
    val transactions by viewModel.selectedCustomerTransactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Customer Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x330D1333)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x4410B981), RoundedCornerShape(16.dp))
                .testTag("customer_ledger_header")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = customer.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "📱 ${customer.mobileNumber}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Outstanding", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", customer.totalPendingBalance)}",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.testTag("customer_ledger_balance")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: Jama Karein vs Add Udhar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenJamaDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_ledger_jama_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Jama Karein", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jama Karein (+)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onOpenAddUdharDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_ledger_add_udhar_button")
                    ) {
                        Icon(imageVector = Icons.Default.RemoveCircle, contentDescription = "Add Udhar", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Udhar (+)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Reminder Action Button inside Ledger Header
                Button(
                    onClick = { onOpenWhatsAppReminder(customer, ReminderType.POLITE) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_ledger_whatsapp_reminder_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "WhatsApp Reminder", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send WhatsApp Payment Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Reminder Chips
                Text("QUICK REMINDER TEMPLATES", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onOpenWhatsAppReminder(customer, ReminderType.POLITE) },
                        label = { Text("💬 Polite", fontSize = 11.sp, color = EmeraldLight) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0x2210B981)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color(0x4410B981)),
                        modifier = Modifier.testTag("quick_chip_polite")
                    )

                    FilterChip(
                        selected = false,
                        onClick = { onOpenWhatsAppReminder(customer, ReminderType.URGENT) },
                        label = { Text("⚠️ Urgent", fontSize = 11.sp, color = Color(0xFFF87171)) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0x22EF4444)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color(0x44EF4444)),
                        modifier = Modifier.testTag("quick_chip_urgent")
                    )

                    FilterChip(
                        selected = false,
                        onClick = { onOpenWhatsAppReminder(customer, ReminderType.STATEMENT) },
                        label = { Text("📄 Statement", fontSize = 11.sp, color = ElectricVioletLight) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0x226366F1)),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color(0x446366F1)),
                        modifier = Modifier.testTag("quick_chip_statement")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ledger Transaction History",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No ledger history found for this customer", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("customer_ledger_transactions_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(transactions) { tx ->
                    LedgerTransactionRow(tx = tx)
                }
            }
        }
    }
}

@Composable
private fun LedgerTransactionRow(tx: CustomerTransactionEntity) {
    val isJama = tx.type == "CREDIT"
    val formattedDate = remember(tx.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(tx.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isJama) Color(0x2210B981) else Color(0x22EF4444),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isJama) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = tx.type,
                        tint = if (isJama) EmeraldGreen else Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isJama) "Jama (Payment Received)" else "Udhar Given",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${tx.paymentMode}]",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = formattedDate,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${if (isJama) "- ₹" else "+ ₹"}${String.format(Locale.US, "%.2f", tx.amount)}",
                color = if (isJama) EmeraldLight else Color(0xFFF87171),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsAppReminderModalDialog(
    customer: CustomerEntity,
    businessName: String,
    initialType: ReminderType,
    transactions: List<CustomerTransactionEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }

    var messageText by remember(selectedType, customer, businessName) {
        mutableStateOf(
            WhatsAppReminderUtils.buildReminderMessage(
                customerName = customer.name,
                businessName = businessName,
                pendingAmount = customer.totalPendingBalance,
                lastTransactionTimestamp = customer.lastTransactionTimestamp,
                reminderType = selectedType,
                transactions = transactions
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "WhatsApp Payment Reminder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "To: ${customer.name} (${customer.mobileNumber})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Outstanding Summary Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x22EF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outstanding Udhar Balance:", color = Color.White, fontSize = 12.sp)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", customer.totalPendingBalance)}",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Reminder Type Quick Selection Chips
                Text("Select Reminder Template", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderType.values().forEach { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = { Text(type.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x2210B981),
                                labelColor = EmeraldLight
                            )
                        )
                    }
                }

                // Message Preview / Custom Editor
                Text("Message Preview (Editable)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("whatsapp_reminder_message_input"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = Color(0x220D1333),
                        unfocusedContainerColor = Color(0x220D1333)
                    )
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        WhatsAppReminderUtils.sendWhatsAppReminder(context, customer.mobileNumber, messageText)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_reminder_send_whatsapp_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send WhatsApp", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        WhatsAppReminderUtils.shareTextViaStandardChooser(context, messageText)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_reminder_share_fallback_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share SMS", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMS / Other Share (Fallback)", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("whatsapp_reminder_cancel_button")
            ) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamaPaymentDialog(
    initialName: String,
    initialMobile: String,
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, amount: Double, mode: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var amountInput by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var noteInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Jama Karein (Receive Payment)",
                color = EmeraldLight,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jama_dialog_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jama_dialog_mobile_input")
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount Received (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jama_dialog_amount_input")
                )

                Text("Payment Method", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Online").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note / Remarks (e.g. Monthly Settlement)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jama_dialog_note_input")
                )

                errorMsg?.let {
                    Text(text = it, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (mobile.isBlank() || amt <= 0.0) {
                        errorMsg = "Please enter valid mobile and positive payment amount"
                    } else {
                        onSave(name.ifBlank { "Customer" }, mobile, amt, paymentMode, noteInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("jama_dialog_save_button")
            ) {
                Text("Save Jama", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUdharEntryDialog(
    initialName: String,
    initialMobile: String,
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, amount: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record New Udhar (Credit)",
                color = Color(0xFFF87171),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_udhar_dialog_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_udhar_dialog_mobile_input")
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Udhar Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_udhar_dialog_amount_input")
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Items / Note (e.g. Kirana Ration)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_udhar_dialog_note_input")
                )

                errorMsg?.let {
                    Text(text = it, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (mobile.isBlank() || amt <= 0.0) {
                        errorMsg = "Please enter valid mobile and Udhar amount"
                    } else {
                        onSave(name.ifBlank { "Customer" }, mobile, amt, noteInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                modifier = Modifier.testTag("add_udhar_dialog_save_button")
            ) {
                Text("Save Udhar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp)
    )
}
