package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseManager
import com.example.data.repository.BillingRepository
import com.example.data.subscription.AppSessionManager
import com.example.data.subscription.PaymentGatewayConfig
import com.example.data.subscription.SessionAccessState
import com.example.ui.navigation.Screen
import com.example.ui.screens.admin.AdminScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.BillingViewModelFactory
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity(), com.razorpay.PaymentResultWithDataListener {
    private var navControllerRef: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial notification intent if app opened from push notification
        handleNotificationIntent(intent)

        // Preload Razorpay Checkout SDK
        try {
            com.razorpay.Checkout.preload(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Razorpay preload failed: ${e.localizedMessage}")
        }

        // Initialize Firebase
        FirebaseManager.initialize(this)

        // Initialize Subscription Manager
        com.example.data.subscription.SubscriptionManager.init(this)

        // Initialize SQLite Room database & repository locally
        val database = AppDatabase.getDatabase(this)
        val repository = BillingRepository(
            userDao = database.userDao(),
            categoryDao = database.categoryDao(),
            invoiceDao = database.invoiceDao(),
            productDao = database.productDao(),
            customerDao = database.customerDao(),
            customerTransactionDao = database.customerTransactionDao()
        )

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                navControllerRef = navController

                // Instantiate the unified ViewModel using our Factory
                val viewModel: BillingViewModel = viewModel(
                    factory = BillingViewModelFactory(repository)
                )

                // Notification Permission Launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
                    } else {
                        android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission denied")
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    try {
                        com.onesignal.OneSignal.Notifications.requestPermission(false)
                    } catch (e: Exception) {
                        android.util.Log.d("MainActivity", "OneSignal requestPermission: ${e.localizedMessage}")
                    }
                }

                // OneSignal Notification Deep Linking Handler
                val deepLinkRoute by SmartPOSApplication.deepLinkRoute.collectAsState()
                LaunchedEffect(deepLinkRoute) {
                    deepLinkRoute?.let { targetRoute ->
                        if (targetRoute.isNotBlank()) {
                            android.util.Log.d("MainActivity", "Executing Deep Link Navigation to: $targetRoute")
                            try {
                                navController.navigate(targetRoute) {
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Deep Link navigation error: ${e.localizedMessage}")
                            }
                            SmartPOSApplication.consumeDeepLinkRoute()
                        }
                    }
                }

                val currentUser by viewModel.currentUser.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current
                var sessionAccessState by remember {
                    mutableStateOf<SessionAccessState>(SessionAccessState.Granted)
                }

                // Subscription State Verification & Auto-Lock on App Launch and Foreground Resume
                DisposableEffect(lifecycleOwner, currentUser) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                            if (currentUser != null) {
                                val state = AppSessionManager.verifyAndEnforceSubscriptionLock(
                                    context = context,
                                    userUid = currentUser?.mobileNumber ?: ""
                                )
                                sessionAccessState = state
                                if (state is SessionAccessState.Locked) {
                                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                                    if (currentRoute != Screen.Paywall.route && currentRoute != Screen.Login.route && currentRoute != Screen.ProfileSetup.route) {
                                        navController.navigate(Screen.Paywall.route) {
                                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // RemoteConfig App Update Checker (config/app_settings)
                var appUpdateInfo by remember { mutableStateOf<com.example.update.AppUpdateInfo?>(null) }
                var showUpdateDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        val updateInfo = com.example.service.RemoteConfigService.checkForAppUpdate(com.example.BuildConfig.VERSION_CODE)
                        if (updateInfo.isUpdateAvailable && updateInfo.latestVersionCode > com.example.BuildConfig.VERSION_CODE) {
                            appUpdateInfo = updateInfo
                            showUpdateDialog = true
                        } else {
                            appUpdateInfo = updateInfo
                            showUpdateDialog = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Remote update checker error: ${e.localizedMessage}")
                    }
                }

                // Reactive notification dispatch (Toasts)
                LaunchedEffect(key1 = true) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                val currentUpdateInfo = appUpdateInfo
                if (showUpdateDialog && currentUpdateInfo != null) {
                    com.example.ui.components.AppUpdateDialog(
                        updateInfo = currentUpdateInfo,
                        onUpdateNow = {
                            com.example.service.RemoteConfigService.openPlayStore(
                                context = context,
                                customUrl = currentUpdateInfo.downloadUrl
                            )
                            if (!currentUpdateInfo.isForceUpdate) {
                                showUpdateDialog = false
                            }
                        },
                        onLater = {
                            if (!currentUpdateInfo.isForceUpdate) {
                                showUpdateDialog = false
                            }
                        }
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateToDashboard = {
                                val userUid = viewModel.currentUser.value?.mobileNumber ?: ""
                                val state = AppSessionManager.verifyAndEnforceSubscriptionLock(
                                    context = context,
                                    userUid = userUid
                                )
                                if (state is SessionAccessState.Locked) {
                                    navController.navigate(Screen.Paywall.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            },
                            onNavigate = { route ->
                                val userUid = viewModel.currentUser.value?.mobileNumber ?: ""
                                val targetRoute = if (route == Screen.Dashboard.route) {
                                    val state = AppSessionManager.verifyAndEnforceSubscriptionLock(
                                        context = context,
                                        userUid = userUid
                                    )
                                    if (state is SessionAccessState.Locked) Screen.Paywall.route else Screen.Dashboard.route
                                } else route

                                navController.navigate(targetRoute) {
                                    if (targetRoute == Screen.Dashboard.route || targetRoute == Screen.Paywall.route || targetRoute == Screen.ProfileSetup.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Signup.route) {
                        // Redirect obsolete signup route to our unified, high-security phone & Google login
                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateToDashboard = {
                                val userUid = viewModel.currentUser.value?.mobileNumber ?: ""
                                val state = AppSessionManager.verifyAndEnforceSubscriptionLock(
                                    context = context,
                                    userUid = userUid
                                )
                                if (state is SessionAccessState.Locked) {
                                    navController.navigate(Screen.Paywall.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            },
                            onNavigate = { route ->
                                val userUid = viewModel.currentUser.value?.mobileNumber ?: ""
                                val targetRoute = if (route == Screen.Dashboard.route) {
                                    val state = AppSessionManager.verifyAndEnforceSubscriptionLock(
                                        context = context,
                                        userUid = userUid
                                    )
                                    if (state is SessionAccessState.Locked) Screen.Paywall.route else Screen.Dashboard.route
                                } else route

                                navController.navigate(targetRoute) {
                                    if (targetRoute == Screen.Dashboard.route || targetRoute == Screen.Paywall.route || targetRoute == Screen.ProfileSetup.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.ProfileSetup.route) {
                        com.example.ui.screens.profile.ProfileSetupScreen(
                            viewModel = viewModel,
                            onSetupSuccess = {
                                navController.navigate(Screen.Paywall.route) {
                                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            val uid = viewModel.currentUser.value?.mobileNumber
                                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            com.example.service.MyFirebaseMessagingService.syncFcmTokenToFirestore(uid)
                        }

                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.HOME,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Admin.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.HOME,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Udhar.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.UDHAR,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Products.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.INVENTORY,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.CreateBill.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.POS,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.History.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.HISTORY,
                            onLogout = {
                                viewModel.logout(context = context) {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Paywall.route) {
                        val userUid = currentUser?.mobileNumber ?: ""
                        val accessState = AppSessionManager.verifyAndEnforceSubscriptionLock(
                            context = context,
                            userUid = userUid
                        )
                        val isLocked = accessState is SessionAccessState.Locked
                        val isMandatory = isLocked || (navController.previousBackStackEntry?.destination?.route == Screen.ProfileSetup.route)
                        val lockReason = (accessState as? SessionAccessState.Locked)?.reason
                            ?: "Mandatory Autopay ₹1 Trial Setup required to activate SmartPOS features."

                        val navigateToDashboard = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Paywall.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }

                        com.example.ui.screens.paywall.PaywallScreen(
                            viewModel = viewModel,
                            onBack = {
                                val isPro = viewModel.subscriptionState.value.isProUser ||
                                        viewModel.subscriptionState.value.autoPayMandateStatus == "ACTIVE" ||
                                        viewModel.subscriptionState.value.autoPayMandateStatus == "TRIAL_ACTIVE"
                                if (isPro) {
                                    navigateToDashboard()
                                } else if (!isMandatory) {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        navigateToDashboard()
                                    }
                                }
                            },
                            onNavigateToDashboard = navigateToDashboard,
                            isMandatory = isMandatory,
                            lockReason = lockReason
                        )
                    }
                }
            }
        }
    }

    /**
     * Razorpay Payment Result Callback on Payment Success.
     * Updates user Firestore profile (isProUser = true, subscriptionStatus = "ACTIVE"),
     * persists state locally in SharedPreferences, shows Toast, and navigates to Dashboard.
     */
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: "pay_success"
        val mandateId = paymentData?.orderId
            ?.takeIf { it.isNotBlank() }
            ?: "MND-RZP-$paymentId"

        val userUid = FirebaseManager.auth?.currentUser?.uid
            ?: FirebaseManager.auth?.currentUser?.phoneNumber
            ?: ""

        PaymentGatewayConfig.handlePaymentSuccess(
            context = this,
            userUid = userUid,
            razorpayPaymentId = mandateId,
            paymentData = paymentData,
            onComplete = {
                runOnUiThread {
                    Toast.makeText(this, "Subscription Activated! 🎉", Toast.LENGTH_LONG).show()
                    navControllerRef?.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Paywall.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        )
    }

    /**
     * Razorpay Payment Result Callback on Payment Error.
     * Displays a clear error message Toast allowing the user to retry.
     */
    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val errorMsg = response ?: "Payment cancelled or authorization failed"
        Toast.makeText(this, "Payment Error ($code): $errorMsg", Toast.LENGTH_LONG).show()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val extras = intent?.extras ?: return
        val rawRoute = extras.getString("screen_route")
            ?: extras.getString("route")
            ?: extras.getString("target_screen")
            ?: extras.getString("screen")

        if (!rawRoute.isNullOrBlank()) {
            val resolvedRoute = SmartPOSApplication.mapRoute(rawRoute)
            SmartPOSApplication.setDeepLinkRoute(resolvedRoute)
        }
    }
}
