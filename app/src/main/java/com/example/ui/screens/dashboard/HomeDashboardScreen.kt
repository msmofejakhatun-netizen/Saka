package com.example.ui.screens.dashboard

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.HomeViewModel

/**
 * Modern, high-craft In-App Welcome & 3-Day Free Trial Onboarding Dialog.
 * Displayed automatically on the user's first login after signup.
 */
@Composable
fun WelcomeTrialOnboardingDialog(
    onDismiss: () -> Unit,
    onStartBilling: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Subtle glow animation for the trial badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val badgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(24.dp)
                .testTag("welcome_dialog_backdrop"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlateNavy),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            EmeraldGreen.copy(alpha = 0.6f),
                            ElectricViolet.copy(alpha = 0.3f),
                            Color(0x22FFFFFF)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("welcome_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("welcome_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hero Rocket Icon Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        EmeraldGreen.copy(alpha = 0.35f),
                                        Color(0x1110B981),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, EmeraldGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚀",
                            fontSize = 34.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Header: "Welcome to SmartPOS! 🚀"
                    Text(
                        text = "Welcome to SmartPOS! 🚀",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("welcome_header")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badge: "3-DAY FREE TRIAL ACTIVE"
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f * badgePulseAlpha),
                        shape = RoundedCornerShape(50.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            EmeraldGreen.copy(alpha = 0.8f * badgePulseAlpha)
                        ),
                        modifier = Modifier.testTag("welcome_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldLight)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3-DAY FREE TRIAL ACTIVE",
                                color = EmeraldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description: "You have full access to Pro features: Unlimited Invoices, Thermal Printing, Udhar Khata, and Cloud Sync."
                    Text(
                        text = "You have full access to Pro features: Unlimited Invoices, Thermal Printing, Udhar Khata, and Cloud Sync.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFCBD5E1),
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .testTag("welcome_description")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Key Features List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeepNavy.copy(alpha = 0.6f))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatureRowItem(
                            icon = Icons.Default.ReceiptLong,
                            title = "Unlimited Invoices",
                            subtitle = "Lightning-fast GST & non-GST billing",
                            iconColor = EmeraldGreen
                        )
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                        FeatureRowItem(
                            icon = Icons.Default.Print,
                            title = "ESC/POS Thermal Printing",
                            subtitle = "58mm & 80mm Bluetooth receipts",
                            iconColor = ElectricVioletLight
                        )
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                        FeatureRowItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Digital Udhar Khata",
                            subtitle = "Customer credit & WhatsApp reminders",
                            iconColor = GoldYellow
                        )
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                        FeatureRowItem(
                            icon = Icons.Default.CloudSync,
                            title = "Real-time Cloud Sync",
                            subtitle = "Offline-first database with auto-sync",
                            iconColor = EmeraldLight
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary Action Button: "Start Billing"
                    Button(
                        onClick = onStartBilling,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_billing_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Start Billing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Start",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Text: "Auto-renewal at ₹79/mo after trial unless cancelled."
                    Text(
                        text = "Auto-renewal at ₹79/mo after trial unless cancelled.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("welcome_secondary_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * HomeDashboardScreen composable wrapper.
 * Integrates HomeViewModel to observe `has_seen_welcome_dialog` and trigger the Onboarding Dialog.
 */
@Composable
fun HomeDashboardScreen(
    billingViewModel: BillingViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateToPOS: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showWelcomeDialog by homeViewModel.showWelcomeDialog.collectAsState()

    // Trigger local preference check on launch
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
                    onNavigateToPOS()
                }
            }
        )
    }
}
