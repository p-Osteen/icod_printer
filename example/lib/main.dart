import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:icod_printer/icod_printer.dart';
import 'screens/setup_screen.dart';
import 'screens/dashboard_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'ICOD PRINTER SDK',
      theme: ThemeData(
        fontFamily: 'Roboto',
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.black, brightness: Brightness.light),
        useMaterial3: true,
      ),
      home: const MainGate(),
    );
  }
}

class MainGate extends StatefulWidget {
  const MainGate({super.key});

  @override
  State<MainGate> createState() => _MainGateState();
}

class _MainGateState extends State<MainGate> {
  PrinterConfig? _savedConfig;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSavedConfig();
  }

  Future<void> _loadSavedConfig() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString('printer_config');
    if (json != null) {
      setState(() {
        _savedConfig = PrinterConfig.fromJson(json);
      });
    }
    setState(() => _isLoading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) return const Scaffold(backgroundColor: Colors.white, body: Center(child: CircularProgressIndicator(color: Colors.black, strokeWidth: 2)));
    
    if (_savedConfig != null) {
      return DashboardScreen(
        config: _savedConfig!,
        onReset: () async {
          final prefs = await SharedPreferences.getInstance();
          await prefs.remove('printer_config');
          setState(() => _savedConfig = null);
        },
      );
    }
    
    return SetupScreen(
      onConfigSaved: (config) {
        setState(() => _savedConfig = config);
      },
    );
  }
}
