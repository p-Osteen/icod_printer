import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:icod_printer/icod_printer.dart';
import 'package:icod_printer/printer_builder.dart';
import '../widgets/minimal_widgets.dart';

class DashboardScreen extends StatefulWidget {
  final PrinterConfig config;
  final VoidCallback onReset;

  const DashboardScreen({
    super.key,
    required this.config,
    required this.onReset,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  PrinterStatus? _currentStatus;
  bool _isConnecting = false;

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  Future<void> _checkStatus() async {
    try {
      final status = await Printer.getStatus(widget.config);
      setState(() => _currentStatus = status);
    } catch (e) {
      if (mounted) setState(() => _currentStatus = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: Text(widget.config.name?.toUpperCase() ?? "PRINTER", style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w900, letterSpacing: 2, color: Colors.black)),
        actions: [
          IconButton(icon: const Icon(Icons.refresh, color: Colors.black), onPressed: _checkStatus),
          IconButton(icon: const Icon(Icons.settings_outlined, color: Colors.black), onPressed: widget.onReset),
        ],
      ),
      body: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: _buildStatusIndicator()),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                const SectionHeader("Kitchen Sink"),
                FeatureTile(
                  title: "Monster Receipt",
                  subtitle: "Print a comprehensive sample showcasing all features",
                  icon: Icons.receipt_long,
                  onTap: _isConnecting ? null : () => _printKitchenSink(context),
                ),
                
                const SectionHeader("Typography"),
                FeatureTile(
                  title: "Bold & Stylized",
                  subtitle: "Tests bold, underline, and color reversal",
                  icon: Icons.text_fields,
                  onTap: () => _printSample(EscPosBuilder()..setBold(true)..line("BOLD TEXT")..underline(true)..line("UNDERLINED")..reverseColor(true)..line(" REVERSED COLOR ")),
                ),
                FeatureTile(
                  title: "Dynamic Sizing",
                  subtitle: "Tests normal, double-height, and large fonts",
                  icon: Icons.format_size,
                  onTap: () => _printSample(EscPosBuilder()..setTextSize(EscPosBuilder.sizeNormal)..line("NORMAL")..setTextSize(EscPosBuilder.sizeDoubleHeight)..line("HEIGHT")..setTextSize(EscPosBuilder.sizeBig)..line("LARGE")),
                ),

                const SectionHeader("Interactive"),
                FeatureTile(
                  title: "QR Code",
                  subtitle: "Generate a scanable 2D symbol",
                  icon: Icons.qr_code_2,
                  onTap: () => _printSample(EscPosBuilder()..setAlignment(EscPosBuilder.alignCenter)..qrCode("https://diode.com")),
                ),
                FeatureTile(
                  title: "Barcode (CODE128)",
                  subtitle: "Generate a standard 1D barcode",
                  icon: Icons.barcode_reader,
                  onTap: () => _printSample(EscPosBuilder()..setAlignment(EscPosBuilder.alignCenter)..barcode("DIODE-123")),
                ),
                
                const SectionHeader("Maintenance"),
                FeatureTile(
                  title: "Partial Cut",
                  subtitle: "Verify the auto-cutter mechanism",
                  icon: Icons.cut,
                  onTap: () => _printSample(EscPosBuilder()..cut()),
                ),
                const SizedBox(height: 64),
              ]),
            ),
          )
        ],
      ),
    );
  }

  Widget _buildStatusIndicator() {
    Color statusColor = Colors.grey.shade300;
    String status = "PRINTER STATE UNKNOWN";
    
    if (_currentStatus != null) {
      if (_currentStatus!.isHealthy) {
        statusColor = Colors.green.shade400;
        status = "DEVICE READY";
      } else if (_currentStatus!.isPaperOut) {
        statusColor = Colors.red.shade400;
        status = "PAPER OUT";
      } else if (_currentStatus!.isCoverOpen) {
        statusColor = Colors.orange.shade400;
        status = "COVER OPEN";
      }
    }

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 24),
      decoration: BoxDecoration(
        color: statusColor.withValues(alpha: 0.15),
      ),
      child: Row(
        children: [
          Container(width: 8, height: 8, decoration: BoxDecoration(color: statusColor, shape: BoxShape.circle)),
          const SizedBox(width: 12),
          Text(status, style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.w900, letterSpacing: 1.5)),
        ],
      ),
    );
  }

  Future<void> _printSample(EscPosBuilder builder) async {
    try {
      await Printer.requestPermission(widget.config);
      await Printer.connect(widget.config);
      builder.feed(1);
      await Printer.printBytes(widget.config, builder.build());
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Error: $e")));
    }
  }

  Future<void> _printKitchenSink(BuildContext context) async {
    setState(() => _isConnecting = true);
    try {
      final hasPermission = await Printer.requestPermission(widget.config);
      if (!hasPermission) throw "Permission Denied";
      
      await Printer.connect(widget.config);
      await Future.delayed(const Duration(milliseconds: 500));

      final builder = EscPosBuilder();
      
      // Header
      builder.setAlignment(EscPosBuilder.alignCenter);
      try {
        final ByteData imgData = await rootBundle.load('assets/logo.png');
        final Uint8List imgBytes = imgData.buffer.asUint8List();
        final decoded = await Printer.decodeImage(imgBytes, width: 384);
        if (decoded != null) builder.image(decoded);
      } catch (_) {
        builder.line("DIODE SDK", size: EscPosBuilder.sizeBig, bold: true);
      }
      
      builder.line("Minimalist UI Demo", bold: true);
      builder.divider();

      // Table
      builder.setAlignment(EscPosBuilder.alignLeft);
      builder.row("Product", "Qty", "Price", bold: true);
      builder.row("Flat Burger", "2", "3.00");
      builder.row("Static Fries", "1", "1.20");
      
      builder.divider();
      builder.setAlignment(EscPosBuilder.alignCenter);
      builder.barcode("MONSTER-123");
      builder.feed(2);
      builder.cut();
      
      await Printer.printBytes(widget.config, builder.build());
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Print Error: $e")));
    } finally {
      if (mounted) setState(() => _isConnecting = false);
    }
  }
}
