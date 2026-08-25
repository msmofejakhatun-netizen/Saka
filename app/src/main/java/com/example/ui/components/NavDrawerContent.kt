package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.db.UserEntity
import com.example.data.subscription.SubscriptionManager
import com.example.ui.screens.dashboard.BottomTab
import com.example.ui.theme.*
import com.example.util.WebUtils

/**
 * Reusable scrollable Navigation Drawer content with profile card,
 * navigation destinations, dynamic subscription status, and legal policy footer.
 */
@Composable
fun NavDrawerContent(
    currentUser: UserEntity?,
    currentTab: BottomTab,
    showProfileScreenOverlay: Boolean,
    showPrinterSettingsOverlay: Boolean,
    onTabSelected: (BottomTab) -> Unit,
    onProfileClick: () -> Unit,
    onPrinterSettingsClick: () -> Unit,
    onPaywallClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val subscriptionState by SubscriptionManager.subscriptionState.collectAsState()
    val isSubscribed = subscriptionState.isProUser ||
            subscriptionState.autoPayMandateStatus == "ACTIVE" ||
            subscriptionState.autoPayMandateStatus == "TRIAL_ACTIVE"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("nav_drawer_scrollable_content")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // User Header Profile Card
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

        // Main Navigation Items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = EmeraldGreen) },
            label = { Text("Home Dashboard", fontWeight = FontWeight.SemiBold) },
            selected = currentTab == BottomTab.HOME && !showProfileScreenOverlay && !showPrinterSettingsOverlay,
            onClick = { onTabSelected(BottomTab.HOME) },
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_home")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.PointOfSale, contentDescription = "POS Bill", tint = EmeraldGreen) },
            label = { Text("POS Terminal & Billing", fontWeight = FontWeight.SemiBold) },
            selected = currentTab == BottomTab.POS && !showProfileScreenOverlay && !showPrinterSettingsOverlay,
            onClick = { onTabSelected(BottomTab.POS) },
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_pos")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory", tint = EmeraldGreen) },
            label = { Text("Inventory & Stock Alert", fontWeight = FontWeight.SemiBold) },
            selected = currentTab == BottomTab.INVENTORY && !showProfileScreenOverlay && !showPrinterSettingsOverlay,
            onClick = { onTabSelected(BottomTab.INVENTORY) },
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_inventory")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.History, contentDescription = "History", tint = EmeraldGreen) },
            label = { Text("Transaction History", fontWeight = FontWeight.SemiBold) },
            selected = currentTab == BottomTab.HISTORY && !showProfileScreenOverlay && !showPrinterSettingsOverlay,
            onClick = { onTabSelected(BottomTab.HISTORY) },
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_history")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Udhar Khata", tint = EmeraldGreen) },
            label = { Text("Udhar Khata (Credit Ledger)", fontWeight = FontWeight.SemiBold) },
            selected = currentTab == BottomTab.UDHAR && !showProfileScreenOverlay && !showPrinterSettingsOverlay,
            onClick = { onTabSelected(BottomTab.UDHAR) },
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_udhar")
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(10.dp))

        // Management & Profile Items
        Text(
            text = "MANAGEMENT & PROFILE",
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = GoldYellow) },
            label = { Text("Business Profile Settings", fontWeight = FontWeight.SemiBold) },
            selected = showProfileScreenOverlay,
            onClick = onProfileClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x33F59E0B)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_profile")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Print, contentDescription = "Thermal Printer", tint = EmeraldGreen) },
            label = { Text("Thermal Printer Setup", fontWeight = FontWeight.SemiBold) },
            selected = showPrinterSettingsOverlay,
            onClick = onPrinterSettingsClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x3310B981)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_printer_settings")
        )

        // Dynamic Pro Membership / Subscription Status Title
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = if (isSubscribed) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                    contentDescription = "Subscription Status",
                    tint = if (isSubscribed) EmeraldGreen else GoldYellow
                )
            },
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSubscribed) "My Subscription Status" else "Pro Membership & ₹1 Trial",
                        fontWeight = FontWeight.Bold,
                        color = if (isSubscribed) EmeraldLight else GoldYellow
                    )
                    Surface(
                        color = if (isSubscribed) EmeraldGreen else GoldYellow,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "ACTIVE" else "PRO",
                            color = if (isSubscribed) Color.White else Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            selected = false,
            onClick = onPaywallClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = if (isSubscribed) Color(0x3310B981) else Color(0x33F59E0B)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_paywall")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Check for Updates", tint = ElectricVioletLight) },
            label = { Text("Check App Updates", fontWeight = FontWeight.Bold, color = Color.White) },
            selected = false,
            onClick = onCheckUpdateClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x338B5CF6)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_check_update")
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "LEGAL & POLICIES",
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Description, contentDescription = "Terms & Conditions", tint = Color(0xFF2DD4BF)) },
            label = { Text("Terms & Conditions", fontWeight = FontWeight.SemiBold, color = Color.White) },
            selected = false,
            onClick = onTermsClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x332DD4BF)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_terms")
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Security, contentDescription = "Privacy Policy", tint = Color(0xFF2DD4BF)) },
            label = { Text("Privacy Policy", fontWeight = FontWeight.SemiBold, color = Color.White) },
            selected = false,
            onClick = onPrivacyClick,
            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color(0x332DD4BF)),
            modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_item_privacy")
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = AccentPink) },
            label = { Text("Logout", fontWeight = FontWeight.Bold, color = AccentPink) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("drawer_item_logout")
        )

        // Footer Section with Terms, Privacy Policy & App Version
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF64748B)),
                modifier = Modifier
                    .clickable { onPrivacyClick() }
                    .padding(vertical = 4.dp)
                    .testTag("drawer_footer_privacy")
            )
            Text(
                text = "•",
                color = Color(0xFF475569),
                fontSize = 12.sp
            )
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF64748B)),
                modifier = Modifier
                    .clickable { onTermsClick() }
                    .padding(vertical = 4.dp)
                    .testTag("drawer_footer_terms")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SmartPOS v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("drawer_footer_version")
        )
    }
}
