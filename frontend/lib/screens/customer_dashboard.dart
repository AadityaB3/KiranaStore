import 'package:flutter/material';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';

class CustomerDashboard extends ConsumerStatefulWidget {
  const CustomerDashboard({super.key});

  @override
  ConsumerState<CustomerDashboard> createState() => _CustomerDashboardState();
}

class _CustomerDashboardState extends ConsumerState<CustomerDashboard> {
  final Map<int, int> _cart = {}; // productId -> quantity
  List<dynamic> _products = [];
  List<dynamic> _categories = [];
  List<dynamic> _orders = [];
  bool _isLoading = true;
  String _searchQuery = '';
  int? _selectedCategoryId;

  @override
  void initState() {
    super.initState();
    _fetchStoreData();
  }

  Future<void> _fetchStoreData() async {
    setState(() => _isLoading = true);
    final api = ref.read(apiServiceProvider);
    try {
      final prodResp = await api.getProducts();
      final catResp = await api.getCategories();
      final orderResp = await api.getOrders();

      if (mounted) {
        setState(() {
          _products = prodResp.data;
          _categories = catResp.data;
          _orders = orderResp.data;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _addToCart(int productId) {
    setState(() {
      _cart[productId] = (_cart[productId] ?? 0) + 1;
    });
  }

  void _removeFromCart(int productId) {
    setState(() {
      if (_cart.containsKey(productId)) {
        if (_cart[productId] == 1) {
          _cart.remove(productId);
        } else {
          _cart[productId] = _cart[productId]! - 1;
        }
      }
    });
  }

  double _getCartTotal() {
    double total = 0;
    _cart.forEach((productId, qty) {
      final prod = _products.firstWhere((p) => p['id'] == productId, orElse: () => null);
      if (prod != null) {
        total += (prod['price'] as num).toDouble() * qty;
      }
    });
    return total;
  }

  Future<void> _checkout() async {
    if (_cart.isEmpty) return;

    final api = ref.read(apiServiceProvider);
    final itemsList = _cart.entries.map((entry) {
      return {
        'productId': entry.key,
        'quantity': entry.value,
      };
    }).toList();

    try {
      // Hardcode address ID 1 as standard mock default flow for direct supporrt
      final resp = await api.placeOrder(
        addressId: 1, 
        paymentMethod: 'COD', 
        items: itemsList
      );
      if (mounted && resp.statusCode == 200) {
        setState(() {
          _cart.clear();
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Order placed successfully! Cash on delivery selected.'),
            backgroundColor: Colors.emerald,
          ),
        );
        _fetchStoreData();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error checkout: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authProvider).userData;
    final filteredProducts = _products.where((p) {
      final nameMatch = p['name'].toString().toLowerCase().contains(_searchQuery.toLowerCase());
      final catMatch = _selectedCategoryId == null || p['category']['id'] == _selectedCategoryId;
      return nameMatch && catMatch;
    }).toList();

    return Scaffold(
      backgroundColor: const Color(0xFFF7F9F7),
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Smart Kirana Store',
              style: TextStyle(fontWeight: FontWeight.black, color: Color(0xFF064E3B)),
            ),
            Text(
              'Welcome, ${user?['name'] ?? 'Customer'}',
              style: const TextStyle(fontSize: 12, color: Color(0xFF64748B)),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout_rounded, color: Colors.redAccent),
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF059669)))
          : RefreshIndicator(
              onRefresh: _fetchStoreData,
              color: const Color(0xFF059669),
              child: Column(
                children: [
                  // Search & Category Filters
                  Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      children: [
                        TextField(
                          onChanged: (val) => setState(() => _searchQuery = val),
                          decoration: InputDecoration(
                            hintText: 'Search items, brand, category...',
                            prefixIcon: const Icon(Icons.search_rounded),
                            filled: true,
                            fillColor: Colors.white,
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(16),
                              borderSide: BorderSide.none,
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          height: 40,
                          child: ListView(
                            scrollDirection: Axis.horizontal,
                            children: [
                              ChoiceChip(
                                label: const Text('All'),
                                selected: _selectedCategoryId == null,
                                selectedColor: const Color(0xFFD1FAE5),
                                onSelected: (_) => setState(() => _selectedCategoryId = null),
                              ),
                              ..._categories.map((cat) {
                                return Padding(
                                  padding: const EdgeInsets.only(left: 8.0),
                                  child: ChoiceChip(
                                    label: Text(cat['name']),
                                    selected: _selectedCategoryId == cat['id'],
                                    selectedColor: const Color(0xFFD1FAE5),
                                    onSelected: (_) => setState(() => _selectedCategoryId = cat['id']),
                                  ),
                                );
                              }),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Catalog list
                  Expanded(
                    child: filteredProducts.isEmpty
                        ? const Center(child: Text('No products match criteria.'))
                        : GridView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              childAspectRatio: 0.78,
                              crossAxisSpacing: 12,
                              mainAxisSpacing: 12,
                            ),
                            itemCount: filteredProducts.length,
                            itemBuilder: (ctx, idx) {
                              final prod = filteredProducts[idx];
                              final pId = prod['id'] as int;
                              final inCartQty = _cart[pId] ?? 0;

                              return Card(
                                color: Colors.white,
                                elevation: 0,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(20),
                                  side: const BorderSide(color: Color(0xFFF1F5F9)),
                                ),
                                child: Padding(
                                  padding: const EdgeInsets.all(12.0),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      // Image Placeholder
                                      Container(
                                        height: 90,
                                        width: double.infinity,
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFF8FAFC),
                                          borderRadius: BorderRadius.circular(14),
                                        ),
                                        child: const Icon(Icons.shopping_basket_outlined, size: 36, color: Color(0xFF94A3B8)),
                                      ),
                                      const SizedBox(height: 8),
                                      Text(
                                        prod['name'],
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(fontWeight: FontWeight.bold),
                                      ),
                                      Text(
                                        prod['unit'] ?? '1 unit',
                                        style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8)),
                                      ),
                                      const Spacer(),
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text(
                                            '₹${prod['price']}',
                                            style: const TextStyle(
                                              fontWeight: FontWeight.black,
                                              fontSize: 16,
                                              color: Color(0xFF059669),
                                            ),
                                          ),
                                          if (inCartQty == 0)
                                            IconButton.filledTonal(
                                              onPressed: () => _addToCart(pId),
                                              style: IconButton.styleFrom(
                                                backgroundColor: const Color(0xFFD1FAE5),
                                              ),
                                              icon: const Icon(Icons.add_rounded, color: Color(0xFF064E3B)),
                                            )
                                          else
                                            Row(
                                              children: [
                                                IconButton(
                                                  visualDensity: VisualDensity.compact,
                                                  icon: const Icon(Icons.remove_circle_outline, color: Color(0xFF059669)),
                                                  onPressed: () => _removeFromCart(pId),
                                                ),
                                                Text('$inCartQty', style: const TextStyle(fontWeight: FontWeight.bold)),
                                                IconButton(
                                                  visualDensity: VisualDensity.compact,
                                                  icon: const Icon(Icons.add_circle_outline, color: Color(0xFF059669)),
                                                  onPressed: () => _addToCart(pId),
                                                ),
                                              ],
                                            ),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                  ),

                  // Bottom Order status ticker / checkout panel
                  if (_cart.isNotEmpty)
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: const BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.only(
                          topLeft: Radius.circular(32),
                          topRight: Radius.circular(32),
                        ),
                        boxShadow: [
                          BoxShadow(color: Colors.black12, blurRadius: 10),
                        ],
                      ),
                      child: SafeArea(
                        child: Row(
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Text('Cart Total', style: TextStyle(color: Color(0xFF94A3B8))),
                                Text(
                                  '₹${_getCartTotal().toStringAsFixed(2)}',
                                  style: const TextStyle(fontSize: 22, fontWeight: FontWeight.black, color: Color(0xFF064E3B)),
                                ),
                              ],
                            ),
                            const Spacer(),
                            SizedBox(
                              height: 50,
                              width: 160,
                              child: ElevatedButton(
                                onPressed: _checkout,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: const Color(0xFF059669),
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                ),
                                child: const Text('Checkout COD', style: TextStyle(fontWeight: FontWeight.bold)),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                  // Live Tracker active segment
                  if (_orders.isNotEmpty) ...[
                    Container(
                      color: Colors.emerald[50],
                      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.local_shipping, size: 18, color: Color(0xFF059669)),
                          const SizedBox(width: 8),
                          Text(
                            'Active Order #${_orders.first['id']} Status: ${_orders.first['status']}',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF064E3B)),
                          ),
                          const Spacer(),
                          TextButton(
                            onPressed: _fetchStoreData,
                            child: const Text('Update'),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}
