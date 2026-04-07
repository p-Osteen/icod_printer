import 'dart:async';

import 'package:flutter/services.dart';

import 'models.dart';
import 'printer_status.dart';

export 'models.dart';
export 'printer_status.dart';

/// The primary interface for interacting with thermal receipt printers.
///
/// This class provides static methods for discovering hardware, managing 
/// connections, and sending ESC/POS commands across various transport layers.
class Printer {
  static const MethodChannel _channel = MethodChannel(
    'com.diode.icod_printer/methods',
  );

  /// Scans for available printers over USB and Bluetooth interfaces.
  /// 
  /// The [type] parameter filters results: use 'usb', 'bluetooth', or 'all' (default).
  static Future<List<PrinterConfig>> discover({String type = 'all'}) async {
    final List<dynamic>? result = await _channel.invokeMethod('discover', {'type': type});
    if (result == null) return [];
    return result.map((e) => PrinterConfig.fromMap(e as Map)).toList();
  }

  /// Converts raw image bytes (PNG/JPG) into ESC/POS-compatible raster data.
  ///
  /// [bytes] contains the raw image source.
  /// [width] defines the target dot width (typically 384 for 58mm/80mm printers).
  static Future<Uint8List?> decodeImage(Uint8List bytes, {int width = 384}) async {
    final Uint8List? result = await _channel.invokeMethod('decodeImage', {
      'bytes': bytes,
      'width': width,
    });
    return result;
  }

  /// Establishes a communication channel with the specified printer.
  static Future<bool> connect(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'connect',
      config.toMap(),
    );
    return result;
  }

  /// Prompts the user for hardware permissions (required for USB on Android).
  static Future<bool> requestPermission(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'requestPermission',
      config.toMap(),
    );
    return result;
  }

  /// Closes the active connection to the printer.
  static Future<bool> disconnect(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'disconnect',
      config.toMap(),
    );
    return result;
  }

  /// Queries the printer for real-time status (Paper Out, Cover Open, etc.).
  static Future<PrinterStatus> getStatus(PrinterConfig config) async {
    final List<dynamic>? results = await _channel.invokeMethod<List<dynamic>>('getStatus', config.toMap());
    if (results == null || results.length < 4) {
      throw Exception('Failed to get printer status');
    }
    return PrinterStatus.fromBytes(
      results[0] as int,
      results[1] as int,
      results[2] as int,
      results[3] as int,
    );
  }

  /// Transmits raw ESC/POS byte commands directly to the printer hardware.
  static Future<bool> printBytes(PrinterConfig config, Uint8List data) async {
    final bool result = await _channel.invokeMethod('print', {
      'config': config.toMap(),
      'data': data,
    });
    return result;
  }
}
