import 'package:flutter/material';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'providers/auth_provider.dart';
import 'screens/auth_screen.dart';
import 'screens/customer_dashboard.dart';
import 'screens/owner_dashboard.dart';
import 'screens/delivery_dashboard.dart';

void main() {
  runApp(
    const ProviderScope(
      child: SmartKiranaApp(),
    ),
  );
}

class SmartKiranaApp extends ConsumerWidget {
  const SmartKiranaApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

    return MaterialApp(
      title: 'Smart Kirana Store',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF059669),
          primary: const Color(0xFF059669),
          secondary: const Color(0xFF10B981),
          background: const Color(0xFFF8FAFC),
        ),
        textTheme: GoogleFonts.plusJakartaSansTextTheme(
          Theme.of(context).textTheme,
        ),
      ),
      home: authState.isLoading
          ? const Scaffold(
              body: Center(
                child: CircularProgressIndicator(color: Color(0xFF059669)),
              ),
            )
          : authState.isAuthenticated
              ? _getRoleSpecificDashboard(authState.userData)
              : const AuthScreen(),
    );
  }

  Widget _getRoleSpecificDashboard(Map<String, dynamic>? userData) {
    if (userData == null || !userData.containsKey('roles')) {
      return const AuthScreen();
    }

    final List<dynamic> roles = userData['roles'];
    if (roles.contains('ROLE_OWNER')) {
      return const OwnerDashboard();
    } else if (roles.contains('ROLE_DELIVERY')) {
      return const DeliveryDashboard();
    } else {
      return const CustomerDashboard();
    }
  }
}
