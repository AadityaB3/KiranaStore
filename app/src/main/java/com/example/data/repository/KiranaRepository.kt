package com.example.data.repository

import android.content.Context
import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.random.Random

class KiranaRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val addressDao = db.addressDao()
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()
    private val wishlistDao = db.wishlistDao()
    private val orderDao = db.orderDao()
    private val storeSettingsDao = db.storeSettingsDao()
    private val notificationDao = db.notificationDao()

    // --- Authentication & Users ---
    suspend fun login(email: String, passwordHash: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = userDao.getUserByEmail(email)
        if (user != null && user.passwordHash == passwordHash) {
            user
        } else {
            null
        }
    }

    suspend fun register(name: String, email: String, passwordHash: String, phone: String, role: String): Boolean = withContext(Dispatchers.IO) {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return@withContext false
        
        val user = UserEntity(
            name = name,
            email = email,
            passwordHash = passwordHash,
            phone = phone,
            role = role,
            walletBalance = if (role == "CUSTOMER") 500.0 else 0.0
        )
        val id = userDao.insertUser(user)
        
        // Seed default address for customers
        if (role == "CUSTOMER" && id > 0) {
            addressDao.insertAddress(
                AddressEntity(
                    userId = id.toInt(),
                    title = "Home",
                    addressLine = "123 Main Street, Sector 4",
                    city = "Mumbai",
                    postalCode = "400001",
                    isDefault = true
                )
            )
        }
        true
    }

    suspend fun getUserById(userId: Int): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun addWalletBalance(userId: Int, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext false
        userDao.updateUser(user.copy(walletBalance = user.walletBalance + amount))
        true
    }

    // --- Seeding Data ---
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val catCount = categoryDao.getCount()
        if (catCount > 0) return@withContext // Database is already seeded

        // 1. Seed Store settings
        storeSettingsDao.insertSettings(
            StoreSettingsEntity(
                id = 1,
                isOpen = true,
                deliveryCharge = 20.0,
                minOrderAmount = 100.0,
                notificationCampaignText = "Welcome to Smart Kirana! Get fresh groceries delivered instantly."
            )
        )

        // 2. Seed Default Users
        // Admin Owner
        userDao.insertUser(
            UserEntity(
                email = "owner@kirana.com",
                passwordHash = "admin",
                name = "Mr. Ramesh Kumar (Owner)",
                phone = "9876543210",
                role = "OWNER",
                walletBalance = 15000.0
            )
        )
        // Delivery Partner
        userDao.insertUser(
            UserEntity(
                email = "delivery@kirana.com",
                passwordHash = "delivery",
                name = "Vijay Shinde (Rider)",
                phone = "9123456780",
                role = "DELIVERY",
                walletBalance = 120.0 // Delivery partner wallet acts as earnings summary
            )
        )
        // Customer
        val customerId = userDao.insertUser(
            UserEntity(
                email = "customer@kirana.com",
                passwordHash = "customer",
                name = "Aarti Sharma",
                phone = "9988776655",
                role = "CUSTOMER",
                walletBalance = 500.0
            )
        )
        addressDao.insertAddress(
            AddressEntity(
                userId = customerId.toInt(),
                title = "Home",
                addressLine = "B-402 Shanti Niketan, Andheri West",
                city = "Mumbai",
                postalCode = "400053",
                isDefault = true
            )
        )
        addressDao.insertAddress(
            AddressEntity(
                userId = customerId.toInt(),
                title = "Office",
                addressLine = "Naman Centre, G-Block, Bandra Kurla Complex",
                city = "Mumbai",
                postalCode = "400051",
                isDefault = false
            )
        )

        // 3. Seed Categories
        val fruitsId = categoryDao.insertCategory(CategoryEntity(name = "Fruits & Vegetables", iconName = "eco"))
        val dairyId = categoryDao.insertCategory(CategoryEntity(name = "Dairy & Bakery", iconName = "egg"))
        val groceryId = categoryDao.insertCategory(CategoryEntity(name = "Grocery & Staples", iconName = "grass"))
        val beverageId = categoryDao.insertCategory(CategoryEntity(name = "Beverages", iconName = "local_drink"))
        val snackId = categoryDao.insertCategory(CategoryEntity(name = "Snacks & Sweets", iconName = "cookie"))
        val householdId = categoryDao.insertCategory(CategoryEntity(name = "Household Care", iconName = "cleaning_services"))

        // 4. Seed Products
        // Fruits
        productDao.insertProduct(ProductEntity(categoryId = fruitsId.toInt(), name = "Fresh Red Apples", description = "Sweet and crisp royal gala apples imported directly from Shimla orchards. Perfect for morning salads.", price = 160.0, unit = "1 kg", stockQuantity = 25, imageResName = "apples"))
        productDao.insertProduct(ProductEntity(categoryId = fruitsId.toInt(), name = "Organic Yellow Bananas", description = "Naturally ripened, chemical-free yellow robusta bananas. Fresh from local organic farms.", price = 60.0, unit = "1 Dozen", stockQuantity = 40, imageResName = "bananas"))
        productDao.insertProduct(ProductEntity(categoryId = fruitsId.toInt(), name = "Fresh Potatoes (Alco)", description = "High-quality starchy potatoes, perfect for making delicious fries or curry. Long-lasting stock.", price = 30.0, unit = "1 kg", stockQuantity = 90, imageResName = "potatoes"))
        
        // Dairy
        productDao.insertProduct(ProductEntity(categoryId = dairyId.toInt(), name = "Taza Fresh Milk", description = "Pasteurized double toned cow's milk. Loaded with vitamins and essential calcium.", price = 66.0, unit = "1 Litre", stockQuantity = 50, imageResName = "milk"))
        productDao.insertProduct(ProductEntity(categoryId = dairyId.toInt(), name = "Pure Salted Butter", description = "Creamy, smooth, and perfectly salted butter block. Classic taste for hot toast.", price = 255.0, unit = "500 g", stockQuantity = 12, imageResName = "butter"))
        productDao.insertProduct(ProductEntity(categoryId = dairyId.toInt(), name = "Whole Wheat Bread", description = "Freshly baked whole grain high fiber bread. Healthy brown bread with no added preservatives.", price = 45.0, unit = "1 Pack", stockQuantity = 30, imageResName = "bread"))

        // Grocery
        productDao.insertProduct(ProductEntity(categoryId = groceryId.toInt(), name = "Premium Basmati Rice", description = "Extra long grain fragrant aged basmati rice. Ideal for biryanis, pulao, and everyday feast.", price = 115.0, unit = "1 kg", stockQuantity = 75, imageResName = "rice"))
        productDao.insertProduct(ProductEntity(categoryId = groceryId.toInt(), name = "Unpolished Toor Dal", description = "Organic split yellow pigeon peas. High in proteins, dietary fibers, and iron.", price = 145.0, unit = "1 kg", stockQuantity = 60, imageResName = "dal"))
        productDao.insertProduct(ProductEntity(categoryId = groceryId.toInt(), name = "Refined Sunflower Oil", description = "Light, healthy, and premium cooking oil containing Vitamin A, D, and E. Great for deep frying.", price = 165.0, unit = "1 Litre", stockQuantity = 3, imageResName = "oil")) // Low Stock

        // Beverages
        productDao.insertProduct(ProductEntity(categoryId = beverageId.toInt(), name = "Golden Assam Tea Bags", description = "Rich and robust black tea leaves sourced from premium Assam gardens. Exquisite aroma.", price = 120.0, unit = "100 Bags", stockQuantity = 18, imageResName = "tea"))
        productDao.insertProduct(ProductEntity(categoryId = beverageId.toInt(), name = "Fresh Orange Juice", description = "100% pure squeezed citrus oranges. No added sugar, colors, or artificial flavor enhancers.", price = 95.0, unit = "1 Litre", stockQuantity = 22, imageResName = "orange_juice"))

        // Snacks
        productDao.insertProduct(ProductEntity(categoryId = snackId.toInt(), name = "Choco Chip Cookies", description = "Loaded with real Hershey chocolate chips and butter. Crispy on edges, chewy in the center.", price = 80.0, unit = "250 g", stockQuantity = 35, imageResName = "cookies"))
        productDao.insertProduct(ProductEntity(categoryId = snackId.toInt(), name = "Crunchy Salted Potato Chips", description = "Thinly sliced classic salted golden potato chips. Crispy and salted to perfection.", price = 40.0, unit = "1 Pack", stockQuantity = 4, imageResName = "chips")) // Low Stock

        // Household
        productDao.insertProduct(ProductEntity(categoryId = householdId.toInt(), name = "Dishwash Liquid Gel", description = "Refreshing lemon scented powerful dishwashing liquid. Instantly cuts through tough grease.", price = 105.0, unit = "500 ml", stockQuantity = 25, imageResName = "dishwash"))

        // Send initial notifications
        notificationDao.insertNotification(
            NotificationEntity(
                userId = customerId.toInt(),
                title = "Welcome Discount! 🎁",
                message = "Congratulations! You have received ₹500 free virtual wallet credits. Start ordering fresh groceries today."
            )
        )
    }

    // --- Store Settings ---
    fun getStoreSettingsFlow(): Flow<StoreSettingsEntity?> = storeSettingsDao.getSettingsFlow()
    suspend fun getStoreSettings(): StoreSettingsEntity? = withContext(Dispatchers.IO) { storeSettingsDao.getSettings() }
    suspend fun updateStoreStatus(isOpen: Boolean) = withContext(Dispatchers.IO) {
        storeSettingsDao.updateStoreStatus(isOpen)
    }
    suspend fun updateCampaignText(campaignText: String) = withContext(Dispatchers.IO) {
        storeSettingsDao.updateCampaignText(campaignText)
        
        // Push campaign notification to all users who are customers
        val customers = userDao.getUsersByRole("CUSTOMER")
        for (cust in customers) {
            notificationDao.insertNotification(
                NotificationEntity(
                    userId = cust.id,
                    title = "Store Announcement 📢",
                    message = campaignText
                )
            )
        }
    }

    // --- Address ---
    fun getAddressesFlow(userId: Int): Flow<List<AddressEntity>> = addressDao.getAddressesByUserId(userId)
    suspend fun saveAddress(address: AddressEntity) = withContext(Dispatchers.IO) {
        if (address.isDefault) {
            addressDao.clearDefaultAddresses(address.userId)
        }
        addressDao.insertAddress(address)
    }
    suspend fun selectDefaultAddress(userId: Int, addressId: Int) = withContext(Dispatchers.IO) {
        addressDao.clearDefaultAddresses(userId)
        addressDao.setDefaultAddress(addressId)
    }
    suspend fun deleteAddress(addressId: Int) = withContext(Dispatchers.IO) {
        addressDao.deleteAddress(addressId)
    }

    // --- Catalog ---
    fun getCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    fun getProductsFlow(): Flow<List<ProductEntity>> = productDao.getActiveProducts()
    fun getProductsByCategoryFlow(catId: Int): Flow<List<ProductEntity>> = productDao.getProductsByCategory(catId)
    fun searchProductsFlow(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)
    
    // Admin Catalog Management
    fun getAllProductsAdminFlow(): Flow<List<ProductEntity>> = productDao.getAllProductsAdmin()
    suspend fun getProductById(productId: Int): ProductEntity? = withContext(Dispatchers.IO) { productDao.getProductById(productId) }
    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        if (product.id == 0) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
        }
    }

    // --- Cart ---
    fun getCartProductsFlow(userId: Int): Flow<List<CartProduct>> = cartDao.getCartProductsFlow(userId)
    
    suspend fun addToCart(userId: Int, productId: Int, quantity: Int) = withContext(Dispatchers.IO) {
        val existing = cartDao.getCartItem(userId, productId)
        if (existing != null) {
            val product = productDao.getProductById(productId)
            val newQty = (existing.quantity + quantity).coerceAtMost(product?.stockQuantity ?: 99)
            if (newQty <= 0) {
                cartDao.deleteCartItem(existing.id)
            } else {
                cartDao.updateCartItem(existing.copy(quantity = newQty))
            }
        } else if (quantity > 0) {
            cartDao.insertCartItem(CartItemEntity(userId = userId, productId = productId, quantity = quantity))
        }
    }

    suspend fun updateCartQty(userId: Int, productId: Int, newQty: Int) = withContext(Dispatchers.IO) {
        val existing = cartDao.getCartItem(userId, productId)
        if (existing != null) {
            if (newQty <= 0) {
                cartDao.deleteCartItem(existing.id)
            } else {
                cartDao.updateCartItem(existing.copy(quantity = newQty))
            }
        } else if (newQty > 0) {
            cartDao.insertCartItem(CartItemEntity(userId = userId, productId = productId, quantity = newQty))
        }
    }

    suspend fun deleteCartItem(cartItemId: Int) = withContext(Dispatchers.IO) {
        cartDao.deleteCartItem(cartItemId)
    }

    suspend fun clearCart(userId: Int) = withContext(Dispatchers.IO) {
        cartDao.clearCart(userId)
    }

    // --- Wishlist ---
    fun getWishlistFlow(userId: Int): Flow<List<WishlistProduct>> = wishlistDao.getWishlistProducts(userId)
    fun isFavoriteFlow(userId: Int, productId: Int): Flow<Boolean> = wishlistDao.isFavoriteFlow(userId, productId)
    
    suspend fun toggleWishlist(userId: Int, productId: Int) = withContext(Dispatchers.IO) {
        val isFav = wishlistDao.isFavorite(userId, productId)
        if (isFav) {
            wishlistDao.deleteWishlistItem(userId, productId)
        } else {
            wishlistDao.insertWishlistItem(WishlistItemEntity(userId = userId, productId = productId))
        }
    }

    // --- Checkout & Orders ---
    // Custom sealed classes for Checkout Result
    sealed class CheckoutResult {
        object Success : CheckoutResult()
        data class Error(val message: String) : CheckoutResult()
    }

    suspend fun placeOrder(
        userId: Int,
        addressText: String,
        paymentMethod: String, // "COD" or "WALLET"
        note: String? = null
    ): CheckoutResult = withContext(Dispatchers.IO) {
        // 1. Check if store is open
        val settings = storeSettingsDao.getSettings()
        if (settings?.isOpen != true) {
            return@withContext CheckoutResult.Error("Order failed: The store is currently CLOSED.")
        }

        // 2. Fetch cart products
        val cartItems = cartDao.getCartProductsList(userId)
        if (cartItems.isEmpty()) {
            return@withContext CheckoutResult.Error("Order failed: Your shopping cart is empty.")
        }

        // 3. Compute totals and check minimum amount
        var subtotal = 0.0
        for (item in cartItems) {
            subtotal += item.price * item.quantity
        }
        val minAmt = settings.minOrderAmount
        if (subtotal < minAmt) {
            return@withContext CheckoutResult.Error("Order failed: Minimum order amount is ₹$minAmt. (Current: ₹$subtotal)")
        }

        val totalAmt = subtotal + settings.deliveryCharge

        // 4. Validate user
        val user = userDao.getUserById(userId) ?: return@withContext CheckoutResult.Error("User session not found.")

        // 5. If Wallet, check balance
        if (paymentMethod == "WALLET") {
            if (user.walletBalance < totalAmt) {
                return@withContext CheckoutResult.Error("Order failed: Insufficient wallet balance. (Need ₹$totalAmt, Have ₹${user.walletBalance})")
            }
        }

        // 6. Check and reduce stock
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)
            if (product == null || product.stockQuantity < item.quantity || !product.isActive) {
                return@withContext CheckoutResult.Error("Order failed: Product '${item.name}' is out of stock or unavailable.")
            }
        }

        // Success flow! Run transactional modifications
        // Deduct stock
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)!!
            productDao.updateProductStock(item.productId, product.stockQuantity - item.quantity)
        }

        // Deduct wallet if applicable
        if (paymentMethod == "WALLET") {
            userDao.updateUser(user.copy(walletBalance = user.walletBalance - totalAmt))
        }

        // Generate dynamic 4-digit OTP code for secure deliveries
        val otpCode = String.format("%04d", Random.nextInt(1000, 9999))

        // Create Order Entity
        val orderEntity = OrderEntity(
            userId = userId,
            addressText = addressText,
            status = "PENDING",
            totalAmount = totalAmt,
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod == "WALLET") "PAID" else "PENDING",
            otpCode = otpCode,
            note = note
        )

        val orderId = orderDao.insertOrder(orderEntity)

        // Create Order Items
        for (item in cartItems) {
            orderDao.insertOrderItem(
                OrderItemEntity(
                    orderId = orderId.toInt(),
                    productId = item.productId,
                    productName = item.name,
                    quantity = item.quantity,
                    priceAtPurchase = item.price
                )
            )
        }

        // Clear cart
        cartDao.clearCart(userId)

        // Add Notification
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Order Placed Successfully! 🎉",
                message = "Your order #$orderId of ₹$totalAmt has been successfully placed and is awaiting store acceptance. Use Delivery OTP: $otpCode when your rider arrives."
            )
        )

        CheckoutResult.Success
    }

    fun getOrdersForUserFlow(userId: Int): Flow<List<OrderEntity>> = orderDao.getOrdersForUser(userId)
    fun getAllOrdersAdminFlow(): Flow<List<OrderEntity>> = orderDao.getAllOrdersAdmin()
    fun getOrdersForDeliveryFlow(driverId: Int): Flow<List<OrderEntity>> = orderDao.getOrdersForDeliveryPartner(driverId)
    fun getUnassignedOrdersFlow(): Flow<List<OrderEntity>> = orderDao.getUnassignedOrders()
    fun getOrderItemsFlow(orderId: Int): Flow<List<OrderItemEntity>> = orderDao.getOrderItemsFlow(orderId)

    suspend fun getOrderById(orderId: Int): OrderEntity? = withContext(Dispatchers.IO) { orderDao.getOrderById(orderId) }
    suspend fun getOrderItems(orderId: Int): List<OrderItemEntity> = withContext(Dispatchers.IO) { orderDao.getOrderItems(orderId) }

    suspend fun updateOrderStatus(orderId: Int, newStatus: String) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        orderDao.updateOrder(order.copy(status = newStatus))

        // Push notification
        val notificationMsg = when (newStatus) {
            "ACCEPTED" -> "Great news! The store has accepted your order #$orderId. Preparing your items now."
            "PREPARING" -> "Your order #$orderId is being packed with care and will be ready for pickup shortly."
            "CANCELLED" -> "Your order #$orderId was cancelled by the store. If you paid via wallet, refund was processed."
            else -> "Your order #$orderId status changed to: $newStatus"
        }

        // Process refund if owner cancels wallet order
        if (newStatus == "CANCELLED" && order.paymentMethod == "WALLET" && order.paymentStatus == "PAID") {
            val user = userDao.getUserById(order.userId)
            if (user != null) {
                userDao.updateUser(user.copy(walletBalance = user.walletBalance + order.totalAmount))
            }
        }

        notificationDao.insertNotification(
            NotificationEntity(
                userId = order.userId,
                title = "Order Status Update 🛍️",
                message = notificationMsg
            )
        )
    }

    suspend fun assignDeliveryPartner(orderId: Int, driverId: Int) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        val driver = userDao.getUserById(driverId) ?: return@withContext

        orderDao.updateOrder(order.copy(
            deliveryPartnerId = driverId,
            status = "OUT_FOR_DELIVERY"
        ))

        // Notify customer
        notificationDao.insertNotification(
            NotificationEntity(
                userId = order.userId,
                title = "Order Out for Delivery! 🛵",
                message = "Rider ${driver.name} is on the way with your order #$orderId. Keep OTP ${order.otpCode} handy to verify delivery."
            )
        )

        // Notify delivery partner
        notificationDao.insertNotification(
            NotificationEntity(
                userId = driverId,
                title = "New Delivery Assigned 📦",
                message = "Order #$orderId from customer ${order.userId} has been assigned to you. Head to the store for pick up."
            )
        )
    }

    suspend fun verifyAndCompleteDelivery(orderId: Int, inputOtp: String): Boolean = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext false
        if (order.otpCode != inputOtp.trim()) {
            return@withContext false
        }

        // OTP matched successfully! Complete delivery
        orderDao.updateOrder(order.copy(
            status = "DELIVERED",
            paymentStatus = "PAID" // COD is now PAID
        ))

        // Credit ₹40 delivery earnings to rider's wallet
        if (order.deliveryPartnerId != null) {
            val driver = userDao.getUserById(order.deliveryPartnerId)
            if (driver != null) {
                userDao.updateUser(driver.copy(walletBalance = driver.walletBalance + 40.0))
            }
            // Send notification to driver
            notificationDao.insertNotification(
                NotificationEntity(
                    userId = order.deliveryPartnerId,
                    title = "Earnings Credited! 💰",
                    message = "Earned ₹40 for completing delivery of order #$orderId. Great job!"
                )
            )
        }

        // Notify customer
        notificationDao.insertNotification(
            NotificationEntity(
                userId = order.userId,
                title = "Order Delivered! Enjoy! 🥳",
                message = "Your order #$orderId was successfully verified and delivered. Thank you for shopping with Smart Kirana!"
            )
        )

        true
    }

    // --- Notifications ---
    fun getNotificationsFlow(userId: Int): Flow<List<NotificationEntity>> = notificationDao.getNotificationsForUser(userId)
    suspend fun markAllNotificationsRead(userId: Int) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    // --- Admin Dashboard Stats ---
    data class AdminStats(
        val todaySales: Double,
        val totalRevenue: Double,
        val totalOrders: Int,
        val pendingOrders: Int,
        val completedOrders: Int,
        val cancelledOrders: Int,
        val totalCustomers: Int,
        val lowStockCount: Int
    )

    suspend fun getAdminStats(): AdminStats = withContext(Dispatchers.IO) {
        val allOrders = orderDao.getAllOrdersAdmin().firstOrNull() ?: emptyList()
        val allProducts = productDao.getAllProductsAdmin().firstOrNull() ?: emptyList()
        val customersCount = userDao.getUsersByRole("CUSTOMER").size

        var todaySales = 0.0
        var totalRev = 0.0
        var totalOrds = 0
        var pendCount = 0
        var compCount = 0
        var cancCount = 0

        // Simple day matching: since standard seeds or fresh orders are today
        val startOfToday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        for (ord in allOrders) {
            totalOrds++
            if (ord.status == "DELIVERED") {
                totalRev += ord.totalAmount
                if (ord.orderDate >= startOfToday) {
                    todaySales += ord.totalAmount
                }
                compCount++
            } else if (ord.status == "CANCELLED") {
                cancCount++
            } else {
                pendCount++
            }
        }

        val lowStock = allProducts.count { it.stockQuantity <= 5 }

        AdminStats(
            todaySales = todaySales,
            totalRevenue = totalRev,
            totalOrders = totalOrds,
            pendingOrders = pendCount,
            completedOrders = compCount,
            cancelledOrders = cancCount,
            totalCustomers = customersCount,
            lowStockCount = lowStock
        )
    }

    // Get Delivery Partners list
    suspend fun getDeliveryPartnersList(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getUsersByRole("DELIVERY")
    }
}
