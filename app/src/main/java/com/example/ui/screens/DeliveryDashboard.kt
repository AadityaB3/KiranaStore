package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.OrderEntity
import com.example.viewmodel.KiranaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboard(
    viewModel: KiranaViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val unassignedOrders by viewModel.unassignedOrders.collectAsState()

    var isOnline by remember { mutableStateOf(true) }
    var activeVerifyOrder by remember { mutableStateOf<OrderEntity?>(null) }
    val otpVerificationSuccess by viewModel.otpVerificationSuccess.collectAsState()
    val context = LocalContext.current

    // Trigger otp error or success toasts
    LaunchedEffect(otpVerificationSuccess) {
        if (otpVerificationSuccess == true) {
            activeVerifyOrder = null
            viewModel.resetOtpVerification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kirana Delivery Desk", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Rider: ${currentUser?.name ?: "Delivery Partner"}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }, modifier = Modifier.testTag("delivery_logout")) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rider Profile & Earnings Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Weekly Rider Earnings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₹${currentUser?.walletBalance ?: 0.0}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            // Online/Offline status switch
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isOnline) "🟢 ONLINE" else "🔴 OFFLINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Switch(
                                    checked = isOnline,
                                    onCheckedChange = { isOnline = it },
                                    modifier = Modifier.testTag("rider_availability_switch")
                                )
                            }
                        }
                    }
                }
            }

            // Unassigned shipments pool (Riders can instantly pick up and self-assign shipments!)
            if (isOnline) {
                item {
                    Text(
                        text = "📦 Unassigned Shipments Pool",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (unassignedOrders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "All store shipments are currently allocated. Check back soon!",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(unassignedOrders) { ord ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pre-packaged Order #${ord.id}", fontWeight = FontWeight.Bold)
                                    Text("Payout: ₹40", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Address: ${ord.addressText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Billing: ₹${ord.totalAmount} (${ord.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.assignDeliveryPartner(ord.id, currentUser?.id ?: 0) },
                                    modifier = Modifier.fillMaxWidth().testTag("self_assign_shipment_${ord.id}")
                                ) {
                                    Text("Accept Delivery Task")
                                }
                            }
                        }
                    }
                }
            }

            // Active Assignments list
            item {
                Text(
                    text = "🏍️ My Assigned Shipments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            val assignedList = orders.filter { it.status == "OUT_FOR_DELIVERY" || it.status == "DELIVERED" }

            if (assignedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active delivery assignments. Go online or look in the pool!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                items(assignedList) { ord ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shipment #${ord.id}", fontWeight = FontWeight.Bold)
                                Text(
                                    text = ord.status,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ord.status == "DELIVERED") Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Delivery Address:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(ord.addressText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Total Amount to Collect: ", fontSize = 11.sp)
                            Text(
                                text = if (ord.paymentMethod == "COD") "₹${ord.totalAmount} (CASH ON DELIVERY)" else "₹0.0 (PAID ONLINE VIA WALLET)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (ord.paymentMethod == "COD") Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                            )

                            if (ord.note != null && ord.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Customer Instruction: ${ord.note}", fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Contact Helper Button
                                IconButton(
                                    onClick = {
                                        // Open helper dialer intent
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:9876543210"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .size(48.dp)
                                        .testTag("call_customer_btn_${ord.id}")
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Map navigation helper
                                IconButton(
                                    onClick = {
                                        // Open maps navigation intent
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(ord.addressText)}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            // Fallback web navigation
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(ord.addressText)}"))
                                            context.startActivity(webIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                        .size(48.dp)
                                        .testTag("navigate_map_btn_${ord.id}")
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Navigate Map", tint = MaterialTheme.colorScheme.primary)
                                }

                                if (ord.status == "OUT_FOR_DELIVERY") {
                                    Button(
                                        onClick = { activeVerifyOrder = ord },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("verify_otp_trigger_${ord.id}")
                                    ) {
                                        Text("Complete (Verify OTP)")
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            "Delivered Successfully 🎉",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // OTP Verification Code Pop-Up Dialog (CRITICAL SECURITY COMPONENT)
    activeVerifyOrder?.let { ord ->
        var inputOtp by remember { mutableStateOf("") }
        var isOtpError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { activeVerifyOrder = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("otp_dialog_${ord.id}")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Verify Customer OTP 🔑",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ask the customer for the 4-digit verification code displayed on their active Order Tracker card.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputOtp,
                        onValueChange = { 
                            inputOtp = it
                            isOtpError = false
                        },
                        placeholder = { Text("Enter 4-Digit OTP") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 4.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input_field")
                    )

                    if (isOtpError || otpVerificationSuccess == false) {
                        Text(
                            text = "❌ Invalid OTP code. Please try again.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                activeVerifyOrder = null
                                viewModel.resetOtpVerification()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { 
                                if (inputOtp.length == 4) {
                                    viewModel.verifyOtpAndCompleteDelivery(ord.id, inputOtp)
                                } else {
                                    isOtpError = true
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_otp_btn")
                        ) {
                            Text("Verify Code")
                        }
                    }
                }
            }
        }
    }
}
