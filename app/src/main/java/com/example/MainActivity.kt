package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExpenseViewModel
import com.example.ui.MainScreen
import com.example.ui.MainTab
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper
import com.example.widget.ExpenseWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldOpenAddTx = intent?.getBooleanExtra(ExpenseWidgetProvider.EXTRA_OPEN_ADD_TRANSACTION, false) ?: false
        val openTabExtra = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)
        val initialTab = if (openTabExtra == NotificationHelper.TAB_EMIS) MainTab.EMIS else MainTab.OVERVIEW

        setContent {
            val viewModel: ExpenseViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            // Request Notification Permission on Android 13+
            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.checkEmiReminders()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MyApplicationTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        initialTab = initialTab,
                        initialOpenAddTransaction = shouldOpenAddTx
                    )
                }
            }
        }
    }
}
