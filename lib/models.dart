import 'dart:convert';

/// Supported communication channels for the printer.
enum TransportType {
  usb,
  bluetooth,
  tcp,
  serial
}

/// Hardware configuration for a specific thermal printer.
class PrinterConfig {
  final TransportType transportType;
  final String? name;
  final String? address;
  final int port;
  final int baudRate;
  final String? usbDeviceName;
  final int paperSize;

  PrinterConfig({
    required this.transportType,
    this.name,
    this.address,
    this.port = 9100,
    this.baudRate = 9600,
    this.usbDeviceName,
    this.paperSize = 80,
  });

  /// Serializes the configuration into a map for platform-level communication or local persistence.
  Map<String, dynamic> toMap() {
    return {
      'transport': transportType.name,
      'name': name,
      'address': address,
      'port': port,
      'baudRate': baudRate,
      'usbDeviceName': usbDeviceName,
      'paperSize': paperSize,
    };
  }

  /// Reconstructs a [PrinterConfig] from a serialized map.
  factory PrinterConfig.fromMap(Map<dynamic, dynamic> map) {
    return PrinterConfig(
      transportType: TransportType.values.firstWhere(
        (e) => e.name == map['transport'],
        orElse: () => TransportType.usb,
      ),
      name: map['name'] as String?,
      address: map['address'] as String?,
      port: map['port'] as int? ?? 9100,
      baudRate: map['baudRate'] as int? ?? 9600,
      usbDeviceName: map['usbDeviceName'] as String?,
      paperSize: map['paperSize'] as int? ?? 80,
    );
  }

  String toJson() => jsonEncode(toMap());
  factory PrinterConfig.fromJson(String source) => PrinterConfig.fromMap(jsonDecode(source));

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PrinterConfig &&
          runtimeType == other.runtimeType &&
          address == other.address &&
          transportType == other.transportType &&
          usbDeviceName == other.usbDeviceName;

  @override
  int get hashCode => address.hashCode ^ transportType.hashCode ^ usbDeviceName.hashCode;
}
