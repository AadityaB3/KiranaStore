package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.OrderEntity
import com.example.data.db.ProductEntity
import com.example.data.db.UserEntity
import com.example.viewmodel.KiranaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboard(
    viewModel: KiranaViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var currentTab by remember { mutableStateOf("STATS") } // STATS, ORDERS, INVENTORY

    // Automatically refresh stats on enter
    LaunchedEffect(Unit) {
        viewModel.refreshAdminStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kirana Admin Desk", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Logged in: ${currentUser?.name}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }, modifier = Modifier.testTag("admin_logout")) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("admin_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == "STATS",
                    onClick = { 
                        currentTab = "STATS"
                        viewModel.refreshAdminStats()
                    },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = null) },
                    label = { Text("Stats") },
                    modifier = Modifier.testTag("nav_admin_stats")
                )
                NavigationBarItem(
                    selected = currentTab == "ORDERS",
                    onClick = { currentTab = "ORDERS" },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    label = { Text("Orders") },
                    modifier = Modifier.testTag("nav_admin_orders")
                )
                NavigationBarItem(
                    selected = currentTab == "INVENTORY",
                    onClick = { currentTab = "INVENTORY" },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    label = { Text("Products") },
                    modifier = Modifier.testTag("nav_admin_inventory")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "STATS" -> AdminStatsTab(viewModel)
                "ORDERS" -> AdminOrdersTab(viewModel)
                "INVENTORY" -> AdminInventoryTab(viewModel)
            }
        }
    }
}

// ==================== OWNER TABS ====================

@Composable
fun AdminStatsTab(viewModel: KiranaViewModel) {
    val stats by viewModel.adminStats.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var campaignText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Store Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Store status open/closed toggle switch (CRITICAL FEATURE FOR CUSTOMER INTERACTION)
        val isOpen = storeSettings?.isOpen ?: true
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isOpen) "🟢 Store is Open" else "🔴 Store is Closed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isOpen) "Customers can order items" else "Checkout locked for all customers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOpen,
                    onCheckedChange = { viewModel.toggleStoreOpen(it) },
                    modifier = Modifier.testTag("store_toggle_switch")
                )
            }
        }

        // Send Push Campaign Message to All Customers
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Broadcast Promotion / Announcement 📢", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Broadcast updates, discounts, or delays instantly to all registered customers.", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = campaignText,
                    onValueChange = { campaignText = it },
                    placeholder = { Text("Enter campaign text, e.g. Monsoon Sale: 15% OFF on groceries!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (campaignText.trim().isNotEmpty()) {
                            viewModel.pushCampaignAnnouncement(campaignText.trim())
                            campaignText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).testTag("broadcast_campaign_btn")
                ) {
                    Text("Push Notification")
                }
            }
        }

        Text("Analytical Highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Grid stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Today's Revenue", value = "₹${stats?.todaySales ?: 0.0}", icon = Icons.Default.TrendingUp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatCard(title = "Total Gross", value = "₹${stats?.totalRevenue ?: 0.0}", icon = Icons.Default.CurrencyRupee, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Total Bookings", value = "${stats?.totalOrders ?: 0}", icon = Icons.Default.Receipt, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
            StatCard(title = "Low Stock Alerts", value = "${stats?.lowStockCount ?: 0} Items", icon = Icons.Default.Warning, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(title = "Active Customers", value = "${stats?.totalCustomers ?: 0}", icon = Icons.Default.Group, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatCard(title = "Pending Bookings", value = "${stats?.pendingOrders ?: 0}", icon = Icons.Default.PendingActions, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = color)
        }
    }
}

@Composable
fun AdminOrdersTab(viewModel: KiranaViewModel) {
    val orders by viewModel.orders.collectAsState()
    val deliveryPartners by viewModel.deliveryPartners.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Order Management Workflow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Accept, prepare, and allocate rider shipments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders placed yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("admin_orders_list")) {
                items(orders) { ord ->
                    OwnerOrderCard(ord, deliveryPartners, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOrderCard(order: OrderEntity, riders: List<UserEntity>, viewModel: KiranaViewModel) {
    val items by viewModel.getOrderItemsFlow(order.id).collectAsState(emptyList())
    var showAssignDropdown by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order ID: #${order.id}", fontWeight = FontWeight.Bold)
                Text(
                    text = order.status,
                    fontWeight = FontWeight.Bold,
                    color = when (order.status) {
                        "PENDING" -> Color(0xFFF59E0B)
                        "ACCEPTED" -> Color(0xFF3B82F6)
                        "PREPARING" -> Color(0xFF8B5CF6)
                        "OUT_FOR_DELIVERY" -> Color(0xFFEC4899)
                        "DELIVERED" -> Color(0xFF10B981)
                        else -> Color.Red
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Address: ${order.addressText}", fontSize = 11.sp)
            Text("Billing Value: ₹${order.totalAmount} (${order.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(6.dp))
            Text("Cart Items:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            items.forEach { item ->
                Text("• ${item.productName} (Qty ${item.quantity})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Workflows (PENDING -> ACCEPTED -> PREPARING -> Assign Delivery -> OUT_FOR_DELIVERY -> DELIVERED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    "PENDING" -> {
                        Button(
                            onClick = { viewModel.updateOrderStatus(order.id, "ACCEPTED") },
                            modifier = Modifier.weight(1f).testTag("accept_order_${order.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Accept Order")
                        }
                        OutlinedButton(
                            onClick = { viewModel.updateOrderStatus(order.id, "CANCELLED") },
                            modifier = Modifier.weight(1f).testTag("cancel_order_${order.id}"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject")
                        }
                    }
                    "ACCEPTED" -> {
                        Button(
                            onClick = { viewModel.updateOrderStatus(order.id, "PREPARING") },
                            modifier = Modifier.fillMaxWidth().testTag("prepare_order_${order.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Start Packing (Preparing)")
                        }
                    }
                    "PREPARING" -> {
                        // Allocation of Delivery partner
                        Button(
                            onClick = { showAssignDropdown = true },
                            modifier = Modifier.fillMaxWidth().testTag("assign_rider_btn_${order.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                        ) {
                            Text("Assign Delivery Partner")
                        }
                    }
                    "OUT_FOR_DELIVERY" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Rider ${order.deliveryPartnerId ?: ""} is out for delivery. Waiting for OTP.",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    "DELIVERED" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delivered successfully. OTP Verified.", fontSize = 11.sp)
                            }
                        }
                    }
                    "CANCELLED" -> {
                        Text("Order was Cancelled", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Dropdown Dialog for driver selection
    if (showAssignDropdown) {
        Dialog(onDismissRequest = { showAssignDropdown = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Delivery Partner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (riders.isEmpty()) {
                        Text("No registered delivery partners found.")
                    } else {
                        for (rider in riders) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.assignDeliveryPartner(order.id, rider.id)
                                        showAssignDropdown = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(rider.name, fontWeight = FontWeight.SemiBold)
                                    Text("Phone: ${rider.phone}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showAssignDropdown = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminInventoryTab(viewModel: KiranaViewModel) {
    val products by viewModel.adminProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Inventory Catalog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Manage stock quantities and active state", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { showAddForm = true }, modifier = Modifier.testTag("add_product_fab")) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No products in inventory.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("inventory_list")) {
                items(products) { prod ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold)
                                Text("Price: ₹${prod.price} | Stock: ${prod.stockQuantity} (${prod.unit})", fontSize = 12.sp)
                                if (prod.stockQuantity <= 5) {
                                    Text("⚠️ Low Stock Warning!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            // Active deactivation toggle switch
                            Switch(
                                checked = prod.isActive,
                                onCheckedChange = { viewModel.toggleProductActive(prod) },
                                modifier = Modifier.testTag("product_active_switch_${prod.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddForm) {
        AddProductDialog(
            categories = categories,
            onDismiss = { showAddForm = false },
            onSubmit = { name, catId, desc, price, unit, stock ->
                viewModel.saveProduct(name, catId, desc, price, unit, stock)
                showAddForm = false
            }
        )
    }
}

@Composable
fun AddProductDialog(
    categories: List<com.example.data.db.CategoryEntity>,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, String, Double, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1) }
    var desc by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("1 kg") }
    var stockStr by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("add_product_form_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add New Catalog Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // Category Selection Trigger
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCatId }?.name ?: "Select Category",
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCatId = cat.id
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Detailed Description") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price in ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Selling Unit (e.g. 1 kg, 500 ml)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Starting Stock Quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0
                            if (name.isNotEmpty() && price > 0 && stock >= 0) {
                                onSubmit(name, selectedCatId, desc, price, unit, stock)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_product_btn")
                    ) {
                        Text("Add Item")
                    }
                }
            }
        }
    }
}
