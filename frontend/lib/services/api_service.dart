import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiService {
  final Dio dio = Dio(BaseOptions(
    baseUrl: 'https://ais-dev-k4bmuanpf2fobjo4j7suaa-643432601175.asia-east1.run.app/api/v1',
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 15),
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
  ));

  final _secureStorage = const FlutterSecureStorage();

  ApiService() {
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await _secureStorage.read(key: 'jwt_token');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (e, handler) {
        // Intercept global connection failures nicely
        return handler.next(e);
      },
    ));
  }

  // --- Auth API ---
  Future<Response> login(String usernameOrEmail, String password) async {
    return dio.post('/auth/login', data: {
      'usernameOrEmail': usernameOrEmail,
      'password': password,
    });
  }

  Future<Response> register({
    required String username,
    required String email,
    required String password,
    required String name,
    required String phone,
    required String role,
    String? vehicleNumber,
  }) async {
    return dio.post('/auth/register', data: {
      'username': username,
      'email': email,
      'password': password,
      'name': name,
      'phone': phone,
      'role': role,
      'vehicleNumber': vehicleNumber,
    });
  }

  // --- Products Catalog API ---
  Future<Response> getProducts() async {
    return dio.get('/products');
  }

  Future<Response> getAdminProducts() async {
    return dio.get('/products/admin-all');
  }

  Future<Response> getCategories() async {
    return dio.get('/products/categories');
  }

  Future<Response> searchProducts(String query) async {
    return dio.get('/products/search', queryParameters: {'query': query});
  }

  Future<Response> createProduct(Map<String, dynamic> productData) async {
    return dio.post('/products', data: productData);
  }

  Future<Response> toggleProductActive(int id, bool isActive) async {
    return dio.put('/products/$id/active', data: {'isActive': isActive});
  }

  // --- Orders API ---
  Future<Response> placeOrder({
    required int addressId,
    required String paymentMethod,
    required List<Map<String, dynamic>> items,
  }) async {
    return dio.post('/orders', data: {
      'addressId': addressId,
      'paymentMethod': paymentMethod,
      'items': items,
    });
  }

  Future<Response> getOrders() async {
    return dio.get('/orders');
  }

  Future<Response> getUnassignedOrders() async {
    return dio.get('/orders/unassigned');
  }

  Future<Response> assignRider(int orderId, int riderId) async {
    return dio.put('/orders/$orderId/assign', data: {'riderId': riderId});
  }

  Future<Response> updateOrderStatus(int orderId, String status) async {
    return dio.put('/orders/$orderId/status', data: {'status': status});
  }

  Future<Response> verifyDeliveryOtp(int orderId, String otp) async {
    return dio.post('/orders/$orderId/verify-otp', data: {'otp': otp});
  }

  // --- Store Operations API ---
  Future<Response> getStoreSettings() async {
    return dio.get('/store/settings');
  }

  Future<Response> updateStoreSettings({bool? isOpen, String? announcement}) async {
    final Map<String, dynamic> data = {};
    if (isOpen != null) data['isOpen'] = isOpen;
    if (announcement != null) data['storeAnnouncement'] = announcement;
    return dio.put('/store/settings', data: data);
  }
}
