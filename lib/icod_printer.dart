import 'dart:async';

import 'package:flutter/services.dart';

import 'models.dart';
import 'printer_status.dart';

export 'models.dart';
export 'printer_status.dart';

/// Main entry point for interacting with thermal receipt printers.
///
/// Supports discovery, connection management, and printing of raw ESC/POS bytes.
class Printer {
  static const MethodChannel _channel = MethodChannel(
    'com.diode.icod_printer/methods',
  );

  /// Discovers available printers (USB and Bluetooth).
  /// [type] can be 'usb', 'bluetooth', or 'all'.
  static Future<List<PrinterConfig>> discover({String type = 'all'}) async {
    final List<dynamic>? result = await _channel.invokeMethod('discover', {'type': type});
    if (result == null) return [];
    return result.map((e) => PrinterConfig.fromMap(e as Map)).toList();
  }

  /// Decodes image bytes into ESC/POS raster format.
  ///
  /// [bytes] should be the raw image data (PNG/JPG).
  /// [width] is the target width in dots (default is 384 for 58mm/80mm printers).
  static Future<Uint8List?> decodeImage(Uint8List bytes, {int width = 384}) async {
    final Uint8List? result = await _channel.invokeMethod('decodeImage', {
      'bytes': bytes,
      'width': width,
    });
    return result;
  }

  /// Connects to a printer using the provided configuration.
  static Future<bool> connect(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'connect',
      config.toMap(),
    );
    return result;
  }

  /// Requests permission to use the printer.
  static Future<bool> requestPermission(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'requestPermission',
      config.toMap(),
    );
    return result;
  }

  /// Disconnects from the printer.
  static Future<bool> disconnect(PrinterConfig config) async {
    final bool result = await _channel.invokeMethod(
      'disconnect',
      config.toMap(),
    );
    return result;
  }

  /// Retrieves the current status (online, paper, cover, error) from the printer.
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

  /// Sends raw ESC/POS bytes to the printer.
  static Future<bool> printBytes(PrinterConfig config, Uint8List data) async {
    final bool result = await _channel.invokeMethod('print', {
      'config': config.toMap(),
      'data': data,
    });
    return result;
  }
}
