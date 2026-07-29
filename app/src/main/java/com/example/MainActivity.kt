package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.VolumeAssistantTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        mainViewModel.refreshState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()

        setContent {
            val userPrefs by mainViewModel.userPreferences.collectAsState()

            VolumeAssistantTheme(themeMode = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Check intent for navigation direct target (e.g. from service notification or popup)
                    val initialDestination = if (intent?.getStringExtra("navigate_to") == "settings") "settings" else "dashboard"

                    NavHost(
                        navController = navController,
                        startDestination = initialDestination
                    ) {
                        composable(
                            route = "dashboard",
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() }
                        ) {
                            DashboardScreen(
                                viewModel = mainViewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onRequestNotificationPermission = { requestNotificationPermission() }
                            )
                        }

                        composable(
                            route = "settings",
                            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                        ) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val windowParams = window.attributes
            try {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay.supportedModes
                val highRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (highRefreshMode != null) {
                    windowParams.preferredDisplayModeId = highRefreshMode.modeId
                }
                @Suppress("DEPRECATION")
                windowParams.preferredRefreshRate = 120f
                window.attributes = windowParams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
