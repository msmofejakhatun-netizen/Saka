package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseManager
import com.example.data.repository.BillingRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.admin.AdminScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.BillingViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase
        FirebaseManager.initialize(this)

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

                // Instantiate the unified ViewModel using our Factory
                val viewModel: BillingViewModel = viewModel(
                    factory = BillingViewModelFactory(repository)
                )

                // Reactive notification dispatch (Toasts)
                LaunchedEffect(key1 = true) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
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
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    if (route == Screen.Dashboard.route || route == Screen.ProfileSetup.route) {
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
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    if (route == Screen.Dashboard.route || route == Screen.ProfileSetup.route) {
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
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            initialTab = com.example.ui.screens.dashboard.BottomTab.HOME,
                            onLogout = {
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
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
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
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
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
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
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
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
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
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
                                viewModel.logout {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
