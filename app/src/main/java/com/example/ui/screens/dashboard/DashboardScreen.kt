package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.InvoiceEntity
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.PremiumGradientBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class BottomTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, "bottom_tab_home"),
    POS("POS Bill", Icons.Default.PointOfSale, "bottom_tab_pos"),
    INVENTORY("Inventory", Icons.Default.Inventory2, "bottom_tab_inventory"),
    HISTORY("History", Icons.Default.History, "bottom_tab_history"),
    UDHAR("Udhar Khata", Icons.Default.AccountBalanceWallet, "bottom_tab_udhar")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BillingViewModel,
    initialTab: BottomTab = BottomTab.HOME,
    onNavigateToAdmin: (() -> Unit)? = null,
    onNavigateToProducts: (() -> Unit)? = null,
    onNavigateToCreateBill: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var showAdminScreenOverlay by remember { mutableStateOf(false) }
    var showProfileScreenOverlay by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(initialTab) {
        currentTab = initialTab
    }

    PremiumGradientBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0F172A),
                    drawerContentColor = Color.White,
                    modifier = Modifier
                        .width(300.dp)
                        .testTag("side_navigation_drawer_sheet")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Business Header in Drawer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x3310B981)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(1.dp, Color(0x3310B981), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(EmeraldGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.businessName?.take(1) ?: "K").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentUser?.businessName ?: "Kirana Billing",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "👤 ${currentUser?.fullName ?: "Store Owner"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Category: ${currentUser?.category ?: "Retail"}",
                                color = EmeraldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Drawer Links
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = EmeraldGreen) },
                        label = { Text("Home Dashboard", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == BottomTab.HOME && !showAdminScreenOverlay && !showProfileScreenOverlay,
                        onClick = {
                            currentTab = BottomTab.HOME
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_home")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = "POS Bill", tint = EmeraldGreen) },
                        label = { Text("POS Terminal & Billing", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == BottomTab.POS && !showAdminScreenOverlay && !showProfileScreenOverlay,
                        onClick = {
                            currentTab = BottomTab.POS
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_pos")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory", tint = EmeraldGreen) },
                        label = { Text("Inventory & Stock Alert", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == BottomTab.INVENTORY && !showAdminScreenOverlay && !showProfileScreenOverlay,
                        onClick = {
                            currentTab = BottomTab.INVENTORY
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_inventory")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History", tint = EmeraldGreen) },
                        label = { Text("Transaction History", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == BottomTab.HISTORY && !showAdminScreenOverlay && !showProfileScreenOverlay,
                        onClick = {
                            currentTab = BottomTab.HISTORY
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_history")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Udhar Khata", tint = EmeraldGreen) },
                        label = { Text("Udhar Khata (Credit Ledger)", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == BottomTab.UDHAR && !showAdminScreenOverlay && !showProfileScreenOverlay,
                        onClick = {
                            currentTab = BottomTab.UDHAR
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_udhar")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "MANAGEMENT & PROFILE",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = ElectricVioletLight) },
                        label = { Text("Admin Panel / Categories", fontWeight = FontWeight.SemiBold) },
                        selected = showAdminScreenOverlay,
                        onClick = {
                            showAdminScreenOverlay = true
                            showProfileScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x336366F1)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_admin")
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = GoldYellow) },
                        label = { Text("Business Profile Settings", fontWeight = FontWeight.SemiBold) },
                        selected = showProfileScreenOverlay,
                        onClick = {
                            showProfileScreenOverlay = true
                            showAdminScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x33F59E0B)),
                        modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_profile")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = AccentPink) },
                        label = { Text("Logout", fontWeight = FontWeight.Bold, color = AccentPink) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onLogout()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("drawer_item_logout")
                    )
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xF00D1333),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color(0x2210B981),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .testTag("main_bottom_navigation_bar")
                    ) {
                        BottomTab.values().forEach { tab ->
                            val selected = currentTab == tab && !showAdminScreenOverlay && !showProfileScreenOverlay
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    currentTab = tab
                                    showAdminScreenOverlay = false
                                    showProfileScreenOverlay = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (selected) EmeraldGreen else Color(0xFF94A3B8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (selected) EmeraldLight else Color(0xFF94A3B8)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0x3310B981)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showAdminScreenOverlay) {
                        com.example.ui.screens.admin.AdminScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showAdminScreenOverlay = false }
                        )
                    } else if (showProfileScreenOverlay) {
                        com.example.ui.screens.profile.ProfileSetupScreen(
                            viewModel = viewModel,
                            onSetupSuccess = { showProfileScreenOverlay = false }
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                            },
                            label = "tab_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                BottomTab.HOME -> HomeDashboardContent(
                                    viewModel = viewModel,
                                    onSelectTab = { currentTab = it },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onLogout = onLogout
                                )
                                BottomTab.POS -> com.example.ui.screens.billing.CreateBillScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentTab = BottomTab.HOME }
                                )
                                BottomTab.INVENTORY -> com.example.ui.screens.products.ProductsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentTab = BottomTab.HOME }
                                )
                                BottomTab.HISTORY -> com.example.ui.screens.billing.TransactionHistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentTab = BottomTab.HOME },
                                    onNavigateToPOS = { currentTab = BottomTab.POS }
                                )
                                BottomTab.UDHAR -> com.example.ui.screens.udhar.UdharKhataScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentTab = BottomTab.HOME },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDashboardContent(
    viewModel: BillingViewModel,
    onSelectTab: (BottomTab) -> Unit,
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val invoicesCount by viewModel.invoicesCount.collectAsState()
    val products by viewModel.products.collectAsState()

    val lowStockCount = remember(products) {
        products.count { it.stockQuantity <= 5.0 }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("dashboard_hamburger_button")
                    ) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Color.White)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser?.businessName ?: "Kirana Billing",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Category: ${currentUser?.category ?: "Retail"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("dashboard_logout_button")
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = AccentPink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0x99090D22)
                ),
                modifier = Modifier.testTag("dashboard_top_bar")
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onSelectTab(BottomTab.POS) },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("dashboard_add_bill_fab")
                    .padding(bottom = 12.dp, end = 8.dp),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Invoice", modifier = Modifier.size(28.dp))
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
            Spacer(modifier = Modifier.height(14.dp))

            // Firebase Live Sync Banner
            val isFirebaseAvailable = com.example.data.firebase.FirebaseManager.isFirebaseAvailable
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isFirebaseAvailable) Color(0x1A10B981) else Color(0x1AEF4444)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isFirebaseAvailable) Color(0x2210B981) else Color(0x22EF4444),
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("dashboard_firebase_status")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isFirebaseAvailable) EmeraldGreen else Color(0xFFEF4444),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isFirebaseAvailable) {
                            "Live Firebase DB Synced"
                        } else {
                            "Running in Offline Room-fallback mode"
                        },
                        color = if (isFirebaseAvailable) EmeraldLight else Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (isFirebaseAvailable) "SECURE" else "LOCAL",
                        color = if (isFirebaseAvailable) EmeraldLight else Color(0xFFF87171),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // High-Level KPI Summary Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_stats_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // KPI 1: Total Revenue Today
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Sales", tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Today's Revenue", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", totalSales ?: 0.0)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("dashboard_total_sales_value")
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0x22FFFFFF)))

                    // KPI 2: Total Invoices
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Receipts", tint = ElectricVioletLight, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Invoices", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "$invoicesCount",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("dashboard_invoices_count_value")
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0x22FFFFFF)))

                    // KPI 3: Total Inventory Items
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("dashboard_products_cta")
                            .clickable { onSelectTab(BottomTab.INVENTORY) }
                    ) {
                        Icon(imageVector = Icons.Default.Inventory2, contentDescription = "Inventory", tint = GoldYellow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Inventory", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${products.size} items",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("dashboard_products_count_value")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick POS Action Terminal Hero Banner
            Card(
                onClick = { onSelectTab(BottomTab.POS) },
                colors = CardDefaults.cardColors(containerColor = Color(0x3310B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x6610B981), RoundedCornerShape(14.dp))
                    .testTag("dashboard_create_bill_cta")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(EmeraldGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "POS Billing",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Open POS Terminal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Fast billing with loose quantity (Kg/Gm/Ltr)", color = EmeraldLight, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open POS",
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Low Stock Warning Alert Banner (if any)
            if (lowStockCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    onClick = { onSelectTab(BottomTab.INVENTORY) },
                    colors = CardDefaults.cardColors(containerColor = Color(0x22F59E0B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x44F59E0B), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low Stock Alert",
                                tint = GoldYellow,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$lowStockCount products low in stock!",
                                color = GoldYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Restock →",
                            color = GoldYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Invisible/Compact trigger tag for admin testTag
            Box(modifier = Modifier.size(1.dp).testTag("dashboard_admin_cta"))

            Spacer(modifier = Modifier.height(18.dp))

            // Recent Transactions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                TextButton(
                    onClick = { onSelectTab(BottomTab.HISTORY) },
                    modifier = Modifier
                        .testTag("dashboard_view_all_history_button")
                        .testTag("dashboard_history_cta")
                ) {
                    Text("View All", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Recent Invoices List
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color(0x44FFFFFF),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No invoices generated yet", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        Text("Tap 'POS Bill' tab below to start billing", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("dashboard_invoices_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(invoices.take(10)) { invoice ->
                        InvoiceItemRow(invoice = invoice)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItemRow(invoice: InvoiceEntity) {
    val dateString = remember(invoice.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(invoice.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x2210B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Receipt",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = invoice.customerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateString · ${invoice.itemsCount} ${if (invoice.itemsCount == 1) "item" else "items"}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", invoice.amount)}",
                    color = EmeraldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color(0x2210B981), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = invoice.status,
                        color = EmeraldLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
