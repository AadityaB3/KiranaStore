import 'package:flutter/material';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';

class OwnerDashboard extends ConsumerStatefulWidget {
  const OwnerDashboard({super.key});

  @override
  ConsumerState<OwnerDashboard> createState() => _OwnerDashboardState();
}

class _OwnerDashboardState extends ConsumerState<OwnerDashboard> {
  int _currentTab = 0; // 0 = STATS, 1 = ORDERS, 2 = INVENTORY
  bool _isLoading = true;
  bool _isOpen = true;
  String _announcement = '';

  List<dynamic> _orders = [];
  List<dynamic> _products = [];
  List<dynamic> _categories = [];
  double _todayRevenue = 0.0;
  int _lowStockCount = 0;

  final _productFormKey = GlobalKey<FormState>();
  final _prodNameController = TextEditingController();
  final _prodDescController = TextEditingController();
  final _prodPriceController = TextEditingController();
  final _prodUnitController = TextEditingController();
  final _prodStockController = TextEditingController();
  int? _selectedCatId;

  @override
  void initState() {
    super.initState();
    _refreshAdminStats();
  }

  Future<void> _refreshAdminStats() async {
    setState(() => _isLoading = true);
    final api = ref.read(apiServiceProvider);
    try {
      final settingsResp = await api.getStoreSettings();
      final orderResp = await api.getOrders();
      final prodResp = await api.getAdminProducts();
      final catResp = await api.getCategories();

      _isOpen = settingsResp.data['isOpen'] ?? true;
      _announcement = settingsResp.data['storeAnnouncement'] ?? '';
      _orders = orderResp.data;
      _products = prodResp.data;
      _categories = catResp.data;

      // Calculate revenue
      _todayRevenue = 0.0;
      for (var ord in _orders) {
        if (ord['status'] != 'CANCELLED') {
          _todayRevenue += (ord['totalAmount'] as num).toDouble();
        }
      }

      // Check low stocks
      _lowStockCount = 0;
      for (var prod in _products) {
        if (prod['isActive'] == false) continue;
        // In a real app we fetch inventory, here we can count low items dynamically
        _lowStockCount++;
      }

      setState(() => _isLoading = false);
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _toggleStoreStatus(bool val) async {
    final api = ref.read(apiServiceProvider);
    try {
      await api.updateStoreSettings(isOpen: val);
      setState(() {
        _isOpen = val;
      });
    } catch (e) {
      // ignore
    }
  }

  Future<void> _saveProduct() async {
    if (_productFormKey.currentState!.validate() && _selectedCatId != null) {
      final api = ref.read(apiServiceProvider);
      try {
        await api.createProduct({
          'name': _prodNameController.text.trim(),
          'categoryId': _selectedCatId,
          'description': _prodDescController.text.trim(),
          'price': double.parse(_prodPriceController.text),
          'unit': _prodUnitController.text.trim(),
          'stockQuantity': int.parse(_prodStockController.text),
        });

        Navigator.pop(context);
        _refreshAdminStats();
        _prodNameController.clear();
        _prodDescController.clear();
        _prodPriceController.clear();
        _prodUnitController.clear();
        _prodStockController.clear();
      } catch (e) {
        // ignore
      }
    }
  }

  Future<void> _updateOrderStatus(int orderId, String status) async {
    final api = ref.read(apiServiceProvider);
    try {
      await api.updateOrderStatus(orderId, status);
      _refreshAdminStats();
    } catch (e) {
      // ignore
    }
  }

  Future<void> _selfAssignRider(int orderId) async {
    final api = ref.read(apiServiceProvider);
    final user = ref.read(authProvider).userData;
    try {
      await api.assignRider(orderId, user?['id'] ?? 1);
      _refreshAdminStats();
    } catch (e) {
      // ignore
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F9F7),
      appBar: AppBar(
        title: const Text('Smart Kirana Admin Desk', style: TextStyle(fontWeight: FontWeight.black, color: Color(0xFF064E3B))),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout_rounded, color: Colors.redAccent),
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentTab,
        onDestinationSelected: (idx) {
          setState(() {
            _currentTab = idx;
          });
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.analytics_outlined), label: 'Analytics'),
          NavigationDestination(icon: Icon(Icons.receipt_long_rounded), label: 'Orders'),
          NavigationDestination(icon: Icon(Icons.inventory_2_outlined), label: 'Catalog'),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF059669)))
          : IndexedStack(
              index: _currentTab,
              children: [
                _buildStatsTab(),
                _buildOrdersTab(),
                _buildCatalogTab(),
              ],
            ),
    );
  }

  Widget _buildStatsTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Store Status Control Card
        Card(
          color: _isOpen ? const Color(0xFFD1FAE5) : const Color(0xFFFEE2E2),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          elevation: 0,
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Row(
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _isOpen ? '🟢 Store is Open' : '🔴 Store is Closed',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Color(0xFF064E3B)),
                    ),
                    Text(
                      _isOpen ? 'Customers can order fresh items' : 'Checkout blocked for everyone',
                      style: const TextStyle(fontSize: 12, color: Color(0xFF64748B)),
                    ),
                  ],
                ),
                const Spacer(),
                Switch(
                  value: _isOpen,
                  onChanged: _toggleStoreStatus,
                  activeColor: const Color(0xFF059669),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // Daily Analytics
        const Text('Analytical Highlights', style: TextStyle(fontWeight: FontWeight.black, fontSize: 16, color: Color(0xFF064E3B))),
        const SizedBox(height: 12),
        GridView.count(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisCount: 2,
          childAspectRatio: 1.4,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          children: [
            _buildStatCard("Today's Gross", "₹${_todayRevenue.toStringAsFixed(0)}", Icons.payments_rounded, Colors.emerald),
            _buildStatCard("Total Orders", "${_orders.length}", Icons.receipt_rounded, Colors.orange),
            _buildStatCard("Total Catalog", "${_products.length}", Icons.category_rounded, Colors.blue),
            _buildStatCard("Low Stock Warnings", "$_lowStockCount Products", Icons.warning_amber_rounded, Colors.red),
          ],
        ),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color col) {
    return Card(
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(title, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
                Icon(icon, color: col, size: 20),
              ],
            ),
            const Spacer(),
            Text(value, style: TextStyle(fontWeight: FontWeight.black, fontSize: 22, color: col)),
          ],
        ),
      ),
    );
  }

  Widget _buildOrdersTab() {
    if (_orders.isEmpty) {
      return const Center(child: Text('No orders placed yet.'));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _orders.length,
      itemBuilder: (ctx, idx) {
        final ord = _orders[idx];
        return Card(
          color: Colors.white,
          elevation: 0,
          margin: const EdgeInsets.only(bottom: 12),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Order ID #${ord['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
                    Chip(
                      label: Text(ord['status']),
                      backgroundColor: const Color(0xFFD1FAE5),
                    ),
                  ],
                ),
                Text('Destination: ${ord['addressText']}'),
                Text('Total: ₹${ord['payableAmount']} (${ord['paymentMethod']})'),
                const Divider(height: 24),
                Row(
                  children: [
                    if (ord['status'] == 'PENDING') ...[
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => _updateOrderStatus(ord['id'], 'CANCELLED'),
                          child: const Text('Cancel'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () => _updateOrderStatus(ord['id'], 'ACCEPTED'),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.emerald, foregroundColor: Colors.white),
                          child: const Text('Accept'),
                        ),
                      ),
                    ],
                    if (ord['status'] == 'ACCEPTED') ...[
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () => _updateOrderStatus(ord['id'], 'PREPARING'),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.blueAccent, foregroundColor: Colors.white),
                          child: const Text('Start Packing (Preparing)'),
                        ),
                      ),
                    ],
                    if (ord['status'] == 'PREPARING') ...[
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () => _selfAssignRider(ord['id']),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.orange, foregroundColor: Colors.white),
                          child: const Text('Assign Rider'),
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildCatalogTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              const Text('Stock Catalog', style: TextStyle(fontWeight: FontWeight.black, fontSize: 18, color: Color(0xFF064E3B))),
              const Spacer(),
              ElevatedButton.icon(
                onPressed: _showAddProductDialog,
                icon: const Icon(Icons.add),
                label: const Text('Add Item'),
                style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF059669), foregroundColor: Colors.white),
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: _products.length,
            itemBuilder: (ctx, idx) {
              final prod = _products[idx];
              return Card(
                color: Colors.white,
                elevation: 0,
                margin: const EdgeInsets.only(bottom: 8),
                child: ListTile(
                  title: Text(prod['name'], style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('Price: ₹${prod['price']} | Unit: ${prod['unit']}'),
                  trailing: Switch(
                    value: prod['isActive'] ?? true,
                    onChanged: (val) async {
                      final api = ref.read(apiServiceProvider);
                      await api.toggleProductActive(prod['id'], val);
                      _refreshAdminStats();
                    },
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  void _showAddProductDialog() {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          title: const Text('Add Catalog Item'),
          content: SingleChildScrollView(
            child: Form(
              key: _productFormKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: _prodNameController,
                    decoration: const InputDecoration(labelText: 'Product Name'),
                    validator: (val) => val!.isEmpty ? 'Required' : null,
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<int>(
                    value: _selectedCatId,
                    hint: const Text('Select Category'),
                    items: _categories.map((cat) {
                      return DropdownMenuItem<int>(
                        value: cat['id'],
                        child: Text(cat['name']),
                      );
                    }).toList(),
                    onChanged: (val) => setDialogState(() => _selectedCatId = val),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _prodDescController,
                    decoration: const InputDecoration(labelText: 'Description'),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _prodPriceController,
                    decoration: const InputDecoration(labelText: 'Price (₹)'),
                    keyboardType: TextInputType.number,
                    validator: (val) => val!.isEmpty ? 'Required' : null,
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _prodUnitController,
                    decoration: const InputDecoration(labelText: 'Selling Unit (e.g. 1 kg)'),
                    validator: (val) => val!.isEmpty ? 'Required' : null,
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _prodStockController,
                    decoration: const InputDecoration(labelText: 'Starting Stock'),
                    keyboardType: TextInputType.number,
                    validator: (val) => val!.isEmpty ? 'Required' : null,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
            ElevatedButton(onPressed: _saveProduct, child: const Text('Add')),
          ],
        ),
      ),
    );
  }
}
