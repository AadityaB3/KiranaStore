package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Wrapper classes for Joins
data class CartProduct(
    val cartItemId: Int,
    val productId: Int,
    val name: String,
    val price: Double,
    val unit: String,
    val imageResName: String,
    val quantity: Int,
    val stockQuantity: Int
)

data class WishlistProduct(
    val wishlistItemId: Int,
    val productId: Int,
    val name: String,
    val price: Double,
    val unit: String,
    val imageResName: String,
    val stockQuantity: Int
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY isDefault DESC")
    fun getAddressesByUserId(userId: Int): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Query("UPDATE addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultAddresses(userId: Int)

    @Query("UPDATE addresses SET isDefault = 1 WHERE id = :addressId")
    suspend fun setDefaultAddress(addressId: Int)

    @Query("DELETE FROM addresses WHERE id = :addressId")
    suspend fun deleteAddress(addressId: Int)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsAdmin(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :catId AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(catId: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' AND isActive = 1")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :productId")
    suspend fun updateProductStock(productId: Int, newStock: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int
}

@Dao
interface CartDao {
    @Query("""
        SELECT c.id as cartItemId, p.id as productId, p.name, p.price, p.unit, p.imageResName, c.quantity, p.stockQuantity 
        FROM cart_items c 
        JOIN products p ON c.productId = p.id 
        WHERE c.userId = :userId
    """)
    fun getCartProductsFlow(userId: Int): Flow<List<CartProduct>>

    @Query("""
        SELECT c.id as cartItemId, p.id as productId, p.name, p.price, p.unit, p.imageResName, c.quantity, p.stockQuantity 
        FROM cart_items c 
        JOIN products p ON c.productId = p.id 
        WHERE c.userId = :userId
    """)
    suspend fun getCartProductsList(userId: Int): List<CartProduct>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItem(userId: Int, productId: Int): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :cartItemId")
    suspend fun deleteCartItem(cartItemId: Int)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)
}

@Dao
interface WishlistDao {
    @Query("""
        SELECT w.id as wishlistItemId, p.id as productId, p.name, p.price, p.unit, p.imageResName, p.stockQuantity
        FROM wishlist_items w 
        JOIN products p ON w.productId = p.id 
        WHERE w.userId = :userId
    """)
    fun getWishlistProducts(userId: Int): Flow<List<WishlistProduct>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE userId = :userId AND productId = :productId)")
    fun isFavoriteFlow(userId: Int, productId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE userId = :userId AND productId = :productId)")
    suspend fun isFavorite(userId: Int, productId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(wishlist: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND productId = :productId")
    suspend fun deleteWishlistItem(userId: Int, productId: Int)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getOrdersForUser(userId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrdersAdmin(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE deliveryPartnerId = :driverId ORDER BY orderDate DESC")
    fun getOrdersForDeliveryPartner(driverId: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = 'PREPARING' AND deliveryPartnerId IS NULL ORDER BY orderDate ASC")
    fun getUnassignedOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Int): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsFlow(orderId: Int): Flow<List<OrderItemEntity>>
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: StoreSettingsEntity)

    @Query("UPDATE store_settings SET isOpen = :isOpen WHERE id = 1")
    suspend fun updateStoreStatus(isOpen: Boolean)

    @Query("UPDATE store_settings SET notificationCampaignText = :campaignText WHERE id = 1")
    suspend fun updateCampaignText(campaignText: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)
}
