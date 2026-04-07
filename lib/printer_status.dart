/// Represents the current physical paper state of the printer.
enum PaperStatus {
  ok,
  nearEnd,
  out,
  unknown;

  static PaperStatus fromByte(int byte) {
    // Bits 5 and 6 indicate absolute paper depletion
    if ((byte & 0x60) == 0x60) {
      return PaperStatus.out;
    }
    // Bits 2 and 3 indicate that the paper roll is almost empty
    if ((byte & 0x0C) == 0x0C) {
      return PaperStatus.nearEnd;
    }
    return PaperStatus.ok;
  }
}

/// Represents various hardware error states.
enum PrinterError {
  none,
  autocutterError,
  unrecoverableError,
  overheating,
  unknown;

  static PrinterError fromByte(int byte) {
    if ((byte & 0x40) == 0x40) {
      return PrinterError.unrecoverableError;
    }
    if ((byte & 0x08) == 0x08) {
      return PrinterError.autocutterError;
    }
    if ((byte & 0x10) == 0x10) {
      return PrinterError.overheating;
    }
    return PrinterError.none;
  }
}

/// Unified status report for a thermal printer, combining paper, error, and online states.
class PrinterStatus {
  final PaperStatus paper;
  final PrinterError error;
  final bool isOnline;
  final bool isCoverOpen;

  PrinterStatus({
    required this.paper,
    required this.error,
    required this.isOnline,
    required this.isCoverOpen,
  });

  /// True if the printer is online, has paper, the cover is closed, and no errors exist.
  bool get isHealthy => isOnline && paper == PaperStatus.ok && error == PrinterError.none && !isCoverOpen;

  /// True if the paper roll is completely empty.
  bool get isPaperOut => paper == PaperStatus.out;

  /// Constructs a [PrinterStatus] report from raw hardware response bytes.
  factory PrinterStatus.fromBytes(int pStatus, int offStatus, int errStatus, int pSensorStatus) {
    return PrinterStatus(
      isOnline: (pStatus & 0x08) == 0x00, // Status bit 3 (Low = Online)
      isCoverOpen: (pStatus & 0x04) == 0x04, // Status bit 2 (High = Open)
      error: PrinterError.fromByte(errStatus),
      paper: PaperStatus.fromByte(pSensorStatus),
    );
  }

  @override
  String toString() {
    return 'PrinterStatus(paper: ${paper.name}, error: ${error.name}, online: $isOnline)';
  }
}
