package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerDashboard
import com.example.ui.screens.DeliveryDashboard
import com.example.ui.screens.OwnerDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.KiranaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: KiranaViewModel = viewModel()
                val currentUser by viewModel.currentUser.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (currentUser == null) {
                        AuthScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        when (currentUser?.role) {
                            "CUSTOMER" -> CustomerDashboard(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            "OWNER" -> OwnerDashboard(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            "DELIVERY" -> DeliveryDashboard(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            else -> AuthScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
