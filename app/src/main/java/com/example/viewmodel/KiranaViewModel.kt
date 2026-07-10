package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.KiranaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KiranaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = KiranaRepository(db)

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess.asStateFlow()

    // --- Active Store State ---
    val storeSettings: StateFlow<StoreSettingsEntity?> = repository.getStoreSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Catalog Filter State ---
    val categories: StateFlow<List<CategoryEntity>> = repository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Reactive Catalog Shelf ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> = combine(
        _selectedCategoryId,
        _searchQuery
    ) { catId, query ->
        Pair(catId, query)
    }.flatMapLatest { (catId, query) ->
        if (query.isNotEmpty()) {
            repository.searchProductsFlow(query)
        } else if (catId != null) {
            repository.getProductsByCategoryFlow(catId)
        } else {
            repository.getProductsFlow()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Admin Product Catalog ---
    val adminProducts: StateFlow<List<ProductEntity>> = repository.getAllProductsAdminFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Reactive Cart State ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val cartItems: StateFlow<List<CartProduct>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getCartProductsFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = cartItems.map { items ->
        items.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Reactive Wishlist State ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val wishlistItems: StateFlow<List<WishlistProduct>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getWishlistFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Reactive Orders depending on current User Role ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<OrderEntity>> = _currentUser
        .flatMapLatest { user ->
            when (user?.role) {
                "CUSTOMER" -> repository.getOrdersForUserFlow(user.id)
                "DELIVERY" -> repository.getOrdersForDeliveryFlow(user.id)
                "OWNER" -> repository.getAllOrdersAdminFlow()
                else -> flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Admin Assignment & Analytics ---
    val unassignedOrders: StateFlow<List<OrderEntity>> = repository.getUnassignedOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _adminStats = MutableStateFlow<KiranaRepository.AdminStats?>(null)
    val adminStats: StateFlow<KiranaRepository.AdminStats?> = _adminStats.asStateFlow()

    private val _deliveryPartners = MutableStateFlow<List<UserEntity>>(emptyList())
    val deliveryPartners: StateFlow<List<UserEntity>> = _deliveryPartners.asStateFlow()

    // --- Notifications ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getNotificationsFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Seed database immediately on first initialization
            repository.seedDatabaseIfEmpty()
            
            // Initial load of riders
            loadDeliveryPartners()
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, passwordHash: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.login(email, passwordHash)
            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                onComplete(true)
                
                // Refresh specific role states
                if (user.role == "OWNER") {
                    refreshAdminStats()
                    loadDeliveryPartners()
                }
            } else {
                _loginError.value = "Invalid email or password."
                onComplete(false)
            }
        }
    }

    fun register(name: String, email: String, passwordHash: String, phone: String, role: String) {
        viewModelScope.launch {
            val success = repository.register(name, email, passwordHash, phone, role)
            _registerSuccess.value = success
            if (success) {
                _loginError.value = null
            } else {
                _loginError.value = "Email is already registered."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _registerSuccess.value = false
    }

    fun resetRegisterSuccess() {
        _registerSuccess.value = false
    }

    fun addWalletFunds(amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val success = repository.addWalletBalance(user.id, amount)
            if (success) {
                // Refresh current session state
                _currentUser.value = repository.getUserById(user.id)
            }
        }
    }

    // --- Store settings ---
    fun toggleStoreOpen(isOpen: Boolean) {
        viewModelScope.launch {
            repository.updateStoreStatus(isOpen)
        }
    }

    fun pushCampaignAnnouncement(announcement: String) {
        viewModelScope.launch {
            repository.updateCampaignText(announcement)
        }
    }

    // --- Address Actions ---
    fun getAddressesFlow(): Flow<List<AddressEntity>> {
        val userId = _currentUser.value?.id ?: return flowOf(emptyList())
        return repository.getAddressesFlow(userId)
    }

    fun saveAddress(title: String, addressLine: String, city: String, postalCode: String, isDefault: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveAddress(
                AddressEntity(
                    userId = user.id,
                    title = title,
                    addressLine = addressLine,
                    city = city,
                    postalCode = postalCode,
                    isDefault = isDefault
                )
            )
        }
    }

    fun setDefaultAddress(addressId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.selectDefaultAddress(user.id, addressId)
        }
    }

    fun deleteAddress(addressId: Int) {
        viewModelScope.launch {
            repository.deleteAddress(addressId)
        }
    }

    // --- Catalog Filter Actions ---
    fun selectCategory(catId: Int?) {
        _selectedCategoryId.value = catId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Cart Actions ---
    fun addToCart(productId: Int, quantity: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addToCart(user.id, productId, quantity)
        }
    }

    fun updateCartQty(productId: Int, quantity: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateCartQty(user.id, productId, quantity)
        }
    }

    fun removeFromCart(cartItemId: Int) {
        viewModelScope.launch {
            repository.deleteCartItem(cartItemId)
        }
    }

    fun clearCart() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearCart(user.id)
        }
    }

    // --- Wishlist Actions ---
    fun toggleWishlist(productId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleWishlist(user.id, productId)
        }
    }

    fun isFavoriteFlow(productId: Int): Flow<Boolean> {
        val user = _currentUser.value ?: return flowOf(false)
        return repository.isFavoriteFlow(user.id, productId)
    }

    // --- Checkout & Ordering ---
    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    sealed class CheckoutUiState {
        object Idle : CheckoutUiState()
        object Loading : CheckoutUiState()
        object Success : CheckoutUiState()
        data class Error(val message: String) : CheckoutUiState()
    }

    fun checkoutOrder(addressText: String, paymentMethod: String, note: String? = null) {
        val user = _currentUser.value ?: return
        _checkoutState.value = CheckoutUiState.Loading
        viewModelScope.launch {
            val result = repository.placeOrder(user.id, addressText, paymentMethod, note)
            when (result) {
                is KiranaRepository.CheckoutResult.Success -> {
                    _checkoutState.value = CheckoutUiState.Success
                    // Refresh user session balance
                    _currentUser.value = repository.getUserById(user.id)
                }
                is KiranaRepository.CheckoutResult.Error -> {
                    _checkoutState.value = CheckoutUiState.Error(result.message)
                }
            }
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutUiState.Idle
    }

    // --- Order Items Flow ---
    fun getOrderItemsFlow(orderId: Int): Flow<List<OrderItemEntity>> {
        return repository.getOrderItemsFlow(orderId)
    }

    // --- Owner Actions ---
    fun refreshAdminStats() {
        viewModelScope.launch {
            _adminStats.value = repository.getAdminStats()
        }
    }

    private fun loadDeliveryPartners() {
        viewModelScope.launch {
            _deliveryPartners.value = repository.getDeliveryPartnersList()
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            refreshAdminStats()
        }
    }

    fun assignDeliveryPartner(orderId: Int, driverId: Int) {
        viewModelScope.launch {
            repository.assignDeliveryPartner(orderId, driverId)
            refreshAdminStats()
        }
    }

    fun saveProduct(name: String, categoryId: Int, description: String, price: Double, unit: String, stock: Int) {
        viewModelScope.launch {
            repository.saveProduct(
                ProductEntity(
                    categoryId = categoryId,
                    name = name,
                    description = description,
                    price = price,
                    unit = unit,
                    stockQuantity = stock,
                    imageResName = "groceries"
                )
            )
            refreshAdminStats()
        }
    }

    fun toggleProductActive(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product.copy(isActive = !product.isActive))
            refreshAdminStats()
        }
    }

    // --- Delivery Partner Actions ---
    private val _otpVerificationSuccess = MutableStateFlow<Boolean?>(null)
    val otpVerificationSuccess: StateFlow<Boolean?> = _otpVerificationSuccess.asStateFlow()

    fun verifyOtpAndCompleteDelivery(orderId: Int, otpCode: String) {
        viewModelScope.launch {
            val success = repository.verifyAndCompleteDelivery(orderId, otpCode)
            _otpVerificationSuccess.value = success
            if (success) {
                // Refresh rider balance if they are the current user
                _currentUser.value?.let { user ->
                    _currentUser.value = repository.getUserById(user.id)
                }
            }
        }
    }

    fun resetOtpVerification() {
        _otpVerificationSuccess.value = null
    }

    // --- Notifications actions ---
    fun markAllNotificationsAsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.id)
        }
    }
}
