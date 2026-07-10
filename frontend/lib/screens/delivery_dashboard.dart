import 'package:flutter/material';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';

class DeliveryDashboard extends ConsumerStatefulWidget {
  const DeliveryDashboard({super.key});

  @override
  ConsumerState<DeliveryDashboard> createState() => _DeliveryDashboardState();
}

class _DeliveryDashboardState extends ConsumerState<DeliveryDashboard> {
  bool _isLoading = true;
  List<dynamic> _myShipments = [];
  List<dynamic> _availablePool = [];
  double _myEarnings = 0.0;

  final _otpController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _fetchDeliveries();
  }

  Future<void> _fetchDeliveries() async {
    setState(() => _isLoading = true);
    final api = ref.read(apiServiceProvider);
    try {
      final mineResp = await api.getOrders();
      final poolResp = await api.getUnassignedOrders();

      if (mounted) {
        setState(() {
          _myShipments = mineResp.data;
          _availablePool = poolResp.data;

          // Calculate earnings: mock ₹40 per successful delivery
          _myEarnings = 0.0;
          for (var ord in _myShipments) {
            if (ord['status'] == 'DELIVERED') {
              _myEarnings += 40.0;
            }
          }
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _acceptOrder(int orderId) async {
    final api = ref.read(apiServiceProvider);
    final rider = ref.read(authProvider).userData;
    try {
      await api.assignRider(orderId, rider?['id'] ?? 1);
      _fetchDeliveries();
    } catch (e) {
      // ignore
    }
  }

  Future<void> _completeDelivery(int orderId) async {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Verify Delivery OTP'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Enter the 4-digit code provided by the customer to finish transaction:'),
            const SizedBox(height: 12),
            TextField(
              controller: _otpController,
              keyboardType: TextInputType.number,
              maxLength: 4,
              decoration: const InputDecoration(
                hintText: 'e.g. 1234',
                filled: true,
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              final api = ref.read(apiServiceProvider);
              try {
                final resp = await api.verifyDeliveryOtp(orderId, _otpController.text.trim());
                Navigator.pop(ctx);
                _otpController.clear();
                if (resp.statusCode == 200) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Verification successful! Payment ledger updated.'), backgroundColor: Colors.emerald),
                  );
                }
                _fetchDeliveries();
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Invalid OTP code. Delivery validation failed!'), backgroundColor: Colors.red),
                );
              }
            },
            child: const Text('Verify Delivery'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F9F7),
      appBar: AppBar(
        title: const Text('Rider Delivery Hub', style: TextStyle(fontWeight: FontWeight.black, color: Color(0xFF064E3B))),
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
              onRefresh: _fetchDeliveries,
              color: const Color(0xFF059669),
              child: ListView(
                padding: const EdgeInsets.all(16.0),
                children: [
                  // Earnings Summary card
                  Card(
                    color: const Color(0xFF064E3B),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                    elevation: 0,
                    child: Padding(
                      padding: const EdgeInsets.all(24.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Your Total Earnings', style: TextStyle(color: Color(0xFFA7F3D0), fontSize: 13, fontWeight: FontWeight.bold)),
                          const SizedBox(height: 6),
                          Text(
                            '₹${_myEarnings.toStringAsFixed(2)}',
                            style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.black),
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'Priced at ₹40 incentive per successful grocery delivery',
                            style: TextStyle(color: Color(0xFFA7F3D0), fontSize: 11),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Active Trips
                  const Text('Your Active Shipments', style: TextStyle(fontWeight: FontWeight.black, fontSize: 16, color: Color(0xFF064E3B))),
                  const SizedBox(height: 12),
                  if (_myShipments.where((o) => o['status'] != 'DELIVERED').isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Center(child: Text('No active deliveries assigned yet.', style: TextStyle(color: Colors.grey))),
                    )
                  else
                    ..._myShipments.where((o) => o['status'] != 'DELIVERED').map((ord) {
                      return Card(
                        color: Colors.white,
                        margin: const EdgeInsets.only(bottom: 12),
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('Order ID #${ord['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
                                  Chip(label: Text(ord['status']), backgroundColor: Colors.orange[50]),
                                ],
                              ),
                              Text('Address: ${ord['addressText']}'),
                              Text('Payment details: ₹${ord['payableAmount']} (${ord['paymentMethod']})'),
                              const Divider(height: 24),
                              SizedBox(
                                width: double.infinity,
                                height: 48,
                                child: ElevatedButton.icon(
                                  onPressed: () => _completeDelivery(ord['id']),
                                  icon: const Icon(Icons.qr_code_scanner_rounded),
                                  label: const Text('Complete Order (Enter OTP)'),
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFF059669),
                                    foregroundColor: Colors.white,
                                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),

                  const SizedBox(height: 20),

                  // Open Pool Jobs
                  const Text('Available Nearby Jobs Pool', style: TextStyle(fontWeight: FontWeight.black, fontSize: 16, color: Color(0xFF064E3B))),
                  const SizedBox(height: 12),
                  if (_availablePool.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Center(child: Text('No new jobs available. Check back soon!', style: TextStyle(color: Colors.grey))),
                    )
                  else
                    ..._availablePool.map((ord) {
                      return Card(
                        color: Colors.white,
                        margin: const EdgeInsets.only(bottom: 12),
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('Order #${ord['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
                                  const Chip(label: Text('Ready for Pick'), backgroundColor: Color(0xFFD1FAE5)),
                                ],
                              ),
                              Text('Delivery Point: ${ord['addressText']}'),
                              const SizedBox(height: 12),
                              SizedBox(
                                width: double.infinity,
                                height: 44,
                                child: OutlinedButton(
                                  onPressed: () => _acceptOrder(ord['id']),
                                  style: OutlinedButton.styleFrom(
                                    side: const BorderSide(color: Color(0xFF059669)),
                                    foregroundColor: const Color(0xFF059669),
                                  ),
                                  child: const Text('Accept Delivery Job'),
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }
}
