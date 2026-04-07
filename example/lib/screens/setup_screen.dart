import 'package:flutter/material.dart';
import 'package:icod_printer/icod_printer.dart';
import 'package:icod_printer/printer_builder.dart';
import 'package:permission_handler/permission_handler.dart';
import '../widgets/minimal_widgets.dart';

class SetupScreen extends StatefulWidget {
  final Function(PrinterConfig) onConfigSaved;
  const SetupScreen({super.key, required this.onConfigSaved});

  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> {
  TransportType _selectedType = TransportType.usb;
  List<PrinterConfig> _discoveredDevices = [];
  PrinterConfig? _selectedDevice;
  bool _isScanning = false;
  String _status = "Hardware Selection";

  @override
  void initState() {
    super.initState();
    _scan();
  }

  Future<void> _scan() async {
    setState(() {
      _isScanning = true;
      _discoveredDevices = [];
      _selectedDevice = null;
    });

    try {
      if (_selectedType == TransportType.usb) {
        final list = await Printer.discover(type: 'usb');
        setState(() => _discoveredDevices = list);
      } else if (_selectedType == TransportType.bluetooth) {
        await Permission.location.request();
        await Permission.bluetoothScan.request();
        await Permission.bluetoothConnect.request();
        final list = await Printer.discover(type: 'bluetooth');
        setState(() => _discoveredDevices = list);
      } else if (_selectedType == TransportType.serial) {
        final List<PrinterConfig> serials = [];
        for (int i = 0; i <= 4; i++) {
          serials.add(
            PrinterConfig(
              transportType: TransportType.serial,
              address: '/dev/ttyS$i',
              name: 'Serial Port S$i',
            ),
          );
        }
        setState(() => _discoveredDevices = serials);
      }
    } catch (e) {
      if (mounted) setState(() => _status = "Error: $e");
    } finally {
      if (mounted) setState(() => _isScanning = false);
    }
  }

  Future<void> _testPrint() async {
    if (_selectedDevice == null) return;
    try {
      final hasPermission = await Printer.requestPermission(_selectedDevice!);
      if (!hasPermission) throw "Permission Denied";

      await Printer.connect(_selectedDevice!);
      await Future.delayed(const Duration(milliseconds: 500));

      final builder = EscPosBuilder();
      builder.setAlignment(EscPosBuilder.alignCenter);
      builder.line("CONNECTION TEST SUCCESS", bold: true);
      builder.line("Device: ${_selectedDevice!.name}");
      builder.feed(4);
      builder.cut();

      await Printer.printBytes(_selectedDevice!, builder.build());
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text("Test Failed: $e")));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // Fixed Header
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 60, 24, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _status.toUpperCase(),
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 2,
                      color: Colors.black26,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    "Install Printer",
                    style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
                  ),
                ],
              ),
            ),

            // Minimal Segmented Control (Tabs)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: MinimalSegmentedControl<TransportType>(
                values: const [
                  TransportType.usb,
                  TransportType.bluetooth,
                  TransportType.serial,
                ],
                labels: const ["USB", "Bluetooth", "Serial"],
                selectedValue: _selectedType,
                onSelected: (val) {
                  setState(() => _selectedType = val);
                  _scan();
                },
              ),
            ),

            // Scanning Indicator
            if (_isScanning)
              const LinearProgressIndicator(
                minHeight: 2,
                backgroundColor: Colors.white,
                color: Colors.black,
              )
            else
              const SizedBox(height: 2),

            // Scrollable List
            Expanded(
              child: _discoveredDevices.isEmpty && !_isScanning
                  ? const Center(
                      child: Text(
                        "NO DEVICES FOUND",
                        style: TextStyle(
                          color: Colors.black12,
                          fontSize: 11,
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.only(top: 12),
                      itemCount: _discoveredDevices.length,
                      itemBuilder: (context, index) {
                        final d = _discoveredDevices[index];
                        final isSelected =
                            _selectedDevice?.address == d.address &&
                            _selectedDevice?.usbDeviceName == d.usbDeviceName;
                        return InkWell(
                          onTap: () => setState(() => _selectedDevice = d),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 24,
                              vertical: 24,
                            ),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? Colors.grey.shade50
                                  : Colors.transparent,
                              border: Border(
                                bottom: BorderSide(color: Colors.grey.shade100),
                              ),
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        d.name ?? "UNKNOWN DEVICE",
                                        style: TextStyle(
                                          fontWeight: isSelected
                                              ? FontWeight.bold
                                              : FontWeight.normal,
                                          fontSize: 16,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        d.address ?? "DEVICE INTERFACE",
                                        style: const TextStyle(
                                          color: Colors.black26,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                if (isSelected)
                                  const Icon(Icons.check, size: 18),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),

            // Fixed Bottom Buttons
            IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    child: MinimalButton(
                      label: "TEST PRINTER",
                      isPrimary: false,
                      onPressed: _selectedDevice != null ? _testPrint : null,
                    ),
                  ),
                  SizedBox(width: 4),
                  Container(width: 1, color: Colors.grey.shade200),
                  SizedBox(width: 4),
                  Expanded(
                    child: MinimalButton(
                      label: "SAVE CONFIG",
                      onPressed: _selectedDevice != null
                          ? () => widget.onConfigSaved(_selectedDevice!)
                          : null,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
