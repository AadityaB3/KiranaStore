package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.*
import com.example.viewmodel.KiranaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    viewModel: KiranaViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    
    var currentTab by remember { mutableStateOf("SHOP") } // SHOP, WISHLIST, CART, ORDERS, PROFILE
    
    val cartItems by viewModel.cartItems.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Kirana",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Welcome, ${currentUser?.name ?: "Customer"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                },
                actions = {
                    // Wallet Balance Quick View
                    AssistChip(
                        onClick = { currentTab = "PROFILE" },
                        label = { Text("₹${currentUser?.walletBalance ?: 0.0}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("wallet_balance_chip")
                    )

                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("customer_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == "SHOP",
                    onClick = { currentTab = "SHOP" },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text("Shop") },
                    modifier = Modifier.testTag("nav_shop")
                )
                NavigationBarItem(
                    selected = currentTab == "WISHLIST",
                    onClick = { currentTab = "WISHLIST" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Wishlist") },
                    modifier = Modifier.testTag("nav_wishlist")
                )
                NavigationBarItem(
                    selected = currentTab == "CART",
                    onClick = { currentTab = "CART" },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text(cartCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        }
                    },
                    label = { Text("Cart") },
                    modifier = Modifier.testTag("nav_cart")
                )
                NavigationBarItem(
                    selected = currentTab == "ORDERS",
                    onClick = { currentTab = "ORDERS" },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Orders") },
                    modifier = Modifier.testTag("nav_orders")
                )
                NavigationBarItem(
                    selected = currentTab == "PROFILE",
                    onClick = { currentTab = "PROFILE" },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Account") },
                    modifier = Modifier.testTag("nav_profile")
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
                "SHOP" -> ShopTab(viewModel)
                "WISHLIST" -> WishlistTab(viewModel)
                "CART" -> CartTab(viewModel, onCheckoutSuccess = { currentTab = "ORDERS" })
                "ORDERS" -> OrdersTab(viewModel)
                "PROFILE" -> ProfileTab(viewModel)
            }
        }
    }
}

// ==================== TABS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTab(viewModel: KiranaViewModel) {
    val categories by viewModel.categories.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val products by viewModel.products.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var activeProductDetail by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Real-time Store Status Banner
        val isOpen = storeSettings?.isOpen ?: true
        val bannerBg = if (isOpen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer
        val bannerText = if (isOpen) "🟢 Store is Open • Super Fast Instant Delivery!" else "🔴 Shop Closed • We are not accepting orders currently"
        
        Surface(
            color = bannerBg,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_status_banner")
        ) {
            Text(
                text = bannerText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search fresh apples, basmati rice, milk...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("product_search_input")
        )

        // Category Scrolling List
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("categories_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCatId == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("All Products") },
                    leadingIcon = if (selectedCatId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("category_chip_all")
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCatId == cat.id,
                    onClick = { viewModel.selectCategory(cat.id) },
                    label = { Text(cat.name) },
                    leadingIcon = if (selectedCatId == cat.id) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        {
                            val iconVec = when (cat.iconName) {
                                "eco" -> Icons.Default.Eco
                                "egg" -> Icons.Default.Egg
                                "grass" -> Icons.Default.Grass
                                "local_drink" -> Icons.Default.LocalDrink
                                "cookie" -> Icons.Default.Cookie
                                "cleaning_services" -> Icons.Default.CleaningServices
                                else -> Icons.Default.ShoppingBag
                            }
                            Icon(iconVec, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.testTag("category_chip_${cat.id}")
                )
            }
        }

        // Product Grid
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products found matching filters", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("products_grid"),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products) { prod ->
                    ProductCard(
                        product = prod,
                        viewModel = viewModel,
                        onClick = { activeProductDetail = prod }
                    )
                }
            }
        }
    }

    // Product Details Dialog
    activeProductDetail?.let { prod ->
        ProductDetailDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { activeProductDetail = null }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    viewModel: KiranaViewModel,
    onClick: () -> Unit
) {
    val isFavorite by viewModel.isFavoriteFlow(product.id).collectAsState(false)
    val cartItems by viewModel.cartItems.collectAsState()
    val cartQty = cartItems.find { it.productId == product.id }?.quantity ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Display placeholder graphic/icon based on category
                val iconVec = when (product.imageResName) {
                    "apples" -> Icons.Default.Eco
                    "bananas" -> Icons.Default.Eco
                    "potatoes" -> Icons.Default.Eco
                    "milk" -> Icons.Default.Egg
                    "butter" -> Icons.Default.Egg
                    "bread" -> Icons.Default.Egg
                    "rice" -> Icons.Default.Grass
                    "dal" -> Icons.Default.Grass
                    "oil" -> Icons.Default.Grass
                    "tea" -> Icons.Default.LocalDrink
                    "orange_juice" -> Icons.Default.LocalDrink
                    "cookies" -> Icons.Default.Cookie
                    "chips" -> Icons.Default.Cookie
                    "dishwash" -> Icons.Default.CleaningServices
                    else -> Icons.Default.ShoppingBag
                }

                Icon(
                    imageVector = iconVec,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Favorite Toggle Button
                IconButton(
                    onClick = { viewModel.toggleWishlist(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag("wishlist_toggle_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Low stock visual badge
                if (product.stockQuantity <= 5 && product.stockQuantity > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(topEnd = 8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Only ${product.stockQuantity} Left",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (product.stockQuantity == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.unit,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${product.price}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Add / Quantity Controls
                    if (product.stockQuantity > 0) {
                        if (cartQty > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartQty(product.id, cartQty - 1) },
                                    modifier = Modifier.size(24.dp).testTag("decrease_qty_${product.id}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = cartQty.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQty(product.id, cartQty + 1) },
                                    modifier = Modifier.size(24.dp).testTag("increase_qty_${product.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.addToCart(product.id, 1) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .testTag("add_to_cart_btn_${product.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: ProductEntity,
    viewModel: KiranaViewModel,
    onDismiss: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartQty = cartItems.find { it.productId == product.id }?.quantity ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("product_detail_dialog_${product.id}"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Unit: ${product.unit} | Price: ₹${product.price}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Stock status: ${if (product.stockQuantity > 0) "In Stock (${product.stockQuantity} units available)" else "Out of Stock"}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (product.stockQuantity > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (product.stockQuantity > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quantity in Cart:", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.updateCartQty(product.id, cartQty - 1) },
                                shape = RoundedCornerShape(8.dp),
                                enabled = cartQty > 0
                            ) {
                                Text("-")
                            }
                            Text(
                                text = cartQty.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = { viewModel.updateCartQty(product.id, cartQty + 1) },
                                shape = RoundedCornerShape(8.dp),
                                enabled = cartQty < product.stockQuantity
                            ) {
                                Text("+")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Shop")
                }
            }
        }
    }
}

@Composable
fun WishlistTab(viewModel: KiranaViewModel) {
    val wishlistItems by viewModel.wishlistItems.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Wishlist",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (wishlistItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your wishlist is empty.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("wishlist_list")
            ) {
                items(wishlistItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("₹${item.price} / ${item.unit}", fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.toggleWishlist(item.productId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.addToCart(item.productId, 1) },
                                enabled = item.stockQuantity > 0,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(if (item.stockQuantity > 0) "Add Cart" else "Out Stock", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartTab(
    viewModel: KiranaViewModel,
    onCheckoutSuccess: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartSubtotal by viewModel.cartTotal.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val addresses by viewModel.getAddressesFlow().collectAsState(emptyList())

    val deliveryCharge = storeSettings?.deliveryCharge ?: 20.0
    val minAmt = storeSettings?.minOrderAmount ?: 100.0
    val totalAmount = cartSubtotal + deliveryCharge

    var selectedPaymentMethod by remember { mutableStateOf("COD") } // COD, WALLET
    var selectedAddressText by remember { mutableStateOf("") }
    var orderNote by remember { mutableStateOf("") }

    val checkoutState by viewModel.checkoutState.collectAsState()

    // Auto select default address
    LaunchedEffect(addresses) {
        val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
        if (defaultAddr != null) {
            selectedAddressText = "${defaultAddr.title}: ${defaultAddr.addressLine}, ${defaultAddr.city} (${defaultAddr.postalCode})"
        }
    }

    LaunchedEffect(checkoutState) {
        if (checkoutState is KiranaViewModel.CheckoutUiState.Success) {
            onCheckoutSuccess()
            viewModel.resetCheckoutState()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Shopping Cart",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your cart is empty.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("cart_items_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("₹${item.price} • ${item.unit}", fontSize = 12.sp)
                                Text("Total: ₹${item.price * item.quantity}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.updateCartQty(item.productId, item.quantity - 1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                }
                                Text(item.quantity.toString(), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.updateCartQty(item.productId, item.quantity + 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                                IconButton(onClick = { viewModel.removeFromCart(item.cartItemId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Delivery Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Address Dropdown Select or Add address link
                    if (addresses.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Please add a delivery address in the Account tab first!",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        // Display default chosen address
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delivering To:", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(selectedAddressText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Order note
                    OutlinedTextField(
                        value = orderNote,
                        onValueChange = { orderNote = it },
                        label = { Text("Add delivery instruction / notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ElevatedCard(
                            onClick = { selectedPaymentMethod = "COD" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == "COD") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("payment_cod")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = null)
                                Text("Cash on Delivery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ElevatedCard(
                            onClick = { selectedPaymentMethod = "WALLET" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == "WALLET") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("payment_wallet")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                Text("Store Wallet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkout billing summery
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cart Subtotal:")
                                Text("₹$cartSubtotal")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Charges:")
                                Text("₹$deliveryCharge")
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total:", fontWeight = FontWeight.Bold)
                                Text("₹$totalAmount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkout status warnings
                    if (cartSubtotal < minAmt) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Minimum order amount is ₹$minAmt to place order. Add more items to checkout.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else if (storeSettings?.isOpen != true) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🚫 Store is CLOSED. Checkout is locked. Try again later during shop hours.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (checkoutState is KiranaViewModel.CheckoutUiState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (checkoutState as KiranaViewModel.CheckoutUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.checkoutOrder(selectedAddressText, selectedPaymentMethod, orderNote) },
                        enabled = cartSubtotal >= minAmt && (storeSettings?.isOpen == true) && addresses.isNotEmpty() && checkoutState != KiranaViewModel.CheckoutUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_order_btn")
                    ) {
                        if (checkoutState == KiranaViewModel.CheckoutUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Place Order via ${if (selectedPaymentMethod == "COD") "COD" else "Wallet"}", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun OrdersTab(viewModel: KiranaViewModel) {
    val orders by viewModel.orders.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Order History",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You haven't placed any orders yet.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("orders_list")
            ) {
                items(orders) { ord ->
                    OrderTrackerCard(ord, viewModel)
                }
            }
        }
    }
}

@Composable
fun OrderTrackerCard(order: OrderEntity, viewModel: KiranaViewModel) {
    val items by viewModel.getOrderItemsFlow(order.id).collectAsState(emptyList())
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                // Distinct Status chips
                val statusColor = when (order.status) {
                    "PENDING" -> Color(0xFFF59E0B) // Gold
                    "ACCEPTED" -> Color(0xFF3B82F6) // Blue
                    "PREPARING" -> Color(0xFF8B5CF6) // Purple
                    "OUT_FOR_DELIVERY" -> Color(0xFFEC4899) // Coral
                    "DELIVERED" -> Color(0xFF10B981) // Green
                    "CANCELLED" -> Color(0xFFEF4444) // Red
                    else -> MaterialTheme.colorScheme.secondary
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(order.status) },
                    colors = SuggestionChipDefaults.suggestionChipColors(labelColor = statusColor),
                    modifier = Modifier.testTag("order_status_chip_${order.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Date: ${java.text.DateFormat.getDateTimeInstance().format(order.orderDate)}", fontSize = 12.sp)
            Text("Amount Paid: ₹${order.totalAmount} via ${order.paymentMethod}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Delivery Address: ${order.addressText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            // Delivery OTP - VERY IMPORTANT FOR OTP VERIFICATION SECURITY
            if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rider Delivery Verification OTP: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.otpCode,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("order_otp_code_${order.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dropdown expand to see invoice items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Invoice Items ▲" else "View Invoice Items ▼",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Invoice Bill Items:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${item.productName} (x${item.quantity})", fontSize = 12.sp)
                            Text("₹${item.priceAtPurchase * item.quantity}", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileTab(viewModel: KiranaViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val addresses by viewModel.getAddressesFlow().collectAsState(emptyList())
    val notifications by viewModel.notifications.collectAsState()

    var activeFundsAmount by remember { mutableStateOf("") }
    
    // Address fields
    var addressTitle by remember { mutableStateOf("") }
    var addressLine by remember { mutableStateOf("") }
    var addressCity by remember { mutableStateOf("") }
    var addressZip by remember { mutableStateOf("") }

    var addressFormVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "My Profile & Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Wallet Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Store Wallet Balance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "₹${currentUser?.walletBalance ?: 0.0}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = activeFundsAmount,
                            onValueChange = { activeFundsAmount = it },
                            placeholder = { Text("Amount in ₹") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("wallet_fund_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = activeFundsAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.addWalletFunds(amt)
                                    activeFundsAmount = ""
                                }
                            },
                            modifier = Modifier.height(50.dp).testTag("add_funds_btn")
                        ) {
                            Text("Add Cash")
                        }
                    }
                }
            }
        }

        // Addresses Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Delivery Addresses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { addressFormVisible = !addressFormVisible }) {
                    Icon(
                        imageVector = if (addressFormVisible) Icons.Default.Close else Icons.Default.AddLocation,
                        contentDescription = "Add Address"
                    )
                }
            }
        }

        if (addressFormVisible) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New Address", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = addressTitle, onValueChange = { addressTitle = it }, label = { Text("Title (e.g. Home, Office)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = addressLine, onValueChange = { addressLine = it }, label = { Text("Street Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = addressCity, onValueChange = { addressCity = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = addressZip, onValueChange = { addressZip = it }, label = { Text("Postal Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (addressTitle.isNotEmpty() && addressLine.isNotEmpty() && addressCity.isNotEmpty() && addressZip.isNotEmpty()) {
                                    viewModel.saveAddress(addressTitle, addressLine, addressCity, addressZip, addresses.isEmpty())
                                    addressTitle = ""
                                    addressLine = ""
                                    addressCity = ""
                                    addressZip = ""
                                    addressFormVisible = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_address_btn")
                        ) {
                            Text("Save Address")
                        }
                    }
                }
            }
        }

        if (addresses.isEmpty()) {
            item {
                Text("No addresses saved. Add one to checkout cart.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            items(addresses) { addr ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = addr.isDefault,
                            onClick = { viewModel.setDefaultAddress(addr.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${addr.title} ${if (addr.isDefault) "⭐ (Default)" else ""}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("${addr.addressLine}, ${addr.city} (${addr.postalCode})", fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.deleteAddress(addr.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Notification Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications & Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                    Text("Clear All")
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Text("No notifications.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            items(notifications) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (!notif.isRead) "NEW" else "",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
