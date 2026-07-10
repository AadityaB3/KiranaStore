import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../services/api_service.dart';

final apiServiceProvider = Provider((ref) => ApiService());

class AuthState {
  final bool isAuthenticated;
  final bool isLoading;
  final String? errorMessage;
  final Map<String, dynamic>? userData;

  AuthState({
    this.isAuthenticated = false,
    this.isLoading = false,
    this.errorMessage,
    this.userData,
  });

  AuthState copyWith({
    bool? isAuthenticated,
    bool? isLoading,
    String? errorMessage,
    Map<String, dynamic>? userData,
  }) {
    return AuthState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage ?? this.errorMessage,
      userData: userData ?? this.userData,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final ApiService _apiService;
  final _secureStorage = const FlutterSecureStorage();

  AuthNotifier(this._apiService) : super(AuthState()) {
    _tryAutoLogin();
  }

  Future<void> _tryAutoLogin() async {
    state = state.copyWith(isLoading: true);
    final savedUser = await _secureStorage.read(key: 'user_data');
    final savedToken = await _secureStorage.read(key: 'jwt_token');

    if (savedUser != null && savedToken != null) {
      state = AuthState(
        isAuthenticated: true,
        isLoading: false,
        userData: json.decode(savedUser),
      );
    } else {
      state = AuthState(isLoading: false);
    }
  }

  Future<bool> login(String usernameOrEmail, String password) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final response = await _apiService.login(usernameOrEmail, password);
      if (response.statusCode == 200) {
        final data = response.data;
        await _secureStorage.write(key: 'jwt_token', value: data['token']);
        await _secureStorage.write(key: 'user_data', value: json.encode(data));

        state = AuthState(
          isAuthenticated: true,
          isLoading: false,
          userData: data,
        );
        return true;
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: "Authentication failed. Double check your username and credentials.",
      );
    }
    return false;
  }

  Future<bool> register({
    required String username,
    required String email,
    required String password,
    required String name,
    required String phone,
    required String role,
    String? vehicleNumber,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final response = await _apiService.register(
        username: username,
        email: email,
        password: password,
        name: name,
        phone: phone,
        role: role,
        vehicleNumber: vehicleNumber,
      );
      if (response.statusCode == 200) {
        state = state.copyWith(isLoading: false);
        return true;
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: "Registration failed. Username/Email might be taken.",
      );
    }
    return false;
  }

  Future<void> logout() async {
    await _secureStorage.delete(key: 'jwt_token');
    await _secureStorage.delete(key: 'user_data');
    state = AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  final api = ref.watch(apiServiceProvider);
  return AuthNotifier(api);
});
