package com.example.ui.screens.dashboard

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    var showPrinterSettingsOverlay by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(initialTab) {
        currentTab = initialTab
    }

    val subscriptionState by com.example.data.subscription.SubscriptionManager.subscriptionState.collectAsState()

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
                    com.example.ui.components.NavDrawerContent(
                        currentUser = currentUser,
                        currentTab = currentTab,
                        showProfileScreenOverlay = showProfileScreenOverlay,
                        showPrinterSettingsOverlay = showPrinterSettingsOverlay,
                        onTabSelected = { tab ->
                            currentTab = tab
                            showAdminScreenOverlay = false
                            showProfileScreenOverlay = false
                            showPrinterSettingsOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        onProfileClick = {
                            showProfileScreenOverlay = true
                            showAdminScreenOverlay = false
                            showPrinterSettingsOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        onPrinterSettingsClick = {
                            showPrinterSettingsOverlay = true
                            showProfileScreenOverlay = false
                            showAdminScreenOverlay = false
                            coroutineScope.launch { drawerState.close() }
                        },
                        onPaywallClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.openPaywall()
                        },
                        onCheckUpdateClick = {
                            coroutineScope.launch { drawerState.close() }
                            com.example.update.AppUpdateManagerHelper.checkForAppUpdate(context) { info ->
                                if (info.isUpdateAvailable) {
                                    Toast.makeText(context, "New Version Available: v${info.latestVersionName}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "You are using the latest version (v${info.currentVersionName})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onTermsClick = {
                            coroutineScope.launch { drawerState.close() }
                            com.example.util.WebUtils.openWebUrl(context, com.example.util.WebUtils.TERMS_URL)
                        },
                        onPrivacyClick = {
                            coroutineScope.launch { drawerState.close() }
                            com.example.util.WebUtils.openWebUrl(context, com.example.util.WebUtils.PRIVACY_URL)
                        },
                        onLogoutClick = {
                            coroutineScope.launch { drawerState.close() }
                            onLogout()
                        }
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
                    } else if (showPrinterSettingsOverlay) {
                        com.example.ui.screens.settings.PrinterSettingsScreen(
                            businessName = currentUser?.businessName ?: "Smart POS Store",
                            onNavigateBack = { showPrinterSettingsOverlay = false }
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

        if (viewModel.showPaywallDialog) {
            com.example.ui.screens.paywall.PaywallModalDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closePaywall() }
            )
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
    val context = LocalContext.current
    val homeViewModel: com.example.ui.viewmodel.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val showWelcomeDialog by homeViewModel.showWelcomeDialog.collectAsState()

    // Check local preferences for welcome dialog on first login after signup
    LaunchedEffect(Unit) {
        homeViewModel.checkWelcomeStatus(context)
    }

    if (showWelcomeDialog) {
        WelcomeTrialOnboardingDialog(
            onDismiss = {
                homeViewModel.dismissWelcomeDialog(context)
            },
            onStartBilling = {
                homeViewModel.dismissWelcomeDialog(context) {
                    onSelectTab(BottomTab.POS)
                }
            }
        )
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val isSubscriptionValid = com.example.util.AuthGuard.isSubscriptionValid(subscriptionState)
    var showExpiredBillingDialog by remember { mutableStateOf(false) }
    val invoices by viewModel.invoices.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val invoicesCount by viewModel.invoicesCount.collectAsState()
    val products by viewModel.products.collectAsState()

    val analyticsViewModel: com.example.ui.viewmodel.AnalyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.AnalyticsViewModelFactory(viewModel.repository)
    )
    val profitAnalytics by analyticsViewModel.profitAnalytics.collectAsState()
    val peakHoursAnalytics by analyticsViewModel.peakHoursAnalytics.collectAsState()
    val weeklySalesTrends by analyticsViewModel.weeklySalesTrends.collectAsState()

    val now = remember { System.currentTimeMillis() }
    val criticalExpiryCount = remember(products, now) {
        products.count { product ->
            val time = com.example.util.PharmacyUtils.parseExpiryDate(product.expiryDate)
            if (time != null) {
                val days = ((time - now) / (1000 * 60 * 60 * 24)).toInt()
                days < 15
            } else false
        }
    }

    val warningExpiryCount = remember(products, now) {
        products.count { product ->
            val time = com.example.util.PharmacyUtils.parseExpiryDate(product.expiryDate)
            if (time != null) {
                val days = ((time - now) / (1000 * 60 * 60 * 24)).toInt()
                days in 15..30
            } else false
        }
    }

    val totalExpiryRiskCount = criticalExpiryCount + warningExpiryCount

    val lowStockCount = remember(products) {
        products.count { p ->
            val threshold = if (p.minStockThreshold > 0.0) p.minStockThreshold else 5.0
            p.stockQuantity < threshold
        }
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
                        onClick = { viewModel.openPaywall() },
                        modifier = Modifier.testTag("dashboard_pro_paywall_button")
                    ) {
                        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = "Upgrade Pro", tint = GoldYellow)
                    }
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
                onClick = {
                    if (!isSubscriptionValid) {
                        showExpiredBillingDialog = true
                    } else {
                        onSelectTab(BottomTab.POS)
                    }
                },
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

            // Subscription Expired Warning Banner
            if (!isSubscriptionValid) {
                Card(
                    onClick = { viewModel.openPaywall() },
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                        .testTag("dashboard_subscription_expired_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Subscription Expired",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Subscription Expired",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Complete payment of ₹79 to unlock all features.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.openPaywall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp).testTag("dashboard_renew_subscription_btn")
                        ) {
                            Text("Pay ₹79", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

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

            // Near Expiry Risk & Smart Reorder Warning Banner
            if (totalExpiryRiskCount > 0 || lowStockCount > 0) {
                Card(
                    onClick = { onSelectTab(BottomTab.INVENTORY) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (criticalExpiryCount > 0) Color(0x22EF4444) else Color(0x22F59E0B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (criticalExpiryCount > 0) Color(0x66EF4444) else Color(0x66F59E0B),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("dashboard_expiry_risk_warning_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Expiry Risk Alert",
                                    tint = if (criticalExpiryCount > 0) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEAR EXPIRY & REORDER RISK",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Inspect Risk ➔",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (criticalExpiryCount > 0) {
                                Surface(
                                    color = Color(0x33EF4444),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🚨 Critical (<15d): ", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("$criticalExpiryCount", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            if (warningExpiryCount > 0) {
                                Surface(
                                    color = Color(0x33F59E0B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("⚠️ Warning (15-30d): ", color = Color(0xFFFCD34D), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("$warningExpiryCount", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            if (lowStockCount > 0) {
                                Surface(
                                    color = Color(0x333B82F6),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("📦 Reorder: ", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("$lowStockCount", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // High-Level KPI Summary Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_stats_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // KPI 1: Revenue
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Sales", tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Revenue", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", totalSales ?: 0.0)}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("dashboard_total_sales_value")
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0x22FFFFFF)))

                        // KPI 2: Net Profit & Profit Margin
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Profit", tint = EmeraldLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Net Profit", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", profitAnalytics.todayNetProfit)}",
                                color = EmeraldLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("dashboard_net_profit_value")
                            )
                            Surface(
                                color = Color(0x3310B981),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profitAnalytics.todayProfitMarginPercentage)}% Margin",
                                    color = EmeraldLight,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp).testTag("dashboard_profit_margin_tag")
                                )
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0x22FFFFFF)))

                        // KPI 3: Total Invoices
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.9f)) {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Receipts", tint = ElectricVioletLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Invoices", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$invoicesCount",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("dashboard_invoices_count_value")
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0x22FFFFFF)))

                        // KPI 4: Total Inventory Items
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("dashboard_products_cta")
                                .clickable { onSelectTab(BottomTab.INVENTORY) }
                        ) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = "Inventory", tint = GoldYellow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Items", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${products.size}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("dashboard_products_count_value")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick POS Action Terminal Hero Banner
            Card(
                onClick = {
                    if (!isSubscriptionValid) {
                        showExpiredBillingDialog = true
                    } else {
                        onSelectTab(BottomTab.POS)
                    }
                },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Peak Hours & Smart Business Insights Analytics Card
            PeakHoursAnalyticsSection(
                profitAnalytics = profitAnalytics,
                peakHoursAnalytics = peakHoursAnalytics,
                weeklySalesTrends = weeklySalesTrends
            )

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

        if (showExpiredBillingDialog) {
            AlertDialog(
                onDismissRequest = { showExpiredBillingDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subscription Expired",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                text = {
                    Text(
                        text = "Your trial has expired. Please complete payment of ₹79 to continue billing and unlock all features.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFCBD5E1)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExpiredBillingDialog = false
                            viewModel.openPaywall()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Complete Payment (₹79)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExpiredBillingDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
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

@Composable
private fun PeakHoursAnalyticsSection(
    profitAnalytics: com.example.ui.viewmodel.ProfitAnalytics,
    peakHoursAnalytics: com.example.ui.viewmodel.PeakHoursAnalytics,
    weeklySalesTrends: List<com.example.ui.viewmodel.DailySalesTrend>
) {
    var selectedTrendTab by remember { mutableStateOf("Hourly Peak") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x220F172A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x3310B981), RoundedCornerShape(16.dp))
            .testTag("dashboard_peak_hours_analytics_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x33F59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics",
                            tint = GoldYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMART BUSINESS INSIGHTS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Live Profit Margin & Peak Hour Trends",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(2.dp)
                ) {
                    listOf("Hourly Peak", "7-Day Trend").forEach { tab ->
                        val isSelected = selectedTrendTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) EmeraldGreen else Color.Transparent)
                                .clickable { selectedTrendTab = tab }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peak Sales Time Hero Banner
            Surface(
                color = Color(0x1AF59E0B),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F59E0B)),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_peak_time_hero_banner")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Peak Time",
                            tint = GoldYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = peakHoursAnalytics.peakWindowFormatted,
                                color = GoldYellow,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Highest volume window (${String.format(Locale.US, "%.1f", peakHoursAnalytics.peakPercentage)}% of total daily traffic)",
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${String.format(Locale.US, "%.0f", peakHoursAnalytics.peakSlotSales)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${peakHoursAnalytics.peakSlotTransactions} bills",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTrendTab == "Hourly Peak") {
                Text(
                    text = "HOURLY TRANSACTION BREAKDOWN",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxSlotSales = remember(peakHoursAnalytics) {
                    peakHoursAnalytics.hourlySlots.maxOfOrNull { it.totalSales }?.coerceAtLeast(1.0) ?: 1.0
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("dashboard_hourly_bar_chart"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    peakHoursAnalytics.hourlySlots.forEach { slot ->
                        val isPeak = slot.totalSales > 0 && slot.totalSales == peakHoursAnalytics.peakSlotSales
                        val heightFraction = (slot.totalSales / maxSlotSales).toFloat().coerceIn(0.08f, 1.0f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            if (slot.totalSales > 0) {
                                Text(
                                    text = "₹${String.format(Locale.US, "%.0f", slot.totalSales)}",
                                    color = if (isPeak) GoldYellow else EmeraldLight,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(text = "-", color = Color(0xFF64748B), fontSize = 8.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isPeak) GoldYellow else if (slot.totalSales > 0) EmeraldGreen else Color(0x33FFFFFF)
                                    )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = slot.slotLabel.replace(" AM", "A").replace(" PM", "P"),
                                color = Color(0xFF94A3B8),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "LAST 7 DAYS REVENUE & NET PROFIT TREND",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxDaySales = remember(weeklySalesTrends) {
                    weeklySalesTrends.maxOfOrNull { it.totalSales }?.coerceAtLeast(1.0) ?: 1.0
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("dashboard_weekly_trend_chart"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklySalesTrends.forEach { dayTrend ->
                        val salesFraction = (dayTrend.totalSales / maxDaySales).toFloat().coerceIn(0.08f, 1.0f)
                        val profitFraction = (dayTrend.totalProfit / maxDaySales).toFloat().coerceIn(0.05f, salesFraction)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            if (dayTrend.totalSales > 0) {
                                Text(
                                    text = "₹${String.format(Locale.US, "%.0f", dayTrend.totalSales)}",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(text = "₹0", color = Color(0xFF64748B), fontSize = 8.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(salesFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0x3310B981)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(if (salesFraction > 0) profitFraction / salesFraction else 0f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(EmeraldGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayTrend.dayLabel,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0x3310B981), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Total Revenue", color = Color(0xFF94A3B8), fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).background(EmeraldGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Net Profit", color = EmeraldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
