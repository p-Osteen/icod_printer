import 'dart:convert';
import 'dart:typed_data';

/// A builder class to generate ESC/POS byte commands for thermal printers.
///
/// Provides a high-level API for text alignment, styling, barcodes, and QR codes.
class EscPosBuilder {
  final List<int> _bytes = [];
  final String charset;
  final int paperSize;
  late final int _maxChars;

  /// Creates a new [EscPosBuilder] with a specific [charset] and [paperSize].
  ///
  /// [paperSize] can be 80 (default) or 58.
  EscPosBuilder({this.charset = 'GBK', this.paperSize = 80}) {
    _maxChars = paperSize == 80 ? 48 : 32;
    initialize();
  }

  void initialize() {
    _bytes.addAll([0x1B, 0x40]); // Reset the printer
    _bytes.addAll([0x1B, 0x32]); // Set default line spacing
    _bytes.addAll([0x1B, 0x21, 0x00]); // Reset font magnification variables
    setTextSize(sizeNormal);
    setFont(0);
  }

  /// Sets the font for subsequent text (0: Standard Font A, 1: Small Font B).
  void setFont(int font) {
    _bytes.addAll([0x1B, 0x4D, font]); // Apply font selection command
  }

  /// Sets the text alignment (0: Left, 1: Center, 2: Right).
  void setAlignment(int align) {
    _bytes.addAll([0x1B, 0x61, align]); // Apply alignment command
  }

  /// Sets the text growth size (normal, big, double height, double width).
  void setTextSize(int size) {
    _bytes.addAll([0x1D, 0x21, size]); // Set character size override
    // Also use legacy command for better compatibility
    int escSize = 0;
    if (size == sizeDoubleHeight) escSize = 0x10;
    if (size == sizeDoubleWidth) escSize = 0x20;
    if (size == sizeBig) escSize = 0x30;
    _bytes.addAll([0x1B, 0x21, escSize]);
  }

  /// Enables or disables bold text emphasis.
  void setBold(bool isBold) {
    _bytes.addAll([0x1B, 0x45, isBold ? 0x01 : 0x00]); // Toggle emphasized mode
  }

  /// Adds a string of text with optional styling.
  void text(String content, {int? align, int? size, bool? bold}) {
    if (align != null) setAlignment(align);
    if (size != null) setTextSize(size);
    if (bold != null) setBold(bold);
    _bytes.addAll(utf8.encode(content));
  }

  /// Adds a line of text (with newline) and optional styling.
  void line(String content, {int? align, int? size, bool? bold}) {
    text(content, align: align, size: size, bold: bold);
    feed();
  }

  /// Adds one or more empty lines (Line Feed).
  void feed([int lines = 1]) {
    for (var i = 0; i < lines; i++) {
      _bytes.add(0x0A); // LF
    }
  }

  /// Performs a partial cut of the paper.
  void cut() {
    feed(4);
    _bytes.addAll([0x1D, 0x56, 0x01]); // Partial paper cut command
  }

  /// Performs a full cut of the paper.
  void fullCut() {
    feed(4);
    _bytes.addAll([0x1D, 0x56, 0x00]); // Full paper cut command
  }

  void setDoubleHeight(bool enabled) {
    // GS ! n (bits 4-7 for width, 0-3 for height)
    // 0x01 = bit 0 (Double Height), 0x00 = Normal
    _bytes.addAll([0x1D, 0x21, enabled ? 0x01 : 0x00]);
  }

  void setDoubleWidth(bool enabled) {
    // GS ! n (bits 4-7 for width, 0-3 for height)
    // 0x10 = bit 4 (Double Width), 0x00 = Normal
    _bytes.addAll([0x1D, 0x21, enabled ? 0x10 : 0x00]);
  }

  /// Sets custom line spacing in dots.
  void setLineSpacing(int dots) {
    _bytes.addAll([0x1B, 0x33, dots]); // Apply variable line spacing
  }

  void resetLineSpacing() {
    // ESC 2
    _bytes.addAll([0x1B, 0x32]);
  }

  /// Adds a horizontal divider line using the specified [char].
  void divider([String char = '-']) {
    line(char * _maxChars);
  }

  /// Adds a QR code symbol with the specified [size] (1-16).
  void qrCode(String content, {int size = 8}) {
    final List<int> data = utf8.encode(content);
    final int pL = (data.length + 3) & 0xFF;
    final int pH = (data.length + 3) >> 8;

    // 1. Set QR Code Module Size
    _bytes.addAll([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size]);
    
    // 2. Set Error Correction Level (L)
    _bytes.addAll([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30]);
    
    // 3. Store Data in Symbol Storage Area
    _bytes.addAll([0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30]);
    _bytes.addAll(data);
    
    // 4. Print Symbol
    _bytes.addAll([0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30]);
  }

  /// Adds a barcode (e.g., CODE128) with the specified [type].
  void barcode(String content, {int type = 67}) {
    final List<int> data = utf8.encode(content);
    _bytes.addAll([0x1D, 0x6B, type, data.length]);
    _bytes.addAll(data);
  }

  /// Adds a table row with three columns and specified [widths].
  void row(String col1, String col2, String col3, {List<int> widths = const [30, 6, 12], bool bold = false}) {
    setBold(bold);
    final totalWidth = widths.reduce((a, b) => a + b);
    var targetLine = col1.padRight(widths[0]);
    targetLine += col2.padLeft(widths[1] ~/ 2).padRight(widths[1]);
    targetLine += col3.padLeft(widths[2]);
    final len = targetLine.length < totalWidth ? targetLine.length : totalWidth;
    final finalLen = len < _maxChars ? len : _maxChars;
    _bytes.addAll(utf8.encode(targetLine.substring(0, finalLen)));
    feed();
    if (bold) setBold(false);
  }

  /// Adds a pre-decoded ESC/POS image (raster bytes).
  void image(Uint8List rasterBytes) {
    _bytes.addAll(rasterBytes);
  }

  void underline(bool enabled) {
    _bytes.addAll([0x1B, 0x2D, enabled ? 0x01 : 0x00]);
  }

  void reverseColor(bool enabled) {
    _bytes.addAll([0x1D, 0x42, enabled ? 0x01 : 0x00]);
  }

  /// Finalizes the build and returns the accumulated ESC/POS bytes.
  Uint8List build() {
    return Uint8List.fromList(_bytes);
  }

  // Common Alignments
  static const int alignLeft = 0;
  static const int alignCenter = 1;
  static const int alignRight = 2;

  // Sizes
  static const int sizeNormal = 0x00;
  static const int sizeDoubleHeight = 0x01;
  static const int sizeDoubleWidth = 0x10;
  static const int sizeBig = 0x11;
}
